package com.mirae.hair.domain.order.domain;

import com.mirae.hair.domain.product.domain.Product;
import com.mirae.hair.domain.product.domain.ProductOption;
import com.mirae.hair.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 주문항목 엔티티 (가격 Snapshot)
 *
 * ★ 이 엔티티가 feature/05-order의 가장 핵심적인 부분이다 ★
 *
 * 왜 "가격 Snapshot"이 필요한가?
 * → 상품 가격은 언제든 변경될 수 있다 (인상, 할인 등).
 * → OrderItem이 Product를 참조만 하고 가격을 복사하지 않으면:
 *   - 3월에 45,000원에 판매한 주문이
 *   - 4월에 상품 가격이 50,000원으로 오르면
 *   - 3월 주문도 50,000원으로 표시되어 매출 데이터가 틀어진다!
 * → 그래서 주문 시점의 가격을 OrderItem에 "복사 저장(Snapshot)"한다.
 * → 이후 상품 가격이 바뀌어도 주문 기록은 영향을 받지 않는다.
 *
 * Snapshot 대상 필드:
 * → productName: 주문 시점의 상품명 (상품명이 바뀔 수도 있으므로)
 * → optionName: 주문 시점의 옵션명
 * → unitPrice: 주문 시점의 단가 (Product.price + ProductOption.additionalPrice)
 *
 * 왜 productOptionId를 FK가 아닌 일반 컬럼으로 저장하는가?
 * → FK로 연결하면 ProductOption이 삭제될 때 주문항목도 영향을 받는다.
 * → 일반 컬럼으로 저장하면 ProductOption이 삭제(Soft Delete)되어도 주문 이력은 보존된다.
 * → "이 주문이 어떤 옵션이었는지" 참조할 수 있되, 강한 결합은 피한다.
 */
@Entity
@Table(name = "order_items")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OrderItem extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 소속 주문 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    /** 주문 시점의 상품 옵션 ID (참조용, FK 아님) */
    @Column(nullable = false)
    private Long productOptionId;

    /** ★ Snapshot ★ 주문 시점의 상품명 */
    @Column(nullable = false, length = 100)
    private String productName;

    /** ★ Snapshot ★ 주문 시점의 옵션명 */
    @Column(nullable = false, length = 100)
    private String optionName;

    /**
     * ★ Snapshot ★ 주문 시점의 단가
     * → Product.price + ProductOption.additionalPrice
     * → 이 값은 주문 이후 절대 변경되지 않는다.
     */
    @Column(nullable = false)
    private int unitPrice;

    /** 주문 수량 */
    @Column(nullable = false)
    private int quantity;

    /**
     * 소계 (unitPrice × quantity)
     * → 미리 계산하여 저장한다.
     * → 조회할 때마다 곱셈을 하지 않아도 된다.
     */
    @Column(nullable = false)
    private int subtotal;

    @Builder
    private OrderItem(Order order, Long productOptionId, String productName,
                      String optionName, int unitPrice, int quantity) {
        this.order = order;
        this.productOptionId = productOptionId;
        this.productName = productName;
        this.optionName = optionName;
        this.unitPrice = unitPrice;
        this.quantity = quantity;
        this.subtotal = unitPrice * quantity;
    }

    /**
     * 주문항목 생성 (가격 Snapshot 적용)
     *
     * Product와 ProductOption에서 현재 가격과 이름을 "복사"하여 저장한다.
     * → 이 시점 이후 Product/ProductOption의 값이 바뀌어도 이 OrderItem은 영향 없음.
     *
     * @param order    소속 주문
     * @param product  상품 (이름, 기본가격 참조)
     * @param option   옵션 (옵션명, 추가가격, 재고 참조)
     * @param quantity 주문 수량
     */
    public static OrderItem createSnapshot(Order order, Product product,
                                           ProductOption option, int quantity) {
        int unitPrice = product.getPrice() + option.getAdditionalPrice();

        return OrderItem.builder()
                .order(order)
                .productOptionId(option.getId())
                .productName(product.getName())
                .optionName(option.getOptionName())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .build();
    }
}
