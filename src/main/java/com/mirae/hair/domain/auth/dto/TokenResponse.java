package com.mirae.hair.domain.auth.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * 토큰 응답 DTO
 *
 * 로그인 성공 시 클라이언트에게 반환하는 토큰 정보.
 *
 * 왜 AccessToken과 RefreshToken을 분리하는가?
 * → AccessToken: 짧은 만료 시간(30분), API 호출 시 사용
 * → RefreshToken: 긴 만료 시간(7일), AccessToken 갱신용
 * → 만약 AccessToken만 있고 만료 시간이 길면,
 *   토큰이 탈취되었을 때 오랫동안 악용될 수 있다.
 * → 짧은 AccessToken + 긴 RefreshToken 조합이면,
 *   AccessToken 탈취 시 30분만 유효하고, RefreshToken은 노출 빈도가 낮다.
 */
@Getter
@Builder
public class TokenResponse {

    /** API 호출 시 Authorization 헤더에 담는 토큰 */
    private final String accessToken;

    /** AccessToken 만료 시 갱신용 토큰 */
    private final String refreshToken;

    /** 토큰 타입 (항상 "Bearer") */
    private final String tokenType;

    /**
     * 정적 팩토리 메서드
     * → TokenResponse.of(accessToken, refreshToken) 으로 깔끔하게 생성
     */
    public static TokenResponse of(String accessToken, String refreshToken) {
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .build();
    }
}
