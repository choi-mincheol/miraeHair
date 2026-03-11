package com.mirae.hair.domain.customer.domain;

import com.mirae.hair.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 고객(미용실) 엔티티
 *
 * 이 시스템에서 "고객"은 개인 소비자가 아니라 "미용실(거래처)"이다.
 * → B2B 미용 제품 판매 시스템이므로, 미용실 자체가 고객이다.
 * → 미용실에 방문하는 개인 소비자는 이 시스템의 관리 대상이 아니다.
 *
 * 보안 (AES-256 암호화):
 * → 사업자번호(businessNumber)와 전화번호(phone)는 개인정보에 해당한다.
 * → DB에 평문으로 저장하면 DB 유출 시 개인정보가 노출된다 (ISMS 위반).
 * → AES-256으로 암호화하여 저장하고, 조회 시 복호화하여 응답한다.
 * → 암호화/복호화는 Service 계층에서 AES256Util로 처리한다.
 *   (Entity에서 직접 하지 않는 이유: Entity는 순수 도메인 객체로 유지하기 위해)
 *
 * 사업자등록번호 유효성 검증:
 * → 사업자번호는 10자리 가중치 검증 알고리즘으로 유효성을 체크한다.
 * → BusinessNumberValidator.isValid()로 검증 후 저장한다.
 */
@Entity
@Table(name = "customers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 미용실명 (예: 헤어살롱 미래) */
    @Column(nullable = false, length = 100)
    private String shopName;

    /** 대표자명 */
    @Column(nullable = false, length = 50)
    private String ownerName;

    /**
     * 사업자등록번호 (AES-256 암호화 저장)
     *
     * 왜 VARCHAR(200)인가?
     * → 원본은 "123-45-67890" (12자)이지만,
     *   AES-256 암호화 + Base64 인코딩하면 길이가 크게 늘어난다.
     * → 암호화된 문자열을 저장할 충분한 크기가 필요하다.
     */
    @Column(nullable = false, unique = true, length = 200)
    private String businessNumber;

    /** 전화번호 (AES-256 암호화 저장) */
    @Column(nullable = false, length = 200)
    private String phone;

    /** 주소 */
    @Column(nullable = false, length = 300)
    private String address;

    /** 메모 (거래 특이사항, 주문 빈도 등) */
    @Column(columnDefinition = "TEXT")
    private String memo;

    /** 삭제 여부 (Soft Delete) */
    @Column(name = "is_deleted", nullable = false)
    private boolean deleted = false;

    @Builder
    private Customer(String shopName, String ownerName, String businessNumber,
                     String phone, String address, String memo) {
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.businessNumber = businessNumber;
        this.phone = phone;
        this.address = address;
        this.memo = memo;
    }

    /**
     * 고객(미용실) 생성 정적 팩토리 메서드
     *
     * 주의: businessNumber, phone은 이미 암호화된 값이 전달되어야 한다.
     * → 암호화는 Service 계층에서 AES256Util로 처리한다.
     * → Entity는 암호화 여부를 모른다 (관심사 분리).
     */
    public static Customer create(String shopName, String ownerName, String businessNumber,
                                  String phone, String address, String memo) {
        return Customer.builder()
                .shopName(shopName)
                .ownerName(ownerName)
                .businessNumber(businessNumber)
                .phone(phone)
                .address(address)
                .memo(memo)
                .build();
    }

    /**
     * 고객 정보 수정
     *
     * 사업자번호도 수정 가능 (단, Service에서 유효성 검증 후 암호화된 값 전달)
     */
    public void update(String shopName, String ownerName, String businessNumber,
                       String phone, String address, String memo) {
        this.shopName = shopName;
        this.ownerName = ownerName;
        this.businessNumber = businessNumber;
        this.phone = phone;
        this.address = address;
        this.memo = memo;
    }

    /**
     * 고객 삭제 (Soft Delete)
     */
    public void softDelete() {
        this.deleted = true;
    }
}
