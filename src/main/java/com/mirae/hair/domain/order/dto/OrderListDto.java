package com.mirae.hair.domain.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 주문 목록 조회 응답 DTO
 *
 * MyBatis로 조회한 결과를 매핑한다.
 * → 목록에서는 간략 정보만 표시 (주문항목 상세는 제외)
 * → itemCount: 해당 주문에 포함된 항목 수 (SQL에서 COUNT로 계산)
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderListDto {

    private Long id;

    /** 고객(미용실)명 */
    private String customerShopName;

    /** 총 주문 금액 */
    private int totalAmount;

    /** 주문항목 수 */
    private int itemCount;

    /** 주문 상태 (CONFIRMED / CANCELLED) */
    private String status;

    /** 주문일시 */
    private LocalDateTime orderedAt;
}
