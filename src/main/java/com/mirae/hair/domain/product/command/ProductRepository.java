package com.mirae.hair.domain.product.command;

import com.mirae.hair.domain.product.domain.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 상품 JPA Repository (Command용)
 *
 * CQRS 패턴에서 Command(생성/수정/삭제) 담당:
 * → save(): 상품 저장 (CascadeType.ALL이므로 옵션도 함께 저장됨)
 * → findByIdAndDeletedFalse(): 삭제되지 않은 상품 조회 (수정/삭제 시 사용)
 *
 * 왜 조회(Query)에 이 Repository를 사용하지 않는가? (CQRS)
 * → 목록 조회(페이징, 검색, 필터)는 SQL이 복잡해질 수 있다.
 * → JPA의 JPQL/Criteria로 작성하면 코드가 복잡하고 가독성이 떨어진다.
 * → MyBatis로 직접 SQL을 작성하면 더 직관적이고 성능 튜닝도 쉽다.
 * → Command는 JPA, Query는 MyBatis — 이것이 CQRS 패턴이다.
 */
public interface ProductRepository extends JpaRepository<Product, Long> {

    /**
     * 삭제되지 않은 상품 조회 (Soft Delete 필터링)
     * → 수정/삭제 시 대상 상품을 찾을 때 사용한다.
     * → is_deleted = true인 상품은 이미 삭제된 것이므로 조회에서 제외한다.
     */
    Optional<Product> findByIdAndDeletedFalse(Long id);
}
