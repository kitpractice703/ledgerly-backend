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

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final TransactionService transactionService;
    private final UserService userService;
    private final BudgetService budgetService;

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
