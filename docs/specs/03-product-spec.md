# Feature: 03-product (상품 CRUD — CQRS 첫 적용)

## 1. 유저 스토리

> 관리자로서, 미용 제품을 등록/조회/수정/삭제하고 싶다. 그래서 미용실(고객)에게 판매할 상품을 체계적으로 관리할 수 있다.

- US-01: 관리자로서, 새로운 미용 제품을 등록하고 싶다. 그래서 미용실에 판매할 수 있다.
- US-02: 관리자로서, 상품에 옵션(용량/색상)을 추가하고 싶다. 그래서 같은 제품의 다양한 규격을 관리할 수 있다.
- US-03: 관리자로서, 상품 목록을 페이징/검색/필터로 조회하고 싶다. 그래서 원하는 상품을 빠르게 찾을 수 있다.
- US-04: 관리자로서, 상품 상세 정보(옵션, 재고 포함)를 확인하고 싶다. 그래서 판매 현황을 파악할 수 있다.
- US-05: 관리자로서, 상품 정보를 수정하고 싶다. 그래서 가격 변경이나 정보 업데이트를 할 수 있다.
- US-06: 관리자로서, 상품을 삭제하고 싶다. 그래서 더 이상 판매하지 않는 상품을 정리할 수 있다.
- US-07: 관리자로서, 카테고리를 등록/관리하고 싶다. 그래서 상품 분류를 유연하게 운영할 수 있다.
- US-08: 관리자로서, 재고를 입고/출고 처리하고 싶다. 그래서 정확한 재고 현황을 유지할 수 있다.

## 2. 기능 요구사항 (Functional Requirements)

| ID | 기능 | 설명 | 우선순위 |
|----|------|------|----------|
| FR-01 | 상품 등록 | 상품명, 브랜드, 카테고리, 기본가격, 설명 입력 → Product 생성 | 높음 |
| FR-02 | 상품 목록 조회 | 페이징 + 검색(상품명/브랜드) + 카테고리 필터 | 높음 |
| FR-03 | 상품 상세 조회 | ID로 단건 조회 (옵션 + 재고 포함) | 높음 |
| FR-04 | 상품 수정 | 상품명, 가격, 설명 등 수정 | 중간 |
| FR-05 | 상품 삭제 | Soft Delete (isDeleted 플래그) | 중간 |
| FR-06 | 상품 옵션 관리 | 상품에 옵션(용량/색상) 추가/수정/삭제 | 중간 |
| FR-07 | 카테고리 목록 조회 | 전체 카테고리 목록 반환 | 높음 |
| FR-08 | 카테고리 등록 | 새 카테고리 추가 | 중간 |
| FR-09 | 재고 입고 | 옵션 단위로 수량 증가 | 중간 |
| FR-10 | 재고 출고 | 옵션 단위로 수량 감소 (재고 부족 시 예외) | 중간 |

## 3. 비기능 요구사항 (Non-Functional Requirements)

- NFR-01: 모든 API 응답 시간 500ms 이내
- NFR-02: 입력값 검증 (Bean Validation) — 상품명 필수, 가격 0 이상, 재고 0 이상
- NFR-03: 인증된 ADMIN만 접근 가능 (JWT 토큰 필수)
- NFR-04: Soft Delete — 삭제 시 실제 DB에서 제거하지 않고 is_deleted = true 처리
- NFR-05: 목록 조회는 MyBatis(Query), 등록/수정/삭제는 JPA(Command) → CQRS
- NFR-06: 재고 출고 시 재고 부족(음수) 방지 검증 필수

## 4. API 명세

### 4-1. 상품 API

| Method | URL | 설명 | 요청 Body | 응답 |
|--------|-----|------|-----------|------|
| POST | /api/products | 상품 등록 (옵션 포함) | ProductCreateRequest | ApiResponse\<Long\> |
| GET | /api/products | 상품 목록 조회 | ?page=0&size=10&keyword=&categoryId= | ApiResponse\<Page\<ProductListDto\>\> |
| GET | /api/products/{id} | 상품 상세 조회 | - | ApiResponse\<ProductDetailDto\> |
| PUT | /api/products/{id} | 상품 수정 | ProductUpdateRequest | ApiResponse\<Long\> |
| DELETE | /api/products/{id} | 상품 삭제 (Soft) | - | ApiResponse\<Void\> |

### 4-2. 카테고리 API

| Method | URL | 설명 | 요청 Body | 응답 |
|--------|-----|------|-----------|------|
| POST | /api/categories | 카테고리 등록 | CategoryCreateRequest | ApiResponse\<Long\> |
| GET | /api/categories | 카테고리 목록 | - | ApiResponse\<List\<CategoryDto\>\> |

### 4-3. 재고 API

| Method | URL | 설명 | 요청 Body | 응답 |
|--------|-----|------|-----------|------|
| POST | /api/inventory/in | 입고 처리 | InventoryRequest | ApiResponse\<Void\> |
| POST | /api/inventory/out | 출고 처리 | InventoryRequest | ApiResponse\<Void\> |

