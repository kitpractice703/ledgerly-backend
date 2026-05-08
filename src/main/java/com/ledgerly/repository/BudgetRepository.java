package com.ledgerly.repository;

import com.ledgerly.domain.Budget;
import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    List<Budget> findByUserAndYearAndMonth(User user, Integer year, Integer month);

    // 중복 등록 방지와 예산 초과 여부 확인 두 곳에서 사용합니다.
    Optional<Budget> findByUserAndCategoryIdAndYearAndMonth(
            User user, Long categoryId, Integer year, Integer month
    );
}
