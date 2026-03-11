package com.mirae.hair.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

/**
 * JPA Auditing 설정
 *
 * 왜 JPA Auditing을 쓰는가?
 * → 매번 entity.setCreatedAt(LocalDateTime.now()) 같은 코드를 직접 작성하면:
 *   1) 코드 중복이 발생하고
 *   2) 실수로 빠뜨리면 null이 들어가서 버그가 생긴다.
 * → JPA Auditing을 활성화하면 @CreatedDate, @LastModifiedDate 등이
 *   엔티티 저장/수정 시 자동으로 값을 채워준다.
 *
 * 왜 @EnableJpaAuditing을 별도 Config 클래스에 두는가?
 * → @SpringBootApplication(메인 클래스)에 넣어도 동작하지만,
 *   테스트 시 @DataJpaTest와 충돌이 발생할 수 있다.
 * → 별도 Config 클래스에 두면 테스트에서 필요한 설정만 선택적으로 로드할 수 있다.
 */
@Configuration
@EnableJpaAuditing
public class JpaConfig {

    /**
     * AuditorAware: "현재 작업을 수행하는 사용자가 누구인지" 알려주는 Bean
     *
     * BaseEntity의 @CreatedBy, @LastModifiedBy 값을 채울 때 사용된다.
     *
     * SecurityContext에서 인증된 사용자의 이메일을 가져온다.
     * → 인증 정보가 없으면 (로그인 전, 익명 사용자) "SYSTEM"을 반환한다.
     * → 인증 정보가 있으면 JWT에서 추출한 이메일(subject)을 반환한다.
     */
    @Bean
    public AuditorAware<String> auditorAware() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            // 인증 정보가 없거나, 익명 사용자인 경우 → "SYSTEM"
            if (authentication == null
                    || !authentication.isAuthenticated()
                    || authentication instanceof AnonymousAuthenticationToken) {
                return Optional.of("SYSTEM");
            }

            // 인증된 사용자 → 이메일(JWT subject) 반환
            return Optional.of(authentication.getName());
        };
    }
}
