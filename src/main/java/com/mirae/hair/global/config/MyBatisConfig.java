package com.mirae.hair.global.config;

import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 설정
 *
 * 왜 JPA와 MyBatis를 같이 쓰는가? (CQRS 패턴)
 * → JPA는 객체 중심이라 단순한 CRUD(생성/수정/삭제)에 강하다.
 * → 하지만 복잡한 조회(JOIN, 서브쿼리, 통계 등)는 JPA로 작성하면 코드가 복잡해진다.
 * → MyBatis는 SQL을 직접 작성하므로 복잡한 조회에 적합하다.
 *
 * CQRS 패턴 적용:
 * → Command(생성/수정/삭제): JPA Repository 사용
 * → Query(조회): MyBatis Mapper 사용
 * → 이렇게 분리하면 각각의 장점을 살릴 수 있다.
 *
 * @MapperScan: 지정된 패키지 하위에서 @Mapper가 붙은 인터페이스를 찾아서
 *              MyBatis Mapper로 등록한다.
 *              각 도메인의 query 패키지에 Mapper 인터페이스를 작성하면 자동으로 인식된다.
 */
@Configuration
@MapperScan(
        basePackages = "com.mirae.hair.domain",
        annotationClass = Mapper.class
)
public class MyBatisConfig {
}
