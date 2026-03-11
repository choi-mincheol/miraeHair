package com.mirae.hair.global.exception;

import lombok.Getter;

/**
 * 비즈니스 로직에서 발생하는 커스텀 예외 클래스
 *
 * 왜 BusinessException이 필요한가?
 * → "상품을 찾을 수 없음", "재고 부족", "중복 이메일" 같은 비즈니스 예외를
 *   Java의 기본 예외(IllegalArgumentException 등)로 던지면,
 *   어떤 에러인지 구분하기 어렵고, 에러 코드도 전달할 수 없다.
 * → BusinessException에 ErrorCode를 담아 던지면,
 *   GlobalExceptionHandler가 ErrorCode를 꺼내서 적절한 HTTP 응답을 만들 수 있다.
 *
 * 사용 예시:
 *   throw new BusinessException(ErrorCode.RESOURCE_NOT_FOUND);
 *   → GlobalExceptionHandler가 잡아서 404 응답을 반환한다.
 *
 * 대안: 도메인마다 별도 예외 클래스를 만드는 방법
 * → ProductNotFoundException, CustomerNotFoundException... 클래스가 계속 늘어난다.
 * → BusinessException + ErrorCode enum 조합이면 하나의 클래스로 모든 비즈니스 예외를 처리할 수 있다.
 * → 새로운 에러가 필요하면 ErrorCode enum에 값만 추가하면 된다.
 */
@Getter
public class BusinessException extends RuntimeException {

    /** 이 예외에 해당하는 에러 코드 */
    private final ErrorCode errorCode;

    /**
     * 왜 RuntimeException을 상속하는가?
     * → Checked Exception(Exception)을 상속하면 모든 호출부에서 try-catch가 필요하다.
     * → RuntimeException(Unchecked)을 상속하면 try-catch 없이 던질 수 있고,
     *   GlobalExceptionHandler가 전역에서 한 번에 처리한다.
     * → Spring의 @Transactional도 기본적으로 RuntimeException만 롤백한다.
     */
    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }
}
