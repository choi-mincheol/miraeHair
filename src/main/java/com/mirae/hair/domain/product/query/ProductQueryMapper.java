package com.mirae.hair.domain.product.query;

import com.mirae.hair.domain.product.dto.ProductDetailDto;
import com.mirae.hair.domain.product.dto.ProductListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 상품 조회 MyBatis Mapper 인터페이스
 *
 * CQRS 패턴에서 Query(조회) 담당:
 * → 실제 SQL은 resources/mapper/product/ProductQueryMapper.xml에 작성한다.
 * → 이 인터페이스의 메서드명과 XML의 <select id="">가 1:1로 매핑된다.
 *
 * 왜 @Mapper 어노테이션이 필요한가?
 * → MyBatisConfig에서 @MapperScan(annotationClass = Mapper.class)으로 설정했다.
 * → @Mapper가 붙은 인터페이스만 MyBatis Mapper로 등록된다.
 * → JPA Repository와 MyBatis Mapper를 명확히 구분할 수 있다.
 *
 * 왜 @Param을 사용하는가?
 * → MyBatis XML에서 #{keyword}, #{categoryId}로 파라미터를 참조한다.
 * → @Param으로 이름을 지정하지 않으면 #{param1}, #{param2}로만 참조 가능하다.
 * → @Param("keyword")로 지정하면 #{keyword}로 참조할 수 있어 가독성이 좋다.
 */
@Mapper
public interface ProductQueryMapper {

    /**
     * 상품 목록 조회 (페이징 + 검색 + 카테고리 필터)
     *
     * @param keyword    검색어 (상품명 또는 브랜드명, nullable)
     * @param categoryId 카테고리 ID (nullable — null이면 전체)
     * @param offset     건너뛸 행 수 (페이지 * 사이즈)
     * @param size       한 페이지당 조회할 행 수
     * @return 상품 목록 (간략 정보)
     */
    List<ProductListDto> selectProductList(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId,
            @Param("offset") long offset,
            @Param("size") int size
    );

    /**
     * 상품 총 건수 (페이징 계산용)
     * → Spring Data의 Page 객체를 만들려면 전체 건수가 필요하다.
     */
    long countProducts(
            @Param("keyword") String keyword,
            @Param("categoryId") Long categoryId
    );

    /**
     * 상품 상세 조회 (옵션 목록 포함)
     * → MyBatis의 <collection>으로 옵션을 중첩 매핑한다.
     *
     * @param id 상품 ID
     * @return 상품 상세 정보 (옵션 포함), 없으면 null
     */
    ProductDetailDto selectProductDetail(@Param("id") Long id);
}
