package com.mirae.hair.global.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;

/**
 * 모든 API의 공통 응답 래퍼
 *
 * 왜 ApiResponse로 감싸는가?
 * → API마다 응답 형식이 다르면, 프론트엔드에서 매번 다른 파싱 로직을 작성해야 한다.
 * → 모든 API가 동일한 형식({success, data, message, code})으로 응답하면
 *   프론트엔드는 하나의 공통 로직으로 처리할 수 있다.
 *
 * 왜 @Builder + private 생성자인가?
 * → new ApiResponse(true, data, "성공", 200) 같이 쓰면
 *   파라미터 순서를 헷갈려서 버그가 생길 수 있다 (true와 200의 위치를 바꾸면?)
 * → 정적 팩토리 메서드(success(), fail())를 사용하면
 *   ApiResponse.success(data) 한 줄로 명확하게 의도를 표현할 수 있다.
 *
 * 대안: ResponseEntity<T>를 직접 반환하는 방법도 있지만,
 * → success/code/message 같은 메타 정보를 매번 Controller에서 세팅해야 해서 번거롭다.
 * → ApiResponse로 한 번 감싸면 Controller 코드가 깔끔해진다.
 *
 * @param <T> 응답 데이터의 타입 (제네릭)
 */
@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)  // null인 필드는 JSON에서 제외
public class ApiResponse<T> {

    /** 요청 성공 여부 */
    private final boolean success;

    /** 응답 데이터 (실패 시 null) */
    private final T data;

    /** 응답 메시지 */
    private final String message;

    /** HTTP 상태 코드 */
    private final int code;

    @Builder
    private ApiResponse(boolean success, T data, String message, int code) {
        this.success = success;
        this.data = data;
        this.message = message;
        this.code = code;
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
                .code(200)
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
                .code(200)
                .build();
    }

    /**
     * 실패 응답
     * 사용 예: return ApiResponse.fail(400, "입력값이 올바르지 않습니다");
     */
    public static <T> ApiResponse<T> fail(int code, String message) {
        return ApiResponse.<T>builder()
                .success(false)
                .message(message)
                .code(code)
                .build();
    }
}
