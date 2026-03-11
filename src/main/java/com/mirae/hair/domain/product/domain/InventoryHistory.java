package com.mirae.hair.domain.product.domain;

import com.mirae.hair.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 재고 입출고 이력 엔티티
 *
 * 왜 재고 이력을 별도 테이블로 관리하는가?
 * → ProductOption.stockQuantity만 있으면 "현재 재고"만 알 수 있다.
 * → "언제, 누가, 얼마나, 왜 입고/출고했는지" 추적이 불가능하다.
 * → 이력 테이블을 두면:
 *   1) 재고 변동 감사(audit) 추적이 가능하다
 *   2) 재고 불일치 시 원인을 파악할 수 있다
 *   3) 월별/분기별 입출고 통계를 낼 수 있다
 *
 * ProductOption.stockQuantity vs InventoryHistory:
 * → stockQuantity = 현재 상태 (실시간 조회용, "지금 몇 개 남았는가?")
 * → InventoryHistory = 변동 이력 (감사 추적용, "어떻게 이 수량이 되었는가?")
 * → 둘 다 필요하다. stockQuantity만 있으면 이력 추적 불가, 이력만 있으면 SUM 쿼리가 느리다.
 */
@Entity
@Table(name = "inventory_history")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InventoryHistory extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 대상 상품 옵션 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_option_id", nullable = false)
    private ProductOption productOption;

    /**
     * 입출고 구분 (IN: 입고, OUT: 출고)
     * → @Enumerated(STRING)으로 문자열 저장 ("IN", "OUT")
     * → ORDINAL(숫자)은 enum 순서 변경 시 기존 데이터가 꼬이므로 사용하지 않는다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private InventoryType type;

    /** 입출고 수량 (항상 양수) */
    @Column(nullable = false)
    private int quantity;

    /** 사유 (예: "3월 정기 입고", "주문 #1234 출고") */
    @Column(length = 200)
    private String reason;

    @Builder
    private InventoryHistory(ProductOption productOption, InventoryType type,
                             int quantity, String reason) {
        this.productOption = productOption;
        this.type = type;
        this.quantity = quantity;
        this.reason = reason;
    }

    /**
     * 입출고 이력 생성 정적 팩토리 메서드
     *
     * 이 엔티티는 한번 생성되면 수정되지 않는다 (Immutable한 이력 데이터).
     * → 이력 데이터를 수정하면 감사 추적의 의미가 없어지기 때문이다.
     * → 따라서 update() 메서드가 없다.
     */
    public static InventoryHistory create(ProductOption productOption, InventoryType type,
                                          int quantity, String reason) {
        return InventoryHistory.builder()
                .productOption(productOption)
                .type(type)
                .quantity(quantity)
                .reason(reason)
                .build();
    }
}
