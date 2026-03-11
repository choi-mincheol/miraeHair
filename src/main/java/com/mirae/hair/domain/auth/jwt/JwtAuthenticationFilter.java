package com.mirae.hair.domain.auth.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 매 요청마다 JWT 토큰을 검증하는 필터
 *
 * 동작 흐름:
 * 1) 클라이언트가 "Authorization: Bearer {토큰}" 헤더와 함께 API를 호출
 * 2) 이 필터가 헤더에서 토큰을 추출
 * 3) JwtTokenProvider로 토큰 유효성 검증
 * 4) 유효하면 → Authentication 객체를 SecurityContext에 저장 → Controller 진입
 * 5) 유효하지 않으면 → SecurityContext 비어있음 → Spring Security가 401 반환
 *
 * 왜 OncePerRequestFilter를 상속하는가?
 * → 일반 Filter는 요청이 forward/include될 때 여러 번 실행될 수 있다.
 * → OncePerRequestFilter는 이름 그대로 "요청당 한 번만" 실행되는 것을 보장한다.
 * → JWT 검증은 한 번만 하면 되므로 OncePerRequestFilter가 적합하다.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 실제 필터 로직
     *
     * 토큰이 없거나 유효하지 않으면 SecurityContext가 비어있는 상태로 다음 필터로 넘긴다.
     * → Spring Security가 비어있는 SecurityContext를 보고 "인증되지 않음"으로 판단한다.
     * → 이 필터에서 직접 401을 반환하지 않는 이유:
     *   인증이 필요 없는 URL(로그인, 회원가입)도 이 필터를 거치기 때문이다.
     *   SecurityConfig에서 URL별 권한 설정으로 분기한다.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        // 1) Authorization 헤더에서 토큰 추출
        String token = resolveToken(request);

        // 2) 토큰이 존재하고 유효하면 SecurityContext에 인증 정보 저장
        if (StringUtils.hasText(token) && jwtTokenProvider.validateToken(token)) {
            Authentication authentication = jwtTokenProvider.getAuthentication(token);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("인증 성공: {}", authentication.getName());
        }

        // 3) 다음 필터로 전달
        filterChain.doFilter(request, response);
    }

    /**
     * Authorization 헤더에서 Bearer 토큰을 추출
     *
     * 헤더 형식: "Authorization: Bearer eyJhbGciOi..."
     * → "Bearer " 접두사를 제거하고 순수 토큰 문자열만 반환한다.
     */
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
