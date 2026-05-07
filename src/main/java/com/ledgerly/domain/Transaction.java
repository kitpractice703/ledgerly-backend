package com.ledgerly.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Transaction - 개별 수입·지출 거래 내역 엔티티
 *
 * <p>[설계] 거래 날짜를 {@link LocalDate}로, 생성 시각을 {@link LocalDateTime}으로 구분합니다.
 * 사용자가 입력하는 "거래 발생일"과 "레코드 생성 시각"은 별개의 개념입니다.</p>
 *
 * <p>[보안] {@code user} 연관관계에 {@code @JsonIgnore}를 적용하여 거래 응답에 다른
 * 사용자 정보가 포함되지 않도록 합니다. 카테고리는 응답에 포함되지만 소유자 정보는 제외됩니다.</p>
 *
 * <p>[설계] {@code user}와 {@code category} 모두 {@code FetchType.LAZY}로 설정하여
 * 기본 조회 시 불필요한 JOIN을 방지합니다. 필요할 때만 연관 엔티티를 로드합니다.</p>
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "transaction")
@Getter
@Setter
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [보안] 거래 소유자입니다. API 응답에 사용자 정보가 노출되지 않도록 @JsonIgnore 적용
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 카테고리는 응답 DTO에 포함되지만, 소유자(user) 필드는 @JsonIgnoreProperties로 제외됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Integer amount;

    // 메모는 선택 입력 항목으로 nullable입니다.
    private String description;

    // "INCOME"(수입) 또는 "EXPENSE"(지출)
    @Column(nullable = false)
    private String type;

    // 사용자가 직접 입력하는 거래 발생일 (레코드 생성 시각인 createdAt과 구별)
    @Column(nullable = false)
    private LocalDate transactionDate;

    // [설계] updatable = false로 레코드 생성 후 변경 불가한 감사 필드로 관리합니다.
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
