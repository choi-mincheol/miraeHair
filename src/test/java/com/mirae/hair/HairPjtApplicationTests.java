package com.mirae.hair;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 애플리케이션 컨텍스트 로딩 테스트
 *
 * 왜 @ActiveProfiles("test")가 필요한가?
 * → 기본 프로필(dev)은 PostgreSQL + Redis 연결이 필요하다.
 * → 테스트 환경에서는 외부 인프라 없이 실행되어야 한다.
 * → "test" 프로필은 H2 인메모리 DB를 사용하고, Redis를 비활성화한다.
 * → 이렇게 하면 로컬에 PostgreSQL/Redis가 없어도 테스트가 성공한다.
 */
@SpringBootTest
@ActiveProfiles("test")
class HairPjtApplicationTests {

	@Test
	void contextLoads() {
	}

}
