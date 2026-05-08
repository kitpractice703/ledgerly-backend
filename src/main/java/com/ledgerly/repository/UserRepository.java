package com.ledgerly.repository;

import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    // findByEmail로도 가능하지만, 존재 여부만 확인할 때는 EXISTS 쿼리가 생성되어 더 가볍습니다.
    boolean existsByEmail(String email);
}
