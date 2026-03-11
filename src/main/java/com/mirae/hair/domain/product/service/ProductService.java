package com.mirae.hair.domain.product.service;

import com.mirae.hair.domain.product.command.CategoryRepository;
import com.mirae.hair.domain.product.command.ProductRepository;
import com.mirae.hair.domain.product.domain.Category;
import com.mirae.hair.domain.product.domain.Product;
import com.mirae.hair.domain.product.domain.ProductOption;
import com.mirae.hair.domain.product.dto.*;
import com.mirae.hair.domain.product.query.ProductQueryMapper;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 상품 서비스 (Command + Query 통합)
 *
 * JPA(등록/수정/삭제)와 MyBatis(조회)를 하나의 서비스에서 함께 사용한다.
 * → 클래스 레벨에 @Transactional을 붙이고,
 *   조회 메서드에만 @Transactional(readOnly = true)를 개별 지정한다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductQueryMapper productQueryMapper;

    // ─────────────────────────────────────────
    // Command (등록/수정/삭제) — JPA
    // ─────────────────────────────────────────

    /**
     * 상품 등록 (옵션 포함)
     *
     * 처리 흐름:
     * 1) 카테고리 존재 여부 확인
     * 2) Product 엔티티 생성
     * 3) 각 옵션 → ProductOption 엔티티 생성 → Product에 추가
     * 4) productRepository.save() → CascadeType.ALL로 옵션도 함께 저장
     */
    public Long createProduct(ProductCreateRequest request) {
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        Product product = Product.create(
                request.getName(),
                request.getBrand(),
                category,
                request.getPrice(),
                request.getDescription()
        );

        for (ProductOptionRequest optionRequest : request.getOptions()) {
            ProductOption option = ProductOption.create(
                    product,
                    optionRequest.getOptionName(),
                    optionRequest.getAdditionalPrice(),
                    optionRequest.getStockQuantity()
            );
            product.addOption(option);
        }

        Product saved = productRepository.save(product);
        return saved.getId();
    }

    /**
     * 상품 수정 (기본 정보만)
     *
     * 왜 명시적 save()를 호출하지 않는가?
     * → JPA의 더티체킹(Dirty Checking) 덕분이다.
     * → @Transactional 안에서 엔티티의 필드를 변경하면,
     *   트랜잭션 종료 시 JPA가 변경을 감지해서 자동으로 UPDATE SQL을 실행한다.
     */
    public Long updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        product.update(
                request.getName(),
                request.getBrand(),
                category,
                request.getPrice(),
                request.getDescription()
        );

        return product.getId();
    }

    /**
     * 상품 삭제 (Soft Delete)
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.softDelete();
    }

    // ─────────────────────────────────────────
    // Query (조회) — MyBatis
    // ─────────────────────────────────────────

    /**
     * 상품 목록 조회 (페이징 + 검색 + 카테고리 필터)
     *
     * 왜 @Transactional(readOnly = true)인가?
     * → 조회 전용이므로 JPA 더티체킹을 생략해서 성능 향상
     * → 실수로 데이터를 수정하는 것도 방지
     */
    @Transactional(readOnly = true)
    public Page<ProductListDto> getProductList(String keyword, Long categoryId, Pageable pageable) {
        long offset = pageable.getOffset();
        int size = pageable.getPageSize();

        List<ProductListDto> products = productQueryMapper.selectProductList(keyword, categoryId, offset, size);
        long totalCount = productQueryMapper.countProducts(keyword, categoryId);

        return new PageImpl<>(products, pageable, totalCount);
    }

    /**
     * 상품 상세 조회 (옵션 목록 포함)
     */
    @Transactional(readOnly = true)
    public ProductDetailDto getProductDetail(Long id) {
        ProductDetailDto product = productQueryMapper.selectProductDetail(id);

        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return product;
    }
}
