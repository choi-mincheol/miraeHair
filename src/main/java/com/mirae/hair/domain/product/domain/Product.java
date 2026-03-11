package com.mirae.hair.domain.product.domain;

import com.mirae.hair.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * 상품 엔티티
 *
 * 미용 제품(샴푸, 트리트먼트, 염모제 등)의 기본 정보를 담는 엔티티이다.
 * 하나의 상품은 여러 옵션(용량, 색상 등)을 가질 수 있다.
 *
 * 예시:
 * - 상품: "모로칸오일 트리트먼트" (기본가격 45,000원)
 *   - 옵션1: "100ml" (추가가격 0원) → 최종 45,000원
 *   - 옵션2: "200ml" (추가가격 15,000원) → 최종 60,000원
 *
 * 가격 구조:
 * → Product.price = 기본 가격
 * → ProductOption.additionalPrice = 옵션별 추가 가격
 * → 최종 판매가 = price + additionalPrice
 * → 주문 시에는 이 최종 가격을 OrderItem에 Snapshot으로 저장한다 (feature/05-order에서 구현)
 *
 * 왜 재고(stockQuantity)가 Product가 아닌 ProductOption에 있는가?
 * → 같은 샴푸라도 250ml과 500ml의 재고가 다를 수 있다.
 * → 옵션 단위로 재고를 관리해야 정확한 재고 파악이 가능하다.
 */
@Entity
@Table(name = "products")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 상품명 (예: 모로칸오일 트리트먼트) */
    @Column(nullable = false, length = 100)
    private String name;

    /** 브랜드명 (예: 모로칸오일, 케라시스) */
    @Column(nullable = false, length = 100)
    private String brand;

    /**
     * 카테고리 (ManyToOne)
     *
     * 왜 @ManyToOne(fetch = LAZY)인가?
     * → EAGER(즉시 로딩)이면 상품을 조회할 때마다 카테고리도 항상 JOIN해서 가져온다.
     * → LAZY(지연 로딩)이면 실제로 category.getName()을 호출할 때만 쿼리가 실행된다.
     * → 목록 조회 시 카테고리 정보가 항상 필요한 건 아니므로 LAZY가 성능에 유리하다.
     * → 참고: MyBatis 조회에서는 JPA 연관관계와 무관하게 직접 JOIN SQL을 작성한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    /** 기본 판매 가격 (원) — 옵션의 추가 가격과 합산하여 최종 가격이 결정된다 */
    @Column(nullable = false)
    private int price;

    /** 상품 설명 — TEXT 타입으로 긴 설명도 저장 가능 */
    @Column(columnDefinition = "TEXT")
    private String description;

    /**
     * 삭제 여부 (Soft Delete)
     *
     * 왜 Soft Delete를 사용하는가?
     * → 실제로 DELETE 하면 주문 이력에서 "어떤 상품이었는지" 참조할 수 없게 된다.
     * → is_deleted = true로 표시만 하고, 조회 시 WHERE is_deleted = false 조건으로 필터링한다.
     * → 데이터 복구도 가능하고, 통계/이력 관리에도 유리하다.
     */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    /**
     * 상품 옵션 목록 (OneToMany)
     *
     * 왜 cascade = ALL, orphanRemoval = true인가?
     * → cascade = ALL: Product를 저장하면 연결된 옵션도 함께 저장/수정/삭제된다.
     *   → productRepository.save(product) 한 번으로 옵션까지 모두 저장된다.
     * → orphanRemoval = true: 옵션 리스트에서 제거된 옵션은 DB에서도 자동 삭제된다.
     *   → product.getOptions().remove(option) 하면 DB에서도 DELETE 된다.
     *
     * 왜 mappedBy = "product"인가?
     * → 양방향 관계에서 외래키의 주인은 "N" 쪽(ProductOption)이다.
     * → Product는 읽기 전용으로 옵션 목록을 참조한다.
     */
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductOption> options = new ArrayList<>();

    @Builder
    private Product(String name, String brand, Category category, int price, String description) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    /**
     * 상품 생성 정적 팩토리 메서드
     */
    public static Product create(String name, String brand, Category category,
                                 int price, String description) {
        return Product.builder()
                .name(name)
                .brand(brand)
                .category(category)
                .price(price)
                .description(description)
                .build();
    }

    /**
     * 상품에 옵션 추가
     *
     * 왜 addOption 메서드가 필요한가?
     * → 양방향 연관관계에서는 양쪽 모두에 관계를 설정해야 한다.
     * → options.add(option)만 하면 ProductOption의 product 필드가 null이다.
     * → 이 메서드에서 양쪽 관계를 모두 설정해서 실수를 방지한다.
     */
    public void addOption(ProductOption option) {
        this.options.add(option);
    }

    /**
     * 상품 정보 수정
     * → @Setter 없이 명시적인 메서드로만 값을 변경한다.
     */
    public void update(String name, String brand, Category category,
                       int price, String description) {
        this.name = name;
        this.brand = brand;
        this.category = category;
        this.price = price;
        this.description = description;
    }

    /**
     * 상품 삭제 (Soft Delete)
     * → is_deleted를 true로 변경한다.
     * → 실제 DB에서 삭제하지 않으므로 복구가 가능하다.
     */
    public void softDelete() {
        this.deleted = true;
    }
}
