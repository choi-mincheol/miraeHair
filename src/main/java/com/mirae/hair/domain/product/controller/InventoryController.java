package com.mirae.hair.domain.product.controller;

import com.mirae.hair.domain.product.command.InventoryCommandService;
import com.mirae.hair.domain.product.dto.InventoryRequest;
import com.mirae.hair.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 재고 관리 REST API Controller
 *
 * 입고(/in)와 출고(/out)를 별도 URL로 구분한다.
 * → 같은 InventoryRequest DTO를 사용하지만, 비즈니스 로직이 다르다.
 * → 입고: 재고 증가 (항상 성공)
 * → 출고: 재고 감소 (부족하면 실패)
 *
 * 왜 PUT이 아닌 POST인가?
 * → PUT은 "리소스를 대체"하는 의미이다 (예: 재고를 50으로 세팅).
 * → POST는 "리소스를 처리"하는 의미이다 (예: 재고에 20을 더하기).
 * → 입고/출고는 "수량을 더하기/빼기"이므로 POST가 적합하다.
 */
@Tag(name = "재고", description = "재고 입고/출고 관리 API")
@RestController
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryCommandService inventoryCommandService;

    /**
     * 재고 입고
     *
     * @param request 입고 요청 (옵션 ID, 수량, 사유)
     * @return 성공 응답
     */
    @Operation(summary = "재고 입고", description = "상품 옵션의 재고를 입고 처리합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "입고 성공")
    @PostMapping("/in")
    public ResponseEntity<ApiResponse<Void>> stockIn(
            @RequestBody @Valid InventoryRequest request) {
        inventoryCommandService.stockIn(request);
        return ResponseEntity.ok(ApiResponse.success());
    }

    /**
     * 재고 출고
     *
     * @param request 출고 요청 (옵션 ID, 수량, 사유)
     * @return 성공 응답
     * @throws com.mirae.hair.global.exception.BusinessException 재고 부족 시 (INSUFFICIENT_STOCK)
     */
    @Operation(summary = "재고 출고", description = "상품 옵션의 재고를 출고 처리합니다. 재고 부족 시 에러가 반환됩니다.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "출고 성공")
    @PostMapping("/out")
    public ResponseEntity<ApiResponse<Void>> stockOut(
            @RequestBody @Valid InventoryRequest request) {
        inventoryCommandService.stockOut(request);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
