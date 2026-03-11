# Feature: 02-security-jwt (Spring Security + JWT 인증/인가)

## 1. 유저 스토리

> 관리자로서, 로그인하여 인증된 상태에서만 시스템을 사용하고 싶다. 그래서 비인가 접근으로부터 데이터를 보호할 수 있다.

- US-01: 관리자로서, 이메일/비밀번호로 로그인하고 싶다. 그래서 JWT 토큰을 발급받아 API에 접근할 수 있다.
- US-02: 관리자로서, 새로운 관리자 계정을 등록하고 싶다. 그래서 다른 관리자도 시스템을 사용할 수 있다.
- US-03: 관리자로서, 토큰이 만료되면 갱신하고 싶다. 그래서 다시 로그인하지 않아도 된다.
- US-04: 시스템으로서, 비인가 접근을 차단하고 싶다. 그래서 데이터의 보안이 유지된다.
- US-05: 시스템으로서, 개인정보를 암호화하여 저장하고 싶다. 그래서 DB가 유출되어도 정보가 보호된다.

## 2. 기능 요구사항 (Functional Requirements)

| ID | 기능 | 설명 | 우선순위 |
|----|------|------|----------|
| FR-01 | 회원가입 API | 이메일, 비밀번호, 이름 입력 → Member 생성 | 높음 |
| FR-02 | 로그인 API | 이메일 + 비밀번호 → AccessToken + RefreshToken 발급 | 높음 |
| FR-03 | 토큰 재발급 API | RefreshToken → 새 AccessToken 발급 | 중간 |
| FR-04 | JWT 토큰 생성/검증 | HS256 서명, AccessToken(30분), RefreshToken(7일) | 높음 |
| FR-05 | 요청별 토큰 검증 필터 | Authorization 헤더에서 Bearer 토큰 추출 → 검증 → SecurityContext에 저장 | 높음 |
| FR-06 | SecurityConfig | CSRF 비활성화, STATELESS 세션, URL별 권한 설정 | 높음 |
| FR-07 | AES-256 암호화 유틸 | 개인정보 양방향 암호화/복호화 | 중간 |
| FR-08 | XSS 필터 | XSS 공격 방어 필터 | 중간 |
| FR-09 | 로깅 인터셉터 | 요청/응답 로깅 | 낮음 |
| FR-10 | 보안 헤더 설정 | CSP, X-Frame-Options, X-Content-Type-Options 등 | 중간 |

## 3. 비기능 요구사항 (Non-Functional Requirements)

- NFR-01: 비밀번호는 BCrypt 단방향 해시로 저장 (평문 저장 금지)
- NFR-02: JWT Secret Key는 환경변수로 관리 (코드에 하드코딩 금지)
- NFR-03: AES-256 암호화 키도 환경변수로 관리
- NFR-04: 인증 실패 시 401, 권한 부족 시 403 응답 (ApiResponse 형식)
- NFR-05: 보안 헤더로 XSS, 클릭재킹, MIME 스니핑 방어

## 4. API 명세

| Method | URL | 인증 | 설명 | 요청 | 응답 |
|--------|-----|------|------|------|------|
| POST | /api/auth/signup | 불필요 | 회원가입 | SignupRequest | ApiResponse\<Long\> |
| POST | /api/auth/login | 불필요 | 로그인 | LoginRequest | ApiResponse\<TokenResponse\> |
| POST | /api/auth/reissue | 필요 | 토큰 재발급 | ReissueRequest | ApiResponse\<TokenResponse\> |

### 요청/응답 DTO

**SignupRequest**
```json
{
  "email": "admin@mirae.com",       // @Email, @NotBlank
  "password": "Admin1234!",         // @NotBlank, @Size(min=8)
  "name": "홍길동"                   // @NotBlank, @Size(max=50)
}
```

**LoginRequest**
```json
{
  "email": "admin@mirae.com",       // @Email, @NotBlank
  "password": "Admin1234!"          // @NotBlank
}
```

**ReissueRequest**
```json
{
  "refreshToken": "eyJhbGciOi..."   // @NotBlank
}
```

**TokenResponse**
```json
{
  "accessToken": "eyJhbGciOi...",
  "refreshToken": "eyJhbGciOi...",
  "tokenType": "Bearer"
}
```

## 5. ERD (Entity 설계)

### members 테이블
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 회원 ID |
| email | VARCHAR(100) | NOT NULL, UNIQUE | 이메일 (로그인 ID) |
| password | VARCHAR(255) | NOT NULL | 비밀번호 (BCrypt 해시) |
| name | VARCHAR(50) | NOT NULL | 이름 |
| role | VARCHAR(20) | NOT NULL, DEFAULT 'ROLE_ADMIN' | 권한 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | 삭제 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 생성자 (BaseEntity) |
| updated_by | VARCHAR(50) | NOT NULL | 수정자 (BaseEntity) |

### MemberRole enum
```
ROLE_ADMIN  - 본사 관리자 (모든 기능 접근)
```

## 6. 인증 흐름도

