package com.mirae.hair.global.exception;

import lombok.Builder;
import lombok.Getter;

/**
 * 에러 발생 시 클라이언트에게 반환되는 응답 DTO
 *
 * 왜 ApiResponse와 별도로 ErrorResponse가 필요한가?
 * → ApiResponse는 성공/실패 모두를 담는 범용 래퍼이고,
 *   ErrorResponse는 에러에 특화된 정보(에러 코드명, HTTP 상태)를 담는다.
 * → GlobalExceptionHandler에서 예외를 잡아서 이 형식으로 변환해 반환한다.
 *
 * 응답 예시:
 * {
 *   "success": false,
 *   "code": "PRODUCT_NOT_FOUND",
 *   "message": "상품을 찾을 수 없습니다",
 *   "status": 404
 * }
 */
@Getter
@Builder
public class ErrorResponse {

    /** 항상 false (에러 응답이니까) */
    private final boolean success;

    /** ErrorCode enum의 이름 (예: "PRODUCT_NOT_FOUND") */
    private final String code;

    /** 사용자에게 보여줄 에러 메시지 */
    private final String message;

    /** HTTP 상태 코드 (예: 404) */
    private final int status;

    /**
     * ErrorCode enum으로부터 ErrorResponse를 생성하는 정적 팩토리 메서드
     *
     * 왜 정적 팩토리 메서드(from)를 사용하는가?
     * → new ErrorResponse(false, code.name(), code.getMessage(), code.getStatus().value())
     *   이렇게 생성자를 직접 호출하면 파라미터가 많아서 실수하기 쉽다.
     * → ErrorResponse.from(ErrorCode.PRODUCT_NOT_FOUND) 한 줄이면 깔끔하다.
     */
    public static ErrorResponse from(ErrorCode errorCode) {
        return ErrorResponse.builder()
                .success(false)
                .code(errorCode.name())
                .message(errorCode.getMessage())
                .status(errorCode.getStatus().value())
                .build();
    }
}
