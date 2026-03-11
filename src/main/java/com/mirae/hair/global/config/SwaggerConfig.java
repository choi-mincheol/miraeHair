package com.mirae.hair.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger (OpenAPI 3.0) 설정
 *
 * 왜 Swagger를 쓰는가?
 * → API 문서를 수동으로 작성하면 코드와 문서가 불일치하는 문제가 발생한다.
 *   (API를 수정했는데 문서를 안 고치면 프론트엔드 개발자가 혼란스럽다)
 * → Swagger를 쓰면 코드(Controller)에서 API 문서가 자동 생성된다.
 * → http://localhost:8080/swagger-ui.html 에서 바로 확인 + 테스트 가능하다.
 *
 * JWT 인증 설정을 포함하는 이유:
 * → feature/02-security-jwt에서 JWT 인증이 추가되면,
 *   Swagger에서 API를 테스트할 때 "Authorize" 버튼으로 토큰을 입력할 수 있어야 한다.
 * → 미리 SecurityScheme을 설정해두면 나중에 바로 사용할 수 있다.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI openAPI() {
        // JWT Bearer 인증 방식 정의
        String securitySchemeName = "JWT";

        return new OpenAPI()
                // API 기본 정보
                .info(new Info()
                        .title("미래헤어 API")
                        .version("1.0.0")
                        .description("미용 제품 판매 관리 시스템 API 문서"))
                // 모든 API에 JWT 인증 적용 (Swagger UI에서 "Authorize" 버튼 활성화)
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                // JWT 인증 스키마 정의
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT 토큰을 입력하세요 (Bearer 접두사 불필요)")));
    }
}
