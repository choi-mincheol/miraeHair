package com.mirae.hair.global.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * 모든 엔티티의 부모 클래스 (JPA Auditing)
 *
 * 왜 BaseEntity가 필요한가?
 * → 모든 테이블에 생성일/수정일/생성자/수정자가 필요한데,
 *   매번 엔티티마다 동일한 필드를 반복 작성하면 코드 중복이 발생한다.
 * → BaseEntity를 상속하면 이 4개 필드가 자동으로 포함되고,
 *   JPA Auditing이 값을 자동으로 채워준다.
 *
 * 왜 @MappedSuperclass인가?
 * → @Entity가 아니라 @MappedSuperclass를 사용해야 한다.
 * → @Entity를 쓰면 BaseEntity용 테이블이 별도로 생기지만,
 *   @MappedSuperclass는 자식 엔티티의 테이블에 컬럼만 추가해준다.
 * → 즉, "테이블 없이 필드만 상속"하는 것이다.
 *
 * 왜 @EntityListeners(AuditingEntityListener.class)인가?
 * → JPA에게 "이 엔티티가 저장/수정될 때 Auditing 이벤트를 발생시켜라"고 알려주는 것.
 * → 이게 없으면 @CreatedDate, @LastModifiedDate가 동작하지 않는다.
 */
@Getter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

    /**
     * 생성일시 - 엔티티가 처음 저장될 때 자동으로 현재 시각이 들어간다.
     * updatable = false: 한번 저장되면 수정 불가 (생성일은 변하면 안 되니까)
     */
    @CreatedDate
    @Column(updatable = false, nullable = false)
    private LocalDateTime createdAt;

    /**
     * 수정일시 - 엔티티가 수정될 때마다 자동으로 현재 시각으로 업데이트된다.
     */
    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 생성자 - 엔티티를 처음 저장한 사용자 정보
     * 현재는 "SYSTEM"으로 고정, feature/02-security-jwt에서 실제 로그인 사용자로 교체 예정
     */
    @CreatedBy
    @Column(updatable = false, nullable = false)
    private String createdBy;

    /**
     * 수정자 - 엔티티를 마지막으로 수정한 사용자 정보
     */
    @LastModifiedBy
    private String updatedBy;
}
