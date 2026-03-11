# Feature: 04-customer (고객/미용실 CRUD — CQRS 두 번째 적용)

## 1. 유저 스토리

> 관리자로서, 거래처(미용실) 정보를 등록/조회/수정/삭제하고 싶다. 그래서 어떤 미용실에 어떤 제품을 판매했는지 체계적으로 관리할 수 있다.

- US-01: 관리자로서, 새로운 미용실(거래처)을 등록하고 싶다. 그래서 주문 시 고객을 선택할 수 있다.
- US-02: 관리자로서, 미용실 목록을 검색/필터로 조회하고 싶다. 그래서 원하는 거래처를 빠르게 찾을 수 있다.
- US-03: 관리자로서, 미용실 상세 정보를 확인하고 싶다. 그래서 연락처, 주소 등을 파악할 수 있다.
- US-04: 관리자로서, 미용실 정보를 수정하고 싶다. 그래서 변경된 주소나 연락처를 반영할 수 있다.
- US-05: 관리자로서, 미용실을 삭제하고 싶다. 그래서 더 이상 거래하지 않는 미용실을 정리할 수 있다.

## 2. 기능 요구사항 (Functional Requirements)

| ID | 기능 | 설명 | 우선순위 |
|----|------|------|----------|
| FR-01 | 고객 등록 | 미용실명, 대표자명, 사업자번호, 전화번호, 주소 입력 → Customer 생성 | 높음 |
| FR-02 | 고객 목록 조회 | 페이징 + 검색(미용실명/대표자명) | 높음 |
| FR-03 | 고객 상세 조회 | ID로 단건 조회 | 높음 |
| FR-04 | 고객 수정 | 미용실명, 대표자명, 전화번호, 주소 등 수정 | 중간 |
| FR-05 | 고객 삭제 | Soft Delete (isDeleted 플래그) | 중간 |

## 3. 비기능 요구사항 (Non-Functional Requirements)

- NFR-01: 모든 API 응답 시간 500ms 이내
- NFR-02: 입력값 검증 (Bean Validation) — 미용실명 필수, 전화번호 형식 검증
- NFR-03: 인증된 ADMIN만 접근 가능 (JWT 토큰 필수)
- NFR-04: Soft Delete — 삭제 시 실제 DB에서 제거하지 않고 is_deleted = true 처리
- NFR-05: 사업자번호 중복 검사 (같은 사업자번호로 중복 등록 방지)
- NFR-06: 전화번호, 사업자번호 등 개인정보는 AES-256 암호화 저장 (ISMS 대비)
- NFR-07: 조회는 MyBatis(Query), 등록/수정/삭제는 JPA(Command) → CQRS

## 4. API 명세

| Method | URL | 설명 | 요청 Body | 응답 |
|--------|-----|------|-----------|------|
| POST | /api/customers | 고객 등록 | CustomerCreateRequest | ApiResponse\<Long\> |
| GET | /api/customers | 고객 목록 조회 | ?page=0&size=10&keyword= | ApiResponse\<Page\<CustomerListDto\>\> |
| GET | /api/customers/{id} | 고객 상세 조회 | - | ApiResponse\<CustomerDetailDto\> |
| PUT | /api/customers/{id} | 고객 수정 | CustomerUpdateRequest | ApiResponse\<Long\> |
| DELETE | /api/customers/{id} | 고객 삭제 (Soft) | - | ApiResponse\<Void\> |

### 4-1. 요청/응답 DTO 상세

**CustomerCreateRequest**
```json
{
  "shopName": "헤어살롱 미래",
  "ownerName": "김미래",
  "businessNumber": "123-45-67890",
  "phone": "02-1234-5678",
  "address": "서울시 강남구 역삼동 123-4",
  "memo": "월 2회 정기 주문 거래처"
}
```

**CustomerUpdateRequest**
```json
{
  "shopName": "헤어살롱 미래 (강남점)",
  "ownerName": "김미래",
  "phone": "02-9876-5432",
  "address": "서울시 강남구 테헤란로 456",
  "memo": "월 4회로 주문 빈도 증가"
}
```

**CustomerListDto** (목록 조회용 — 간략 정보)
```json
{
  "id": 1,
  "shopName": "헤어살롱 미래",
  "ownerName": "김미래",
  "phone": "02-1234-5678",
  "address": "서울시 강남구 역삼동 123-4",
  "createdAt": "2026-03-11T10:00:00"
}
```

