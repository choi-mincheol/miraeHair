package com.mirae.hair.domain.product.command;

import com.mirae.hair.domain.product.domain.InventoryHistory;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 재고 입출고 이력 JPA Repository (Command용)
 *
 * 입고/출고 시 이력을 기록하는 용도로 사용한다.
 * → InventoryHistory는 한번 생성되면 수정/삭제하지 않는다 (Immutable 이력).
 * → save()만 사용한다.
 */
public interface InventoryHistoryRepository extends JpaRepository<InventoryHistory, Long> {
}
