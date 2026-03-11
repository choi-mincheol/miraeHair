package com.mirae.hair.domain.auth.controller;

import com.mirae.hair.domain.auth.dto.LoginRequest;
import com.mirae.hair.domain.auth.dto.ReissueRequest;
import com.mirae.hair.domain.auth.dto.SignupRequest;
import com.mirae.hair.domain.auth.dto.TokenResponse;
import com.mirae.hair.domain.auth.service.AuthService;
import com.mirae.hair.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API 컨트롤러
 *
 * 모든 인증 관련 API의 진입점이다.
 * SecurityConfig에서 /api/auth/** 패턴을 permitAll()로 설정했으므로
 * 이 컨트롤러의 API들은 인증 없이 접근할 수 있다.
 *
 * 왜 @RestController인가?
 * → @Controller + @ResponseBody의 합체 버전.
 * → 모든 메서드의 반환값이 자동으로 JSON으로 변환된다.
 * → HTML 뷰가 아닌 REST API를 만들 때 사용한다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "인증", description = "로그인/회원가입/토큰 재발급 API")
public class AuthController {

    private final AuthService authService;

    /**
     * 회원가입 API
     *
     * @Valid: 요청 DTO의 Bean Validation 어노테이션(@NotBlank, @Email 등)을 실행한다.
     * → 검증 실패 시 GlobalExceptionHandler의 handleMethodArgumentNotValid()로 넘어간다.
     *
     * 왜 ResponseEntity.status(CREATED)인가?
     * → HTTP 201(Created)은 "새로운 리소스가 생성되었다"는 의미이다.
     * → 단순 성공(200)과 구분하여 RESTful API 설계 원칙을 따른다.
     */
    @PostMapping("/signup")
    @Operation(summary = "회원가입", description = "새로운 관리자 계정을 등록합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이메일 중복")
    public ResponseEntity<ApiResponse<Long>> signup(@Valid @RequestBody SignupRequest request) {
        Long memberId = authService.signup(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(memberId, "회원가입이 완료되었습니다"));
    }

    /**
     * 로그인 API
     */
    @PostMapping("/login")
    @Operation(summary = "로그인", description = "이메일과 비밀번호로 로그인하여 JWT 토큰을 발급받습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "이메일 또는 비밀번호 오류")
    public ResponseEntity<ApiResponse<TokenResponse>> login(@Valid @RequestBody LoginRequest request) {
        TokenResponse tokenResponse = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "로그인 성공"));
    }

    /**
     * 토큰 재발급 API
     */
    @PostMapping("/reissue")
    @Operation(summary = "토큰 재발급", description = "RefreshToken으로 새 AccessToken을 발급받습니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 재발급 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "유효하지 않은 RefreshToken")
    public ResponseEntity<ApiResponse<TokenResponse>> reissue(@Valid @RequestBody ReissueRequest request) {
        TokenResponse tokenResponse = authService.reissue(request);
        return ResponseEntity.ok(ApiResponse.success(tokenResponse, "토큰 재발급 성공"));
    }
}
