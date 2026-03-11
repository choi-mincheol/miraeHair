package com.mirae.hair.domain.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JWT 토큰 생성/검증/파싱을 담당하는 핵심 컴포넌트
 *
 * JWT(JSON Web Token)란?
 * → 서버가 클라이언트에게 발급하는 "인증서" 같은 것.
 * → 이 토큰 안에 "이 사람은 admin@mirae.com이고, ROLE_ADMIN 권한이 있다"는 정보가 담겨 있다.
 * → 서버는 이 토큰의 서명을 확인하면 DB 조회 없이도 사용자를 인증할 수 있다.
 *
 * 왜 세션 대신 JWT를 쓰는가?
 * → 세션: 서버 메모리에 로그인 정보를 저장 → 서버가 여러 대면 세션 동기화가 필요
 * → JWT: 토큰 자체에 정보가 담겨 있어 서버에 저장할 필요 없음 (Stateless)
 * → 서버 확장(Scale-out)이 쉽고, MSA 환경에 적합하다.
 *
 * JWT 구조: Header.Payload.Signature
 * → Header: 알고리즘 정보 (HS256)
 * → Payload: 사용자 정보 (email, role, 만료시간)
 * → Signature: Header + Payload를 Secret Key로 서명 (위변조 방지)
 */
@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    /**
     * 왜 생성자에서 SecretKey를 만드는가?
     * → @Value로 String을 주입받아 SecretKey 객체로 변환한다.
     * → 매번 토큰 생성/검증 시마다 변환하면 비효율적이므로, 한 번만 만들어 재사용한다.
     * → Keys.hmacShaKeyFor()는 HMAC-SHA256에 적합한 키를 생성한다.
     */
    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration}") long accessTokenExpiration,
            @Value("${jwt.refresh-token-expiration}") long refreshTokenExpiration
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiration = accessTokenExpiration;
        this.refreshTokenExpiration = refreshTokenExpiration;
    }

    /** AccessToken 생성 (30분) */
    public String createAccessToken(String email, String role) {
        return createToken(email, role, accessTokenExpiration);
    }

    /** RefreshToken 생성 (7일) */
    public String createRefreshToken(String email, String role) {
        return createToken(email, role, refreshTokenExpiration);
    }

    /**
     * JWT 토큰 생성 공통 메서드
     *
     * subject: 토큰의 주인 (email)
     * claim("role"): 추가 정보 (권한)
     * issuedAt: 발급 시각
     * expiration: 만료 시각
     * signWith: Secret Key로 서명 → 위변조 시 검증 실패
     */
    private String createToken(String email, String role, long expiration) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .subject(email)
                .claim("role", role)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(secretKey)
                .compact();
    }

    /**
     * 토큰 유효성 검증
     *
     * 검증하는 것들:
     * 1) 서명이 올바른지 (Secret Key로 검증)
     * 2) 만료되지 않았는지
     * 3) 형식이 올바른지
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (ExpiredJwtException e) {
            log.warn("만료된 JWT 토큰: {}", e.getMessage());
        } catch (JwtException e) {
            log.warn("유효하지 않은 JWT 토큰: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 토큰에서 Authentication 객체 추출
     *
     * 왜 Authentication 객체를 만드는가?
     * → Spring Security는 SecurityContext에 Authentication 객체가 있어야 "인증된 사용자"로 인식한다.
     * → JWT에서 email과 role을 꺼내서 Authentication 객체를 만들고,
     *   SecurityContextHolder에 저장하면 Spring Security가 인증 완료로 처리한다.
     *
     * UserDetails: Spring Security가 사용자 정보를 담는 표준 인터페이스
     * UsernamePasswordAuthenticationToken: 인증 완료된 Authentication 구현체
     */
    public Authentication getAuthentication(String token) {
        Claims claims = parseClaims(token);
        String email = claims.getSubject();
        String role = claims.get("role", String.class);

        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        UserDetails principal = new User(email, "", authorities);

        return new UsernamePasswordAuthenticationToken(principal, token, authorities);
    }

    /** 토큰에서 이메일(subject) 추출 */
    public String getEmail(String token) {
        return parseClaims(token).getSubject();
    }

    /** 토큰의 Payload(Claims) 파싱 */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
