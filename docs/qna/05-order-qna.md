# Feature 05: 주문/판매 (가격 Snapshot) — 면접 Q&A

---

## Q1. 가격 Snapshot이란 무엇이고, 왜 필요한가요?

**A:** 주문 시점의 상품 가격을 OrderItem에 복사 저장하는 것입니다.

```
주문 시점: 모로칸오일 100ml = 45,000원 → OrderItem.unitPrice = 45,000
일주일 후: 모로칸오일 100ml 가격이 50,000원으로 인상
→ OrderItem에는 여전히 45,000원이 기록 (주문 시점 가격 보존)
```

Snapshot이 없으면:
- 상품 가격이 변경되면 **과거 주문 금액도 바뀌어** 버립니다.
- 3월에 45,000원에 팔았는데, 4월에 가격 인상하면 3월 매출이 50,000원으로 뻥튀기됩니다.
- 상품이 삭제되면 주문 상세를 조회할 수 없게 됩니다.

```java
// OrderItem.createSnapshot() — 가격을 "복사"하는 핵심 코드
public static OrderItem createSnapshot(Order order, Product product,
                                        ProductOption option, int quantity) {
    int unitPrice = product.getPrice() + option.getAdditionalPrice();
    return OrderItem.builder()
            .productName(product.getName())     // 상품명 복사
            .optionName(option.getOptionName())  // 옵션명 복사
            .unitPrice(unitPrice)                // 가격 복사
            .quantity(quantity)
            .subtotal(unitPrice * quantity)
            .build();
}
```

---

## Q2. 주문 취소를 DELETE가 아닌 POST /cancel로 구현한 이유는?

**A:** REST API에서 HTTP 메서드는 **의미(Semantics)**를 가집니다.

| HTTP 메서드 | 의미 | 주문 취소에 적합? |
|------------|------|-----------------|
| DELETE | 리소스를 삭제한다 | X — 주문을 삭제하는 게 아님 |
| PUT | 리소스 전체를 교체한다 | X — 상태만 변경하는 것 |
| PATCH | 리소스 일부를 수정한다 | △ — 가능하지만 의미가 약함 |
| POST /cancel | "취소" 행위를 수행한다 | O — 가장 명확 |

주문 취소는 "삭제"가 아니라 **"상태 변경"**입니다:
- 취소된 주문도 기록으로 남아야 합니다 (회계, 감사 추적).
- DELETE로 구현하면 주문 이력이 사라집니다.
- POST /cancel은 "취소라는 행위를 수행한다"는 의미로 REST 설계에 가장 적합합니다.

---

## Q3. 주문 등록 시 가격을 클라이언트에서 받지 않고 서버에서 조회하는 이유는?

**A:** 클라이언트가 보내는 가격을 그대로 저장하면 **가격 조작이 가능**합니다.

```json
// 악의적 요청 — 45,000원짜리 상품을 1,000원으로 주문
{
  "customerId": 1,
  "items": [{ "productOptionId": 1, "quantity": 1, "unitPrice": 1000 }]
}
```

"가격은 항상 서버에서 조회"하는 것이 보안의 기본 원칙입니다:
```java
// 서버에서 가격 조회 (조작 불가)
ProductOption option = productOptionRepository.findById(optionId);
int unitPrice = option.getProduct().getPrice() + option.getAdditionalPrice();
```

클라이언트에서는 `productOptionId`와 `quantity`만 보내고, 가격은 서버가 결정합니다.

---

## Q4. cancelOrder에서 JOIN FETCH를 사용하는 이유는?

**A:** N+1 문제를 방지하기 위해서입니다.

```java
// LAZY 로딩 (N+1 발생)
Order order = orderRepository.findById(id);  // 쿼리 1번
for (OrderItem item : order.getItems()) {    // 여기서 추가 쿼리 1번
    // item마다 ProductOption 조회 → 추가 쿼리 N번
}
// 총: 1 + 1 + N번 쿼리

// JOIN FETCH (쿼리 1번)
Order order = orderRepository.findByIdWithItems(id);  // 쿼리 1번으로 Order + Items 함께 조회
for (OrderItem item : order.getItems()) {
    // 이미 로딩됨 → 추가 쿼리 없음
}
```

```java
// Repository에 정의된 JOIN FETCH 쿼리
@Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
Optional<Order> findByIdWithItems(@Param("id") Long id);
```

---

## Q5. 주문 등록이 하나의 트랜잭션이어야 하는 이유는?

**A:** 주문 등록은 여러 단계로 구성됩니다:

```
1) 고객 확인 → 2) 상품 옵션 조회 → 3) 재고 차감 → 4) OrderItem 생성 → 5) Order 저장
```

만약 3단계(재고 차감)까지 성공하고 5단계(저장)에서 실패하면?
- 재고는 차감되었는데 주문 기록이 없음 → **데이터 불일치**
- 실제보다 재고가 적게 표시됨 → 판매 기회 손실

`@Transactional`로 묶으면:
- 5단계 중 하나라도 실패하면 **전체 롤백** (1~4단계도 되돌림)
- 모두 성공해야 **커밋** → 데이터 정합성 보장

```java
@Transactional  // 이 메서드 안의 모든 DB 작업이 하나의 트랜잭션
public Long createOrder(OrderCreateRequest request) {
    // 하나라도 실패하면 → 전체 롤백
    // 모두 성공하면 → 커밋
}
```
