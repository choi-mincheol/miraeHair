package com.mirae.hair.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리 핸들러
 *
 * 왜 GlobalExceptionHandler가 필요한가?
 * → 예외 처리를 각 Controller에서 하면 try-catch 코드가 중복되고, 응답 형식도 제각각이 된다.
 * → @RestControllerAdvice를 사용하면 모든 Controller에서 발생하는 예외를 한 곳에서 처리할 수 있다.
 * → "예외가 발생하면 이렇게 응답해라"를 한 번만 정의하면 끝이다.
 *
 * 왜 @RestControllerAdvice인가? (@ControllerAdvice와 차이)
 * → @ControllerAdvice: 뷰(HTML)를 반환할 때 사용
 * → @RestControllerAdvice: JSON을 반환할 때 사용 (@ControllerAdvice + @ResponseBody)
 * → 우리는 REST API이므로 @RestControllerAdvice를 사용한다.
 *
 * 동작 흐름:
 * 1) Service에서 throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
 * 2) GlobalExceptionHandler가 자동으로 잡아서
 * 3) ErrorResponse 형식으로 변환 + 적절한 HTTP 상태 코드와 함께 반환
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 비즈니스 예외 처리
     * BusinessException이 발생하면 이 메서드가 자동으로 호출된다.
     *
     * 예: throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND)
     * → HTTP 404 + {"success": false, "code": "RESOURCE_NOT_FOUND", ...}
     */
    @ExceptionHandler(BusinessException.class)
    protected ResponseEntity<ErrorResponse> handleBusinessException(BusinessException e) {
        log.error("BusinessException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        ErrorResponse response = ErrorResponse.from(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }

    /**
     * @Valid 유효성 검증 실패 시 처리
     * 요청 DTO에 @NotBlank, @Min 등의 제약 조건을 위반하면 이 메서드가 호출된다.
     *
     * 예: ProductCreateRequest의 name이 빈 문자열이면
     * → HTTP 400 + {"success": false, "code": "INVALID_INPUT", "message": "상품명은 필수입니다", ...}
     *
     * 왜 필드별 에러 메시지를 모아서 반환하는가?
     * → 프론트엔드에서 어떤 필드가 잘못됐는지 한눈에 보여줄 수 있다.
     * → "이름이 비었습니다, 가격은 0 이상이어야 합니다" 같은 메시지를 한 번에 전달한다.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    protected ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        log.error("Validation Exception: {}", e.getMessage());

        // 모든 필드 에러 메시지를 모아서 하나의 문자열로 합친다
        String errorMessage = e.getBindingResult()
                .getFieldErrors()
                .stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        ErrorResponse response = ErrorResponse.builder()
                .success(false)
                .code(ErrorCode.INVALID_INPUT.name())
                .message(errorMessage)
                .status(ErrorCode.INVALID_INPUT.getStatus().value())
                .build();

        return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(response);
    }

    /**
     * 그 외 모든 예외 처리 (최후의 안전망)
     *
     * 왜 이 핸들러가 필요한가?
     * → BusinessException이나 Validation 예외가 아닌 예상치 못한 에러가 발생할 수 있다.
     *   (예: NullPointerException, DB 연결 실패 등)
     * → 이 핸들러가 없으면 Spring 기본 에러 페이지(Whitelabel Error Page)가 나온다.
     * → 이 핸들러가 있으면 항상 우리가 정한 형식(ErrorResponse)으로 응답한다.
     *
     * 주의: 운영 환경에서는 e.getMessage()에 민감한 정보가 포함될 수 있으므로
     * 고정 메시지("서버 내부 오류")를 반환하고, 상세 내용은 로그에만 기록한다.
     */
    @ExceptionHandler(Exception.class)
    protected ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Unhandled Exception: ", e);
        ErrorCode errorCode = ErrorCode.INTERNAL_SERVER_ERROR;
        ErrorResponse response = ErrorResponse.from(errorCode);
        return ResponseEntity.status(errorCode.getStatus()).body(response);
    }
}
