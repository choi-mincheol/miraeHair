package com.mirae.hair.domain.product.service;

import com.mirae.hair.domain.product.command.CategoryRepository;
import com.mirae.hair.domain.product.domain.Category;
import com.mirae.hair.domain.product.dto.CategoryCreateRequest;
import com.mirae.hair.domain.product.dto.CategoryDto;
import com.mirae.hair.domain.product.dto.CategoryUpdateRequest;
import com.mirae.hair.domain.product.query.CategoryQueryMapper;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카테고리 서비스 (Command + Query 통합)
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryQueryMapper categoryQueryMapper;

    // ─────────────────────────────────────────
    // Command (등록/수정) — JPA
    // ─────────────────────────────────────────

    /**
     * 카테고리 등록
     */
    public Long createCategory(CategoryCreateRequest request) {
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = Category.create(request.getName(), request.getDisplayOrder());
        Category saved = categoryRepository.save(category);

        return saved.getId();
    }

    /**
     * 카테고리 수정
     */
    public Long updateCategory(Long id, CategoryUpdateRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));

        category.update(request.getName(), request.getDisplayOrder());

        return category.getId();
    }

    // ─────────────────────────────────────────
    // Query (조회) — MyBatis
    // ─────────────────────────────────────────

    /**
     * 카테고리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<CategoryDto> getCategoryList() {
        return categoryQueryMapper.selectCategoryList();
    }
}
