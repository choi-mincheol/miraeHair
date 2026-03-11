package com.mirae.hair.domain.auth.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mirae.hair.global.dto.ApiResponse;
import com.mirae.hair.global.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * 인가(Authorization) 실패 시 403 응답을 반환하는 핸들러
 *
 * 언제 호출되는가?
 * → 인증은 되었지만(토큰은 유효) 해당 API에 대한 권한이 없을 때
 * → 예: ROLE_CUSTOMER가 관리자 전용 API에 접근할 때
 *
 * 인증(Authentication) vs 인가(Authorization):
 * → 인증: "너 누구야?" → 401 (JwtAuthenticationEntryPoint)
 * → 인가: "너 이거 할 수 있어?" → 403 (이 핸들러)
 */
@Slf4j
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException
    ) throws IOException {
        log.warn("접근 권한 없음 - URI: {}", request.getRequestURI());

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ApiResponse<Void> apiResponse = ApiResponse.fail(ErrorCode.FORBIDDEN);
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}
