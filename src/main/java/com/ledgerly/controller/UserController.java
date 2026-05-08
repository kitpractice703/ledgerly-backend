package com.ledgerly.controller;

import com.ledgerly.domain.User;
import com.ledgerly.dto.ChangePasswordDto;
import com.ledgerly.dto.UpdateUsernameDto;
import com.ledgerly.dto.UserProfileDto;
import com.ledgerly.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

/**
 * UserController - 로그인한 사용자의 프로필 조회·수정 및 비밀번호 변경 컨트롤러
 *
 * "/me" 경로 규칙을 사용하여 항상 현재 인증된 사용자 자신의 리소스만 접근합니다.
 * 타인의 userId를 경로 변수로 받는 방식을 사용하지 않아 IDOR 취약점의 여지를 없앱니다.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** 현재 로그인한 사용자의 프로필(id, email, username)을 조회합니다. */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDto> getProfile(@AuthenticationPrincipal UserDetails userDetails) {
        User user = userService.findByEmail(userDetails.getUsername());
        return ResponseEntity.ok(new UserProfileDto(user.getId(), user.getEmail(), user.getUsername()));
    }

    /** 현재 사용자의 닉네임을 수정합니다. */
    @PutMapping("/me")
    public ResponseEntity<?> updateProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody UpdateUsernameDto dto,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        User user = userService.findByEmail(userDetails.getUsername());
        userService.updateUsername(user, dto.getUsername());
        return ResponseEntity.ok().build();
    }

    /**
     * 비밀번호를 변경합니다.
     *
     * 현재 비밀번호를 입력받아 서비스 레이어에서 BCrypt 비교로 검증합니다.
     * 현재 비밀번호가 틀리면 400 Bad Request를 반환합니다.
     */
    @PutMapping("/me/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordDto dto,
            BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            return ResponseEntity.badRequest().body(bindingResult.getAllErrors().get(0).getDefaultMessage());
        }
        User user = userService.findByEmail(userDetails.getUsername());
        try {
            userService.changePassword(user, dto.getCurrentPassword(), dto.getNewPassword());
            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
