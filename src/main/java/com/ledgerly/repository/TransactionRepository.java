package com.ledgerly.repository;

import com.ledgerly.domain.Transaction;
import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * 거래 내역 Repository입니다.
 * 단순 조회는 메서드명 규칙으로, 집계는 @Query JPQL로 구현합니다.
 */
public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(
            User user, LocalDate startDate, LocalDate endDate
    );

    // 대상 거래가 없으면 SUM이 null을 반환합니다. 호출 측에서 null → 0 처리를 합니다.
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

    // 반환: [월(int), 타입(String), 합계(long)]. 거래 없는 달은 결과에 빠집니다.
    @Query("SELECT MONTH(t.transactionDate), t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND YEAR(t.transactionDate) = :year " +
            "GROUP BY MONTH(t.transactionDate), t.type " +
            "ORDER BY MONTH(t.transactionDate)")
    List<Object[]> findMonthlyTrend(@Param("user") User user, @Param("year") int year);

    // GROUP BY에 category.id를 포함하여 이름이 같은 다른 카테고리가 합산되지 않도록 합니다.
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

    @Query("SELECT t.type, SUM(t.amount) FROM Transaction t " +
            "WHERE t.user = :user AND YEAR(t.transactionDate) = :year " +
            "GROUP BY t.type")
    List<Object[]> findAnnualSummary(@Param("user") User user, @Param("year") int year);
}
