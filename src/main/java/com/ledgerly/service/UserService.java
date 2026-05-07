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
 * UserService - 회원 가입·조회·정보 수정·비밀번호 변경 비즈니스 로직을 담당하는 서비스
 *
 * <p>[설계] 회원가입 시 기본 카테고리를 자동 생성합니다. 신규 사용자가 카테고리를 별도로
 * 등록하지 않아도 즉시 거래 내역을 추가할 수 있어 첫 사용 경험(Onboarding)이 개선됩니다.
 * 기본 카테고리는 사용자별로 독립적으로 생성되므로 다른 사용자의 카테고리와 격리됩니다.</p>
 *
 * <p>[보안] 비밀번호는 BCrypt로 단방향 해시화하여 저장합니다. 평문 비밀번호는 서버에 절대 저장되지 않으며,
 * 검증 시 {@link PasswordEncoder#matches}로 해시 비교만 수행합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CategoryRepository categoryRepository;

    // [설계] 기본 카테고리 목록을 상수로 관리하여 코드와 데이터를 함께 버전 관리합니다.
    //        {카테고리명, 타입(EXPENSE/INCOME)} 쌍으로 구성됩니다.
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

    /**
     * 신규 회원을 등록하고 기본 카테고리를 함께 생성합니다.
     *
     * <p>[보안] 이메일 중복 검사를 먼저 수행하여 동일 이메일로의 중복 가입을 방지합니다.</p>
     * <p>[설계] 사용자 저장과 카테고리 생성을 하나의 트랜잭션으로 묶어, 카테고리 생성 실패 시
     * 사용자 생성도 롤백되어 데이터 정합성을 보장합니다.</p>
     *
     * @throws IllegalArgumentException 이미 사용 중인 이메일인 경우
     */
    @Transactional
    public User register(String email, String password, String username) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        User user = new User();
        user.setEmail(email);
        // [보안] 평문 비밀번호를 BCrypt로 해시화하여 저장
        user.setPassword(passwordEncoder.encode(password));
        user.setUsername(username);
        User savedUser = userRepository.save(user);

        // 회원가입 직후 기본 카테고리를 사용자별로 생성하여 즉시 사용 가능한 상태로 만듭니다.
        for (String[] entry : DEFAULT_CATEGORIES) {
            Category category = new Category();
            category.setUser(savedUser);
            category.setName(entry[0]);
            category.setType(entry[1]);
            categoryRepository.save(category);
        }

        return savedUser;
    }

    /**
     * 이메일로 사용자를 조회합니다. 로그인 이후 인증된 요청에서 사용자 엔티티를 가져올 때 사용합니다.
     *
     * @throws IllegalArgumentException 존재하지 않는 이메일인 경우
     */
    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
    }

    /** 사용자 닉네임을 변경합니다. */
    @Transactional
    public void updateUsername(User user, String username) {
        user.setUsername(username);
        userRepository.save(user);
    }

    /**
     * 비밀번호를 변경합니다.
     *
     * <p>[보안] 현재 비밀번호를 먼저 검증한 후 새 비밀번호로 교체하여,
     * 세션 탈취 등의 상황에서 비밀번호 무단 변경을 방지합니다.</p>
     *
     * @throws IllegalArgumentException 현재 비밀번호가 일치하지 않는 경우
     */
    @Transactional
    public void changePassword(User user, String currentPassword, String newPassword) {
        // [보안] BCrypt 해시 비교로 현재 비밀번호 검증 (평문 비교 불가)
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
