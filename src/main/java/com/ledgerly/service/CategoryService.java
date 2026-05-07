package com.ledgerly.service;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * CategoryService - 카테고리 CRUD 비즈니스 로직을 담당하는 서비스
 *
 * <p>[보안] 모든 조회·수정·삭제 메서드에서 {@link CategoryRepository#findByIdAndUser}를 사용합니다.
 * ID만으로 조회하지 않고 사용자를 함께 조건으로 포함하여, 타인의 카테고리 ID를 입력해도
 * Optional.empty()가 반환되고 예외로 처리됩니다. IDOR 방지가 Repository 레벨에서 이루어집니다.</p>
 *
 * <p>[설계] 수정·삭제 시 별도의 소유권 비교 로직 없이 {@code findByIdAndUser}의 결과만으로
 * 접근 제어가 완성됩니다. 코드가 간결하면서도 보안이 유지됩니다.</p>
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    /** 새 카테고리를 생성하고 저장합니다. */
    @Transactional
    public Category save(User user, String name, String type) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        return categoryRepository.save(category);
    }

    /** 현재 사용자의 모든 카테고리를 조회합니다. */
    @Transactional(readOnly = true)
    public List<Category> findAll(User user) {
        return categoryRepository.findByUser(user);
    }

    /**
     * ID와 소유자로 카테고리를 조회합니다. 거래 생성·수정 시 카테고리 소유권 검증에 사용됩니다.
     *
     * <p>[보안] 사용자가 일치하지 않으면 예외를 발생시켜 타인의 카테고리 접근을 차단합니다.</p>
     *
     * @throws IllegalArgumentException 카테고리가 존재하지 않거나 소유자가 다른 경우
     */
    @Transactional(readOnly = true)
    public Category findById(Long id, User user) {
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    /**
     * 카테고리 이름과 타입을 수정합니다.
     *
     * <p>[보안] {@code findByIdAndUser}로 소유권 검증 후 변경합니다.
     * JPA 더티 체킹으로 별도 save() 호출 없이 트랜잭션 커밋 시 UPDATE가 실행됩니다.</p>
     */
    @Transactional
    public void update(Long id, User user, String name, String type) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("수정 권한이 없습니다."));
        category.setName(name);
        category.setType(type);
    }

    /**
     * 카테고리를 삭제합니다.
     *
     * <p>[설계] 해당 카테고리를 참조하는 거래 내역이 있으면 DB 외래키 제약으로 삭제가 거부됩니다.
     * 이 경우 {@link org.springframework.dao.DataIntegrityViolationException}이 발생하며,
     * {@link com.ledgerly.controller.CategoryController}에서 캐치하여 사용자 친화적 메시지를 반환합니다.</p>
     */
    @Transactional
    public void delete(Long id, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("삭제 권한이 없습니다."));
        categoryRepository.delete(category);
    }
}
