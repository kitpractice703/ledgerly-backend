package com.ledgerly.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * 사용자 계정 엔티티입니다.
 * 테이블명을 users로 지정한 이유는 user가 MySQL 예약어라 충돌이 생기기 때문입니다.
 * password 필드는 @JsonIgnore로 API 응답에서 제외합니다.
 */
@Entity
@Table(name = "users")
@Setter
@Getter
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private String username;

    @Column(updatable = false) // 생성 후 변경 불가능한 감사 필드
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
