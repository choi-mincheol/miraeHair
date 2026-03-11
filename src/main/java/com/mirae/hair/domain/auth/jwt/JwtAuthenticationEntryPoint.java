package com.mirae.hair.domain.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirae.hair.global.dto.ApiResponse;
import com.mirae.hair.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인증(Authentication) 실패 시 401 응답을 반환하는 핸들러
 *
 * 언제 호출되는가?
 * → 토큰 없이 인증이 필요한 API에 접근할 때
 * → 만료되거나 유효하지 않은 토큰으로 접근할 때
 *
 * 왜 별도 핸들러가 필요한가?
 * → Spring Security 기본 동작은 로그인 페이지로 redirect하는 것이다.
 * → REST API에서는 redirect가 아닌 JSON 에러 응답을 반환해야 한다.
 * → 이 핸들러가 없으면 브라우저에서 HTML 로그인 페이지가 뜨는 문제가 생긴다.
 */
@Slf4j
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException
    ) throws IOException {
        log.warn("인증 실패 - URI: {}", request.getRequestURI());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Void> apiResponse = ApiResponse.fail(ErrorCode.UNAUTHORIZED);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