**CustomerDetailDto** (상세 조회용 — 전체 정보)
```json
{
  "id": 1,
  "shopName": "헤어살롱 미래",
  "ownerName": "김미래",
  "businessNumber": "123-45-67890",
  "phone": "02-1234-5678",
  "address": "서울시 강남구 역삼동 123-4",
  "memo": "월 2회 정기 주문 거래처",
  "deleted": false,
  "createdAt": "2026-03-11T10:00:00",
  "updatedAt": "2026-03-11T10:00:00",
  "createdBy": "admin@mirae.com"
}
```

## 5. ERD (Entity 설계)

### customers 테이블
| 컬럼명 | 타입 | 제약조건 | 설명 |
|--------|------|----------|------|
| id | BIGSERIAL | PK | 고객 ID |
| shop_name | VARCHAR(100) | NOT NULL | 미용실명 |
| owner_name | VARCHAR(50) | NOT NULL | 대표자명 |
| business_number | VARCHAR(200) | NOT NULL, UNIQUE | 사업자번호 (AES-256 암호화 저장) |
| phone | VARCHAR(200) | NOT NULL | 전화번호 (AES-256 암호화 저장) |
| address | VARCHAR(300) | NOT NULL | 주소 |
| memo | TEXT | NULLABLE | 메모 (특이사항, 주문 빈도 등) |
| is_deleted | BOOLEAN | NOT NULL, DEFAULT false | 삭제 여부 |
| created_at | TIMESTAMP | NOT NULL | 생성일시 (BaseEntity) |
| updated_at | TIMESTAMP | NOT NULL | 수정일시 (BaseEntity) |
| created_by | VARCHAR(50) | NOT NULL | 생성자 (BaseEntity) |
| updated_by | VARCHAR(50) | | 수정자 (BaseEntity) |

### 암호화 대상 필드
| 필드 | 이유 |
|------|------|
| business_number | 사업자번호는 개인정보 (ISMS 대비) |
| phone | 전화번호는 개인정보 (ISMS 대비) |

→ AES256Util로 암호화하여 DB에 저장, 조회 시 복호화하여 응답

## 6. 파일 구조 (구현 예정)

```
domain/customer/
├── controller/
│   └── CustomerController.java          # REST API
├── command/
│   ├── CustomerCommandService.java      # 등록/수정/삭제 (JPA)
│   └── CustomerRepository.java          # JPA Repository
├── query/
│   ├── CustomerQueryService.java        # 목록/상세 조회 (MyBatis)
│   └── CustomerQueryMapper.java         # MyBatis Mapper 인터페이스
├── domain/
│   └── Customer.java                    # 고객 엔티티
└── dto/
    ├── CustomerCreateRequest.java       # 등록 요청
    ├── CustomerUpdateRequest.java       # 수정 요청
    ├── CustomerListDto.java             # 목록 조회 응답
    └── CustomerDetailDto.java           # 상세 조회 응답

resources/mapper/customer/
└── CustomerQueryMapper.xml              # MyBatis SQL
```

## 7. CQRS 적용 방식

```
[CustomerController] ─── POST/PUT/DELETE ──→ [CustomerCommandService] ──→ [CustomerRepository (JPA)]
                     └── GET ──────────────→ [CustomerQueryService]   ──→ [CustomerQueryMapper (MyBatis)]
```

## 8. 불명확한 사항 (Clarifications Needed)

- **Q1: 사업자번호/전화번호를 AES-256 암호화해서 저장할까요?**
  - feature/02에서 AES256Util이 이미 구현되어 있습니다.
  - **방안 A**: 암호화 저장 (ISMS 기준 준수, 포트폴리오에서 보안 역량 어필)
  - **방안 B**: 평문 저장 (구현이 단순)
  - → 추천: **방안 A** (포트폴리오에서 보안 의식을 보여줄 수 있음)

- **Q2: 사업자번호 수정을 허용할까요?**
  - → ✅ **수정 가능** (단, 저장 전 사업자번호 유효성 검증 필수)
  - → BusinessNumberValidator 유틸 추가: 한국 사업자등록번호 10자리 검증 알고리즘 적용

## 9. 의존성

- **선행 feature**: feature/01-project-init (BaseEntity, ApiResponse, ErrorCode)
- **선행 feature**: feature/02-security-jwt (AES256Util, 인증 필터)
- **후행 feature**: feature/05-order (Order가 Customer를 참조)

## 10. 구현 순서

상품(feature/03)과 거의 동일한 CQRS 패턴이므로 빠르게 구현 가능:

1. **Customer 엔티티** — AES-256 암호화 적용이 핵심 포인트
2. **DTO** — 요청/응답 DTO
3. **Repository + CommandService** — JPA (등록/수정/삭제)
4. **QueryMapper + QueryService** — MyBatis (목록/상세 조회)
5. **Controller** — REST API
6. **ErrorCode 추가** — 고객 도메인용 에러 코드
