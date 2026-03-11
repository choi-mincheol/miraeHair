package com.mirae.hair.domain.order.command;

import com.mirae.hair.domain.order.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

/**
 * 주문 JPA Repository (Command용)
 *
 * CQRS 패턴에서 Command(생성/취소) 담당.
 */
public interface OrderRepository extends JpaRepository<Order, Long> {

    /**
     * 주문 + 주문항목을 함께 조회 (N+1 방지)
     *
     * 왜 JOIN FETCH를 사용하는가?
     * → 주문 취소 시 각 OrderItem의 재고를 복원해야 한다.
     * → LAZY 로딩이면 order.getItems()를 호출할 때 별도 쿼리가 발생한다 (N+1 문제).
     * → JOIN FETCH로 한 번의 쿼리에 주문 + 항목을 모두 가져온다.
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
