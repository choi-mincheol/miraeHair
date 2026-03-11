package com.mirae.hair.domain.product.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 등록 요청 DTO
 *
 * 왜 Entity를 직접 요청으로 받지 않는가?
 * → Entity는 DB 테이블과 1:1로 매핑된 "영속 객체"이다.
 * → 클라이언트 요청을 Entity로 직접 받으면:
 *   1) 클라이언트가 id, createdAt 등 보내면 안 되는 값을 보낼 수 있다
 *   2) API 스펙 변경이 Entity 변경을 유발한다 (결합도 증가)
 * → DTO로 받아서 필요한 값만 Entity로 변환하는 것이 안전하다.
 */
@Getter
@NoArgsConstructor
public class CategoryCreateRequest {

    /** 카테고리명 (예: 샴푸, 트리트먼트) */
    @NotBlank(message = "카테고리명은 필수입니다")
    private String name;

    /** 표시 순서 (0 이상) */
    @Min(value = 0, message = "표시 순서는 0 이상이어야 합니다")
    private int displayOrder;
}
