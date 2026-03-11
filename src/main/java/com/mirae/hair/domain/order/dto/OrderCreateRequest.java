package com.mirae.hair.domain.order.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 주문 등록 요청 DTO
 *
 * 고객(미용실)을 선택하고, 주문할 상품 옵션/수량을 전달한다.
 * → 가격은 서버에서 조회 + Snapshot 처리 (클라이언트 가격 신뢰하지 않음)
 * → 재고 차감도 서버에서 자동 처리
 */
@Getter
@NoArgsConstructor
public class OrderCreateRequest {

    /** 주문 고객(미용실) ID */
    @NotNull(message = "고객 ID는 필수입니다")
    private Long customerId;

    /** 주문항목 목록 (최소 1개) */
    @NotEmpty(message = "주문항목은 최소 1개 이상이어야 합니다")
    @Valid
    private List<OrderItemRequest> items;

    /** 주문 메모 (선택) */
    private String memo;
}
