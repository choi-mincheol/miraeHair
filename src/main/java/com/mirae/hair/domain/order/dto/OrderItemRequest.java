package com.mirae.hair.domain.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문항목 요청 DTO (주문 등록 시 각 항목)
 *
 * OrderCreateRequest 내부에 List로 포함된다.
 * → 어떤 옵션을, 몇 개 주문하는지만 전달한다.
 * → 가격은 서버에서 조회하여 Snapshot으로 저장한다 (클라이언트가 보내는 가격은 신뢰하지 않는다).
 *
 * 왜 가격을 클라이언트에서 받지 않는가?
 * → 클라이언트가 보내는 가격을 그대로 저장하면, 가격을 조작할 수 있다.
 * → 서버에서 DB의 현재 가격을 직접 조회하여 저장하는 것이 안전하다.
 */
@Getter
@NoArgsConstructor
public class OrderItemRequest {

    /** 주문할 상품 옵션 ID */
    @NotNull(message = "상품 옵션 ID는 필수입니다")
    private Long productOptionId;

    /** 주문 수량 (1 이상) */
    @Min(value = 1, message = "주문 수량은 1 이상이어야 합니다")
    private int quantity;
}
