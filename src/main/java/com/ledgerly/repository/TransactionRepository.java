package com.ledgerly.repository;

import com.ledgerly.domain.Transaction;
import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * TransactionRepository - 거래 내역 데이터 접근 인터페이스
 *
 * <p>[설계] 단순 조회는 Spring Data JPA 메서드 이름 규칙으로, 집계 쿼리는 {@code @Query}
 * JPQL로 구현합니다. 집계 쿼리는 메서드명 규칙으로 표현할 수 없기 때문입니다.</p>
 *
 * <p>[설계] 모든 {@code @Query}는 테이블명 대신 엔티티명을 사용하는 JPQL로 작성합니다.
 * 이를 통해 테이블명이 변경되어도 쿼리 수정 없이 JPA 엔티티 매핑만 변경하면 됩니다.</p>
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    /**
     * 특정 사용자의 날짜 범위 내 거래 내역을 날짜 내림차순으로 조회합니다.
     * 메서드명이 길지만 Spring Data JPA가 자동으로 JPQL을 생성하므로 구현 코드가 불필요합니다.
     */
    List<Transaction> findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(
            User user,
            LocalDate startDate,
            LocalDate endDate
    );

    /**
     * 카테고리·타입·기간 조건의 거래 금액 합계를 반환합니다.
     *
     * <p>[설계] 예산 소진율 계산에 사용됩니다. 대상 거래가 없으면 SUM 결과가 null이므로
     * 호출 측({@link com.ledgerly.service.TransactionService#sumByUserAndCategoryAndMonth})에서
     * null을 0으로 처리합니다.</p>
     */
    @Query("SELECT SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user " +
            "AND t.category.id = :categoryId " +
            "AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate")
    Integer sumAmountByUserAndCategoryAndTypeDateBetween(
            @Param("user") User user,
            @Param("categoryId") Long category,
            @Param("type") String type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 연도의 월별·타입별 거래 금액 합계를 반환합니다.
     *
     * <p>반환 형식: {@code Object[]{월(int), 타입(String), 금액합계(long)}}</p>
     * <p>[설계] 거래가 없는 달은 결과에 포함되지 않습니다. 서비스 레이어에서 12개월로 보완합니다.</p>
     */
    @Query("SELECT MONTH(t.transactionDate), t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND YEAR(t.transactionDate) = :year " +
            "GROUP BY MONTH(t.transactionDate), t.type " +
            "ORDER BY MONTH(t.transactionDate)")
    List<Object[]> findMonthlyTrend(
            @Param("user") User user,
            @Param("year") int year
    );

    /**
     * 특정 연월의 카테고리별 거래 금액 합계를 내림차순으로 반환합니다.
     *
     * <p>반환 형식: {@code Object[]{카테고리명(String), 금액합계(long)}}</p>
     * <p>[설계] {@code GROUP BY t.category.id, t.category.name}으로 카테고리 ID와 이름을
     * 함께 그룹화하여 이름이 같은 다른 카테고리가 합산되는 것을 방지합니다.</p>
     */
    @Query("SELECT t.category.name, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND t.type = :type " +
            "AND t.transactionDate BETWEEN :startDate AND :endDate " +
            "GROUP BY t.category.id, t.category.name " +
            "ORDER BY SUM(t.amount) DESC")
    List<Object[]> findCategoryBreakdown(
            @Param("user") User user,
            @Param("type") String type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate
    );

    /**
     * 특정 연도의 타입별(수입/지출) 총액을 반환합니다.
     *
     * <p>반환 형식: {@code Object[]{타입(String), 금액합계(long)}}</p>
     */
    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND YEAR(t.transactionDate) = :year " +
            "GROUP BY t.type")
    List<Object[]> findAnnualSummary(
            @Param("user") User user,
            @Param("year") int year
    );
}


