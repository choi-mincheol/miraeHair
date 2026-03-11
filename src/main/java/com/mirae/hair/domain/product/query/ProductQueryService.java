package com.mirae.hair.domain.product.query;

import com.mirae.hair.domain.product.dto.CategoryDto;
import com.mirae.hair.domain.product.dto.ProductDetailDto;
import com.mirae.hair.domain.product.dto.ProductListDto;
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
 * 상품/카테고리 Query 서비스 (조회 전용)
 *
 * CQRS 패턴에서 Query 담당:
 * → 모든 조회를 MyBatis Mapper로 처리한다.
 * → JPA Repository는 사용하지 않는다 (Command에서만 사용).
 *
 * 왜 @Transactional(readOnly = true)인가?
 * → 조회 전용이므로 JPA 더티체킹(변경 감지)을 생략해서 성능이 향상된다.
 * → 실수로 데이터를 수정하는 것도 방지한다.
 * → 비유: "읽기 전용 모드 — 볼 수만 있고 편집은 안 된다"
 *
 * MyBatis 조회 + Spring Data Page:
 * → MyBatis는 Spring Data의 Pageable을 직접 지원하지 않는다.
 * → 그래서 Pageable에서 offset, size를 추출해서 SQL LIMIT/OFFSET에 전달한다.
 * → 결과를 PageImpl로 감싸서 Spring Data의 Page 인터페이스를 반환한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductQueryService {

    private final ProductQueryMapper productQueryMapper;
    private final CategoryQueryMapper categoryQueryMapper;

    /**
     * 상품 목록 조회 (페이징 + 검색 + 카테고리 필터)
     *
     * 처리 흐름:
     * 1) Pageable에서 offset, size 추출
     * 2) MyBatis로 목록 조회 (LIMIT/OFFSET)
     * 3) MyBatis로 총 건수 조회
     * 4) PageImpl로 감싸서 반환
     *
     * 왜 PageImpl을 사용하는가?
     * → Spring Data JPA의 Repository는 Page를 자동으로 반환한다.
     * → MyBatis는 이 기능이 없으므로, 수동으로 PageImpl을 생성해야 한다.
     * → PageImpl(content, pageable, totalElements)로 Page 객체를 만들 수 있다.
     * → 이렇게 하면 Controller에서 JPA든 MyBatis든 동일한 Page<T> 타입으로 처리 가능하다.
     *
     * @param keyword    검색어 (상품명/브랜드명, nullable)
     * @param categoryId 카테고리 ID (nullable — null이면 전체)
     * @param pageable   페이징 정보 (page, size)
     * @return 상품 목록 (Page 형태)
     */
    public Page<ProductListDto> getProductList(String keyword, Long categoryId, Pageable pageable) {
        // MyBatis는 Pageable을 직접 지원하지 않으므로 offset/size를 추출
        long offset = pageable.getOffset();
        int size = pageable.getPageSize();

        // 목록 조회 + 총 건수 조회
        List<ProductListDto> products = productQueryMapper.selectProductList(keyword, categoryId, offset, size);
        long totalCount = productQueryMapper.countProducts(keyword, categoryId);

        // PageImpl로 감싸서 반환
        return new PageImpl<>(products, pageable, totalCount);
    }

    /**
     * 상품 상세 조회 (옵션 목록 포함)
     *
     * @param id 상품 ID
     * @return 상품 상세 정보 (옵션 포함)
     * @throws BusinessException 상품이 존재하지 않는 경우 (PRODUCT_NOT_FOUND)
     */
    public ProductDetailDto getProductDetail(Long id) {
        ProductDetailDto product = productQueryMapper.selectProductDetail(id);

        if (product == null) {
            throw new BusinessException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        return product;
    }

    /**
     * 카테고리 목록 조회
     *
     * @return 전체 카테고리 목록 (표시 순서대로)
     */
    public List<CategoryDto> getCategoryList() {
        return categoryQueryMapper.selectCategoryList();
    }
}
