package com.mirae.hair.domain.product.domain;

import com.mirae.hair.global.entity.BaseEntity;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상품 옵션 엔티티
 *
 * 하나의 상품(Product)에 여러 옵션이 존재할 수 있다.
 * 예: "모로칸오일 트리트먼트" → [100ml, 200ml, 500ml]
 *
 * 왜 옵션을 별도 엔티티로 분리하는가?
 * → 같은 상품이라도 용량(100ml/200ml)이나 색상에 따라 가격과 재고가 다를 수 있다.
 * → Product에 stockQuantity를 두면 옵션별 재고 관리가 불가능하다.
 * → 옵션별로 추가 가격과 재고를 개별 관리할 수 있어야 한다.
 *
 * 가격 계산:
 * → 최종 가격 = Product.price(기본가격) + ProductOption.additionalPrice(추가가격)
 * → 예: 기본가격 45,000원 + 200ml 옵션 추가가격 15,000원 = 60,000원
 *
 * 재고 관리:
 * → stockQuantity는 현재 재고 수량을 실시간으로 반영한다.
 * → 입고 시 increaseStock(), 출고 시 decreaseStock()으로만 변경 가능하다.
 * → InventoryHistory에 입출고 이력이 별도로 기록된다.
 */
@Entity
@Table(name = "product_options")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductOption extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 소속 상품 (ManyToOne)
     *
     * 왜 FetchType.LAZY인가?
     * → 옵션을 조회할 때 상품 정보가 항상 필요한 건 아니다.
     * → LAZY로 설정하면 실제로 option.getProduct()를 호출할 때만 상품을 로딩한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    /** 옵션명 (예: 100ml, 200ml, 블랙, 브라운) */
    @Column(nullable = false, length = 100)
    private String optionName;

    /**
     * 추가 가격 (기본가격에 더해지는 금액)
     * → 0이면 기본가격 그대로
     * → 예: 200ml 옵션이면 15,000원 추가 → 최종가격 = 기본가격 + 15,000
     */
    @Column(nullable = false)
    private int additionalPrice;

    /**
     * 현재 재고 수량
     * → 입고(increaseStock) 시 증가, 출고(decreaseStock) 시 감소
     * → 0 미만으로 내려갈 수 없다 (음수 재고 방지)
     */
    @Column(nullable = false)
    private int stockQuantity;

    /** 삭제 여부 (Soft Delete) */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Builder
    private ProductOption(Product product, String optionName,
                          int additionalPrice, int stockQuantity) {
        this.product = product;
        this.optionName = optionName;
        this.additionalPrice = additionalPrice;
        this.stockQuantity = stockQuantity;
    }

    /**
     * 옵션 생성 정적 팩토리 메서드
     */
    public static ProductOption create(Product product, String optionName,
                                       int additionalPrice, int stockQuantity) {
        return ProductOption.builder()
                .product(product)
                .optionName(optionName)
                .additionalPrice(additionalPrice)
                .stockQuantity(stockQuantity)
                .build();
    }

    /**
     * 재고 입고 (증가)
     *
     * 왜 this.stockQuantity += quantity가 아닌 메서드로 감싸는가?
     * → @Setter로 아무 값이나 설정할 수 있으면 음수 재고가 될 수도 있다.
     * → 도메인 메서드로 감싸면 비즈니스 규칙(양수만 허용)을 강제할 수 있다.
     */
    public void increaseStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "입고 수량은 1 이상이어야 합니다");
        }
        this.stockQuantity += quantity;
    }

    /**
     * 재고 출고 (감소)
     *
     * 왜 재고 부족 검증이 필요한가?
     * → 재고보다 많은 수량을 출고하면 stockQuantity가 음수가 된다.
     * → 음수 재고는 데이터 무결성을 깨뜨리고, 없는 물건을 판 것이 된다.
     * → 출고 전에 반드시 재고 >= 출고수량인지 확인한다.
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "출고 수량은 1 이상이어야 합니다");
        }
        if (this.stockQuantity < quantity) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK,
                    String.format("재고가 부족합니다. 현재 재고: %d, 요청 수량: %d", this.stockQuantity, quantity));
        }
        this.stockQuantity -= quantity;
    }

    /**
     * 옵션 정보 수정
     */
    public void update(String optionName, int additionalPrice) {
        this.optionName = optionName;
        this.additionalPrice = additionalPrice;
    }

    /**
     * 옵션 삭제 (Soft Delete)
     */
    public void softDelete() {
        this.deleted = true;
    }
}
