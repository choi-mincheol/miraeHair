# Feature 02: Spring Security + JWT 인증 — 면접 Q&A

---

## Q1. 세션 기반 인증 대신 JWT를 선택한 이유는 무엇인가요?

**A:** 세션 기반은 서버가 사용자 상태를 메모리에 저장합니다. JWT는 **Stateless**입니다.

| | 세션 기반 | JWT |
|--|----------|-----|
| 상태 저장 | 서버 메모리 (HttpSession) | 클라이언트 (토큰 자체에 정보 포함) |
| 서버 확장 | 세션 공유 필요 (Redis, Sticky Session) | 서버가 몇 대든 상관없음 |
| 적합 환경 | 전통적 웹 (SSR) | REST API, MSA, 모바일 앱 |

이 프로젝트는 REST API 서버이므로 JWT가 적합합니다. 서버를 여러 대로 확장해도 별도의 세션 공유 설정이 필요 없습니다.

---

## Q2. AccessToken과 RefreshToken을 왜 나누나요?

**A:** 보안과 편의성의 균형을 맞추기 위해서입니다.

- **AccessToken (30분):** 짧은 만료 시간 → 탈취되어도 피해 시간이 제한적
- **RefreshToken (7일):** 긴 만료 시간 → AccessToken이 만료되면 새로 발급받는 용도

만약 AccessToken만 있고 만료 시간이 7일이면? → 탈취 시 7일 동안 악용 가능
만약 AccessToken만 있고 만료 시간이 30분이면? → 30분마다 재로그인 필요 (UX 최악)

RefreshToken이 있으면: AccessToken 만료 → RefreshToken으로 자동 재발급 → 사용자는 재로그인 불필요

---

## Q3. 비밀번호를 BCrypt로 해시하는 이유는? AES-256으로 암호화하면 안 되나요?

**A:** 비밀번호는 **단방향 해시**(BCrypt)가 정답이고, **양방향 암호화**(AES-256)를 쓰면 안 됩니다.

| | BCrypt (단방향) | AES-256 (양방향) |
|--|----------------|-----------------|
| 복호화 가능? | 불가능 | 가능 (키가 있으면) |
| DB 유출 시 | 원문 비밀번호를 알 수 없음 | 키도 유출되면 모든 비밀번호 복원 가능 |
| 검증 방법 | 입력값을 해시해서 저장값과 비교 | 저장값을 복호화해서 입력값과 비교 |

비밀번호는 "원래 값을 알 필요 없고, 맞는지만 확인하면 되는" 데이터입니다. 그래서 단방향 해시가 적합합니다.
반면 전화번호, 사업자번호 같은 개인정보는 조회 시 원문이 필요하므로 AES-256 양방향 암호화를 씁니다.

---

## Q4. Spring Security의 인증 흐름을 설명해주세요.

**A:**
```
1) 클라이언트 → GET /api/products (Authorization: Bearer {token})
2) JwtAuthenticationFilter가 요청을 가로챔
3) 헤더에서 Bearer 토큰 추출
4) JwtTokenProvider.validateToken()으로 서명/만료 검증
5) 유효하면 → Authentication 객체 생성 → SecurityContext에 저장
6) Controller에 도달 → 인증된 사용자로 처리
```

핵심은 **Filter**입니다. Spring Security는 서블릿 필터 체인으로 동작하며, 우리가 만든 `JwtAuthenticationFilter`가 `UsernamePasswordAuthenticationFilter` 앞에 위치합니다.

---

## Q5. CSRF를 비활성화해도 괜찮은 이유는?

**A:** CSRF(Cross-Site Request Forgery)는 **쿠키 기반 세션**에서 발생하는 공격입니다.

- 브라우저가 쿠키를 자동으로 보내기 때문에, 악성 사이트에서 사용자 모르게 요청을 보낼 수 있습니다.
- JWT는 쿠키가 아니라 **Authorization 헤더**에 토큰을 담아 보냅니다.
- 헤더는 브라우저가 자동으로 보내지 않으므로 CSRF 공격이 성립하지 않습니다.

따라서 JWT 기반 Stateless API에서는 CSRF 보호가 불필요하며, 오히려 활성화하면 모든 POST 요청에 CSRF 토큰을 보내야 해서 불편합니다.
