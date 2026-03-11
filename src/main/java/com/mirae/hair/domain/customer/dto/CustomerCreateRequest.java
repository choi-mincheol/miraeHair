package com.mirae.hair.domain.customer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객(미용실) 등록 요청 DTO
 *
 * 클라이언트에서는 평문(원본)을 전달한다.
 * → 암호화는 Service 계층에서 처리한다 (DTO는 암호화를 모른다).
 * → 사업자번호는 Service에서 유효성 검증 후 암호화하여 저장한다.
 */
@Getter
@NoArgsConstructor
public class CustomerCreateRequest {

    /** 미용실명 */
    @NotBlank(message = "미용실명은 필수입니다")
    private String shopName;

    /** 대표자명 */
    @NotBlank(message = "대표자명은 필수입니다")
    private String ownerName;

    /**
     * 사업자등록번호 (하이픈 포함/미포함 모두 허용)
     * 예: "123-45-67890" 또는 "1234567890"
     * → Service에서 유효성 검증 + AES-256 암호화 후 저장
     */
    @NotBlank(message = "사업자등록번호는 필수입니다")
    private String businessNumber;

    /** 전화번호 */
    @NotBlank(message = "전화번호는 필수입니다")
    private String phone;

    /** 주소 */
    @NotBlank(message = "주소는 필수입니다")
    private String address;

    /** 메모 (선택) */
    private String memo;
}
