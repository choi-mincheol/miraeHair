# Feature: 05-order (주문/판매 — 가격 Snapshot 필수)

## 1. 유저 스토리

> 관리자로서, 미용실(고객)에게 제품을 판매한 주문을 등록/조회/관리하고 싶다. 그래서 매출과 거래 내역을 체계적으로 관리할 수 있다.

- US-01: 관리자로서, 미용실에 상품을 판매한 주문을 등록하고 싶다. 그래서 거래 기록이 남는다.
- US-02: 관리자로서, 주문 시 여러 상품(옵션)을 한 번에 담고 싶다. 그래서 한 건의 주문으로 처리할 수 있다.
- US-03: 관리자로서, 주문 목록을 기간/고객별로 조회하고 싶다. 그래서 매출 현황을 파악할 수 있다.
- US-04: 관리자로서, 주문 상세를 확인하고 싶다. 그래서 어떤 상품이 얼마에 팔렸는지 알 수 있다.
- US-05: 관리자로서, 주문을 취소하고 싶다. 그래서 잘못된 주문을 처리할 수 있다.

## 2. 기능 요구사항 (Functional Requirements)

| ID | 기능 | 설명 | 우선순위 |
|----|------|------|----------|
| FR-01 | 주문 등록 | 고객 선택 + 상품 옵션/수량 입력 → Order + OrderItem 생성 | 높음 |
| FR-02 | 주문 시 가격 Snapshot | OrderItem에 주문 시점의 가격을 복사 저장 (이후 상품 가격이 바뀌어도 영향 없음) | 높음 |
| FR-03 | 주문 시 재고 차감 | 주문 등록 시 해당 옵션의 재고를 자동 감소 | 높음 |
| FR-04 | 주문 목록 조회 | 페이징 + 기간 필터 + 고객 필터 | 높음 |
| FR-05 | 주문 상세 조회 | 주문 + 주문항목(상품, 가격, 수량) 포함 | 높음 |
| FR-06 | 주문 취소 | 주문 상태를 CANCELLED로 변경 + 재고 복원 | 중간 |

## 3. 비기능 요구사항 (Non-Functional Requirements)

- NFR-01: 모든 API 응답 시간 500ms 이내
- NFR-02: 입력값 검증 (Bean Validation)
- NFR-03: 인증된 ADMIN만 접근 가능 (JWT 토큰 필수)
- NFR-04: **가격 Snapshot 필수** — OrderItem 생성 시 Product/Option의 현재 가격을 반드시 복사 저장
- NFR-05: 주문 등록은 하나의 트랜잭션 (주문 생성 + 재고 차감이 함께 성공/실패)
- NFR-06: 조회는 MyBatis(Query), 등록/취소는 JPA(Command) → CQRS

## 4. API 명세

| Method | URL | 설명 | 요청 Body | 응답 |
|--------|-----|------|-----------|------|
| POST | /api/orders | 주문 등록 | OrderCreateRequest | ApiResponse\<Long\> |
| GET | /api/orders | 주문 목록 조회 | ?page=0&size=10&customerId=&startDate=&endDate= | ApiResponse\<Page\<OrderListDto\>\> |
| GET | /api/orders/{id} | 주문 상세 조회 | - | ApiResponse\<OrderDetailDto\> |
| POST | /api/orders/{id}/cancel | 주문 취소 | - | ApiResponse\<Void\> |

### 4-1. 요청/응답 DTO 상세

**OrderCreateRequest**
```json
{
  "customerId": 1,
  "items": [
    { "productOptionId": 1, "quantity": 3 },
    { "productOptionId": 5, "quantity": 1 }
  ],
  "memo": "3월 정기 주문"
}
```

**OrderListDto** (목록 조회용)
```json
{
  "id": 1,
  "customerShopName": "헤어살롱 미래",
  "totalAmount": 195000,
  "itemCount": 2,
  "status": "CONFIRMED",
  "orderedAt": "2026-03-11T10:00:00"
}
```

**OrderDetailDto** (상세 조회용)
```json
{
  "id": 1,
  "customerId": 1,
  "customerShopName": "헤어살롱 미래",
  "totalAmount": 195000,
  "status": "CONFIRMED",
  "memo": "3월 정기 주문",
  "orderedAt": "2026-03-11T10:00:00",
  "items": [
    {
      "id": 1,
      "productName": "모로칸오일 트리트먼트",
      "optionName": "100ml",
      "unitPrice": 45000,
      "quantity": 3,
      "subtotal": 135000
    },
    {
      "id": 2,
      "productName": "모로칸오일 트리트먼트",
      "optionName": "200ml",
      "unitPrice": 60000,
      "quantity": 1,
      "subtotal": 60000
    }
  ]
}
```

## 5. ERD (Entity 설계)

### 테이블 관계도
```
customers (1) ──── (N) orders (1) ──── (N) order_items
                                              │
                                     product_options (참조, FK 없이 Snapshot)
```

### OrderStatus enum
| 값 | 한글명 | 설명 |
|----|--------|------|
| CONFIRMED | 확정 | 주문 완료 (기본) |
| CANCELLED | 취소 | 주문 취소됨 |

