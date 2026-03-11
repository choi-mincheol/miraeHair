package com.mirae.hair.domain.product.command;

import com.mirae.hair.domain.product.domain.Category;
import com.mirae.hair.domain.product.dto.CategoryCreateRequest;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 카테고리 Command 서비스 (생성/수정/삭제)
 *
 * CQRS 패턴에서 Command 담당:
 * → 데이터를 변경하는 작업(생성, 수정, 삭제)만 이 서비스에서 처리한다.
 * → 조회는 ProductQueryService(MyBatis)에서 처리한다.
 *
 * 왜 @Transactional이 필요한가?
 * → DB에 데이터를 변경하는 작업은 트랜잭션 안에서 실행되어야 한다.
 * → 중간에 에러가 나면 모든 변경 사항이 롤백(취소)된다.
 * → 비유: "은행 송금에서 출금은 됐는데 입금이 실패하면, 출금도 취소해야 한다"
 *   → 이것이 트랜잭션의 원자성(Atomicity)이다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CategoryCommandService {

    private final CategoryRepository categoryRepository;

    /**
     * 카테고리 등록
     *
     * @param request 카테고리 등록 요청 DTO
     * @return 생성된 카테고리 ID
     * @throws BusinessException 이미 존재하는 카테고리명인 경우 (CATEGORY_ALREADY_EXISTS)
     */
    public Long createCategory(CategoryCreateRequest request) {
        // 카테고리명 중복 검사
        if (categoryRepository.existsByName(request.getName())) {
            throw new BusinessException(ErrorCode.CATEGORY_ALREADY_EXISTS);
        }

        Category category = Category.create(request.getName(), request.getDisplayOrder());
        Category saved = categoryRepository.save(category);

        return saved.getId();
    }
}
