package com.mirae.hair.global.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * HTTP 요청/응답 로깅 인터셉터
 *
 * 왜 Interceptor로 로깅하는가?
 * → 각 Controller에 log.info("요청 도착: ...") 코드를 넣으면 중복이 발생한다.
 * → Interceptor로 한 곳에서 처리하면 모든 API의 요청/응답을 자동으로 로깅할 수 있다.
 *
 * Filter vs Interceptor:
 * → Filter: Spring 밖(Servlet 레벨)에서 동작, 모든 요청에 적용
 * → Interceptor: Spring 안(MVC 레벨)에서 동작, Controller 호출 전후에 실행
 * → 로깅은 Controller 레벨의 정보(핸들러, 실행 시간)가 필요하므로 Interceptor가 적합하다.
 *
 * 로그 출력 예시:
 * [REQ] GET /api/products (IP: 127.0.0.1)
 * [RES] GET /api/products → 200 (15ms)
 */
@Slf4j
@Component
public class LoggingInterceptor implements HandlerInterceptor {

    private static final String START_TIME = "startTime";

    /**
     * Controller 실행 전에 호출
     * → 요청 정보(메서드, URI, IP)를 로깅하고, 시작 시간을 기록한다.
     */
    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
    ) {
        log.info("[REQ] {} {} (IP: {})",
                request.getMethod(),
                request.getRequestURI(),
                request.getRemoteAddr()
        );
        request.setAttribute(START_TIME, System.currentTimeMillis());
        return true;  // true를 반환해야 Controller로 진입한다
    }

    /**
     * 요청 처리 완료 후 호출 (뷰 렌더링 후)
     * → 응답 상태 코드와 처리 시간을 로깅한다.
     * → 예외 발생 시에도 호출되므로 에러 로깅에도 활용할 수 있다.
     */
    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        long startTime = (Long) request.getAttribute(START_TIME);
        long duration = System.currentTimeMillis() - startTime;

        log.info("[RES] {} {} → {} ({}ms)",
                request.getMethod(),
                request.getRequestURI(),
                response.getStatus(),
                duration
        );
    }
}
