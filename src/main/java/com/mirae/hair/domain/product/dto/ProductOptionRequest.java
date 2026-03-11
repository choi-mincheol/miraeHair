package com.mirae.hair.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 옵션 요청 DTO (상품 등록 시 옵션 정보)
 *
 * ProductCreateRequest 내부에 List로 포함된다.
 * 예: options: [{ "optionName": "100ml", "additionalPrice": 0, "stockQuantity": 50 }]
 */
@Getter
@NoArgsConstructor
public class ProductOptionRequest {

    /** 옵션명 (예: 100ml, 200ml, 블랙) */
    @NotBlank(message = "옵션명은 필수입니다")
    private String optionName;

    /** 추가 가격 (기본가격에 더해지는 금액, 0 이상) */
    @Min(value = 0, message = "추가 가격은 0 이상이어야 합니다")
    private int additionalPrice;

    /** 초기 재고 수량 (0 이상) */
    @Min(value = 0, message = "재고 수량은 0 이상이어야 합니다")
    private int stockQuantity;
}
