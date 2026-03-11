package com.mirae.hair.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;

/**
 *
 * 모든 API의 공통 응답 래퍼 (성공/실패 모두 이 하나의 클래스로 통일)
 *
 * 왜 ApiResponse 하나로 통일하는가?
 * → 성공 응답과 에러 응답의 형식이 다르면, 프론트엔드에서 두 가지 파싱 로직이 필요하다.
 * → 하나의 형식으로 통일하면 프론트엔드에서 response.success로 분기만 하면 된다.
 *
 * 성공 응답 예시:
 * { "success": true, "data": {...}, "message": "조회 성공", "status": 200 }
 *
 * 실패 응답 예시:
 * { "success": false, "message": "상품을 찾을 수 없습니다", "status": 404, "errorCode": "RESOURCE_NOT_FOUND" }
 *
 * @param <T> 응답 데이터의 타입 (제네릭)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)  // null인 필드는 JSON에서 제외
public class ApiResponse<T> {

    /** 요청 성공 여부 */
    private final boolean success;

    /** 응답 데이터 (실패 시 null → JSON에서 제외됨) */
    private final T data;

    /** 응답 메시지 */
    private final String message;

    /** HTTP 상태 코드 (200, 400, 404, 500 등) */
    private final int status;

    /** 에러 코드명 (성공 시 null → JSON에서 제외됨, 실패 시 "RESOURCE_NOT_FOUND" 등) */
    private final String errorCode;

    @Builder
    private ApiResponse(boolean success, T data, String message, int status, String errorCode) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.status = status;
        this.errorCode = errorCode;
    }

    /**
     * 성공 응답 (데이터 + 메시지)
     * 사용 예: return ApiResponse.success(productDto, "상품 조회 성공");
     */
    public static <T> ApiResponse<T> success(T data, String message) {
        return ApiResponse.<T>builder()
                .success(true)
                .data(data)
                .message(message)
                .status(200)
                .build();
    }

    /**
     * 성공 응답 (데이터만)
     * 사용 예: return ApiResponse.success(productDto);
     */
    public static <T> ApiResponse<T> success(T data) {
        return success(data, null);
    }

    /**
     * 성공 응답 (데이터 없음, 생성/삭제 등)
     * 사용 예: return ApiResponse.success();
     */
    public static ApiResponse<Void> success() {
        return ApiResponse.<Void>builder()
                .success(true)
                .status(200)
                .build();
    }

    /**
     * 실패 응답 (ErrorCode enum 기반)
     * GlobalExceptionHandler에서 사용한다.
     * 사용 예: return ApiResponse.fail(ErrorCode.RESOURCE_NOT_FOUND);
     */
    public static ApiResponse<Void> fail(ErrorCode code) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(code.getMessage())
                .status(code.getStatus().value())
                .errorCode(code.name())
                .build();
    }

    /**
     * 실패 응답 (ErrorCode + 커스텀 메시지)
     * Validation 에러처럼 기본 메시지 대신 상세 메시지를 전달할 때 사용한다.
     * 사용 예: return ApiResponse.fail(ErrorCode.INVALID_INPUT, "name: 필수 입력입니다");
     */
    public static ApiResponse<Void> fail(ErrorCode code, String message) {
        return ApiResponse.<Void>builder()
                .success(false)
                .message(message)
                .status(code.getStatus().value())
                .errorCode(code.name())
                .build();
    }
}
