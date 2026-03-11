package com.mirae.hair.domain.customer.command;

import com.mirae.hair.domain.customer.domain.Customer;
import com.mirae.hair.domain.customer.dto.CustomerCreateRequest;
import com.mirae.hair.domain.customer.dto.CustomerUpdateRequest;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import com.mirae.hair.global.util.AES256Util;
import com.mirae.hair.global.util.BusinessNumberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 고객 Command 서비스 (생성/수정/삭제)
 *
 * CQRS 패턴에서 Command 담당.
 *
 * 이 서비스의 핵심 책임 2가지:
 * 1) 사업자번호 유효성 검증 — 등록/수정 시 모두 적용
 * 2) 개인정보 암호화 — 사업자번호, 전화번호를 AES-256으로 암호화하여 저장
 *
 * 왜 암호화를 Entity가 아닌 Service에서 하는가?
 * → Entity는 "도메인 비즈니스 규칙"만 담당해야 한다 (순수 도메인 객체).
 * → 암호화는 "인프라 관심사"이므로 Service에서 처리하는 것이 관심사 분리 원칙에 맞다.
 * → Entity가 AES256Util을 의존하면 테스트하기도 어려워진다.
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerCommandService {

    private final CustomerRepository customerRepository;
    private final AES256Util aes256Util;

    /**
     * 고객(미용실) 등록
     *
     * 처리 흐름:
     * 1) 사업자번호 유효성 검증 (가중치 검증 알고리즘)
     * 2) 사업자번호/전화번호 AES-256 암호화
     * 3) 암호화된 사업자번호로 중복 검사
     * 4) Customer 엔티티 생성 및 저장
     *
     * @param request 고객 등록 요청 DTO (평문 데이터)
     * @return 생성된 고객 ID
     */
    public Long createCustomer(CustomerCreateRequest request) {
        // 1. 사업자번호 유효성 검증
        validateBusinessNumber(request.getBusinessNumber());

        // 2. 개인정보 암호화
        String encryptedBizNum = aes256Util.encrypt(
                BusinessNumberValidator.normalize(request.getBusinessNumber()));
        String encryptedPhone = aes256Util.encrypt(request.getPhone());

        // 3. 사업자번호 중복 검사 (암호화된 값으로 비교)
        if (customerRepository.existsByBusinessNumber(encryptedBizNum)) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
        }

        // 4. 엔티티 생성 및 저장
        Customer customer = Customer.create(
                request.getShopName(),
                request.getOwnerName(),
                encryptedBizNum,
                encryptedPhone,
                request.getAddress(),
                request.getMemo()
        );

        Customer saved = customerRepository.save(customer);
        return saved.getId();
    }

    /**
     * 고객 정보 수정
     *
     * 사업자번호도 수정 가능:
     * → 수정 시에도 유효성 검증 + 중복 검사를 수행한다.
     * → 중복 검사 시 자기 자신(id)은 제외한다 (existsByBusinessNumberAndIdNot).
     *
     * @param id      수정할 고객 ID
     * @param request 수정 요청 DTO (평문 데이터)
     * @return 수정된 고객 ID
     */
    public Long updateCustomer(Long id, CustomerUpdateRequest request) {
        // 1. 고객 조회
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        // 2. 사업자번호 유효성 검증
        validateBusinessNumber(request.getBusinessNumber());

        // 3. 개인정보 암호화
        String encryptedBizNum = aes256Util.encrypt(
                BusinessNumberValidator.normalize(request.getBusinessNumber()));
        String encryptedPhone = aes256Util.encrypt(request.getPhone());

        // 4. 사업자번호 중복 검사 (자기 자신 제외)
        if (customerRepository.existsByBusinessNumberAndIdNot(encryptedBizNum, id)) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
        }

        // 5. 정보 수정 (더티체킹으로 자동 UPDATE)
        customer.update(
                request.getShopName(),
                request.getOwnerName(),
                encryptedBizNum,
                encryptedPhone,
                request.getAddress(),
                request.getMemo()
        );

        return customer.getId();
    }

    /**
     * 고객 삭제 (Soft Delete)
     *
     * @param id 삭제할 고객 ID
     */
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        customer.softDelete();
    }

    /**
     * 사업자번호 유효성 검증 (공통 메서드)
     *
     * 등록/수정 시 모두 호출된다.
     * → 유효하지 않은 사업자번호면 BusinessException을 던진다.
     */
    private void validateBusinessNumber(String businessNumber) {
        if (!BusinessNumberValidator.isValid(businessNumber)) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_NUMBER);
        }
    }
}
