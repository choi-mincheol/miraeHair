# Feature: 01-project-init (프로젝트 초기 설정)

## 1. 유저 스토리

> 개발자로서, 프로젝트의 공통 기반 코드를 구축하고 싶다. 그래서 이후 모든 feature를 일관된 구조 위에서 빠르게 개발할 수 있다.

- US-01: 개발자로서, DDD 패키지 구조를 잡고 싶다. 그래서 도메인별로 코드를 분리하여 유지보수할 수 있다.
- US-02: 개발자로서, JPA + MyBatis 설정을 완료하고 싶다. 그래서 CQRS 패턴을 적용할 준비가 된다.
- US-03: 개발자로서, 공통 응답 래퍼(ApiResponse)를 만들고 싶다. 그래서 모든 API가 동일한 응답 형식을 갖는다.
- US-04: 개발자로서, 전역 예외 처리 체계를 구축하고 싶다. 그래서 예외 발생 시 일관된 에러 응답을 반환한다.
- US-05: 개발자로서, BaseEntity를 만들고 싶다. 그래서 모든 엔티티에 생성일/수정일이 자동으로 기록된다.
- US-06: 개발자로서, Swagger(OpenAPI)를 설정하고 싶다. 그래서 API 문서가 자동 생성된다.

## 2. 기능 요구사항 (Functional Requirements)

| ID | 기능 | 설명 | 우선순위 |
|----|------|------|----------|
| FR-01 | build.gradle 의존성 추가 | JPA, MyBatis, Swagger, Validation, PostgreSQL, Redis, JWT, Security 등 | 높음 |
| FR-02 | application.yml 설정 | 공통 설정 + dev 프로필 분리 | 높음 |
| FR-03 | DDD 패키지 구조 생성 | global, domain(auth/product/customer/order), infra 패키지 | 높음 |
| FR-04 | BaseEntity 구현 | createdAt, updatedAt, createdBy, updatedBy + JPA Auditing | 높음 |
| FR-05 | ApiResponse\<T\> 구현 | success/fail 정적 팩토리 메서드, HTTP 상태 코드 포함 | 높음 |
| FR-06 | ErrorCode enum 구현 | HTTP 상태 + 에러 코드 + 메시지 관리 | 높음 |
| FR-07 | ErrorResponse 구현 | 에러 응답 DTO | 높음 |
| FR-08 | GlobalExceptionHandler 구현 | @RestControllerAdvice 기반 전역 예외 처리 | 높음 |
| FR-09 | SwaggerConfig 구현 | OpenAPI 3.0 설정, JWT 인증 헤더 포함 | 중간 |
| FR-10 | JpaConfig 구현 | JPA Auditing 활성화 | 높음 |
| FR-11 | MyBatisConfig 구현 | Mapper 스캔 설정 | 높음 |

## 3. 비기능 요구사항 (Non-Functional Requirements)

- NFR-01: 모든 API 응답은 `ApiResponse<T>` 형식으로 통일
- NFR-02: 예외 발생 시 `ErrorResponse` 형식으로 반환 (스택 트레이스 노출 금지)
- NFR-03: application.yml에 민감 정보(DB 비밀번호 등)는 환경변수로 분리
- NFR-04: UTF-8 인코딩 통일

## 4. 생성할 파일 목록

### 4-1. 설정 파일
| 파일 | 설명 |
|------|------|
| `build.gradle` | 의존성 추가 (기존 파일 수정) |
| `src/main/resources/application.yml` | 공통 설정 (기존 yaml → yml 변경) |
| `src/main/resources/application-dev.yml` | 개발 환경 설정 (PostgreSQL local) |

