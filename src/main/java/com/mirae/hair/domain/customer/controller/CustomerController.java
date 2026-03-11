package com.mirae.hair.domain.customer.controller;

import com.mirae.hair.domain.customer.dto.*;
import com.mirae.hair.domain.customer.service.CustomerService;
import com.mirae.hair.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 고객(미용실) REST API Controller
 *
 * 고객(미용실) CRUD API
 * → CustomerService에 등록/수정/삭제/조회를 위임한다.
 */
@Tag(name = "고객(미용실)", description = "고객(미용실/거래처) 관리 API")
@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerService customerService;

    /**
     * 고객 등록
     */
    @Operation(summary = "고객 등록", description = "새로운 미용실(거래처)을 등록합니다. 사업자번호 유효성 검증 + AES-256 암호화 저장")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCustomer(
            @RequestBody @Valid CustomerCreateRequest request) {
        Long customerId = customerService.createCustomer(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(customerId, "고객이 등록되었습니다"));
    }

    /**
     * 고객 목록 조회 (페이징 + 검색)
     */
    @Operation(summary = "고객 목록 조회", description = "고객 목록을 페이징/검색으로 조회합니다 (미용실명, 대표자명 검색)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<CustomerListDto>>> getCustomerList(
            @Parameter(description = "검색어 (미용실명/대표자명)")
            @RequestParam(required = false) String keyword,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<CustomerListDto> customers = customerService.getCustomerList(keyword, pageable);
        return ResponseEntity.ok(ApiResponse.success(customers, "고객 목록 조회 성공"));
    }

    /**
     * 고객 상세 조회
     */
    @Operation(summary = "고객 상세 조회", description = "고객 상세 정보를 조회합니다 (사업자번호 포함, 복호화)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CustomerDetailDto>> getCustomerDetail(
            @PathVariable Long id) {
        CustomerDetailDto customer = customerService.getCustomerDetail(id);
        return ResponseEntity.ok(ApiResponse.success(customer, "고객 상세 조회 성공"));
    }

    /**
     * 고객 수정
     */
    @Operation(summary = "고객 수정", description = "고객 정보를 수정합니다. 사업자번호 변경 시 유효성 재검증")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Long>> updateCustomer(
            @PathVariable Long id,
            @RequestBody @Valid CustomerUpdateRequest request) {
        Long customerId = customerService.updateCustomer(id, request);
        return ResponseEntity.ok(ApiResponse.success(customerId, "고객 정보가 수정되었습니다"));
    }

    /**
     * 고객 삭제 (Soft Delete)
     */
    @Operation(summary = "고객 삭제", description = "고객을 삭제(Soft Delete)합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteCustomer(@PathVariable Long id) {
        customerService.deleteCustomer(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
