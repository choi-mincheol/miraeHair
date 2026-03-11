package com.mirae.hair.domain.product.controller;

import com.mirae.hair.domain.product.dto.CategoryCreateRequest;
import com.mirae.hair.domain.product.dto.CategoryUpdateRequest;
import com.mirae.hair.domain.product.dto.CategoryDto;
import com.mirae.hair.domain.product.service.CategoryService;
import com.mirae.hair.global.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 카테고리 REST API Controller
 *
 * 카테고리 CRUD API
 * → CategoryService에 등록/수정/조회를 위임한다.
 */
@Tag(name = "카테고리", description = "카테고리 관리 API")
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 카테고리 등록
     *
     * 왜 ResponseEntity로 감싸는가?
     * → HTTP 상태 코드를 명시적으로 지정할 수 있다.
     * → 생성 성공 시 201(CREATED)을 반환한다. 200(OK)과 구분하기 위해서이다.
     * → REST API 관례: 리소스 생성 시 201, 일반 성공 시 200
     */
    @Operation(summary = "카테고리 등록", description = "새로운 카테고리를 등록합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createCategory(
            @RequestBody @Valid CategoryCreateRequest request) {
        Long categoryId = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(categoryId, "카테고리가 등록되었습니다"));
    }

    /**
     * 카테고리 목록 조회
     */
    @Operation(summary = "카테고리 목록 조회", description = "전체 카테고리 목록을 표시 순서대로 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDto>>> getCategoryList() {
        List<CategoryDto> categories = categoryService.getCategoryList();
        return ResponseEntity.ok(ApiResponse.success(categories, "카테고리 목록 조회 성공"));
    }

    /**
     * 카테고리 수정
     */
    @Operation(summary = "카테고리 수정", description = "카테고리 정보를 수정합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Long>> updateCategory(
            @PathVariable Long id,
            @RequestBody @Valid CategoryUpdateRequest request) {
        Long categoryId = categoryService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.success(categoryId, "카테고리가 수정되었습니다"));
    }
}
