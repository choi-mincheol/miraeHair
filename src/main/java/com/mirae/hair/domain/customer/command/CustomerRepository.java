package com.mirae.hair.domain.customer.command;

import com.mirae.hair.domain.customer.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 고객 JPA Repository (Command용)
 *
 * CQRS 패턴에서 Command(생성/수정/삭제) 담당.
 *
 * 사업자번호 중복 검사 주의:
 * → DB에는 암호화된 사업자번호가 저장되어 있다.
 * → existsByBusinessNumber()에 전달하는 값도 암호화된 값이어야 한다.
 * → 같은 평문을 같은 키로 암호화하면 항상 같은 암호문이 나오므로 (CBC + 고정 IV),
 *   암호화된 값으로 비교해도 정확하다.
 */
public interface CustomerRepository extends JpaRepository<Customer, Long> {

    /** 삭제되지 않은 고객 조회 */
    Optional<Customer> findByIdAndDeletedFalse(Long id);

    /** 사업자번호 존재 여부 (암호화된 값으로 비교) */
    boolean existsByBusinessNumber(String encryptedBusinessNumber);

    /** 특정 ID를 제외한 사업자번호 존재 여부 (수정 시 중복 검사용) */
    boolean existsByBusinessNumberAndIdNot(String encryptedBusinessNumber, Long id);
}
