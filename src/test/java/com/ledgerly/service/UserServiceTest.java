package com.ledgerly.service;

import com.ledgerly.domain.Category;
import com.ledgerly.domain.User;
import com.ledgerly.repository.CategoryRepository;
import com.ledgerly.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("회원가입 성공")
    void register_success() {
        // given
        when(userRepository.existsByEmail("test@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));


        // when
        User result = userService.register("test@test.com", "password123", "김인태");

        // then
        assertThat(result.getEmail()).isEqualTo("test@test.com");
        assertThat(result.getPassword()).isEqualTo("encodedPassword");
        assertThat(result.getUsername()).isEqualTo("김인태");
        verify(userRepository, times(1)).save(any(User.class));
        verify(categoryRepository, times(8)).save(any(Category.class));

    }

    @Test
    @DisplayName("이메일 중복 시 예외 발생")
    void register_duplicateEmail_throwsException() {
        //given
        when(userRepository.existsByEmail("test@test.com")).thenReturn(true);

        // when, then
        assertThatThrownBy(() -> userService.register("test@test.com", "password123", "김인태"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 이메일입니다.");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("존재하지 않는 이메일로 조회 시 예외 발생")
    void findByEmail_notFound_throwsException() {
        // given
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> userService.findByEmail("notexist@test.com")
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않는 사용자입니다.");
    }

}