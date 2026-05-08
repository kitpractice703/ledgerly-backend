package com.ledgerly.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * 카테고리별 월간 예산 엔티티입니다.
 * 연도·월을 별도 컬럼으로 관리합니다. 날짜 컬럼 하나로 관리하는 것보다
 * 연월 조건 쿼리가 단순하고, year/month 컬럼명은 SQL 예약어 충돌을 피해 별칭을 사용했습니다.
 */
@Entity
@Table(name = "budget")
@Getter
@Setter
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    @Column(nullable = false)
    private Integer limitAmount;

    @Column(name = "budget_year", nullable = false)
    private Integer year;

    @Column(name = "budget_month", nullable = false)
    private Integer month;
}
