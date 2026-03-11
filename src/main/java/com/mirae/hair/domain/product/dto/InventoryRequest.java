package com.mirae.hair.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 입고/출고 요청 DTO
 *
 * 입고(/api/inventory/in)와 출고(/api/inventory/out)에서 동일한 DTO를 사용한다.
 * → 요청 구조가 동일하므로 (대상 옵션, 수량, 사유) 하나의 DTO로 통일한다.
 * → 입고/출고 구분은 URL 경로로 한다 (/in vs /out).
 */
@Getter
@NoArgsConstructor
public class InventoryRequest {

    /** 대상 상품 옵션 ID */
    @NotNull(message = "상품 옵션 ID는 필수입니다")
    private Long productOptionId;

    /** 입고/출고 수량 (1 이상) */
    @Min(value = 1, message = "수량은 1 이상이어야 합니다")
    private int quantity;

    /** 사유 (선택, 예: "3월 정기 입고", "주문 #1234 출고") */
    private String reason;
}
