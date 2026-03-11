package com.mirae.hair.domain.order.controller;

import com.mirae.hair.domain.order.command.OrderCommandService;
import com.mirae.hair.domain.order.dto.OrderCreateRequest;
import com.mirae.hair.domain.order.dto.OrderDetailDto;
import com.mirae.hair.domain.order.dto.OrderListDto;
import com.mirae.hair.domain.order.query.OrderQueryService;
import com.mirae.hair.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

/**
 * 주문 REST API Controller
 *
 * CQRS 패턴 적용:
 * → POST(등록/취소) → OrderCommandService (JPA)
 * → GET(목록/상세) → OrderQueryService (MyBatis)
 */
@Tag(name = "주문", description = "주문/판매 관리 API")
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderCommandService orderCommandService;
    private final OrderQueryService orderQueryService;

    /**
     * 주문 등록 (가격 Snapshot + 재고 차감)
     */
    @Operation(summary = "주문 등록", description = "미용실에 상품을 판매한 주문을 등록합니다. 가격 Snapshot 저장 + 재고 자동 차감")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createOrder(
            @RequestBody @Valid OrderCreateRequest request) {
        Long orderId = orderCommandService.createOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(orderId, "주문이 등록되었습니다"));
    }

    /**
     * 주문 목록 조회 (페이징 + 고객 필터 + 기간 필터)
     *
     * 왜 @DateTimeFormat이 필요한가?
     * → 쿼리 파라미터는 문자열로 전달된다 (예: "2026-03-01").
     * → @DateTimeFormat(iso = DATE)를 붙이면 "yyyy-MM-dd" 형식의 문자열을
     *   LocalDate 객체로 자동 변환해준다.
     */
    @Operation(summary = "주문 목록 조회", description = "주문 목록을 고객/기간별로 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<OrderListDto>>> getOrderList(
            @Parameter(description = "고객(미용실) ID")
            @RequestParam(required = false) Long customerId,
            @Parameter(description = "시작일 (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료일 (yyyy-MM-dd)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<OrderListDto> orders = orderQueryService.getOrderList(customerId, startDate, endDate, pageable);
        return ResponseEntity.ok(ApiResponse.success(orders, "주문 목록 조회 성공"));
    }

    /**
     * 주문 상세 조회 (주문항목 포함)
     */
    @Operation(summary = "주문 상세 조회", description = "주문 상세 정보를 주문항목(가격 Snapshot)과 함께 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDetailDto>> getOrderDetail(
            @PathVariable Long id) {
        OrderDetailDto order = orderQueryService.getOrderDetail(id);
        return ResponseEntity.ok(ApiResponse.success(order, "주문 상세 조회 성공"));
    }

    /**
     * 주문 취소 (상태 변경 + 재고 복원)
     *
     * 왜 DELETE가 아닌 POST /cancel인가?
     * → DELETE는 "리소스를 삭제"하는 의미이다.
     * → 주문 취소는 "삭제"가 아니라 "상태를 변경"하는 것이다.
     * → 취소된 주문도 기록으로 남아야 하므로 DELETE는 적합하지 않다.
     * → POST /cancel은 "취소라는 행위를 수행한다"는 의미로 더 적합하다.
     */
    @Operation(summary = "주문 취소", description = "주문을 취소합니다. 재고가 자동으로 복원됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공")
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<Void>> cancelOrder(@PathVariable Long id) {
        orderCommandService.cancelOrder(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
