# Feature 03: 상품 CRUD (CQRS 적용) — 면접 Q&A

---

## Q1. CQRS 패턴을 왜 적용했나요? 한 서비스에서 다 하면 안 되나요?

**A:** 작은 프로젝트에서는 한 서비스에서 다 해도 됩니다. 하지만 CQRS를 적용하면:

- **조회 최적화:** MyBatis로 필요한 컬럼만 SELECT하고, JOIN/GROUP BY도 자유롭게 작성
- **명령과 조회의 관심사 분리:** 등록/수정은 "데이터 정합성"이 중요하고, 조회는 "성능"이 중요
- **코드 가독성:** CommandService는 비즈니스 로직에만, QueryService는 조회에만 집중

```
Controller
├── POST/PUT/DELETE → CommandService → JPA Repository (트랜잭션, 영속성 컨텍스트)
└── GET             → QueryService   → MyBatis Mapper (SQL 직접 작성, 성능 최적화)
```

면접에서 "왜 JPA만 안 쓰나요?"라고 물으면:
> "JPA는 엔티티 단위로 조회하기 때문에, 목록 조회에서 불필요한 컬럼까지 가져옵니다. MyBatis로 필요한 데이터만 DTO로 매핑하면 더 효율적입니다."

---

## Q2. Entity에 @Setter를 안 쓰는 이유는?

**A:** `@Setter`를 쓰면 아무 곳에서나 `product.setPrice(0)`처럼 값을 바꿀 수 있습니다. 이러면:

- 어디서 값이 바뀌었는지 추적이 어렵습니다 (setter 호출 지점이 수십 곳).
- 비즈니스 규칙을 무시하고 값을 변경할 수 있습니다 (가격을 음수로 설정 등).

대신:
```java
// 생성: @Builder + 정적 팩토리 메서드
Product product = Product.create(request);

// 수정: 도메인 메서드 (비즈니스 규칙을 메서드 안에 캡슐화)
product.update(name, brand, price, description, category);

// 재고: 도메인 메서드에서 검증
option.decreaseStock(quantity);  // 재고 부족하면 예외 발생
```

값 변경은 **의미 있는 도메인 메서드**를 통해서만 가능하도록 제한합니다.

---

## Q3. Soft Delete를 사용하는 이유는? 실제로 DELETE하면 안 되나요?

**A:** 실무에서 물리 삭제(Hard Delete)는 거의 쓰지 않습니다.

| | Soft Delete | Hard Delete |
|--|------------|-------------|
| 방식 | `is_deleted = true` | `DELETE FROM products WHERE ...` |
| 복구 | 가능 (플래그만 되돌리면 됨) | 불가능 (백업에서 복원해야 함) |
| 연관 데이터 | 주문 이력에서 상품 참조 유지 | FK 위반 또는 CASCADE 삭제 |
| 감사 추적 | 삭제 기록이 남음 | 기록 자체가 사라짐 |

특히 이 프로젝트에서 상품을 물리 삭제하면, 해당 상품이 포함된 과거 주문 이력이 깨집니다.

---

## Q4. 카테고리를 enum 대신 별도 테이블로 만든 이유는?

**A:** enum은 코드에 고정되어 있어서, 카테고리를 추가/수정하려면 **코드를 수정하고 재배포**해야 합니다.

별도 테이블로 만들면:
- 관리자가 화면에서 카테고리를 동적으로 추가/수정할 수 있습니다.
- 재배포 없이 운영 중에 변경 가능합니다.
- 표시 순서(`display_order`)도 DB에서 관리할 수 있습니다.

enum이 적합한 경우: 주문 상태(CONFIRMED/CANCELLED)처럼 비즈니스 로직과 밀접하게 연관된 값.
테이블이 적합한 경우: 카테고리처럼 운영 중 변경될 수 있는 참조 데이터.

---

## Q5. ProductOption의 재고 차감 로직을 Service가 아닌 Entity에 넣은 이유는?

**A:** **도메인 주도 설계(DDD)**의 핵심 원칙입니다.

```java
// Entity에 비즈니스 로직 (Rich Domain Model)
public void decreaseStock(int quantity) {
    if (this.stockQuantity < quantity) {
        throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
    }
    this.stockQuantity -= quantity;
}
```

서비스에 넣으면:
```java
// Service에 비즈니스 로직 (Anemic Domain Model — 빈약한 도메인)
if (option.getStockQuantity() < quantity) { throw ... }
option.setStockQuantity(option.getStockQuantity() - quantity);
```

Entity에 넣으면:
- 재고 차감 규칙이 `ProductOption` 안에 캡슐화됩니다.
- 어디서 `decreaseStock()`을 호출해도 동일한 검증이 적용됩니다.
- Service가 아닌 다른 곳에서 호출해도 재고가 음수가 되지 않습니다.

---

## Q6. CommandService와 QueryService를 굳이 클래스로 나눈 이유는? 하나의 Service에서 JPA와 MyBatis를 같이 쓰면 안 되나요?

**A:** 하나의 Service에서 둘 다 써도 기술적으로 문제 없습니다. 잘 동작합니다.

```java
// 이렇게 해도 동작함
@Service
public class ProductService {
    private final ProductRepository productRepository;      // JPA
    private final ProductQueryMapper productQueryMapper;    // MyBatis

    public Long createProduct(...) { ... }              // JPA로 저장
    public Page<ProductListDto> getList(...) { ... }    // MyBatis로 조회
}
```

나눈 이유는 크게 두 가지입니다:

**1) 서비스가 커졌을 때의 유지보수성**

| | 하나로 합침 | 분리 |
|--|-----------|------|
| 작은 프로젝트 | 충분함 | 다소 과한 느낌 |
| 서비스가 커지면 | 메서드 20~30개로 불어남 | 각각 10~15개로 관리 가능 |

실무에서 서비스가 커지면 CRUD + 조회 + 통계 + 엑셀 등 메서드가 쌓이면서 수백 줄이 됩니다. 그때 "조회만 모은 클래스" / "변경만 모은 클래스"로 나누는 게 자연스럽습니다.

**2) 트랜잭션 설정을 클래스 레벨에서 깔끔하게 지정**

```java
@Transactional                    // CommandService — 읽기+쓰기
public class ProductCommandService { ... }

@Transactional(readOnly = true)   // QueryService — 읽기 전용 (더티체킹 생략, 성능 향상)
public class ProductQueryService { ... }
```

합치면 메서드마다 `@Transactional` / `@Transactional(readOnly = true)`를 개별 지정해야 해서 실수할 여지가 생깁니다.

**솔직한 결론:** 이 프로젝트 규모에서는 합쳐도 됩니다. CQRS 패턴 학습 + 포트폴리오 어필 목적으로 분리한 것이며, 면접에서는 **"서비스가 커졌을 때의 유지보수성과 트랜잭션 분리를 고려해 나눴다"**고 답하면 됩니다.
