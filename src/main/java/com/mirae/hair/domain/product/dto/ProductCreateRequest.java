package com.mirae.hair.domain.product.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 상품 등록 요청 DTO
 *
 * 상품과 옵션을 한 번에 등록한다.
 * → 상품 없이 옵션만 존재할 수 없으므로, 생성 시 함께 받는다.
 *
 * 왜 @Valid를 options 리스트에 붙이는가?
 * → @Valid는 중첩된 객체의 유효성 검증을 활성화한다.
 * → 이게 없으면 ProductOptionRequest 내부의 @NotBlank, @Min 등이 동작하지 않는다.
 * → 리스트 안의 각 요소마다 유효성 검증이 실행된다.
 *
 * 왜 @NotEmpty를 options에 붙이는가?
 * → 상품에는 최소 1개 이상의 옵션이 있어야 재고 관리가 가능하다.
 * → 옵션이 없는 상품은 판매/재고 관리가 불가능하다.
 */
@Getter
@NoArgsConstructor
public class ProductCreateRequest {

    /** 상품명 */
    @NotBlank(message = "상품명은 필수입니다")
    private String name;

    /** 브랜드명 */
    @NotBlank(message = "브랜드명은 필수입니다")
    private String brand;

    /** 카테고리 ID */
    @NotNull(message = "카테고리는 필수입니다")
    private Long categoryId;

    /** 기본 판매 가격 (원) */
    @Min(value = 0, message = "가격은 0 이상이어야 합니다")
    private int price;

    /** 상품 설명 (선택) */
    private String description;

    /**
     * 옵션 목록 (최소 1개 필수)
     * → @Valid: 리스트 내부 각 요소의 유효성 검증을 활성화
     * → @NotEmpty: null이거나 빈 리스트이면 검증 실패
     */
    @NotEmpty(message = "옵션은 최소 1개 이상 등록해야 합니다")
    @Valid
    private List<ProductOptionRequest> options;
}
