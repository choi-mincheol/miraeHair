package com.mirae.hair.domain.auth.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 회원 권한(역할) enum
 *
 * 왜 String이 아닌 enum으로 권한을 관리하는가?
 * → DB에 "ADMIN", "admin", "Admin" 등 다양한 형태가 섞이면 권한 체크가 불안정해진다.
 * → enum으로 관리하면 컴파일 타임에 오타를 잡을 수 있고, 허용된 값만 사용된다.
 *
 * 왜 "ROLE_" 접두사가 붙는가?
 * → Spring Security의 hasRole("ADMIN")은 내부적으로 "ROLE_ADMIN"을 찾는다.
 * → 이 규칙을 따르지 않으면 권한 체크가 동작하지 않는다.
 * → hasAuthority("ROLE_ADMIN")을 쓰면 접두사가 필요 없지만, Spring Security 관례를 따르는 것이 혼란을 줄인다.
 *
 * 향후 확장 시:
 * → ROLE_MANAGER (지역 관리자), ROLE_CUSTOMER (미용실 계정) 등 추가 가능
 */
@Getter
@AllArgsConstructor
public enum MemberRole {

    ROLE_ADMIN("관리자");

    /** 권한 설명 (한글) */
    private final String description;
}
