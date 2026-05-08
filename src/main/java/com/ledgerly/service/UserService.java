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

/**
 * 회원가입·정보 수정·비밀번호 변경을 처리하는 서비스입니다.
 *
 * 회원가입 시 기본 카테고리를 함께 생성합니다. 처음 가입한 사용자도
 * 별도 설정 없이 바로 거래 내역을 추가할 수 있도록 하기 위해서입니다.
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;

    // 신규 가입자에게 제공하는 기본 카테고리 목록입니다.
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

        // 사용자 저장과 카테고리 생성이 같은 트랜잭션 안에 있어, 카테고리 저장이 실패하면 함께 롤백됩니다.
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
        // BCrypt는 단방향 해시라 평문 비교가 불가능하므로 matches()로 검증합니다.
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
