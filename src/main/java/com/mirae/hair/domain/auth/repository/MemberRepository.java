package com.mirae.hair.domain.auth.repository;

import com.mirae.hair.domain.auth.domain.Member;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 회원 JPA Repository
 *
 * 왜 JpaRepository를 상속하는가?
 * → JpaRepository를 상속하면 save(), findById(), findAll(), delete() 등의
 *   기본 CRUD 메서드를 직접 구현하지 않아도 자동으로 사용할 수 있다.
 * → Spring Data JPA가 런타임에 구현 클래스를 자동 생성해준다.
 *
 * 메서드 이름 규칙 (Query Method):
 * → findByEmail → SELECT * FROM members WHERE email = ?
 * → existsByEmail → SELECT count(*) > 0 FROM members WHERE email = ?
 * → Spring Data JPA가 메서드 이름을 파싱하여 SQL을 자동 생성한다.
 */
public interface MemberRepository extends JpaRepository<Member, Long> {

    /**
     * 이메일로 활성 회원 조회 (삭제되지 않은 회원만)
     * → 로그인 시 사용: 이메일로 회원을 찾고, 비밀번호를 비교한다.
     * → isDeleted = false 조건으로 탈퇴한 회원은 제외한다.
     */
    Optional<Member> findByEmailAndDeletedFalse(String email);

    /**
     * 이메일 중복 체크
     * → 회원가입 시 같은 이메일로 가입하는 것을 방지한다.
     */
    boolean existsByEmail(String email);
}
