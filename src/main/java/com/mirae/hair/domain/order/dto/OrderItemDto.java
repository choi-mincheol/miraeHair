package com.mirae.hair.domain.order.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 주문항목 응답 DTO (주문 상세 내 중첩)
 *
 * MyBatis <collection>으로 매핑된다.
 * → 가격 Snapshot 데이터를 그대로 보여준다.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderItemDto {

    private Long id;

    /** Snapshot — 주문 시점 상품명 */
    private String productName;

    /** Snapshot — 주문 시점 옵션명 */
    private String optionName;

    /** Snapshot — 주문 시점 단가 */
    private int unitPrice;

    /** 주문 수량 */
    private int quantity;

    /** 소계 (unitPrice × quantity) */
    private int subtotal;
}
