package com.ledgerly.dto;

import com.ledgerly.domain.Transaction;
import lombok.Getter;

import java.time.LocalDate;

@Getter
public class TransactionResponseDto {

    private final Long id;
    private final LocalDate transactionDate;
    private final CategoryInfo category;
    private final String description;
    private final String type;
    private final Integer amount;

    @Getter
    public static class CategoryInfo {
        private final Long id;
        private final String name;
        private final String type;

        public CategoryInfo(com.ledgerly.domain.Category category) {
            this.id = category.getId();
            this.name = category.getName();
            this.type = category.getType();
        }
    }

    public TransactionResponseDto(Transaction tx) {
        this.id = tx.getId();
        this.transactionDate = tx.getTransactionDate();
        this.category = new CategoryInfo(tx.getCategory());
        this.description = tx.getDescription();
        this.type = tx.getType();
        this.amount = tx.getAmount();
    }
}
