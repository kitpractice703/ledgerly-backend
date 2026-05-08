package com.ledgerly.repository;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByUser(User user);

    // ID 단독이 아닌 사용자를 함께 조건으로 걸어, 타인의 카테고리 ID를 넣어도 결과가 나오지 않습니다.
    Optional<Category> findByIdAndUser(Long id, User user);
}
