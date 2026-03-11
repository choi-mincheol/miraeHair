package com.mirae.hair.domain.product.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 상품 목록 조회 응답 DTO
 *
 * MyBatis로 조회한 결과를 매핑하는 DTO이다.
 * → 목록 조회에서는 간략한 정보만 보여준다 (상세 설명, 옵션 등은 제외).
 * → 불필요한 데이터를 줄여서 네트워크 부하를 낮춘다.
 *
 * 왜 @Setter가 있는가?
 * → MyBatis는 resultMap으로 매핑할 때 기본적으로 setter 메서드를 사용한다.
 * → Entity에서는 @Setter 금지이지만, DTO는 단순한 데이터 운반 객체이므로 @Setter를 사용해도 괜찮다.
 * → Entity는 "비즈니스 규칙"을 지켜야 하지만, DTO는 "데이터 전달"이 목적이기 때문이다.
 *
 * totalStock:
 * → 해당 상품의 모든 옵션 재고 합계 (SUM(product_options.stock_quantity))
 * → MyBatis SQL에서 계산하여 매핑한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductListDto {

    private Long id;
    private String name;
    private String brand;
    private Long categoryId;
    private String categoryName;
    private int price;

    /** 전체 옵션 재고 합계 */
    private int totalStock;

    private LocalDateTime createdAt;
}
