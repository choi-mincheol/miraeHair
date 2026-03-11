package com.mirae.hair.domain.order.domain;

/**
 * 주문 상태 enum
 *
 * 현재는 2가지 상태만 관리한다 (포트폴리오 범위):
 * → 향후 E-commerce 확장 시 PENDING(대기), SHIPPED(배송중), DELIVERED(배송완료) 등 추가 가능
 *
 * 왜 상태를 enum으로 관리하는가?
 * → String으로 "confirmed", "CONFIRMED", "확정" 등 다양한 값이 들어갈 수 있다.
 * → enum으로 정의하면 컴파일 타임에 잘못된 상태 값을 잡을 수 있다.
 */
public enum OrderStatus {

    /** 주문 확정 (정상 상태) */
    CONFIRMED("확정"),

    /** 주문 취소됨 */
    CANCELLED("취소");

    private final String description;

    OrderStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
