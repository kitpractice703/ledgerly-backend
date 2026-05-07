package com.ledgerly.security;

import com.ledgerly.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * CustomUserDetailsService - Spring Security의 인증 처리를 위한 사용자 정보 로드 서비스
 *
 * <p>[설계] {@link UserDetailsService}를 구현하여 Spring Security의 인증 메커니즘과 통합합니다.
 * {@link com.ledgerly.controller.AuthController}에서 {@code AuthenticationManager.authenticate()}를
 * 호출하면 내부적으로 이 서비스의 {@code loadUserByUsername}이 호출됩니다.</p>
 *
 * <p>[설계] username 파라미터로 이메일을 사용합니다. Spring Security의 기본 username 개념을
 * 이 프로젝트에서는 이메일로 대체하여 이메일 기반 로그인을 구현합니다.</p>
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * 이메일로 사용자를 조회하고 Spring Security의 {@link UserDetails} 객체로 변환합니다.
     *
     * <p>[설계] 권한을 {@code ROLE_USER} 하나로 단순화합니다. 현재 단일 역할 구조이므로
     * 권한 관리 복잡도를 낮추었습니다. 역할 구분이 필요해지면 DB에 권한 컬럼을 추가할 수 있습니다.</p>
     *
     * @param email 로그인 식별자로 사용되는 이메일
     * @throws UsernameNotFoundException 해당 이메일의 사용자가 없는 경우
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        com.ledgerly.domain.User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("존재하지 않는 사용자입니다."));

        // Spring Security의 User 객체(이메일, 해시된 비밀번호, 권한 목록)를 반환합니다.
        return new User(user.getEmail(), user.getPassword(),
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}
