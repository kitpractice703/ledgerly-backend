package com.ledgerly.repository;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * CategoryRepository - 카테고리 데이터 접근 인터페이스
 *
 * <p>[설계] 카테고리는 사용자별로 격리되므로 모든 조회에 {@code User} 파라미터를 포함합니다.
 * 이를 통해 타 사용자의 카테고리에 접근하는 쿼리가 서비스 레벨 이전에 불가능합니다.</p>
 */
public interface CategoryRepository extends JpaRepository<Category, Long> {

    /** 특정 사용자의 모든 카테고리를 조회합니다. */
    List<Category> findByUser(User user);

    /**
     * ID와 사용자로 카테고리를 조회합니다.
     *
     * <p>[보안] ID만으로 조회하지 않고 사용자를 함께 조건으로 포함하여,
     * 다른 사용자의 카테고리 ID를 입력해도 결과가 반환되지 않습니다.</p>
     */
    Optional<Category> findByIdAndUser(Long id, User user);
}
