package com.mirae.hair.domain.product.controller;

import com.mirae.hair.domain.product.dto.ProductCreateRequest;
import com.mirae.hair.domain.product.dto.ProductDetailDto;
import com.mirae.hair.domain.product.dto.ProductListDto;
import com.mirae.hair.domain.product.dto.ProductUpdateRequest;
import com.mirae.hair.domain.product.service.ProductService;
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
 * 상품 REST API Controller
 *
 * 상품 CRUD API
 * → 등록/수정/삭제는 JPA, 조회는 MyBatis를 사용하는 ProductService에 위임한다.
 */
@Tag(name = "상품", description = "상품 관리 API")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * 상품 등록 (옵션 포함)
     *
     * @param request 상품 등록 요청 (옵션 목록 필수)
     * @return 생성된 상품 ID
     */
    @Operation(summary = "상품 등록", description = "새로운 상품을 옵션과 함께 등록합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공")
    @PostMapping
    public ResponseEntity<ApiResponse<Long>> createProduct(
            @RequestBody @Valid ProductCreateRequest request) {
        Long productId = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(productId, "상품이 등록되었습니다"));
    }

    /**
     * 상품 목록 조회 (페이징 + 검색 + 카테고리 필터)
     *
     * 왜 @PageableDefault를 사용하는가?
     * → 클라이언트가 page, size 파라미터를 보내지 않았을 때 기본값을 설정한다.
     * → 기본값: page=0 (첫 페이지), size=10 (한 페이지 10개)
     * → 사용 예: GET /api/products?page=0&size=10&keyword=모로칸&categoryId=2
     *
     * 왜 keyword와 categoryId를 @RequestParam(required = false)로 받는가?
     * → 검색어 없이 전체 목록을 볼 수도 있고, 검색할 수도 있다.
     * → required = false로 설정하면 파라미터가 없어도 에러가 나지 않고 null이 전달된다.
     * → MyBatis에서 null이면 해당 조건을 생략한다 (<if test="keyword != null">).
     *
     * @param keyword    검색어 (상품명/브랜드명, 선택)
     * @param categoryId 카테고리 ID (선택)
     * @param pageable   페이징 정보
     * @return 상품 목록 (Page 형태)
     */
    @Operation(summary = "상품 목록 조회", description = "상품 목록을 페이징/검색/필터로 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping
    public ResponseEntity<ApiResponse<Page<ProductListDto>>> getProductList(
            @Parameter(description = "검색어 (상품명/브랜드명)")
            @RequestParam(required = false) String keyword,
            @Parameter(description = "카테고리 ID")
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 10) Pageable pageable) {
        Page<ProductListDto> products = productService.getProductList(keyword, categoryId, pageable);
        return ResponseEntity.ok(ApiResponse.success(products, "상품 목록 조회 성공"));
    }

    /**
     * 상품 상세 조회 (옵션 포함)
     *
     * 왜 @PathVariable을 사용하는가?
     * → RESTful API에서 특정 리소스를 식별할 때 URL 경로에 ID를 포함시킨다.
     * → GET /api/products/1 → 1번 상품 조회
     * → 쿼리 파라미터(?id=1)보다 URL이 깔끔하고, REST 관례에 맞다.
     *
     * @param id 상품 ID
     * @return 상품 상세 정보 (옵션 목록 포함)
     */
    @Operation(summary = "상품 상세 조회", description = "상품 상세 정보를 옵션과 함께 조회합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductDetail(
            @PathVariable Long id) {
        ProductDetailDto product = productService.getProductDetail(id);
        return ResponseEntity.ok(ApiResponse.success(product, "상품 상세 조회 성공"));
    }

    /**
     * 상품 수정
     *
     * @param id      수정할 상품 ID
     * @param request 수정 요청 DTO
     * @return 수정된 상품 ID
     */
    @Operation(summary = "상품 수정", description = "상품 기본 정보를 수정합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공")
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Long>> updateProduct(
            @PathVariable Long id,
            @RequestBody @Valid ProductUpdateRequest request) {
        Long productId = productService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.success(productId, "상품이 수정되었습니다"));
    }

    /**
     * 상품 삭제 (Soft Delete)
     *
     * @param id 삭제할 상품 ID
     * @return 성공 응답
     */
    @Operation(summary = "상품 삭제", description = "상품을 삭제(Soft Delete)합니다")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공")
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.success());
    }
}
