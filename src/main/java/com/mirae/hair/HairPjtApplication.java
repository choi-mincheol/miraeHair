package com.mirae.hair;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 애플리케이션 메인 클래스
 *
 * feature/02-security-jwt에서 SecurityConfig를 직접 작성했으므로
 * Security 자동 설정 exclude를 제거하였다.
 * → SecurityConfig.java에서 SecurityFilterChain을 커스텀 설정한다.
 */
@SpringBootApplication
public class HairPjtApplication {

	public static void main(String[] args) {
		SpringApplication.run(HairPjtApplication.class, args);
	}

}
