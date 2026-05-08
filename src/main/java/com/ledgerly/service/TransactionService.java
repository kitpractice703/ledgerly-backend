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

/**
 * 거래 내역 CRUD를 처리하는 서비스입니다.
 *
 * 수정·삭제 시 요청자와 리소스 소유자가 같은지 반드시 확인합니다.
 * URL에 타인의 거래 ID를 직접 입력해도 접근할 수 없도록 막기 위해서입니다.
 */
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

    @Transactional
    public Transaction save(User user, Long categoryId, Integer amount,
                            String description, String type, LocalDate date) {
        // categoryService.findById가 해당 카테고리가 이 사용자의 것인지도 함께 확인합니다.
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
        // withDayOfMonth로 마지막날을 구하면 윤달(2월 29일 등)을 별도 처리하지 않아도 됩니다.
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

        // 다른 사용자의 거래를 ID로 직접 조회하는 경우를 막습니다.
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

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        Category category = categoryService.findById(categoryId, user);
        transaction.setCategory(category);
        transaction.setAmount(amount);
        transaction.setDescription(description);
        transaction.setType(type);
        transaction.setTransactionDate(transactionDate);
        // 별도 save() 없이 트랜잭션 커밋 시 JPA 더티 체킹으로 UPDATE가 실행됩니다.
    }

    @Transactional
    public void delete(Long transactionId, User user) {
        Transaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 거래 내역입니다."));

        if (!transaction.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        transactionRepository.delete(transaction);
    }

    /**
     * 연도·월·카테고리·타입 필터를 조합하여 거래 목록을 조회합니다.
     * 필터 조합이 동적이어서 QueryDSL 없이 스트림으로 간단하게 처리했습니다.
     * month가 null이면 해당 연도 전체를 조회합니다.
     */
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> findByUserWithFilters(User user, int year, Integer month,
                                                              Long categoryId, String type) {
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
    public int sumByUserAndCategoryAndMonth(User user, Long categoryId, String type, int year, int month) {
        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.withDayOfMonth(startDate.lengthOfMonth());

        Integer sum = transactionRepository.sumAmountByUserAndCategoryAndTypeDateBetween(
                user, categoryId, type, startDate, endDate
        );

        // 대상 거래가 없으면 SUM 결과가 null로 내려오므로 0으로 처리합니다.
        return sum != null ? sum : 0;
    }
}