```
[로그인 요청]
  POST /api/auth/login {email, password}
    → AuthService.login()
    → MemberRepository.findByEmail()
    → BCrypt 비밀번호 검증
    → JwtTokenProvider.createAccessToken() + createRefreshToken()
    → TokenResponse 반환

[인증된 API 요청]
  GET /api/products (Authorization: Bearer {accessToken})
    → JwtAuthenticationFilter.doFilterInternal()
    → 헤더에서 토큰 추출
    → JwtTokenProvider.validateToken()
    → JwtTokenProvider.getAuthentication() → SecurityContext에 저장
    → Controller 진입
```

## 7. 생성할 파일 목록

### 7-1. 인증 도메인 (domain/auth/)
| 파일 | 설명 |
|------|------|
| `domain/auth/controller/AuthController.java` | 로그인/회원가입/토큰재발급 API |
| `domain/auth/service/AuthService.java` | 인증 비즈니스 로직 |
| `domain/auth/domain/Member.java` | 회원 엔티티 (BaseEntity 상속) |
| `domain/auth/domain/MemberRole.java` | 권한 enum (ROLE_ADMIN) |
| `domain/auth/repository/MemberRepository.java` | JPA Repository |
| `domain/auth/dto/SignupRequest.java` | 회원가입 요청 DTO |
| `domain/auth/dto/LoginRequest.java` | 로그인 요청 DTO |
| `domain/auth/dto/ReissueRequest.java` | 토큰 재발급 요청 DTO |
| `domain/auth/dto/TokenResponse.java` | 토큰 응답 DTO |
| `domain/auth/jwt/JwtTokenProvider.java` | 토큰 생성/검증/파싱 |
| `domain/auth/jwt/JwtAuthenticationFilter.java` | 요청마다 토큰 검증 필터 |
| `domain/auth/jwt/JwtAccessDeniedHandler.java` | 403 에러 핸들러 |
| `domain/auth/jwt/JwtAuthenticationEntryPoint.java` | 401 에러 핸들러 |

### 7-2. 전역 설정/유틸 (global/)
| 파일 | 설명 |
|------|------|
| `global/config/SecurityConfig.java` | Spring Security 설정 (신규) |
| `global/util/AES256Util.java` | AES-256 암호화/복호화 |
| `global/filter/XssFilter.java` | XSS 방어 필터 |
| `global/interceptor/LoggingInterceptor.java` | 요청/응답 로깅 |

### 7-3. 수정할 기존 파일
| 파일 | 변경 내용 |
|------|-----------|
| `HairPjtApplication.java` | Security exclude 제거 |
| `global/config/WebConfig.java` | LoggingInterceptor 등록 |
| `global/exception/ErrorCode.java` | 인증 관련 에러 코드 추가 |
| `application.yml` | JWT, AES 설정 추가 |
| `application-dev.yml` | JWT Secret Key, AES Key (개발용) |
| `global/config/JpaConfig.java` | AuditorAware를 SecurityContext 기반으로 변경 |

### 7-4. 설정 파일
| 파일 | 설명 |
|------|------|
| `application.yml` | JWT 만료시간, AES 설정 |
| `application-dev.yml` | 개발용 Secret Key |

## 8. Security URL 권한 설정 (예정)

| URL 패턴 | 접근 권한 |
|----------|----------|
| `POST /api/auth/**` | 모두 허용 (로그인/회원가입) |
| `GET /swagger-ui/**` | 모두 허용 (API 문서) |
| `GET /v3/api-docs/**` | 모두 허용 (API 문서) |
| 그 외 모든 API | 인증 필요 (ROLE_ADMIN) |

## 9. 불명확한 사항 (Clarifications Needed)

- **Q1**: RefreshToken 저장 방식 — Redis는 feature/06에서 다루는데, 지금은 어떻게 할까요?
  - 방안 A: DB(members 테이블에 refresh_token 컬럼 추가)에 저장
  - 방안 B: RefreshToken은 클라이언트에만 보관, 서버에서는 유효성만 검증 (서명 기반)
  - 방안 C: feature/02에서는 AccessToken만 구현, RefreshToken은 feature/06에서 Redis와 함께 구현
  → 제 제안: **방안 B** (서버에 저장하지 않고 서명 기반 검증, 가장 단순)

- **Q2**: 회원가입 API 접근 제한 — 아무나 회원가입 가능하게 할까요, 아니면 기존 관리자만 새 관리자를 등록할 수 있게 할까요?
  - 방안 A: 누구나 회원가입 가능 (개발/테스트 편의)
  - 방안 B: 인증된 ADMIN만 회원 등록 가능 (실무에 가까움)
  → 제 제안: **방안 A** (포트폴리오이므로 개발 편의 우선, 추후 권한 제한 가능)

- **Q3**: AES-256 암호화 대상 — 현재 Member 엔티티에서 암호화할 필드가 있을까요?
  - email은 로그인 ID로 검색이 필요해서 암호화하면 검색이 어렵다
  - name은 암호화 대상이 될 수 있다
  - 또는 feature/04-customer에서 미용실 연락처 등에 적용?
  → 제 제안: AES256Util만 만들어두고, 실제 적용은 customer feature에서 진행

## 10. 의존성

- **선행 feature**: feature/01-project-init (BaseEntity, ApiResponse, ErrorCode, GlobalExceptionHandler)
- **후행 feature**: feature/03~05에서 SecurityConfig의 URL 권한 설정 확장
