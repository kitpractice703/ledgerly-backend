package com.ledgerly.service;

import com.ledgerly.domain.User;
import com.ledgerly.dto.AnnualSummaryDto;
import com.ledgerly.dto.CategoryBreakdownDto;
import com.ledgerly.dto.MonthlyTrendDto;
import com.ledgerly.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.*;

/**
 * ReportService - 재정 통계 데이터를 집계하는 리포트 서비스
 *
 * <p>[설계] 모든 메서드에 {@code @Transactional(readOnly = true)}를 적용합니다.
 * 읽기 전용 트랜잭션은 JPA의 더티 체킹(변경 감지) 스냅샷을 생성하지 않아 성능이 향상되고,
 * MySQL에서 읽기 전용 최적화(Replica 라우팅 등)를 활용할 수 있습니다.</p>
 *
 * <p>[설계] 집계 쿼리는 {@link TransactionRepository}의 JPQL {@code @Query}로 DB에서 처리하고,
 * 이 서비스는 결과 후처리(빈 달 채우기, 비율 계산 등)만 담당합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class ReportService {

  private final TransactionRepository transactionRepository;

  /**
   * 특정 연도의 월별 수입·지출 트렌드를 12개월 전체로 반환합니다.
   *
   * <p>[설계] DB 쿼리 결과에는 거래가 없는 달의 데이터가 포함되지 않습니다.
   * {@link LinkedHashMap}으로 1~12월을 미리 초기화(income=0, expense=0)한 뒤
   * 쿼리 결과를 덮어쓰는 방식으로 프론트엔드에 항상 12개의 데이터를 제공합니다.
   * 삽입 순서를 유지하는 LinkedHashMap을 사용하여 월 순서를 보장합니다.</p>
   */
  @Transactional(readOnly = true)
  public List<MonthlyTrendDto> getMonthlyTrend(User user, int year) {
    List<Object[]> raw = transactionRepository.findMonthlyTrend(user, year);

    // 1~12월을 0으로 초기화하여 거래가 없는 달도 차트에 표시되도록 합니다.
    Map<Integer, MonthlyTrendDto> map = new LinkedHashMap<>();
    for (int m = 1; m <= 12; m++) {
      map.put(m, new MonthlyTrendDto(m, 0L, 0L));
    }

    // DB 집계 결과를 월-타입 기준으로 매핑합니다.
    for (Object[] row : raw) {
      int month = ((Number) row[0]).intValue();
      String type = (String) row[1];
      long amount = ((Number) row[2]).longValue();
      MonthlyTrendDto dto = map.get(month);
      if ("INCOME".equals(type))
        dto.setIncome(amount);
      else
        dto.setExpense(amount);
    }

    return new ArrayList<>(map.values());
  }

  /**
   * 특정 연월의 카테고리별 수입 또는 지출 금액 분포를 반환합니다.
   *
   * <p>[설계] DB에서 카테고리명과 합계를 함께 조회하여 N+1 문제 없이 단일 쿼리로 처리합니다.</p>
   *
   * @param type "INCOME" 또는 "EXPENSE"
   */
  @Transactional(readOnly = true)
  public List<CategoryBreakdownDto> getCategoryBreakdown(User user, int year, int month, String type) {
    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
    List<Object[]> raw = transactionRepository.findCategoryBreakdown(user, type, startDate, endDate);

    return raw.stream()
        .map(row -> new CategoryBreakdownDto((String) row[0], ((Number) row[1]).longValue()))
        .toList();
  }

  /**
   * 특정 연도의 연간 수입·지출·순이익·저축률 요약을 반환합니다.
   *
   * <p>[설계] 저축률 계산 시 총 수입이 0인 경우(소득 없는 달) 0을 반환하여 분모 0 예외를 방지합니다.</p>
   *
   * @return 연간 요약 DTO (수입이 없으면 저축률 0%)
   */
  @Transactional(readOnly = true)
  public AnnualSummaryDto getAnnualSummary(User user, int year) {
    List<Object[]> raw = transactionRepository.findAnnualSummary(user, year);

    long totalIncome = 0L;
    long totalExpense = 0L;
    for (Object[] row : raw) {
      String type = (String) row[0];
      long amount = ((Number) row[1]).longValue();
      if ("INCOME".equals(type))
        totalIncome = amount;
      else
        totalExpense = amount;
    }

    long netSavings = totalIncome - totalExpense;
    // [설계] 총 수입이 0이면 저축률을 0%로 처리하여 ArithmeticException(/ by zero) 방지
    int savingsRate = totalIncome == 0 ? 0 : (int) (netSavings * 100 / totalIncome);

    return new AnnualSummaryDto(totalIncome, totalExpense, netSavings, savingsRate);
  }
}
