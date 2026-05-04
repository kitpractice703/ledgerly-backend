package com.ledgerly.service;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
    }

    @Transactional
    public void delete(Long id, User user) {
        Category category = categoryRepository.findByIdAndUser(id, user)
                .orElseThrow(() -> new IllegalArgumentException("삭제 권한이 없습니다."));
        categoryRepository.delete(category);
    }
}
