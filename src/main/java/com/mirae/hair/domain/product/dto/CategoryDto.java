package com.mirae.hair.domain.product.dto;

import com.mirae.hair.domain.product.domain.Category;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 카테고리 응답 DTO
 *
 * MyBatis 조회 결과를 매핑하기도 하고, Entity → DTO 변환에도 사용한다.
 * → MyBatis: resultType으로 직접 매핑 (setter 사용)
 * → JPA: from(Category) 정적 메서드로 변환
 *
 * 왜 DTO에 from() 메서드를 두는가?
 * → Entity → DTO 변환 로직이 Service에 흩어지면 중복이 발생한다.
 * → DTO 내부에 from()을 두면 변환 로직이 한 곳에 모여 유지보수가 쉽다.
 * → "이 DTO가 어떤 Entity에서 만들어지는지" 한눈에 알 수 있다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryDto {

    private Long id;
    private String name;
    private int displayOrder;

    /**
     * Entity → DTO 변환
     */
    public static CategoryDto from(Category category) {
        return CategoryDto.builder()
                .id(category.getId())
                .name(category.getName())
                .displayOrder(category.getDisplayOrder())
                .build();
    }
}
