package com.mirae.hair.domain.order.domain;

import com.mirae.hair.domain.customer.domain.Customer;
import com.mirae.hair.global.entity.BaseEntity;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 주문 엔티티
 *
 * "미용실(Customer)에 미용 제품을 판매한 기록"을 담는 엔티티이다.
 * 하나의 주문에는 여러 주문항목(OrderItem)이 포함된다.
 *
 * 주문 생명주기:
 * → 주문 등록 시 CONFIRMED 상태로 생성된다.
 * → 취소 시 CANCELLED 상태로 변경되고, 재고가 복원된다.
 * → 한번 취소된 주문은 다시 확정할 수 없다 (취소 후 재등록으로 처리).
 *
 * totalAmount:
 * → 모든 OrderItem.subtotal의 합계이다.
 * → 주문 생성 시 계산하여 저장한다.
 * → 왜 매번 계산하지 않고 저장하는가?
 *   → 조회할 때마다 SUM을 계산하면 성능이 떨어진다.
 *   → 한번 저장해두면 조회 시 바로 꺼낼 수 있다.
 */
@Entity
@Table(name = "orders")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 주문 고객 (미용실)
     *
     * 왜 Customer를 직접 참조하는가?
     * → 주문이 어떤 미용실의 것인지 알아야 하므로 FK로 연결한다.
     * → LAZY 로딩으로 필요할 때만 Customer를 조회한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    /** 총 주문 금액 (모든 OrderItem.subtotal의 합계) */
    @Column(nullable = false)
    private int totalAmount;

    /**
     * 주문 상태
     * → @Enumerated(STRING): DB에 "CONFIRMED", "CANCELLED" 문자열로 저장
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OrderStatus status;

    /** 주문 메모 (특이사항) */
    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 주문일시 — 주문이 생성된 시점 */
    @Column(nullable = false)
    private LocalDateTime orderedAt;

    /**
     * 주문항목 목록 (OneToMany)
     *
     * CascadeType.ALL:
     * → Order를 저장하면 OrderItem도 함께 저장된다.
     * → orderRepository.save(order) 한 번으로 주문항목까지 모두 INSERT 된다.
     */
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    @Builder
    private Order(Customer customer, int totalAmount, OrderStatus status,
                  String memo, LocalDateTime orderedAt) {
        this.customer = customer;
        this.totalAmount = totalAmount;
        this.status = status;
        this.memo = memo;
        this.orderedAt = orderedAt;
    }

    /**
     * 주문 생성 정적 팩토리 메서드
     *
     * → 생성 시점에는 totalAmount = 0 (항목 추가 후 calculateTotalAmount()로 계산)
     * → 상태는 CONFIRMED로 시작
     * → orderedAt은 현재 시각
     */
    public static Order create(Customer customer, String memo) {
        return Order.builder()
                .customer(customer)
                .totalAmount(0)
                .status(OrderStatus.CONFIRMED)
                .memo(memo)
                .orderedAt(LocalDateTime.now())
                .build();
    }

    /**
     * 주문항목 추가
     */
    public void addItem(OrderItem item) {
        this.items.add(item);
    }

    /**
     * 총 금액 계산 (모든 항목의 subtotal 합계)
     *
     * 왜 항목을 모두 추가한 후에 계산하는가?
     * → 항목이 추가될 때마다 계산하면, 중간 상태의 금액이 저장될 수 있다.
     * → 모든 항목을 추가한 후 한 번에 계산하면 정확한 총 금액이 보장된다.
     */
    public void calculateTotalAmount() {
        this.totalAmount = items.stream()
                .mapToInt(OrderItem::getSubtotal)
                .sum();
    }

    /**
     * 주문 취소
     *
     * 왜 CONFIRMED 상태에서만 취소 가능한가?
     * → 이미 취소된 주문을 다시 취소하면 재고가 이중으로 복원될 수 있다.
     * → 상태 검증 후 변경하여 이중 취소를 방지한다.
     */
    public void cancel() {
        if (this.status != OrderStatus.CONFIRMED) {
            throw new BusinessException(ErrorCode.ORDER_ALREADY_CANCELLED);
        }
        this.status = OrderStatus.CANCELLED;
    }
}
