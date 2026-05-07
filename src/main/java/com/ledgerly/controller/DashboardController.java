package com.ledgerly.controller;

import com.ledgerly.domain.User;
import com.ledgerly.dto.BudgetStatusDto;
import com.ledgerly.dto.DashboardResponseDto;
import com.ledgerly.dto.TransactionResponseDto;
import com.ledgerly.service.BudgetService;
import com.ledgerly.service.TransactionService;
import com.ledgerly.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * DashboardController - 월별 재정 현황 요약 데이터를 제공하는 컨트롤러
 *
 * <p>[설계] 대시보드에 필요한 데이터를 단일 API 호출로 제공하여 프론트엔드의 요청 수를 줄입니다.
 * 거래 내역, 수입·지출 합계, 예산 현황을 {@link DashboardResponseDto} 하나로 묶어 반환합니다.</p>
 *
 * <p>[설계] 수입·지출 합계를 DB 집계 쿼리 대신 이미 조회한 거래 목록을 Java 스트림으로 계산합니다.
 * 거래 목록을 다시 사용할 수 있어 DB 쿼리 수를 줄이고, 데이터 일관성을 보장합니다.</p>
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final BudgetService budgetService;

    /**
     * 특정 연월의 대시보드 데이터(거래 내역·합계·예산 현황)를 반환합니다.
     * year/month 파라미터가 0이면 현재 연월을 기본값으로 사용합니다.
     */
    @GetMapping
    public ResponseEntity<DashboardResponseDto> dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {

        if (year == 0) year = LocalDate.now().getYear();
        if (month == 0) month = LocalDate.now().getMonthValue();

        User user = userService.findByEmail(userDetails.getUsername());

        // 거래 목록을 한 번 조회하여 합계 계산과 응답 구성에 모두 재사용합니다.
        List<TransactionResponseDto> transactions = transactionService.findDtosByUserAndMonth(user, year, month);

        // [설계] 이미 로드된 거래 목록을 스트림으로 합산하여 추가 DB 쿼리를 생략합니다.
        int totalIncome = transactions.stream()
                .filter(t -> "INCOME".equals(t.getType()))
                .mapToInt(TransactionResponseDto::getAmount)
                .sum();

        int totalExpense = transactions.stream()
                .filter(t -> "EXPENSE".equals(t.getType()))
                .mapToInt(TransactionResponseDto::getAmount)
                .sum();

        List<BudgetStatusDto> budgetStatuses = budgetService.findBudgetStatusByUserAndMonth(user, year, month);

        return ResponseEntity.ok(new DashboardResponseDto(
                year, month, totalIncome, totalExpense, totalIncome - totalExpense,
                transactions, budgetStatuses
        ));
    }
}
