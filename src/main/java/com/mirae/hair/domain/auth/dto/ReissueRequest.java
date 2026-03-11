package com.mirae.hair.domain.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 토큰 재발급 요청 DTO
 *
 * AccessToken이 만료되었을 때, RefreshToken을 보내서 새 토큰을 발급받는다.
 * → 사용자가 다시 로그인하지 않아도 세션이 유지되는 효과를 준다.
 */
@Getter
@NoArgsConstructor
public class ReissueRequest {

    @NotBlank(message = "리프레시 토큰은 필수 입력입니다")
    private String refreshToken;
}
