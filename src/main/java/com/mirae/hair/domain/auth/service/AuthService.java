package com.mirae.hair.domain.auth.service;

import com.mirae.hair.domain.auth.domain.Member;
import com.mirae.hair.domain.auth.dto.LoginRequest;
import com.mirae.hair.domain.auth.dto.ReissueRequest;
import com.mirae.hair.domain.auth.dto.SignupRequest;
import com.mirae.hair.domain.auth.dto.TokenResponse;
import com.mirae.hair.domain.auth.jwt.JwtTokenProvider;
import com.mirae.hair.domain.auth.repository.MemberRepository;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 비즈니스 로직 서비스
 *
 * 왜 @Transactional(readOnly = true)를 클래스 레벨에 거는가?
 * → 이 서비스의 대부분 메서드가 조회(login, reissue)이므로 기본값을 readOnly = true로 설정한다.
 * → readOnly = true이면:
 *   1) JPA 더티체킹(변경 감지)을 생략해서 성능이 향상된다.
 *   2) 실수로 데이터를 수정하는 것을 방지한다.
 * → 데이터를 변경하는 메서드(signup)에만 @Transactional을 별도로 붙인다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     *
     * 왜 @Transactional(readOnly = true가 아닌 기본)을 붙이는가?
     * → 이 메서드는 DB에 새로운 Member를 INSERT한다.
     * → readOnly = true이면 데이터 변경이 차단되므로, 쓰기용 @Transactional이 필요하다.
     * → 클래스 레벨의 readOnly = true를 이 메서드에서만 오버라이드한다.
     */
    @Transactional
    public Long signup(SignupRequest request) {
        // 이메일 중복 체크
        if (memberRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.EMAIL_ALREADY_EXISTS);
        }

        /**
         * 왜 passwordEncoder.encode()로 비밀번호를 변환하는가?
         * → 평문 비밀번호("Admin1234!")를 그대로 DB에 저장하면,
         *   DB가 유출되었을 때 모든 사용자의 비밀번호가 노출된다.
         * → BCrypt로 해시하면 "$2a$10$..." 형태의 복원 불가능한 문자열이 저장된다.
         * → 같은 비밀번호도 매번 다른 해시가 생성된다 (Salt 때문).
         */
        Member member = Member.create(
                request.getEmail(),
                passwordEncoder.encode(request.getPassword()),
                request.getName()
        );

        return memberRepository.save(member).getId();
    }

    /**
     * 로그인
     *
     * 보안 주의: "이메일이 없다"와 "비밀번호가 틀렸다"를 구분하지 않고
     * 동일한 에러 메시지(LOGIN_FAILED)를 반환한다.
     * 왜? → 공격자에게 "이 이메일은 존재한다"는 정보를 주지 않기 위해서다.
     * → 이를 "에러 메시지 통일(Error Message Normalization)"이라 한다.
     */
    @Transactional
    public TokenResponse login(LoginRequest request) {
        // 이메일로 회원 조회 (삭제되지 않은 회원만)
        Member member = memberRepository.findByEmailAndDeletedFalse(request.getEmail())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        // 비밀번호 검증 (평문 입력 vs DB의 BCrypt 해시 비교)
        if (!passwordEncoder.matches(request.getPassword(), member.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        // 마지막 로그인 시각 업데이트 (JPA 더티체킹으로 자동 반영)
        member.updateLastLogin();

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                member.getEmail(), member.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(
                member.getEmail(), member.getRole().name());

        log.info("로그인 성공: {}", member.getEmail());
        return TokenResponse.of(accessToken, refreshToken);
    }

    /**
     * 토큰 재발급
     *
     * RefreshToken의 서명을 검증하여 유효하면 새 토큰 세트를 발급한다.
     * → 서버에 RefreshToken을 저장하지 않고 서명 기반으로만 검증한다 (Stateless).
     * → 장점: 서버 저장소(DB/Redis) 불필요, 구현이 단순
     * → 단점: 토큰을 강제 만료시킬 수 없음 (로그아웃 시 블랙리스트 필요 → feature/06에서 Redis로 구현 가능)
     */
    public TokenResponse reissue(ReissueRequest request) {
        String refreshToken = request.getRefreshToken();

        // RefreshToken 유효성 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        // 토큰에서 이메일 추출 후 회원 존재 여부 확인
        String email = jwtTokenProvider.getEmail(refreshToken);
        Member member = memberRepository.findByEmailAndDeletedFalse(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));

        // 새 토큰 발급
        String newAccessToken = jwtTokenProvider.createAccessToken(
                member.getEmail(), member.getRole().name());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(
                member.getEmail(), member.getRole().name());

        log.info("토큰 재발급 성공: {}", email);
        return TokenResponse.of(newAccessToken, newRefreshToken);
    }
}
