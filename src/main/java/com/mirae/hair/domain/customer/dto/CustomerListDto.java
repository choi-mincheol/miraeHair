package com.mirae.hair.domain.customer.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 고객 목록 조회 응답 DTO
 *
 * MyBatis로 조회한 결과를 매핑한다.
 * → 목록에서는 사업자번호를 노출하지 않는다 (개인정보 최소 노출 원칙).
 * → 상세 조회에서만 사업자번호를 포함한다.
 *
 * 왜 목록에서 사업자번호를 빼는가?
 * → 목록 화면에서 모든 거래처의 사업자번호가 노출되면 보안상 위험하다.
 * → "필요한 곳에서만 필요한 정보를 노출"하는 것이 최소 권한 원칙이다.
 *
 * phone, businessNumber 등 암호화 필드:
 * → MyBatis SQL에서 DB의 암호화된 값을 그대로 가져온다.
 * → QueryService에서 AES256Util.decrypt()로 복호화한 후 응답한다.
 */
@Getter
@Setter
@NoArgsConstructor
public class CustomerListDto {

    private Long id;
    private String shopName;
    private String ownerName;
    private String phone;
    private String address;
    private LocalDateTime createdAt;
}
