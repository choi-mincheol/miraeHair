package com.mirae.hair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

/**
 * 애플리케이션 메인 클래스
 *
 * 왜 Security 자동 설정을 exclude 하는가?
 * → spring-boot-starter-security 의존성을 추가하면,
 *   Spring Boot가 자동으로 모든 API에 인증을 요구한다.
 *   (로그인 페이지가 뜨고, 기본 비밀번호가 콘솔에 출력됨)
 * → feature/02-security-jwt에서 직접 SecurityConfig를 작성할 예정이므로,
 *   그때까지는 자동 설정을 비활성화한다.
 * → feature/02 완료 후 이 exclude를 제거하고 SecurityConfig로 대체한다.
 */
@SpringBootApplication(exclude = {
		SecurityAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class
})
public class HairPjtApplication {

	public static void main(String[] args) {
		SpringApplication.run(HairPjtApplication.class, args);
	}

}
