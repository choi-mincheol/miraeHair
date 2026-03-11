package com.mirae.hair.domain.product.command;

import com.mirae.hair.domain.product.domain.ProductOption;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 상품 옵션 JPA Repository (Command용)
 *
 * 주로 재고 입출고(InventoryCommandService)에서 사용한다.
 * → 옵션 ID로 조회 → 재고 수량 변경 → 자동 저장 (더티체킹)
 *
 * 왜 더티체킹(Dirty Checking)으로 저장하는가?
 * → JPA는 @Transactional 안에서 엔티티의 필드를 변경하면,
 *   트랜잭션 종료 시 변경된 부분을 감지해서 자동으로 UPDATE SQL을 실행한다.
 * → 명시적으로 save()를 호출하지 않아도 된다.
 * → 예: option.increaseStock(10) → 트랜잭션 끝나면 자동 UPDATE
 */
public interface ProductOptionRepository extends JpaRepository<ProductOption, Long> {

    /**
     * 삭제되지 않은 옵션 조회
     */
    Optional<ProductOption> findByIdAndDeletedFalse(Long id);
}
