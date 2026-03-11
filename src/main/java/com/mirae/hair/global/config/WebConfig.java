package com.mirae.hair.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 웹 MVC 설정
 *
 * 왜 CORS 설정이 필요한가?
 * → 브라우저는 보안 정책(Same-Origin Policy)에 의해
 *   다른 도메인에서 오는 요청을 기본적으로 차단한다.
 * → 예: 프론트엔드(localhost:3000)에서 백엔드(localhost:8080)로 요청하면 차단됨.
 * → CORS를 설정하면 허용된 도메인에서의 요청을 받아들일 수 있다.
 *
 * 현재는 개발 편의를 위해 모든 도메인("*")을 허용한다.
 * 운영 환경에서는 실제 프론트엔드 도메인만 허용하도록 변경해야 한다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")                     // 모든 URL 패턴에 대해
                .allowedOrigins("*")                    // 모든 도메인 허용 (운영 시 변경 필요)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")  // 허용할 HTTP 메서드
                .allowedHeaders("*");                   // 모든 헤더 허용
    }
}
