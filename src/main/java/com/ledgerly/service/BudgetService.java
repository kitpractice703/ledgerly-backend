package com.ledgerly.service;

import com.ledgerly.domain.Budget;
import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.dto.BudgetStatusDto;
import com.ledgerly.repository.BudgetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 예산 CRUD와 예산 현황 집계를 처리하는 서비스입니다.
 * 예산 현황은 TransactionService에서 실제 지출액을 가져와 한도와 비교합니다.
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    /**
     * 예산을 등록합니다.
     * 같은 카테고리·연월에 예산이 이미 있으면 DB 제약 대신 서비스에서 먼저 막아
     * 명확한 오류 메시지를 내려줍니다.
     */
    @Transactional
    public Budget save(User user, Long categoryId, Integer limitAmount, Integer year, Integer month) {
        budgetRepository.findByUserAndCategoryIdAndYearAndMonth(user, categoryId, year, month)
                .ifPresent(b -> {
                    throw new IllegalArgumentException("이미 해당 카테고리의 예산이 존재합니다.");
                });

        Category category = categoryService.findById(categoryId, user);

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setLimitAmount(limitAmount);
        budget.setYear(year);
        budget.setMonth(month);

        return budgetRepository.save(budget);
    }

    @Transactional(readOnly = true)
    public List<Budget> findByUserAndMonth(User user, Integer year, Integer month) {
        return budgetRepository.findByUserAndYearAndMonth(user, year, month);
    }

    /**
     * 예산 목록에 실제 지출액과 초과 여부를 포함해 반환합니다.
     * 예산마다 TransactionService를 한 번씩 호출하므로, 예산이 많아지면
     * 쿼리 수가 늘어날 수 있습니다. 현재 데이터 규모에서는 허용 가능한 수준입니다.
     */
    @Transactional(readOnly = true)
    public List<BudgetStatusDto> findBudgetStatusByUserAndMonth(User user, Integer year, Integer month) {
        List<Budget> budgets = budgetRepository.findByUserAndYearAndMonth(user, year, month);

        return budgets.stream()
                .map(budget -> {
                    int spent = transactionService.sumByUserAndCategoryAndMonth(
                            user, budget.getCategory().getId(), "EXPENSE", year, month
                    );
                    return new BudgetStatusDto(budget, spent);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    @Transactional
    public void update(Long budgetId, User user, Integer limitAmount) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예산입니다."));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        budget.setLimitAmount(limitAmount);
    }

    @Transactional
    public void delete(Long budgetId, User user) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예산입니다."));

        if (!budget.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        budgetRepository.delete(budget);
    }

    @Transactional(readOnly = true)
    public boolean isExceeded(User user, Long categoryId, Integer year, Integer month) {
        return budgetRepository
                .findByUserAndCategoryIdAndYearAndMonth(user, categoryId, year, month)
                .map(budget -> {
                    int spent = transactionService.sumByUserAndCategoryAndMonth(
                            user, categoryId, "EXPENSE", year, month
                    );
                    return spent >= budget.getLimitAmount();
                })
                .orElse(false);
    }
}
