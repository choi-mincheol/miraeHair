package com.mirae.hair.global.config;

import com.mirae.hair.global.interceptor.LoggingInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정
 *
 * CORS 설정은 SecurityConfig에서 관리한다.
 * → Spring Security의 필터 체인이 Spring MVC보다 먼저 실행되므로,
 *   SecurityConfig에서 CORS를 설정해야 Security 레벨에서 허용된다.
 *
 * 이 클래스에서는 Interceptor 등록 등 MVC 레벨 설정만 담당한다.
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final LoggingInterceptor loggingInterceptor;

    /**
     * Interceptor 등록
     *
     * LoggingInterceptor를 모든 API 요청에 적용한다.
     * → Swagger UI 경로는 제외한다 (불필요한 로그 방지).
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loggingInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/swagger-resources/**"
                );
    }
}
