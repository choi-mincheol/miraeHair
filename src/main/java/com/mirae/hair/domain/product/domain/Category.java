package com.mirae.hair.domain.product.domain;

import com.mirae.hair.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 카테고리 엔티티
 *
 * 왜 enum이 아니라 별도 테이블(Entity)로 관리하는가?
 * → enum은 코드에 고정되어 있어서, 새 카테고리를 추가하려면 코드를 수정하고 재배포해야 한다.
 * → 별도 테이블로 관리하면 관리자가 운영 중에도 카테고리를 동적으로 추가/수정할 수 있다.
 * → 향후 관리자 화면(Thymeleaf)에서 카테고리를 관리하는 기능을 쉽게 추가할 수 있다.
 *
 * displayOrder 필드:
 * → 카테고리 목록을 표시할 때 순서를 지정하기 위한 필드이다.
 * → 예: 샴푸(1), 트리트먼트(2), 염모제(3) ... 기타(99)
 */
@Entity
@Table(name = "categories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Category extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 카테고리명 (예: 샴푸, 트리트먼트, 염모제) - 중복 불가 */
    @Column(nullable = false, unique = true, length = 50)
    private String name;

    /**
     * 표시 순서 - 카테고리 목록에서 정렬 기준
     * → 숫자가 작을수록 앞에 표시된다.
     * → "기타"는 99로 설정해서 항상 마지막에 표시되게 한다.
     */
    @Column(nullable = false)
    private int displayOrder;

    @Builder
    private Category(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }

    /**
     * 카테고리 생성 정적 팩토리 메서드
     *
     * 왜 생성자 대신 정적 팩토리 메서드를 사용하는가?
     * → 메서드 이름(create)으로 "생성"이라는 의도를 명확히 표현할 수 있다.
     * → new Category(name, order) vs Category.create(name, order) — 후자가 더 읽기 쉽다.
     */
    public static Category create(String name, int displayOrder) {
        return Category.builder()
                .name(name)
                .displayOrder(displayOrder)
                .build();
    }

    /**
     * 카테고리 정보 수정
     * → @Setter 대신 명시적인 도메인 메서드로 값을 변경한다.
     * → "어디서든 값을 바꿀 수 있는 @Setter"보다 안전하다.
     */
    public void update(String name, int displayOrder) {
        this.name = name;
        this.displayOrder = displayOrder;
    }
}
