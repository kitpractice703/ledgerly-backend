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
 * 대시보드에 필요한 거래 내역·합계·예산 현황을 단일 API 호출로 반환하는 컨트롤러입니다.
 * 수입·지출 합계는 거래 목록을 한 번만 조회한 뒤 스트림으로 계산하여 추가 DB 쿼리를 줄였습니다.
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final BudgetService budgetService;

    // year/month 기본값을 0으로 받아, 0이면 현재 연월로 대체합니다.
    // defaultValue에 현재 날짜를 직접 지정하면 서버 시작 시점에 고정되어 이 방식을 사용했습니다.
    @GetMapping
    public ResponseEntity<DashboardResponseDto> dashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int year,
            @RequestParam(defaultValue = "0") int month) {

        if (year == 0) year = LocalDate.now().getYear();
        if (month == 0) month = LocalDate.now().getMonthValue();

        User user = userService.findByEmail(userDetails.getUsername());

        List<TransactionResponseDto> transactions = transactionService.findDtosByUserAndMonth(user, year, month);

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
