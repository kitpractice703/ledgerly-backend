package com.ledgerly.repository;

import com.ledgerly.domain.Budget;
import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * BudgetRepository - 예산 데이터 접근 인터페이스
 *
 * <p>[설계] 예산은 (사용자, 카테고리, 연도, 월) 조합으로 유일하게 식별됩니다.
 * 모든 조회에 사용자를 포함하여 데이터 격리를 보장합니다.</p>
 */
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    /** 특정 사용자의 연월 예산 목록을 조회합니다. 대시보드 예산 현황 표시에 사용됩니다. */
    List<Budget> findByUserAndYearAndMonth(User user, Integer year, Integer month);

    /**
     * 특정 사용자·카테고리·연월의 예산을 조회합니다.
     *
     * <p>[설계] 예산 등록 시 같은 카테고리·연월의 중복 등록을 방지하고,
     * 예산 초과 여부 계산 시 해당 예산 한도를 가져오는 데 사용됩니다.</p>
     */
    Optional<Budget> findByUserAndCategoryIdAndYearAndMonth(
            User user,
            Long categoryId,
            Integer year,
            Integer month
    );
}
