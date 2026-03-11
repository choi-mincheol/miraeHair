package com.mirae.hair.domain.product.command;

import com.mirae.hair.domain.product.domain.InventoryHistory;
import com.mirae.hair.domain.product.domain.InventoryType;
import com.mirae.hair.domain.product.domain.ProductOption;
import com.mirae.hair.domain.product.dto.InventoryRequest;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 재고 Command 서비스 (입고/출고)
 *
 * 재고 관리의 핵심 서비스:
 * → 입고: ProductOption의 재고를 증가시키고, 입고 이력을 기록한다.
 * → 출고: ProductOption의 재고를 감소시키고, 출고 이력을 기록한다.
 *
 * 왜 재고 변경과 이력 기록을 하나의 트랜잭션에서 처리하는가?
 * → 재고는 변경됐는데 이력은 기록되지 않으면 데이터 불일치가 발생한다.
 * → 둘을 하나의 @Transactional로 묶으면, 이력 저장이 실패해도 재고 변경이 롤백된다.
 * → "재고 변경 + 이력 기록"은 항상 함께 성공하거나 함께 실패해야 한다 (원자성).
 *
 * 재고 관리 구조:
 * → ProductOption.stockQuantity = 현재 재고 (실시간 조회용)
 * → InventoryHistory = 변동 이력 (감사 추적용)
 * → 둘을 함께 관리해야 "현재 재고가 어떻게 이 값이 되었는지" 추적할 수 있다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryCommandService {

    private final ProductOptionRepository productOptionRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    /**
     * 재고 입고
     *
     * 처리 흐름:
     * 1) 상품 옵션 조회
     * 2) 재고 수량 증가 (ProductOption.increaseStock)
     * 3) 입고 이력 기록 (InventoryHistory 생성)
     *
     * 왜 save()를 명시적으로 호출하지 않는가? (재고 변경)
     * → JPA 더티체킹 덕분이다.
     * → @Transactional 안에서 option.increaseStock()으로 필드를 변경하면,
     *   트랜잭션 종료 시 JPA가 변경을 감지해서 자동 UPDATE한다.
     * → 하지만 InventoryHistory는 새로 생성하는 것이므로 save()가 필요하다.
     *
     * @param request 입고 요청 (옵션 ID, 수량, 사유)
     */
    public void stockIn(InventoryRequest request) {
        // 1. 상품 옵션 조회
        ProductOption option = productOptionRepository.findByIdAndDeletedFalse(request.getProductOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        // 2. 재고 증가 (더티체킹으로 자동 UPDATE)
        option.increaseStock(request.getQuantity());

        // 3. 입고 이력 기록
        InventoryHistory history = InventoryHistory.create(
                option, InventoryType.IN, request.getQuantity(), request.getReason()
        );
        inventoryHistoryRepository.save(history);
    }

    /**
     * 재고 출고
     *
     * 처리 흐름:
     * 1) 상품 옵션 조회
     * 2) 재고 부족 검증 + 수량 감소 (ProductOption.decreaseStock)
     * 3) 출고 이력 기록 (InventoryHistory 생성)
     *
     * 재고 부족 시:
     * → ProductOption.decreaseStock()에서 BusinessException(INSUFFICIENT_STOCK)이 발생한다.
     * → @Transactional에 의해 모든 변경 사항이 롤백된다.
     * → GlobalExceptionHandler가 예외를 잡아서 400 에러를 반환한다.
     *
     * @param request 출고 요청 (옵션 ID, 수량, 사유)
     */
    public void stockOut(InventoryRequest request) {
        // 1. 상품 옵션 조회
        ProductOption option = productOptionRepository.findByIdAndDeletedFalse(request.getProductOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        // 2. 재고 감소 (부족하면 예외 발생 → 트랜잭션 롤백)
        option.decreaseStock(request.getQuantity());

        // 3. 출고 이력 기록
        InventoryHistory history = InventoryHistory.create(
                option, InventoryType.OUT, request.getQuantity(), request.getReason()
        );
        inventoryHistoryRepository.save(history);
    }
}
