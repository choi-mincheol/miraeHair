package com.mirae.hair.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
public class CategoryUpdateRequest {

    @NotBlank(message = "카테고리명은 필수입니다")
    private String name;

    @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다")
    private int displayOrder;
}
