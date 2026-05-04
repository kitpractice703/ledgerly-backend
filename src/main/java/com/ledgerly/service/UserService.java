package com.ledgerly.service;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.repository.CategoryRepository;
import com.ledgerly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;

    private static final List<String[]> DEFAULT_CATEGORIES = List.of(
            new String[]{"식비",      "EXPENSE"},
            new String[]{"교통비",    "EXPENSE"},
            new String[]{"의료/건강", "EXPENSE"},
            new String[]{"쇼핑",      "EXPENSE"},
            new String[]{"문화/여가", "EXPENSE"},
            new String[]{"주거/통신", "EXPENSE"},
            new String[]{"급여",      "INCOME"},
            new String[]{"부업/용돈", "INCOME"}
    );

    @Transactional
    public User register(String email, String password, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(username);
        User savedUser = userRepository.save(user);

        for (String[] entry : DEFAULT_CATEGORIES) {
            Category category = new Category();
            category.setUser(savedUser);
            category.setName(entry[0]);
            category.setType(entry[1]);
            categoryRepository.save(category);
        }

        return savedUser;
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    @Transactional
    public void updateUsername(User user, String username) {
        user.setUsername(username);
        userRepository.save(user);
    }

    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