### 4-4. 요청/응답 DTO 상세

**ProductCreateRequest** (옵션 함께 등록)
```json
{
  "name": "모로칸오일 트리트먼트",
  "brand": "모로칸오일",
  "categoryId": 2,
  "price": 45000,
  "description": "아르간 오일 기반 헤어 트리트먼트",
  "options": [
    { "optionName": "100ml", "additionalPrice": 0, "stockQuantity": 50 },
    { "optionName": "200ml", "additionalPrice": 15000, "stockQuantity": 30 }
  ]
}
```

**ProductUpdateRequest**
```json
{
  "name": "모로칸오일 트리트먼트 (리뉴얼)",
  "brand": "모로칸오일",
  "categoryId": 2,
  "price": 48000,
  "description": "아르간 오일 기반 헤어 트리트먼트 (리뉴얼)"
}
```

**ProductListDto** (목록 조회용 — 간략 정보)
```json
{
  "id": 1,
  "name": "모로칸오일 트리트먼트",
  "brand": "모로칸오일",
  "categoryId": 2,
  "categoryName": "트리트먼트",
  "price": 45000,
  "totalStock": 80,
  "createdAt": "2026-03-11T10:00:00"
}
```

**ProductDetailDto** (상세 조회용 — 옵션 + 재고 포함)
```json
{
  "id": 1,
  "name": "모로칸오일 트리트먼트",
  "brand": "모로칸오일",
  "categoryId": 2,
  "categoryName": "트리트먼트",
  "price": 45000,
  "description": "아르간 오일 기반 헤어 트리트먼트",
  "deleted": false,
  "createdAt": "2026-03-11T10:00:00",
  "updatedAt": "2026-03-11T10:00:00",
  "createdBy": "admin@mirae.com",
  "options": [
    { "id": 1, "optionName": "100ml", "additionalPrice": 0, "stockQuantity": 50 },
    { "id": 2, "optionName": "200ml", "additionalPrice": 15000, "stockQuantity": 30 }
  ]
}
```

**CategoryCreateRequest**
```json
{
  "name": "트리트먼트",
  "displayOrder": 2
}
```

**CategoryDto**
```json
{
  "id": 2,
  "name": "트리트먼트",
  "displayOrder": 2
}
```

**InventoryRequest** (입고/출고 공통)
```json
{
  "productOptionId": 1,
  "quantity": 20,
  "reason": "3월 정기 입고"
}
```

## 5. ERD (Entity 설계)

### 테이블 관계도
```
categories (1) ──── (N) products (1) ──── (N) product_options (1) ──── (N) inventory_history
```

### categories 테이블 (카테고리)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 카테고리 ID |
| name | VARCHAR(50) | NOT NULL, UNIQUE | 카테고리명 (예: 트리트먼트) |
| display_order | INTEGER | NOT NULL, DEFAULT 0 | 표시 순서 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 생성자 (BaseEntity) |
| updated_by | VARCHAR(50) | | 수정자 (BaseEntity) |

### products 테이블 (상품)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 상품 ID |
| name | VARCHAR(100) | NOT NULL | 상품명 |
| brand | VARCHAR(100) | NOT NULL | 브랜드명 |
| category_id | BIGINT | FK → categories.id, NOT NULL | 카테고리 |
| price | INTEGER | NOT NULL, >= 0 | 기본 판매 가격 (원) |
| description | TEXT | NULLABLE | 상품 설명 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | 삭제 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 생성자 (BaseEntity) |
| updated_by | VARCHAR(50) | | 수정자 (BaseEntity) |

### product_options 테이블 (상품 옵션)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 옵션 ID |
| product_id | BIGINT | FK → products.id, NOT NULL | 소속 상품 |
| option_name | VARCHAR(100) | NOT NULL | 옵션명 (예: 100ml, 200ml) |
| additional_price | INTEGER | NOT NULL, DEFAULT 0 | 추가 금액 (기본가격 + 추가금액 = 최종가격) |
| stock_quantity | INTEGER | NOT NULL, DEFAULT 0, >= 0 | 현재 재고 수량 |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | 삭제 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 생성자 (BaseEntity) |
| updated_by | VARCHAR(50) | | 수정자 (BaseEntity) |

### inventory_history 테이블 (재고 입출고 이력)
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 이력 ID |
| product_option_id | BIGINT | FK → product_options.id, NOT NULL | 대상 옵션 |
| type | VARCHAR(10) | NOT NULL | 입출고 구분 (IN / OUT) |
| quantity | INTEGER | NOT NULL, > 0 | 수량 |
| reason | VARCHAR(200) | NULLABLE | 사유 (예: 3월 정기 입고) |
| created_at | TIMESTAMP | NOT NULL | 생성일시 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 처리자 (BaseEntity) |
| updated_by | VARCHAR(50) | | 수정자 (BaseEntity) |

### 초기 카테고리 데이터 (data.sql 또는 서비스에서 등록)
| name | display_order |
|------|---------------|
| 샴푸 | 1 |
| 트리트먼트 | 2 |
| 염모제 | 3 |
| 파마약 | 4 |
| 스타일링 | 5 |
| 케어 | 6 |
| 도구 | 7 |
| 기타 | 99 |

