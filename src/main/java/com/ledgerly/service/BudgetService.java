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
 * BudgetService - 예산 등록·조회·수정·삭제 및 예산 현황 집계 비즈니스 로직을 담당하는 서비스
 *
 * <p>[보안] 수정·삭제 시 요청 사용자와 예산 소유자의 일치 여부를 검증하여 IDOR를 방지합니다.</p>
 *
 * <p>[설계] 예산 현황 계산 시 {@link TransactionService#sumByUserAndCategoryAndMonth}를 호출하여
 * 실제 지출액을 조회합니다. 예산과 거래 도메인을 서비스 레이어에서 조합하고 DTO로 변환합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class BudgetService {

    private final BudgetRepository budgetRepository;
    private final CategoryService categoryService;
    private final TransactionService transactionService;

    /**
     * 카테고리·연월의 예산 한도를 등록합니다.
     *
     * <p>[설계] 동일 (사용자, 카테고리, 연도, 월) 조합의 예산 중복 등록을 방지합니다.
     * DB 유니크 제약 대신 서비스 레이어에서 사전 검증하여 명확한 오류 메시지를 제공합니다.</p>
     *
     * @throws IllegalArgumentException 동일 조건의 예산이 이미 존재하는 경우
     */
    @Transactional
    public Budget save(
            User user,
            Long categoryId,
            Integer limitAmount,
            Integer year,
            Integer month
    ) {
        // [비즈니스] 동일 사용자·카테고리·연월 조합의 예산 중복 등록 방지
        budgetRepository.findByUserAndCategoryIdAndYearAndMonth(
                user,
                categoryId,
                year,
                month
        ).ifPresent(b -> {
            throw new IllegalArgumentException("이미 해당 카테고리의 예산이 존재합니다.");
        });

        // [보안] CategoryService.findById가 카테고리 소유권을 검증합니다.
        Category category = categoryService.findById(categoryId, user);

        Budget budget = new Budget();
        budget.setUser(user);
        budget.setCategory(category);
        budget.setLimitAmount(limitAmount);
        budget.setYear(year);
        budget.setMonth(month);

        return budgetRepository.save(budget);
    }

    /** 특정 사용자의 연월 예산 목록을 조회합니다. */
    @Transactional(readOnly = true)
    public List<Budget> findByUserAndMonth(
            User user,
            Integer year,
            Integer month
    ) {
        return budgetRepository.findByUserAndYearAndMonth(user, year, month);
    }

    /**
     * 예산 목록에 실제 지출액과 초과 여부를 포함한 현황 DTO를 반환합니다.
     *
     * <p>[설계] 각 예산마다 {@link TransactionService}를 호출하여 실제 지출을 조회합니다.
     * 예산 수가 많아지면 N+1 문제가 될 수 있으나, 월별 예산 수는 카테고리 수로 제한되어
     * 현재 데이터 규모에서는 허용 가능한 수준입니다.</p>
     */
    @Transactional(readOnly = true)
    public List<BudgetStatusDto> findBudgetStatusByUserAndMonth(
            User user, Integer year, Integer month
    ) {
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

    /**
     * 예산 한도 금액을 수정합니다.
     *
     * <p>[보안] 소유권 검증 후 한도를 변경합니다. JPA 더티 체킹으로 트랜잭션 커밋 시 UPDATE가 실행됩니다.</p>
     */
    @Transactional
    public void update(Long budgetId, User user, Integer limitAmount) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예산입니다."));

        // [보안] IDOR 방지: 타인의 예산 수정 시도 차단
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        budget.setLimitAmount(limitAmount);
    }

    /**
     * 예산을 삭제합니다.
     *
     * <p>[보안] 소유권 검증 후 삭제합니다. 타인의 예산 삭제 시도를 차단합니다.</p>
     */
    @Transactional
    public void delete(Long budgetId, User user) {
        Budget budget = budgetRepository.findById(budgetId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예산입니다."));

        // [보안] IDOR 방지: 타인의 예산 삭제 시도 차단
        if (!budget.getUser().getId().equals(user.getId())) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        budgetRepository.delete(budget);
    }

    /**
     * 특정 카테고리·연월의 예산이 초과되었는지 확인합니다.
     * 예산이 등록되어 있지 않으면 초과로 판단하지 않고 {@code false}를 반환합니다.
     */
    @Transactional(readOnly = true)
    public boolean isExceeded(
            User user,
            Long categoryId,
            Integer year,
            Integer month
    ) {
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