### orders 테이블
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 주문 ID |
| customer_id | BIGINT | FK → customers.id, NOT NULL | 주문 고객 |
| total_amount | INTEGER | NOT NULL, >= 0 | 총 주문 금액 |
| status | VARCHAR(20) | NOT NULL | 주문 상태 (CONFIRMED, CANCELLED) |
| memo | TEXT | NULLABLE | 주문 메모 |
| ordered_at | TIMESTAMP | NOT NULL | 주문일시 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 생성자 (BaseEntity) |
| updated_by | VARCHAR(50) | | 수정자 (BaseEntity) |

### order_items 테이블 (가격 Snapshot)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 주문항목 ID |
| order_id | BIGINT | FK → orders.id, NOT NULL | 소속 주문 |
| product_option_id | BIGINT | NOT NULL | 주문 시점의 옵션 ID (참조용) |
| product_name | VARCHAR(100) | NOT NULL | **Snapshot** — 주문 시점 상품명 |
| option_name | VARCHAR(100) | NOT NULL | **Snapshot** — 주문 시점 옵션명 |
| unit_price | INTEGER | NOT NULL, >= 0 | **Snapshot** — 주문 시점 단가 (기본가격 + 추가가격) |
| quantity | INTEGER | NOT NULL, >= 1 | 주문 수량 |
| subtotal | INTEGER | NOT NULL, >= 0 | 소계 (unit_price × quantity) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 생성자 (BaseEntity) |
| updated_by | VARCHAR(50) | | 수정자 (BaseEntity) |

### 가격 Snapshot이란?
```
주문 시점: 모로칸오일 100ml = 45,000원 → OrderItem.unitPrice = 45,000
일주일 후: 모로칸오일 100ml 가격이 50,000원으로 인상
→ OrderItem에는 여전히 45,000원이 기록되어 있다 (주문 시점 가격 보존)
→ 만약 Product를 참조만 하고 가격을 복사하지 않으면, 과거 주문 금액이 바뀌어 버린다
```

## 6. 파일 구조 (구현 예정)

```
domain/order/
├── controller/
│   └── OrderController.java              # REST API
├── command/
│   ├── OrderCommandService.java          # 주문 등록/취소 (JPA)
│   ├── OrderRepository.java              # Order JPA Repository
│   └── OrderItemRepository.java          # OrderItem JPA Repository
├── query/
│   ├── OrderQueryService.java            # 주문 조회 (MyBatis)
│   └── OrderQueryMapper.java             # MyBatis Mapper 인터페이스
├── domain/
│   ├── Order.java                        # 주문 엔티티
│   ├── OrderItem.java                    # 주문항목 엔티티 (가격 Snapshot)
│   └── OrderStatus.java                  # 주문 상태 enum
└── dto/
    ├── OrderCreateRequest.java           # 주문 등록 요청
    ├── OrderItemRequest.java             # 주문항목 요청 (옵션ID + 수량)
    ├── OrderListDto.java                 # 주문 목록 응답
    ├── OrderDetailDto.java               # 주문 상세 응답
    └── OrderItemDto.java                 # 주문항목 응답

resources/mapper/order/
└── OrderQueryMapper.xml                  # MyBatis SQL
```

## 7. CQRS 적용 방식

```
[OrderController] ─── POST(등록/취소) ──→ [OrderCommandService] ──→ [OrderRepository (JPA)]
                  │                                               ──→ [ProductOptionRepository (재고 차감)]
                  └── GET(목록/상세) ───→ [OrderQueryService]    ──→ [OrderQueryMapper (MyBatis)]
```

### 주문 등록 로직 흐름
```
1) 고객 존재 확인
2) 각 주문항목:
   → ProductOption 조회
   → 가격 Snapshot 생성 (Product.price + Option.additionalPrice)
   → 재고 차감 (ProductOption.decreaseStock)
3) Order + OrderItem 저장
4) 총 금액 계산 (SUM of subtotals)
```

### 주문 취소 로직 흐름
```
1) 주문 조회 (CONFIRMED 상태만 취소 가능)
2) 주문 상태 → CANCELLED로 변경
3) 각 주문항목의 재고 복원 (ProductOption.increaseStock)
```

## 8. 불명확한 사항 (Clarifications Needed)

- **Q1: 주문 수정(항목 변경)을 이번 feature에서 구현할까요?**
  - → 추천: **이번엔 제외** (등록 + 조회 + 취소만. 수정은 "취소 후 재등록"으로 대체)

- **Q2: 주문 상태를 CONFIRMED/CANCELLED 2개만 둘까요?**
  - → 추천: **2개만** (향후 E-commerce 확장 시 PENDING, SHIPPED, DELIVERED 등 추가 가능)

## 9. 의존성

- **선행 feature**: feature/03-product (Product, ProductOption — 가격 Snapshot 대상)
- **선행 feature**: feature/04-customer (Customer — 주문 고객)
- **후행 feature**: feature/06-redis-cache (주문 통계 캐싱 가능)

## 10. 구현 순서

1. **OrderStatus enum** — 주문 상태
2. **Order, OrderItem 엔티티** — 가격 Snapshot이 핵심
3. **DTO** — 요청/응답 DTO
4. **Repository** — JPA
5. **OrderCommandService** — 주문 등록(재고 차감) + 취소(재고 복원)
6. **MyBatis Mapper + QueryService** — 목록/상세 조회
7. **Controller** — REST API
8. **ErrorCode 추가** — 주문 도메인용
