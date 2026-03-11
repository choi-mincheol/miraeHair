package com.mirae.hair.domain.order.command;

import com.mirae.hair.domain.customer.command.CustomerRepository;
import com.mirae.hair.domain.customer.domain.Customer;
import com.mirae.hair.domain.order.domain.Order;
import com.mirae.hair.domain.order.domain.OrderItem;
import com.mirae.hair.domain.order.dto.OrderCreateRequest;
import com.mirae.hair.domain.order.dto.OrderItemRequest;
import com.mirae.hair.domain.product.command.ProductOptionRepository;
import com.mirae.hair.domain.product.domain.Product;
import com.mirae.hair.domain.product.domain.ProductOption;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 주문 Command 서비스 (등록/취소)
 *
 * 이 서비스가 처리하는 핵심 비즈니스 로직:
 * 1) 주문 등록: 가격 Snapshot 생성 + 재고 차감 (하나의 트랜잭션)
 * 2) 주문 취소: 상태 변경 + 재고 복원 (하나의 트랜잭션)
 *
 * 왜 주문 등록이 복잡한가?
 * → 단순히 "데이터를 저장"하는 게 아니라, 여러 도메인이 협력하는 로직이다.
 * → Customer 조회 → ProductOption 조회 → 재고 검증/차감 → 가격 Snapshot → 저장
 * → 이 모든 과정이 하나의 트랜잭션으로 묶여야 한다 (하나라도 실패하면 전체 롤백).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class OrderCommandService {

    private final OrderRepository orderRepository;
    private final CustomerRepository customerRepository;
    private final ProductOptionRepository productOptionRepository;

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
     * → 예: 실제 가격 45,000원인데 클라이언트가 1,000원으로 보내면?
     * → "가격은 항상 서버에서 조회"하는 것이 보안의 기본 원칙이다.
     *
     * @param request 주문 등록 요청
     * @return 생성된 주문 ID
     */
    public Long createOrder(OrderCreateRequest request) {
        // 1. 고객 조회
        Customer customer = customerRepository.findByIdAndDeletedFalse(request.getCustomerId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 2. 주문 생성
        Order order = Order.create(customer, request.getMemo());

        // 3. 각 주문항목 처리
        for (OrderItemRequest itemRequest : request.getItems()) {
            // 3-1. 옵션 조회
            ProductOption option = productOptionRepository
                    .findByIdAndDeletedFalse(itemRequest.getProductOptionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

            // 3-2. 옵션에서 상품 참조 (LAZY 로딩으로 Product 조회)
            Product product = option.getProduct();

            // 3-3. 재고 차감 (부족하면 예외 → 트랜잭션 롤백)
            option.decreaseStock(itemRequest.getQuantity());

            // 3-4. OrderItem 생성 (★ 가격 Snapshot ★)
            OrderItem orderItem = OrderItem.createSnapshot(
                    order, product, option, itemRequest.getQuantity()
            );
            order.addItem(orderItem);
        }

        // 4. 총 금액 계산
        order.calculateTotalAmount();

        // 5. 저장 (Cascade로 OrderItem도 함께 INSERT)
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
     *
     * 왜 재고를 복원하는가?
     * → 주문 등록 시 재고를 차감했으므로, 취소하면 원래대로 돌려놓아야 한다.
     * → 복원하지 않으면 실제 재고보다 적게 표시되어 판매 기회를 잃는다.
     *
     * 왜 findByIdWithItems(JOIN FETCH)를 사용하는가?
     * → 주문 취소 시 각 OrderItem의 productOptionId로 재고를 복원해야 한다.
     * → LAZY 로딩이면 order.getItems() 시점에 추가 쿼리 발생 (N+1).
     * → JOIN FETCH로 한 번에 가져오면 쿼리 1번으로 처리된다.
     *
     * @param id 취소할 주문 ID
     */
    public void cancelOrder(Long id) {
        // 1. 주문 + 항목 조회
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.ORDER_NOT_FOUND));

        // 2. 주문 취소 (상태 변경)
        order.cancel();

        // 3. 각 항목의 재고 복원
        for (OrderItem item : order.getItems()) {
            ProductOption option = productOptionRepository
                    .findByIdAndDeletedFalse(item.getProductOptionId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

            option.increaseStock(item.getQuantity());
        }
    }
}
