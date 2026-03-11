package com.mirae.hair.domain.product.command;

import com.mirae.hair.domain.product.domain.Category;
import com.mirae.hair.domain.product.domain.Product;
import com.mirae.hair.domain.product.domain.ProductOption;
import com.mirae.hair.domain.product.dto.ProductCreateRequest;
import com.mirae.hair.domain.product.dto.ProductOptionRequest;
import com.mirae.hair.domain.product.dto.ProductUpdateRequest;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 상품 Command 서비스 (생성/수정/삭제)
 *
 * CQRS 패턴에서 Command 담당:
 * → 상품 등록, 수정, 삭제 (데이터 변경)만 처리한다.
 * → 조회는 ProductQueryService(MyBatis)에서 처리한다.
 *
 * 왜 Command와 Query를 분리하는가?
 * → "쓰기"와 "읽기"는 요구사항이 다르다.
 *   - 쓰기: 데이터 무결성, 트랜잭션, 검증이 중요 → JPA가 적합
 *   - 읽기: 성능, 유연한 SQL, 복잡한 JOIN이 중요 → MyBatis가 적합
 * → 분리하면 각각의 장점을 살릴 수 있고, 코드도 역할별로 깔끔하게 나뉜다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class ProductCommandService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    /**
     * 상품 등록 (옵션 포함)
     *
     * 처리 흐름:
     * 1) 카테고리 존재 여부 확인
     * 2) Product 엔티티 생성
     * 3) 각 옵션 → ProductOption 엔티티 생성 → Product에 추가
     * 4) productRepository.save() → CascadeType.ALL로 옵션도 함께 저장
     *
     * 왜 Product를 먼저 생성하고 옵션을 추가하는가?
     * → ProductOption은 Product 없이 존재할 수 없다 (product_id FK NOT NULL).
     * → Product를 먼저 만들고, 옵션에 product 참조를 설정해야 한다.
     * → CascadeType.ALL 덕분에 Product를 save하면 옵션도 자동으로 INSERT된다.
     *
     * @param request 상품 등록 요청 DTO (옵션 목록 포함)
     * @return 생성된 상품 ID
     */
    public Long createProduct(ProductCreateRequest request) {
        // 1. 카테고리 조회 (없으면 예외)
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. 상품 생성
        Product product = Product.create(
                request.getName(),
                request.getBrand(),
                category,
                request.getPrice(),
                request.getDescription()
        );

        // 3. 옵션 생성 및 상품에 추가
        for (ProductOptionRequest optionRequest : request.getOptions()) {
            ProductOption option = ProductOption.create(
                    product,
                    optionRequest.getOptionName(),
                    optionRequest.getAdditionalPrice(),
                    optionRequest.getStockQuantity()
            );
            product.addOption(option);
        }

        // 4. 저장 (CascadeType.ALL → 옵션도 함께 INSERT)
        Product saved = productRepository.save(product);

        return saved.getId();
    }

    /**
     * 상품 수정 (기본 정보만)
     *
     * 왜 옵션은 수정하지 않는가?
     * → 상품 기본 정보(이름, 가격 등)와 옵션은 독립적으로 관리하는 것이 좋다.
     * → 옵션 수정까지 한 번에 처리하면 API가 복잡해진다.
     * → 향후 옵션 수정/추가/삭제는 별도 API로 확장 가능하다.
     *
     * 왜 명시적 save()를 호출하지 않는가?
     * → JPA의 더티체킹(Dirty Checking) 덕분이다.
     * → @Transactional 안에서 엔티티의 필드를 변경하면,
     *   트랜잭션 종료 시 JPA가 변경을 감지해서 자동으로 UPDATE SQL을 실행한다.
     * → product.update(...) 호출만으로 DB에 반영된다.
     *
     * @param id      수정할 상품 ID
     * @param request 수정 요청 DTO
     * @return 수정된 상품 ID
     */
    public Long updateProduct(Long id, ProductUpdateRequest request) {
        // 1. 상품 조회 (삭제되지 않은 것만)
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        // 2. 카테고리 조회
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        // 3. 상품 정보 수정 (더티체킹으로 자동 UPDATE)
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
     *
     * 왜 실제 DELETE가 아닌 Soft Delete인가?
     * → 상품을 물리적으로 삭제하면 주문 이력에서 "어떤 상품이었는지" 알 수 없게 된다.
     * → is_deleted = true로 표시만 하고, 조회 시 필터링한다.
     * → 필요하면 복구도 가능하다.
     *
     * @param id 삭제할 상품 ID
     */
    public void deleteProduct(Long id) {
        Product product = productRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

        product.softDelete();
    }
}
