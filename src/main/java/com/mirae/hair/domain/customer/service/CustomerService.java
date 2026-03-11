package com.mirae.hair.domain.customer.service;

import com.mirae.hair.domain.customer.command.CustomerRepository;
import com.mirae.hair.domain.customer.domain.Customer;
import com.mirae.hair.domain.customer.dto.*;
import com.mirae.hair.domain.customer.query.CustomerQueryMapper;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import com.mirae.hair.global.util.AES256Util;
import com.mirae.hair.global.util.BusinessNumberValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고객(미용실) 서비스 (Command + Query 통합)
 *
 * 핵심 책임:
 * 1) 사업자번호 유효성 검증 — 등록/수정 시 모두 적용
 * 2) 개인정보 암호화 — 사업자번호, 전화번호를 AES-256으로 암호화하여 저장
 * 3) 조회 시 암호화된 필드 복호화
 */
@Service
@RequiredArgsConstructor
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerQueryMapper customerQueryMapper;
    private final AES256Util aes256Util;

    // ─────────────────────────────────────────
    // Command (등록/수정/삭제) — JPA
    // ─────────────────────────────────────────

    /**
     * 고객(미용실) 등록
     *
     * 처리 흐름:
     * 1) 사업자번호 유효성 검증 (가중치 검증 알고리즘)
     * 2) 사업자번호/전화번호 AES-256 암호화
     * 3) 암호화된 사업자번호로 중복 검사
     * 4) Customer 엔티티 생성 및 저장
     */
    public Long createCustomer(CustomerCreateRequest request) {
        validateBusinessNumber(request.getBusinessNumber());

        String encryptedBizNum = aes256Util.encrypt(
                BusinessNumberValidator.normalize(request.getBusinessNumber()));
        String encryptedPhone = aes256Util.encrypt(request.getPhone());

        if (customerRepository.existsByBusinessNumber(encryptedBizNum)) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
        }

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
     */
    public Long updateCustomer(Long id, CustomerUpdateRequest request) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        validateBusinessNumber(request.getBusinessNumber());

        String encryptedBizNum = aes256Util.encrypt(
                BusinessNumberValidator.normalize(request.getBusinessNumber()));
        String encryptedPhone = aes256Util.encrypt(request.getPhone());

        if (customerRepository.existsByBusinessNumberAndIdNot(encryptedBizNum, id)) {
            throw new BusinessException(ErrorCode.BUSINESS_NUMBER_ALREADY_EXISTS);
        }

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
     */
    public void deleteCustomer(Long id) {
        Customer customer = customerRepository.findByIdAndDeletedFalse(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND));

        customer.softDelete();
    }

    // ─────────────────────────────────────────
    // Query (조회) — MyBatis
    // ─────────────────────────────────────────

    /**
     * 고객 목록 조회 (페이징 + 검색)
     * → phone 필드를 복호화하여 응답한다.
     */
    @Transactional(readOnly = true)
    public Page<CustomerListDto> getCustomerList(String keyword, Pageable pageable) {
        long offset = pageable.getOffset();
        int size = pageable.getPageSize();

        List<CustomerListDto> customers = customerQueryMapper.selectCustomerList(keyword, offset, size);
        long totalCount = customerQueryMapper.countCustomers(keyword);

        customers.forEach(dto -> dto.setPhone(aes256Util.decrypt(dto.getPhone())));

        return new PageImpl<>(customers, pageable, totalCount);
    }

    /**
     * 고객 상세 조회
     * → phone, businessNumber 필드를 복호화하여 응답한다.
     */
    @Transactional(readOnly = true)
    public CustomerDetailDto getCustomerDetail(Long id) {
        CustomerDetailDto customer = customerQueryMapper.selectCustomerDetail(id);

        if (customer == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        customer.setBusinessNumber(aes256Util.decrypt(customer.getBusinessNumber()));
        customer.setPhone(aes256Util.decrypt(customer.getPhone()));

        return customer;
    }

    /**
     * 사업자번호 유효성 검증 (공통 메서드)
     */
    private void validateBusinessNumber(String businessNumber) {
        if (!BusinessNumberValidator.isValid(businessNumber)) {
            throw new BusinessException(ErrorCode.INVALID_BUSINESS_NUMBER);
        }
    }
}
