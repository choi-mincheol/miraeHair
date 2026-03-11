# Feature 01: 프로젝트 초기 설정 — 면접 Q&A

---

## Q1. BaseEntity를 왜 사용하나요? 직접 각 엔티티에 createdAt, updatedAt을 넣으면 안 되나요?

**A:** 가능은 하지만, 모든 엔티티에 동일한 필드를 반복 작성하면 코드 중복이 발생합니다.
BaseEntity에 공통 필드(`createdAt`, `updatedAt`, `createdBy`, `updatedBy`)를 모아두고 상속하면:
- 새 엔티티를 만들 때 `extends BaseEntity`만 하면 자동으로 4개 필드가 추가됩니다.
- `@CreatedDate`, `@LastModifiedDate` 등 JPA Auditing이 자동으로 값을 채워줍니다.
- 수동으로 `LocalDateTime.now()`를 호출할 필요가 없어서 실수를 방지합니다.

**핵심 코드:**
```java
@MappedSuperclass        // 테이블을 만들지 않고, 상속받는 엔티티에 컬럼만 추가
@EntityListeners(AuditingEntityManagerFactory.class)  // JPA Auditing 리스너
public abstract class BaseEntity {
    @CreatedDate
    private LocalDateTime createdAt;
}
```

---

## Q2. ApiResponse\<T\>로 응답을 감싸는 이유는 무엇인가요?

**A:** 프론트엔드와 API 통신 시 **일관된 응답 형식**이 없으면 매번 "이 API는 성공 시 뭘 주지?"를 확인해야 합니다.

`ApiResponse<T>`로 통일하면:
```json
// 성공
{ "success": true, "data": { ... }, "message": "조회 성공", "code": 200 }
// 실패
{ "success": false, "data": null, "message": "상품을 찾을 수 없습니다", "code": 404 }
```
- 프론트에서 `response.success`만 체크하면 성공/실패를 판별할 수 있습니다.
- 모든 API가 같은 구조이므로 공통 HTTP 클라이언트를 만들기 쉽습니다.

---

## Q3. GlobalExceptionHandler가 없으면 어떻게 되나요?

**A:** Spring Boot 기본 에러 페이지(Whitelabel Error Page)가 노출되거나, 스택 트레이스가 JSON으로 반환됩니다.

문제점:
- 클라이언트가 에러 형식을 예측할 수 없습니다.
- **스택 트레이스가 외부에 노출되면 보안 취약점**이 됩니다 (내부 패키지 구조, DB 정보 등).

`@RestControllerAdvice`로 GlobalExceptionHandler를 만들면:
- 모든 예외를 한 곳에서 잡아 `ErrorResponse` 형식으로 변환합니다.
- 스택 트레이스는 서버 로그에만 남고, 클라이언트에는 안전한 메시지만 반환합니다.

---

## Q4. ErrorCode를 enum으로 관리하는 이유는? 각 도메인에 Exception 클래스를 만드는 건 안 되나요?

**A:** 둘 다 가능하지만, **ErrorCode enum + BusinessException** 조합이 더 효율적입니다.

| 방식 | 장점 | 단점 |
|------|------|------|
| 도메인별 Exception 클래스 | 타입으로 구분 가능 | 클래스가 폭발적 증가 (ProductNotFoundException, CustomerNotFoundException...) |
| ErrorCode enum | 에러 코드를 한 곳에서 관리, 클래스 1개로 충분 | enum이 커질 수 있음 |

```java
// 이 한 줄로 어디서든 비즈니스 예외를 던질 수 있다
throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
```

---

## Q5. JPA와 MyBatis를 왜 함께 사용하나요? (CQRS)

**A:** 각각 잘하는 영역이 다릅니다.

| | JPA | MyBatis |
|--|-----|---------|
| 강점 | 객체 중심 CRUD, 변경 감지, 연관관계 관리 | 복잡한 SQL, JOIN, 통계 쿼리 |
| 약점 | 복잡한 조회 쿼리는 JPQL/QueryDSL 필요 | 단순 CRUD도 SQL을 직접 작성 |

CQRS 패턴으로 역할을 나누면:
- **Command(등록/수정/삭제):** JPA — 엔티티 중심, 트랜잭션 관리 용이
- **Query(조회):** MyBatis — SQL 직접 작성, 조회 성능 최적화

실무에서도 "JPA + QueryDSL" 또는 "JPA + MyBatis" 조합을 많이 사용합니다.
