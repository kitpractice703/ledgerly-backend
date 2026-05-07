package com.ledgerly.controller;

import com.ledgerly.domain.User;
import com.ledgerly.dto.AuthResponseDto;
import com.ledgerly.dto.LoginRequestDto;
import com.ledgerly.dto.UserRegisterDto;
import com.ledgerly.security.JwtUtil;
import com.ledgerly.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * AuthController - 회원가입과 로그인 엔드포인트를 처리하는 컨트롤러
 *
 * <p>[설계] 인증 관련 엔드포인트만 담당하는 전용 컨트롤러로 분리하여 단일 책임 원칙을 적용합니다.
 * {@code /api/auth/**} 경로는 {@link com.ledgerly.config.SecurityConfig}에서 인증 없이 허용됩니다.</p>
 *
 * <p>[설계] 입력 유효성 검사는 DTO의 Bean Validation 어노테이션({@code @NotBlank}, {@code @Email} 등)과
 * {@code @Valid} + {@link BindingResult} 조합으로 처리합니다. {@link BindingResult}를 사용하면
 * 검증 실패 시 예외를 던지지 않고 응답 본문에 오류 메시지를 담아 반환할 수 있습니다.</p>
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    /**
     * 신규 회원을 등록합니다.
     *
     * <p>유효성 검사 → 이메일 중복 확인 → 사용자 저장 + 기본 카테고리 생성 순서로 처리됩니다.</p>
     *
     * @return 201 Created (성공) / 400 Bad Request (유효성 오류 또는 중복 이메일)
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterDto dto,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            // 첫 번째 유효성 오류 메시지만 반환하여 응답을 단순하게 유지합니다.
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            userService.register(dto.getEmail(), dto.getPassword(), dto.getUsername());
            return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    /**
     * 이메일·비밀번호로 로그인하고 JWT를 발급합니다.
     *
     * <p>[설계] 인증 처리를 {@link AuthenticationManager}에 위임하여 Spring Security의
     * {@link com.ledgerly.security.CustomUserDetailsService}와 BCrypt 비교 로직을 재사용합니다.
     * 컨트롤러가 비밀번호 검증 방식의 세부 구현에 의존하지 않도록 추상화합니다.</p>
     *
     * <p>[보안] 인증 실패 시 이메일/비밀번호 중 어느 쪽이 틀렸는지 구분하지 않는
     * 통합 오류 메시지를 반환하여 계정 열거(Account Enumeration) 공격을 방지합니다.</p>
     *
     * @return 200 OK + JWT·이메일·닉네임 (성공) / 400 Bad Request (유효성 오류) / 401 Unauthorized (인증 실패)
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto dto,
                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            // Spring Security가 이메일·비밀번호를 검증합니다. 실패 시 AuthenticationException 발생.
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        // 인증 성공 후 사용자 정보를 조회하여 JWT를 생성하고 응답합니다.
        User user = userService.findByEmail(dto.getEmail());
        String token = jwtUtil.generateToken(user.getEmail());

        return ResponseEntity.ok(new AuthResponseDto(token, user.getEmail(), user.getUsername()));
    }
}
