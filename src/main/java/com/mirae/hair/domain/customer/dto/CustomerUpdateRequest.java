package com.mirae.hair.domain.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객(미용실) 수정 요청 DTO
 *
 * 사업자번호도 수정 가능하다.
 * → 단, Service에서 사업자번호 유효성 검증(가중치 알고리즘) 후 저장한다.
 */
@Getter
@NoArgsConstructor
public class CustomerUpdateRequest {

    @NotBlank(message = "미용실명은 필수입니다")
    private String shopName;

    @NotBlank(message = "대표자명은 필수입니다")
    private String ownerName;

    /** 사업자등록번호 (수정 가능, 유효성 검증 적용) */
    @NotBlank(message = "사업자등록번호는 필수입니다")
    private String businessNumber;

    @NotBlank(message = "전화번호는 필수입니다")
    private String phone;

    @NotBlank(message = "주소는 필수입니다")
    private String address;

    private String memo;
}
