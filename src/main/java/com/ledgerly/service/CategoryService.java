package com.ledgerly.service;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카테고리 CRUD를 처리하는 서비스입니다.
 *
 * 조회·수정·삭제 모두 findByIdAndUser를 통해 해당 카테고리가 요청자의 것인지
 * Repository 레벨에서 확인합니다. 덕분에 별도의 소유권 비교 로직이 필요 없습니다.
 */
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public Category save(User user, String name, String type) {
        Category category = new Category();
        category.setUser(user);
        category.setName(name);
        category.setType(type);
        return categoryRepository.save(category);
    }

    @Transactional(readOnly = true)
    public List<Category> findAll(User user) {
        return categoryRepository.findByUser(user);
    }

    /**
     * ID로 카테고리를 조회하되, 해당 카테고리가 이 사용자의 것인지도 함께 확인합니다.
     * 거래 생성·수정 시 카테고리 소유권 검증에 사용됩니다.
     */
    @Transactional(readOnly = true)
    public Category findById(Long id, User user) {
        return categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 카테고리입니다."));
    }

    @Transactional
    public void update(Long id, User user, String name, String type) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("수정 권한이 없습니다."));
        category.setName(name);
        category.setType(type);
        // 별도 save() 없이 트랜잭션 종료 시 더티 체킹으로 UPDATE가 실행됩니다.
    }

    /**
     * 카테고리를 삭제합니다.
     * 이 카테고리를 사용 중인 거래 내역이 있으면 DB 외래키 제약으로 삭제가 거부되며,
     * CategoryController에서 해당 예외를 잡아 안내 메시지를 반환합니다.
     */
    @Transactional
    public void delete(Long id, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("삭제 권한이 없습니다."));
        categoryRepository.delete(category);
    }
}
