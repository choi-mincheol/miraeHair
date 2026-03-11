package com.mirae.hair.domain.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 상품 상세 조회 응답 DTO (옵션 목록 포함)
 *
 * MyBatis의 중첩 resultMap으로 매핑된다.
 * → 상품 정보 + 옵션 목록을 한 번의 JOIN 쿼리로 가져온다.
 *
 * MyBatis에서 1:N(컬렉션) 매핑:
 * → <resultMap>에서 <collection> 태그를 사용하면,
 *   JOIN 결과의 여러 행을 하나의 상품 + 여러 옵션 리스트로 자동 그룹핑한다.
 * → 예: products LEFT JOIN product_options → Product 1개 + Option N개로 매핑
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductDetailDto {

    private Long id;
    private String name;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private int price;
    private String description;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;

    /** 옵션 목록 — MyBatis <collection>으로 매핑 */
    private List<ProductOptionDto> options;
}