### 4-2. 전역 공통 (global/)
| 파일 | 설명 |
|------|------|
| `global/config/JpaConfig.java` | JPA Auditing 활성화 |
| `global/config/MyBatisConfig.java` | MyBatis Mapper 스캔 설정 |
| `global/config/SwaggerConfig.java` | OpenAPI 3.0 + JWT 인증 설정 |
| `global/config/WebConfig.java` | CORS, Interceptor 등록 |
| `global/entity/BaseEntity.java` | 생성일/수정일/생성자/수정자 자동 기록 |
| `global/dto/ApiResponse.java` | 공통 응답 래퍼 |
| `global/exception/ErrorCode.java` | 에러 코드 enum |
| `global/exception/ErrorResponse.java` | 에러 응답 DTO |
| `global/exception/BusinessException.java` | 비즈니스 예외 커스텀 클래스 |
| `global/exception/GlobalExceptionHandler.java` | 전역 예외 처리 핸들러 |

### 4-3. 도메인 패키지 (빈 구조만 생성)
```
domain/auth/controller, service, domain, repository, dto, jwt
domain/product/command, query, controller, domain, dto
domain/customer/command, query, controller, domain, dto
domain/order/command, query, controller, domain, dto
```

### 4-4. 인프라 패키지 (빈 구조만 생성)
```
infra/redis
infra/file
```

## 5. 핵심 설계

### 5-1. ApiResponse\<T\> 구조
```java
{
  "success": true,           // 성공 여부
  "data": { ... },           // 응답 데이터 (실패 시 null)
  "message": "조회 성공",     // 메시지
  "code": 200                // HTTP 상태 코드
}
```

### 5-2. ErrorResponse 구조 (실패 시)
```java
{
  "success": false,
  "code": "PRODUCT_NOT_FOUND",  // ErrorCode enum name
  "message": "상품을 찾을 수 없습니다",
  "status": 404
}
```

### 5-3. ErrorCode enum 초기 목록
| 코드 | HTTP 상태 | 메시지 |
|------|-----------|--------|
| INVALID_INPUT | 400 | 입력값이 올바르지 않습니다 |
| UNAUTHORIZED | 401 | 인증이 필요합니다 |
| FORBIDDEN | 403 | 접근 권한이 없습니다 |
| RESOURCE_NOT_FOUND | 404 | 리소스를 찾을 수 없습니다 |
| INTERNAL_SERVER_ERROR | 500 | 서버 내부 오류가 발생했습니다 |

> 도메인별 에러 코드(PRODUCT_NOT_FOUND 등)는 해당 feature에서 추가 예정

### 5-4. BaseEntity 필드
| 필드 | 타입 | 어노테이션 | 설명 |
|------|------|------------|------|
| createdAt | LocalDateTime | @CreatedDate | 생성일시 |
| updatedAt | LocalDateTime | @LastModifiedDate | 수정일시 |
| createdBy | String | @CreatedBy | 생성자 |
| updatedBy | String | @LastModifiedBy | 수정자 |

## 6. 불명확한 사항 (Clarifications Needed)

- **Q1**: `application.yaml`이 이미 존재합니다. `application.yml`로 변경(리네임)해도 괜찮을까요?
  → Spring Boot는 yaml/yml 둘 다 인식하지만, CLAUDE.md 규칙에는 `.yml`로 명시되어 있습니다.

- **Q2**: BaseEntity의 `createdBy`/`updatedBy`는 JWT 인증이 아직 없는 상태에서 기본값을 `"SYSTEM"`으로 넣어도 될까요?
  → feature/02-security-jwt에서 AuditorAware를 구현하면 그때 실제 사용자 정보로 교체됩니다.

- **Q3**: 도메인 패키지에 빈 디렉토리를 만들면 Git에서 추적되지 않습니다. 각 패키지에 `package-info.java`를 넣어서 패키지를 유지할까요, 아니면 해당 feature 구현 시 만들까요?

- **Q4**: Security, Redis 의존성은 이번에 build.gradle에 추가만 하고, 설정은 각 feature에서 진행하는 게 맞을까요? (Security 자동 설정 때문에 임시로 `@SpringBootApplication(exclude = ...)` 처리가 필요할 수 있습니다)

## 7. 의존성

- **선행 feature**: 없음 (첫 번째 feature)
- **후행 feature**: 모든 feature가 이 초기 설정에 의존함