## 6. 파일 구조 (구현 예정)

```
domain/product/
├── controller/
│   ├── ProductController.java              # 상품 API
│   ├── CategoryController.java             # 카테고리 API
│   └── InventoryController.java            # 재고 API
├── command/
│   ├── ProductCommandService.java          # 상품 등록/수정/삭제 (JPA)
│   ├── CategoryCommandService.java         # 카테고리 등록 (JPA)
│   ├── InventoryCommandService.java        # 재고 입고/출고 (JPA)
│   ├── ProductRepository.java              # Product JPA Repository
│   ├── ProductOptionRepository.java        # ProductOption JPA Repository
│   ├── CategoryRepository.java             # Category JPA Repository
│   └── InventoryHistoryRepository.java     # InventoryHistory JPA Repository
├── query/
│   ├── ProductQueryService.java            # 상품 조회 (MyBatis)
│   ├── ProductQueryMapper.java             # MyBatis Mapper 인터페이스
│   └── CategoryQueryMapper.java            # 카테고리 조회 Mapper
├── domain/
│   ├── Product.java                        # 상품 엔티티
│   ├── ProductOption.java                  # 상품 옵션 엔티티
│   ├── Category.java                       # 카테고리 엔티티
│   ├── InventoryHistory.java               # 재고 이력 엔티티
│   └── InventoryType.java                  # 입출고 구분 enum (IN, OUT)
└── dto/
    ├── ProductCreateRequest.java           # 상품 등록 요청 (옵션 포함)
    ├── ProductUpdateRequest.java           # 상품 수정 요청
    ├── ProductOptionDto.java               # 옵션 DTO
    ├── ProductListDto.java                 # 상품 목록 응답
    ├── ProductDetailDto.java               # 상품 상세 응답 (옵션 포함)
    ├── CategoryCreateRequest.java          # 카테고리 등록 요청
    ├── CategoryDto.java                    # 카테고리 응답
    └── InventoryRequest.java               # 입고/출고 요청

resources/mapper/product/
├── ProductQueryMapper.xml                  # 상품 조회 SQL
└── CategoryQueryMapper.xml                 # 카테고리 조회 SQL
```

## 7. CQRS 적용 방식

```
[ProductController]  ─── 등록/수정/삭제 ──→ [ProductCommandService]   ──→ [ProductRepository (JPA)]
                     └── 목록/상세 조회 ──→ [ProductQueryService]     ──→ [ProductQueryMapper (MyBatis)]

[CategoryController] ─── 등록 ──────────→ [CategoryCommandService]  ──→ [CategoryRepository (JPA)]
                     └── 목록 조회 ─────→ [ProductQueryService]     ──→ [CategoryQueryMapper (MyBatis)]

[InventoryController]─── 입고/출고 ─────→ [InventoryCommandService] ──→ [ProductOptionRepository (JPA)]
                                                                    ──→ [InventoryHistoryRepository (JPA)]
```

### 재고 입출고 로직 흐름
```
입고 요청 → ProductOption 조회 → stockQuantity += quantity → InventoryHistory 기록
출고 요청 → ProductOption 조회 → 재고 부족 검증 → stockQuantity -= quantity → InventoryHistory 기록
```

- ProductOption.stockQuantity: 현재 재고 (실시간 조회용)
- InventoryHistory: 입출고 이력 (감사 추적용)

## 8. 결정 사항 (Resolved)

- **Q1: ProductOption, Inventory를 이번에 함께 구현?**
  → ✅ **함께 구현한다** (Product + ProductOption + Inventory + Category 모두 포함)

- **Q2: 카테고리를 enum으로? 별도 테이블로?**
  → ✅ **별도 categories 테이블**로 관리 (관리자가 동적으로 추가/수정 가능)

- **Q3: 상품 이미지는?**
  → ❌ 이번엔 제외 (feature/07에서 구현)

## 9. 의존성

- **선행 feature**: feature/01-project-init (BaseEntity, ApiResponse, ErrorCode, SwaggerConfig)
- **선행 feature**: feature/02-security-jwt (인증 필터, SecurityConfig)
- **후행 feature**: feature/05-order (OrderItem이 Product/ProductOption을 참조, 가격 Snapshot)
- **후행 feature**: feature/06-redis-cache (상품 목록 캐싱)

## 10. 구현 순서

CQRS 학습 효과를 극대화하기 위해 아래 순서로 구현:

1. **Category** (가장 단순) — 테이블 설계 + JPA Entity + 간단 CRUD로 워밍업
2. **Product** (핵심) — CQRS 패턴 본격 적용 (Command: JPA, Query: MyBatis)
3. **ProductOption** — Product와 1:N 연관관계, Cascade 학습
4. **InventoryHistory + 재고 로직** — 비즈니스 로직(재고 부족 검증) 학습
5. **ErrorCode 추가** — 상품 도메인용 에러 코드
