package com.mirae.hair.domain.order.query;

import com.mirae.hair.domain.order.dto.OrderDetailDto;
import com.mirae.hair.domain.order.dto.OrderListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 주문 조회 MyBatis Mapper 인터페이스
 *
 * CQRS 패턴에서 Query(조회) 담당.
 */
@Mapper
public interface OrderQueryMapper {

    /**
     * 주문 목록 조회 (페이징 + 고객 필터 + 기간 필터)
     *
     * @param customerId 고객 ID (nullable — null이면 전체)
     * @param startDate  시작일 (nullable)
     * @param endDate    종료일 (nullable)
     * @param offset     건너뛸 행 수
     * @param size       페이지 크기
     */
    List<OrderListDto> selectOrderList(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("offset") long offset,
            @Param("size") int size
    );

    /** 주문 총 건수 */
    long countOrders(
            @Param("customerId") Long customerId,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 주문 상세 조회 (주문항목 포함)
     * → MyBatis <collection>으로 주문항목을 중첩 매핑
     */
    OrderDetailDto selectOrderDetail(@Param("id") Long id);
}
