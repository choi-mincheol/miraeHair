package com.mirae.hair.domain.order.service;

import com.mirae.hair.domain.customer.command.CustomerRepository;
import com.mirae.hair.domain.customer.domain.Customer;
import com.mirae.hair.domain.order.command.OrderRepository;
import com.mirae.hair.domain.order.domain.Order;
import com.mirae.hair.domain.order.domain.OrderItem;
import com.mirae.hair.domain.order.dto.*;
import com.mirae.hair.domain.order.query.OrderQueryMapper;
import com.mirae.hair.domain.product.command.ProductOptionRepository;
import com.mirae.hair.domain.product.domain.Product;
import com.mirae.hair.domain.product.domain.ProductOption;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 서비스 (Command + Query 통합)
 *
 * 핵심 비즈니스 로직:
 * 1) 주문 등록: 가격 Snapshot 생성 + 재고 차감 (하나의 트랜잭션)
 * 2) 주문 취소: 상태 변경 + 재고 복원 (하나의 트랜잭션)
 * 3) 주문 조회: MyBatis로 목록/상세 조회
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductOptionRepository productOptionRepository;
    private final OrderQueryMapper orderQueryMapper;

    // ─────────────────────────────────────────
    // Command (등록/취소) — JPA
    // ─────────────────────────────────────────

    /**
     * 주문 등록
     *
     * 처리 흐름:
     * 1) 고객 존재 확인
     * 2) Order 생성 (CONFIRMED 상태)
     * 3) 각 주문항목:
     *    → ProductOption 조회
     *    → Product 참조 (옵션에서 product를 가져옴)
     *    → 재고 차감 (option.decreaseStock)
     *    → OrderItem 생성 (가격 Snapshot)
     * 4) 총 금액 계산
     * 5) 저장 (CascadeType.ALL로 OrderItem도 함께)
     *
     * 왜 가격을 클라이언트에서 받지 않고 서버에서 조회하는가?
     * → 클라이언트가 보내는 가격을 그대로 저장하면, 가격 조작이 가능하다.
     * → "가격은 항상 서버에서 조회"하는 것이 보안의 기본 원칙이다.
     */
    public Long createOrder(OrderCreateRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        Order order = Order.create(customer, request.getMemo());

        for (OrderItemRequest itemRequest : request.getItems()) {
            ProductOption option = productOptionRepository
                    .findByIdAndDeletedFalse(itemRequest.getProductOptionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

            Product product = option.getProduct();
            option.decreaseStock(itemRequest.getQuantity());

            OrderItem orderItem = OrderItem.createSnapshot(
                    order, product, option, itemRequest.getQuantity()
            );
            order.addItem(orderItem);
        }

        order.calculateTotalAmount();

        Order saved = orderRepository.save(order);
        return saved.getId();
    }

    /**
     * 주문 취소
     *
     * 처리 흐름:
     * 1) 주문 + 항목 조회 (JOIN FETCH로 N+1 방지)
     * 2) 주문 상태 → CANCELLED (이미 취소된 주문이면 예외)
     * 3) 각 항목의 재고 복원 (ProductOption.increaseStock)
     */
    public void cancelOrder(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        order.cancel();

        for (OrderItem item : order.getItems()) {
            ProductOption option = productOptionRepository
                    .findByIdAndDeletedFalse(item.getProductOptionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

            option.increaseStock(item.getQuantity());
        }
    }

    // ─────────────────────────────────────────
    // Query (조회) — MyBatis
    // ─────────────────────────────────────────

    /**
     * 주문 목록 조회 (페이징 + 고객 필터 + 기간 필터)
     */
    @Transactional(readOnly = true)
    public Page<OrderListDto> getOrderList(Long customerId, LocalDate startDate,
                                           LocalDate endDate, Pageable pageable) {
        long offset = pageable.getOffset();
        int size = pageable.getPageSize();

        List<OrderListDto> orders = orderQueryMapper.selectOrderList(
                customerId, startDate, endDate, offset, size);
        long totalCount = orderQueryMapper.countOrders(customerId, startDate, endDate);

        return new PageImpl<>(orders, pageable, totalCount);
    }

    /**
     * 주문 상세 조회 (주문항목 포함)
     */
    @Transactional(readOnly = true)
    public OrderDetailDto getOrderDetail(Long id) {
        OrderDetailDto order = orderQueryMapper.selectOrderDetail(id);

        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        return order;
    }
}
