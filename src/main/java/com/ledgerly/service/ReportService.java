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
 * 재정 통계를 집계하는 서비스입니다.
 *
 * 집계 쿼리는 TransactionRepository의 @Query로 DB에서 처리하고,
 * 이 서비스는 빈 달 채우기·비율 계산 같은 후처리만 담당합니다.
 */
@Service
@RequiredArgsConstructor
public class ReportService {

  private final TransactionRepository transactionRepository;

  /**
   * 특정 연도의 월별 수입·지출 트렌드를 12개월 전체로 반환합니다.
   * 거래가 없는 달은 DB 결과에 포함되지 않으므로, 1~12월을 미리 0으로 초기화한 뒤
   * 쿼리 결과를 덮어씁니다. LinkedHashMap으로 월 순서를 보장합니다.
   */
  @Transactional(readOnly = true)
  public List<MonthlyTrendDto> getMonthlyTrend(User user, int year) {
    List<Object[]> raw = transactionRepository.findMonthlyTrend(user, year);

    Map<Integer, MonthlyTrendDto> map = new LinkedHashMap<>();
    for (int m = 1; m <= 12; m++) {
      map.put(m, new MonthlyTrendDto(m, 0L, 0L));
    }

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

  @Transactional(readOnly = true)
  public List<CategoryBreakdownDto> getCategoryBreakdown(User user, int year, int month, String type) {
    LocalDate startDate = LocalDate.of(year, month, 1);
    LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
    List<Object[]> raw = transactionRepository.findCategoryBreakdown(user, type, startDate, endDate);

    return raw.stream()
        .map(row -> new CategoryBreakdownDto((String) row[0], ((Number) row[1]).longValue()))
        .toList();
  }

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
    // 수입이 0이면 저축률 계산 시 0 나누기가 발생하므로 그냥 0%로 처리합니다.
    int savingsRate = totalIncome == 0 ? 0 : (int) (netSavings * 100 / totalIncome);

    return new AnnualSummaryDto(totalIncome, totalExpense, netSavings, savingsRate);
  }
}
