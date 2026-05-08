package com.ledgerly.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 거래 내역 엔티티입니다.
 * transactionDate는 사용자가 입력하는 거래 발생일이고,
 * createdAt은 레코드가 DB에 저장된 시각으로 서로 다른 개념입니다.
 *
 * @JsonIgnoreProperties는 Lazy 프록시의 hibernateLazyInitializer 필드가
 * JSON 직렬화될 때 오류가 나는 것을 방지합니다.
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

    @JsonIgnore // 응답에 다른 사용자 정보가 섞이지 않도록 제외합니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Integer amount;

    private String description; // 선택 입력이므로 nullable

    @Column(nullable = false)
    private String type; // "INCOME" 또는 "EXPENSE"

    @Column(nullable = false)
    private LocalDate transactionDate;

    @Column(updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
