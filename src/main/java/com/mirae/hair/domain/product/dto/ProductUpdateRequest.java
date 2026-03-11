package com.mirae.hair.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 수정 요청 DTO
 *
 * 상품의 기본 정보만 수정한다 (옵션은 별도 API로 관리).
 * → 옵션 수정까지 한 번에 처리하면 API가 복잡해지고, 부분 수정이 어려워진다.
 * → 상품 기본 정보와 옵션을 분리하면 각각 독립적으로 수정할 수 있다.
 */
@Getter
@NoArgsConstructor
public class ProductUpdateRequest {

    @NotBlank(message = "상품명은 필수입니다")
    private String name;

    @NotBlank(message = "브랜드명은 필수입니다")
    private String brand;

    @NotNull(message = "카테고리는 필수입니다")
    private Long categoryId;

    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private int price;

    private String description;
}
