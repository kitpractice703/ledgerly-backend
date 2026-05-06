package com.ledgerly.service;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.Transaction;
import com.ledgerly.domain.User;
import com.ledgerly.dto.TransactionResponseDto;
import com.ledgerly.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    @Transactional
    public Transaction save(
            User user,
            Long categoryId,
            Integer amount,
            String description,
            String type,
            LocalDate date
    ) {
        Category category = categoryService.findById(categoryId, user);

        Transaction transaction = new Transaction();
        transaction.setUser(user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTransactionDate(date);

        return transactionRepository.save(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> findDtosByUserAndMonth(User user, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        return transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, startDate, endDate)
                .stream()
                .map(TransactionResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Transaction findById(Long transactionId, User user) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래 내역입니다."));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }
        return transaction;
    }

    @Transactional(readOnly = true)
    public TransactionResponseDto findByIdDto(Long transactionId, User user) {
        return new TransactionResponseDto(findById(transactionId, user));
    }

    @Transactional
    public void update(Long transactionId, User user, Long categoryId,
                       Integer amount, String description, String type, LocalDate transactionDate) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래 내역입니다."));

        // 타인의 거래 내역 수정 시도 차단
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        Category category = categoryService.findById(categoryId, user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTransactionDate(transactionDate);
    }

    @Transactional
    public void delete(Long transactionId, User user) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래 내역입니다."));

        // 타인의 거래 내역 삭제 시도 차단
        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        transactionRepository.delete(transaction);
    }

    @Transactional(readOnly = true)
    public List<TransactionResponseDto> findByUserWithFilters(User user, int year, Integer month, Long categoryId, String type) {
        LocalDate startDate, endDate;
        if (month != null) {
            startDate = LocalDate.of(year, month, 1);
            endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());
        } else {
            startDate = LocalDate.of(year, 1, 1);
            endDate = LocalDate.of(year, 12, 31);
        }

        return transactionRepository
                .findByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, startDate, endDate)
                .stream()
                .filter(t -> categoryId == null || t.getCategory().getId().equals(categoryId))
                .filter(t -> type == null || type.isBlank() || t.getType().equals(type))
                .map(TransactionResponseDto::new)
                .toList();
    }

    @Transactional(readOnly = true)
    public int sumByUserAndCategoryAndMonth(
            User user,
            Long categoryId,
            String type,
            int year,
            int month
    ) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        Integer sum = transactionRepository.sumAmountByUserAndCategoryAndTypeDateBetween(
                user, categoryId, type, startDate, endDate
        );

        return sum != null ? sum : 0; // 대상 거래가 없으면 SUM 결과가 null이므로 0으로 치환
    }


}
