package com.mirae.hair.domain.product.command;

import com.mirae.hair.domain.product.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 카테고리 JPA Repository (Command용)
 *
 * CQRS 패턴에서 Command(생성/수정/삭제) 담당:
 * → save(): 카테고리 저장
 * → findById(): 수정/삭제 시 엔티티 조회
 * → existsByName(): 카테고리명 중복 검사
 *
 * 왜 JpaRepository를 상속하는가?
 * → JpaRepository는 기본 CRUD 메서드(save, findById, delete 등)를 제공한다.
 * → 인터페이스만 선언하면 Spring Data JPA가 구현체를 자동으로 생성해준다.
 * → SQL을 작성하지 않아도 기본적인 데이터 조작이 가능하다.
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /**
     * 카테고리명 존재 여부 확인 (중복 방지)
     * → Spring Data JPA의 쿼리 메서드 네이밍 규칙에 의해 자동으로 SQL이 생성된다.
     * → SELECT COUNT(*) > 0 FROM categories WHERE name = ?
     */
    boolean existsByName(String name);
}
