package com.mirae.hair.domain.customer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 고객 상세 조회 응답 DTO (전체 정보, 사업자번호 포함)
 *
 * MyBatis 결과를 매핑한 후, QueryService에서 암호화 필드를 복호화하여 응답한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomerDetailDto {

    private Long id;
    private String shopName;
    private String ownerName;
    private String businessNumber;
    private String phone;
    private String address;
    private String memo;
    private boolean deleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
}
