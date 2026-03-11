package com.mirae.hair.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 에러 코드를 중앙 관리하는 enum
 *
 * 왜 enum으로 에러 코드를 관리하는가?
 * → 에러 코드를 String으로 흩어놓으면 ("NOT_FOUND", "NOTFOUND", "not_found" 등)
 *   오타/불일치가 발생하고, 어떤 에러 코드가 있는지 파악하기 어렵다.
 * → enum으로 한 곳에 모아놓으면:
 *   1) 컴파일 타임에 오타를 잡을 수 있다
 *   2) IDE 자동완성으로 실수를 줄인다
 *   3) 에러 코드 전체 목록을 한눈에 볼 수 있다
 *
 * 대안: 각 도메인에 별도 Exception 클래스를 만드는 방법도 있지만,
 * → ProductNotFoundException, CustomerNotFoundException... 클래스가 폭발적으로 늘어난다.
 * → ErrorCode enum + BusinessException 조합이면 클래스 하나로 모든 비즈니스 예외를 처리할 수 있다.
 *
 * 도메인별 에러 코드 추가 방법:
 * → 이 enum에 PRODUCT_NOT_FOUND, CUSTOMER_NOT_FOUND 등을 추가하면 된다.
 * → 각 feature에서 필요한 에러 코드를 여기에 추가한다.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // === 공통 에러 ===
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "입력값이 올바르지 않습니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "인증이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "접근 권한이 없습니다"),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "리소스를 찾을 수 없습니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다"),

    // === 인증 관련 에러 ===
    EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 사용 중인 이메일입니다"),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "만료된 토큰입니다"),
    MEMBER_NOT_FOUND(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다"),
    ;

    /** HTTP 상태 코드 (예: 400, 401, 404, 500) */
    private final HttpStatus status;

    /** 사용자에게 보여줄 에러 메시지 */
    private final String message;
}
