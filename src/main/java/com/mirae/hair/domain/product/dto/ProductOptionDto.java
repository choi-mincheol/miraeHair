package com.mirae.hair.domain.product.dto;

import com.mirae.hair.domain.product.domain.ProductOption;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 상품 옵션 응답 DTO
 *
 * 상품 상세 조회 시 옵션 목록으로 포함된다 (ProductDetailDto.options).
 * MyBatis의 <collection> resultMap으로도 매핑된다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductOptionDto {

    private Long id;
    private String optionName;
    private int additionalPrice;
    private int stockQuantity;

    /**
     * Entity → DTO 변환
     */
    public static ProductOptionDto from(ProductOption option) {
        return ProductOptionDto.builder()
                .id(option.getId())
                .optionName(option.getOptionName())
                .additionalPrice(option.getAdditionalPrice())
                .stockQuantity(option.getStockQuantity())
                .build();
    }
}
