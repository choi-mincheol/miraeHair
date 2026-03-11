package com.mirae.hair.domain.order.query;

import com.mirae.hair.domain.order.dto.OrderDetailDto;
import com.mirae.hair.domain.order.dto.OrderListDto;
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
 * 주문 Query 서비스 (조회 전용)
 *
 * CQRS 패턴에서 Query 담당.
 * → MyBatis Mapper로 주문 목록/상세를 조회한다.
 *
 * 가격 Snapshot 덕분에:
 * → 주문 상세 조회 시 Product/ProductOption 테이블을 JOIN하지 않아도 된다!
 * → OrderItem에 저장된 productName, optionName, unitPrice로 충분하다.
 * → 쿼리가 단순해지고, 삭제된 상품이 있어도 주문 이력은 정상 조회된다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OrderQueryService {

    private final OrderQueryMapper orderQueryMapper;

    /**
     * 주문 목록 조회 (페이징 + 고객 필터 + 기간 필터)
     *
     * @param customerId 고객 ID (nullable)
     * @param startDate  시작일 (nullable)
     * @param endDate    종료일 (nullable)
     * @param pageable   페이징 정보
     */
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
    public OrderDetailDto getOrderDetail(Long id) {
        OrderDetailDto order = orderQueryMapper.selectOrderDetail(id);

        if (order == null) {
            throw new BusinessException(ErrorCode.ORDER_NOT_FOUND);
        }

        return order;
    }
}
