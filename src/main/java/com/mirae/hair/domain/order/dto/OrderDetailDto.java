package com.mirae.hair.domain.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 주문 상세 조회 응답 DTO (주문항목 포함)
 *
 * MyBatis 중첩 resultMap으로 매핑한다.
 * → orders JOIN order_items → Order 1개 + OrderItem N개로 그룹핑
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderDetailDto {

    private Long id;
    private Long customerId;
    private String customerShopName;
    private int totalAmount;
    private String status;
    private String memo;
    private LocalDateTime orderedAt;

    /** 주문항목 목록 — MyBatis <collection>으로 매핑 */
    private List<OrderItemDto> items;
}
