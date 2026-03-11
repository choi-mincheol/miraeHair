package com.mirae.hair.global.config;

import com.mirae.hair.domain.auth.jwt.JwtAccessDeniedHandler;
import com.mirae.hair.domain.auth.jwt.JwtAuthenticationEntryPoint;
import com.mirae.hair.domain.auth.jwt.JwtAuthenticationFilter;
import com.mirae.hair.domain.auth.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Spring Security 설정
 *
 * 왜 Spring Security를 사용하는가?
 * → 인증/인가를 직접 구현하면 (if/else로 토큰 체크, 권한 체크) 코드가 복잡하고 보안 허점이 생기기 쉽다.
 * → Spring Security는 검증된 보안 프레임워크로, 필터 체인 기반으로 보안을 처리한다.
 * → URL별 접근 권한 설정, CORS, CSRF, 보안 헤더 등을 선언적으로 설정할 수 있다.
 *
 * 핵심 설정 3가지:
 * 1) CSRF 비활성화: JWT 사용 시 CSRF 토큰이 불필요 (Stateless라서)
 * 2) 세션 STATELESS: JWT는 서버에 상태를 저장하지 않으므로 세션을 비활성화
 * 3) JwtAuthenticationFilter 등록: UsernamePasswordAuthenticationFilter 앞에 배치
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtTokenProvider jwtTokenProvider;
    private final JwtAccessDeniedHandler jwtAccessDeniedHandler;
    private final JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                // CORS 설정 적용
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                /**
                 * CSRF 비활성화
                 * 왜? → CSRF는 쿠키 기반 인증에서 필요한 방어 기법이다.
                 * → JWT는 쿠키가 아닌 Authorization 헤더로 전달되므로 CSRF 공격 대상이 아니다.
                 * → 따라서 JWT 기반 인증에서는 CSRF를 비활성화해도 안전하다.
                 */
                .csrf(AbstractHttpConfigurer::disable)

                /**
                 * 세션 정책: STATELESS
                 * 왜? → JWT는 토큰 자체에 인증 정보가 담겨 있으므로 서버에 세션을 저장할 필요가 없다.
                 * → STATELESS로 설정하면 Spring Security가 세션을 생성하지 않는다.
                 * → 이로 인해 서버 메모리를 절약하고, 수평 확장(Scale-out)이 쉬워진다.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                /**
                 * 보안 헤더 설정
                 * → X-Frame-Options: DENY → 클릭재킹 공격 방어 (iframe에서 페이지 로드 차단)
                 * → X-Content-Type-Options: nosniff → MIME 스니핑 방어 (Spring Security 기본 포함)
                 */
                .headers(headers -> headers
                        .frameOptions(frame -> frame.deny()))

                // 인증/인가 예외 핸들러
                .exceptionHandling(exception -> exception
                        .accessDeniedHandler(jwtAccessDeniedHandler)
                        .authenticationEntryPoint(jwtAuthenticationEntryPoint))

                /**
                 * URL별 접근 권한 설정
                 * → permitAll(): 인증 없이 접근 가능
                 * → authenticated(): 인증된 사용자만 접근 가능
                 *
                 * 순서가 중요하다!
                 * → 구체적인 URL 패턴을 먼저 선언하고, anyRequest()를 마지막에 둔다.
                 */
                .authorizeHttpRequests(auth -> auth
                        // 인증 없이 접근 가능한 URL
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/error").permitAll()
                        // 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated())

                /**
                 * JWT 인증 필터를 UsernamePasswordAuthenticationFilter 앞에 등록
                 * 왜 앞에 등록하는가?
                 * → Spring Security의 기본 인증 필터(UsernamePasswordAuthenticationFilter)보다
                 *   JWT 필터가 먼저 실행되어야 한다.
                 * → JWT 필터에서 토큰을 검증하고 SecurityContext에 인증 정보를 설정하면,
                 *   이후 Spring Security가 이 정보를 기반으로 권한을 체크한다.
                 */
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtTokenProvider),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * 비밀번호 인코더
     *
     * 왜 BCryptPasswordEncoder인가?
     * → BCrypt는 해시 + 솔트(Salt) 방식으로 같은 비밀번호도 매번 다른 해시를 생성한다.
     * → 레인보우 테이블 공격(미리 계산된 해시 테이블로 원본 추측)을 방어한다.
     * → SHA-256보다 의도적으로 느리게 설계되어 브루트포스 공격에 강하다.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * CORS 설정
     *
     * 왜 SecurityConfig에서 CORS를 설정하는가?
     * → Spring Security의 필터 체인이 Spring MVC보다 먼저 실행된다.
     * → WebConfig에서만 CORS를 설정하면 Security 필터에서 차단될 수 있다.
     * → SecurityConfig에서 CORS를 설정하면 Security 레벨에서 먼저 허용한다.
     *
     * allowCredentials(true) + allowedOriginPatterns("*"):
     * → allowedOrigins("*")는 allowCredentials(true)와 함께 사용할 수 없다.
     * → 대신 allowedOriginPatterns("*")를 사용하면 두 설정을 함께 사용할 수 있다.
     * → 운영 환경에서는 반드시 실제 도메인으로 변경해야 한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("*"));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
