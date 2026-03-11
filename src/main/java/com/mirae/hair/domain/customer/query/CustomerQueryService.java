package com.mirae.hair.domain.customer.query;

import com.mirae.hair.domain.customer.dto.CustomerDetailDto;
import com.mirae.hair.domain.customer.dto.CustomerListDto;
import com.mirae.hair.global.exception.BusinessException;
import com.mirae.hair.global.exception.ErrorCode;
import com.mirae.hair.global.util.AES256Util;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 고객 Query 서비스 (조회 전용)
 *
 * CQRS 패턴에서 Query 담당:
 * → MyBatis로 조회한 후, 암호화된 필드를 복호화하여 응답한다.
 *
 * 복호화 처리 흐름:
 * 1) MyBatis가 DB에서 암호화된 데이터를 조회
 * 2) 이 Service에서 AES256Util.decrypt()로 복호화
 * 3) 복호화된 값을 DTO에 설정하여 Controller로 반환
 *
 * 왜 MyBatis SQL이 아닌 Java에서 복호화하는가?
 * → PostgreSQL에 AES 복호화 함수를 설치하는 방법도 있지만,
 *   암호화 키가 DB 서버에 노출되어 보안이 약해진다.
 * → Java 애플리케이션에서만 키를 관리하면, DB가 유출되어도 복호화가 불가능하다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CustomerQueryService {

    private final CustomerQueryMapper customerQueryMapper;
    private final AES256Util aes256Util;

    /**
     * 고객 목록 조회 (페이징 + 검색)
     *
     * → phone 필드를 복호화하여 응답한다.
     * → 목록에서는 사업자번호를 포함하지 않는다 (개인정보 최소 노출 원칙).
     */
    public Page<CustomerListDto> getCustomerList(String keyword, Pageable pageable) {
        long offset = pageable.getOffset();
        int size = pageable.getPageSize();

        List<CustomerListDto> customers = customerQueryMapper.selectCustomerList(keyword, offset, size);
        long totalCount = customerQueryMapper.countCustomers(keyword);

        // 암호화된 전화번호를 복호화
        customers.forEach(dto -> dto.setPhone(aes256Util.decrypt(dto.getPhone())));

        return new PageImpl<>(customers, pageable, totalCount);
    }

    /**
     * 고객 상세 조회
     *
     * → phone, businessNumber 필드를 복호화하여 응답한다.
     */
    public CustomerDetailDto getCustomerDetail(Long id) {
        CustomerDetailDto customer = customerQueryMapper.selectCustomerDetail(id);

        if (customer == null) {
            throw new BusinessException(ErrorCode.CUSTOMER_NOT_FOUND);
        }

        // 암호화된 필드 복호화
        customer.setBusinessNumber(aes256Util.decrypt(customer.getBusinessNumber()));
        customer.setPhone(aes256Util.decrypt(customer.getPhone()));

        return customer;
    }
}
