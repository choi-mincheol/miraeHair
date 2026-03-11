package com.mirae.hair.domain.product.domain;

/**
 * 재고 입출고 구분 enum
 *
 * 왜 enum으로 관리하는가?
 * → "IN", "OUT"을 String으로 사용하면 "in", "In", "입고" 등 오타/불일치가 발생한다.
 * → enum으로 정의하면 컴파일 타임에 잘못된 값을 잡을 수 있다.
 * → DB에는 @Enumerated(STRING)으로 "IN", "OUT" 문자열로 저장된다.
 */
public enum InventoryType {

    /** 입고 - 재고 증가 */
    IN("입고"),

    /** 출고 - 재고 감소 (주문, 반품 등) */
    OUT("출고");

    private final String description;

    InventoryType(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
