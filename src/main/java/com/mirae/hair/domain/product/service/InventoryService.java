package com.mirae.hair.domain.product.service;

import com.mirae.hair.domain.product.command.InventoryHistoryRepository;
import com.mirae.hair.domain.product.command.ProductOptionRepository;
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
 * 재고 서비스 (입고/출고)
 *
 * 재고 변경과 이력 기록을 하나의 트랜잭션에서 처리한다.
 * → "재고 변경 + 이력 기록"은 항상 함께 성공하거나 함께 실패해야 한다 (원자성).
 */
@Service
@RequiredArgsConstructor
@Transactional
public class InventoryService {

    private final ProductOptionRepository productOptionRepository;
    private final InventoryHistoryRepository inventoryHistoryRepository;

    /**
     * 재고 입고
     */
    public void stockIn(InventoryRequest request) {
        ProductOption option = productOptionRepository.findByIdAndDeletedFalse(request.getProductOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        option.increaseStock(request.getQuantity());

        InventoryHistory history = InventoryHistory.create(
                option, InventoryType.IN, request.getQuantity(), request.getReason()
        );
        inventoryHistoryRepository.save(history);
    }

    /**
     * 재고 출고
     */
    public void stockOut(InventoryRequest request) {
        ProductOption option = productOptionRepository.findByIdAndDeletedFalse(request.getProductOptionId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_OPTION_NOT_FOUND));

        option.decreaseStock(request.getQuantity());

        InventoryHistory history = InventoryHistory.create(
                option, InventoryType.OUT, request.getQuantity(), request.getReason()
        );
        inventoryHistoryRepository.save(history);
    }
}
