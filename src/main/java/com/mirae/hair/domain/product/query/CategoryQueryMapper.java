package com.mirae.hair.domain.product.query;

import com.mirae.hair.domain.product.dto.CategoryDto;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * 카테고리 조회 MyBatis Mapper 인터페이스
 *
 * 카테고리 목록은 자주 조회되고, 단순한 쿼리이므로 MyBatis로 깔끔하게 처리한다.
 * → 향후 feature/06-redis-cache에서 카테고리 목록을 캐싱할 예정이다.
 */
@Mapper
public interface CategoryQueryMapper {

    /**
     * 전체 카테고리 목록 조회 (표시 순서대로)
     * → 상품 등록 화면에서 카테고리 드롭다운을 위해 사용한다.
     */
    List<CategoryDto> selectCategoryList();
}
