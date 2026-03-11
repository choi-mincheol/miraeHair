package com.mirae.hair.domain.customer.query;

import com.mirae.hair.domain.customer.dto.CustomerDetailDto;
import com.mirae.hair.domain.customer.dto.CustomerListDto;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 고객 조회 MyBatis Mapper 인터페이스
 *
 * CQRS 패턴에서 Query(조회) 담당.
 * → SQL은 resources/mapper/customer/CustomerQueryMapper.xml에 작성한다.
 *
 * 주의: 이 Mapper가 반환하는 phone, businessNumber는 DB의 암호화된 값 그대로이다.
 * → QueryService에서 AES256Util.decrypt()로 복호화한 후 응답한다.
 */
@Mapper
public interface CustomerQueryMapper {

    /**
     * 고객 목록 조회 (페이징 + 검색)
     *
     * @param keyword 검색어 (미용실명/대표자명, nullable)
     * @param offset  건너뛸 행 수
     * @param size    페이지 크기
     */
    List<CustomerListDto> selectCustomerList(
            @Param("keyword") String keyword,
            @Param("offset") long offset,
            @Param("size") int size
    );

    /** 고객 총 건수 (페이징 계산용) */
    long countCustomers(@Param("keyword") String keyword);

    /**
     * 고객 상세 조회
     *
     * @param id 고객 ID
     * @return 고객 상세 (없으면 null)
     */
    CustomerDetailDto selectCustomerDetail(@Param("id") Long id);
}
