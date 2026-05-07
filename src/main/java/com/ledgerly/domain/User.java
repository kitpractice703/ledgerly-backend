package com.ledgerly.domain;


import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * User - 사용자 계정 엔티티
 *
 * <p>[설계] 테이블명을 {@code users}로 지정합니다. 많은 DB에서 {@code user}가 예약어이므로
 * 충돌을 피하기 위해 복수형 이름을 사용합니다.</p>
 *
 * <p>[보안] {@code password} 필드에 {@code @JsonIgnore}를 적용하여 JSON 직렬화 시
 * 해시된 비밀번호가 API 응답에 포함되지 않도록 합니다.</p>
 */
@Entity
@Table(name = "users")
@Setter
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 이메일을 로그인 식별자로 사용하며, 유일성을 DB 레벨에서도 보장합니다.
    @Column(nullable = false, unique = true)
    private String email;

    // [보안] BCrypt 해시값을 저장합니다. @JsonIgnore로 API 응답에서 제외됩니다.
    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    // [설계] updatable = false로 설정하여 생성 후 수정 불가능한 감사 필드로 관리합니다.
    @Column(updatable = false)
    private LocalDateTime createdAt;

    // JPA가 엔티티를 처음 저장하기 직전에 자동으로 생성 시각을 기록합니다.
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
