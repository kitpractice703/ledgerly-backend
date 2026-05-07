package com.ledgerly.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Budget - 카테고리별 월간 예산 한도 엔티티
 *
 * <p>[설계] 예산은 (사용자, 카테고리, 연도, 월)의 복합 조건으로 식별됩니다.
 * 동일 카테고리에 같은 달의 예산을 중복 등록하지 못하도록 {@link BudgetRepository}에서
 * 유일성을 검사합니다.</p>
 *
 * <p>[설계] 연도·월을 별도 컬럼으로 분리하여 저장합니다. 날짜 컬럼 하나로 관리하는 방식보다
 * 연월 조건 쿼리가 단순하고 인덱스 활용이 용이합니다.</p>
 */
@Entity
@Table(name = "budget")
@Getter
@Setter
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [보안] 예산 소유자입니다. @JsonIgnore로 응답에서 사용자 정보를 제외합니다.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "users_id", nullable = false)
    private User user;

    // 예산을 적용할 카테고리. 응답에 카테고리 이름이 포함됩니다.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private Category category;

    // 해당 카테고리의 월간 지출 한도 금액
    @Column(nullable = false)
    private Integer limitAmount;

    // DB 컬럼명을 명시하여 SQL 예약어(year, month) 충돌을 방지합니다.
    @Column(name = "budget_year", nullable = false)
    private Integer year;

    @Column(name = "budget_month", nullable = false)
    private Integer month;
}
