package com.mirae.hair.domain.auth.domain;

import com.mirae.hair.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원(관리자) 엔티티
 *
 * 이 시스템의 "회원"은 본사 관리자를 의미한다.
 * 미용실(고객)은 별도의 Customer 엔티티로 관리된다.
 *
 * 왜 @Setter를 안 쓰는가?
 * → @Setter를 쓰면 아무 곳에서나 member.setPassword("1234")처럼 값을 바꿀 수 있다.
 * → 이러면 "누가, 왜 값을 바꿨는지" 추적이 어렵고, 의도하지 않은 변경이 발생할 수 있다.
 * → 대신 @Builder로 생성하고, 변경이 필요하면 도메인 메서드(예: changePassword())를 만든다.
 * → 이렇게 하면 비밀번호 변경은 반드시 changePassword()를 통해서만 가능하다.
 *
 * 왜 @NoArgsConstructor(access = PROTECTED)인가?
 * → JPA는 엔티티를 생성할 때 기본 생성자가 필요하다 (프록시 객체 생성용).
 * → public이면 new Member()로 불완전한 객체를 만들 수 있어 위험하다.
 * → protected로 제한하면 JPA만 사용하고, 개발자는 Builder나 팩토리 메서드를 쓰게 된다.
 */
@Entity
@Table(name = "members")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이메일 (로그인 ID로 사용) */
    @Column(nullable = false, unique = true, length = 100)
    private String email;

    /**
     * 비밀번호 (BCrypt 해시로 저장)
     * 왜 평문이 아닌 해시로 저장하는가?
     * → DB가 유출되어도 원본 비밀번호를 알 수 없다.
     * → BCrypt는 단방향 해시라서 해시 → 원본 복원이 불가능하다.
     */
    @Column(nullable = false)
    private String password;

    /** 이름 */
    @Column(nullable = false, length = 50)
    private String name;

    /**
     * 권한 (ROLE_ADMIN 등)
     * @Enumerated(STRING): enum을 문자열로 DB에 저장한다.
     * → ORDINAL(숫자)로 저장하면, enum 순서가 바뀔 때 기존 데이터가 꼬인다.
     * → STRING으로 저장하면 "ROLE_ADMIN" 그대로 저장되어 안전하다.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MemberRole role;

    /** 삭제 여부 (Soft Delete) */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Builder
    private Member(String email, String password, String name, MemberRole role) {
        this.email = email;
        this.password = password;
        this.name = name;
        this.role = role != null ? role : MemberRole.ROLE_ADMIN;
    }

    /**
     * 회원 생성 정적 팩토리 메서드
     *
     * 왜 생성자 대신 정적 팩토리 메서드를 사용하는가?
     * → 메서드 이름으로 "무엇을 하는지" 의도를 표현할 수 있다.
     *   new Member(email, password, name) vs Member.create(email, password, name)
     * → 내부 로직(기본 권한 설정 등)을 캡슐화할 수 있다.
     */
    public static Member create(String email, String encodedPassword, String name) {
        return Member.builder()
                .email(email)
                .password(encodedPassword)
                .name(name)
                .role(MemberRole.ROLE_ADMIN)
                .build();
    }
}
