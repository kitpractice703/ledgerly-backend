package com.ledgerly.dto;

import com.ledgerly.domain.Budget;
import lombok.Getter;

@Getter
public class BudgetStatusDto {

    private final Long budgetId;
    private final String categoryName;
    private final int limitAmount;
    private final int spentAmount;
    private final int remaining;
    private final boolean exceeded;

    public BudgetStatusDto(Budget budget, int spentAmount) {
        this.budgetId = budget.getId();
        this.categoryName = budget.getCategory().getName();
        this.limitAmount = budget.getLimitAmount();
        this.spentAmount = spentAmount;
        this.remaining = budget.getLimitAmount() - spentAmount;
        this.exceeded = spentAmount >= budget.getLimitAmount();
    }
}
