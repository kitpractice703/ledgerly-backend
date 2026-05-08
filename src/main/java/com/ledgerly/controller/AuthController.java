package com.ledgerly.controller;

import com.ledgerly.domain.User;
import com.ledgerly.dto.AuthResponseDto;
import com.ledgerly.dto.LoginRequestDto;
import com.ledgerly.dto.UserRegisterDto;
import com.ledgerly.security.JwtProvider;
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
 * 회원가입과 로그인을 처리하는 컨트롤러입니다.
 * /api/auth/** 경로는 SecurityConfig에서 인증 없이 허용합니다.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;
    private final AuthenticationManager authenticationManager;
    private final JwtProvider jwtProvider;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserRegisterDto dto,
                                      BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
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
     * 로그인 성공 시 JWT를 발급합니다.
     * 인증 실패 메시지는 이메일/비밀번호 중 어느 쪽이 틀렸는지 구분하지 않습니다.
     * 구분할 경우 계정 존재 여부를 노출할 수 있기 때문입니다.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDto dto,
                                   BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest()
                    .body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getEmail(), dto.getPassword())
            );
        } catch (AuthenticationException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        User user = userService.findByEmail(dto.getEmail());
        String token = jwtProvider.generateToken(user.getEmail());

        return ResponseEntity.ok(new AuthResponseDto(token, user.getEmail(), user.getUsername()));
    }
}
