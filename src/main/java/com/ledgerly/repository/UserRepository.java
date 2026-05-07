package com.ledgerly.repository;

import com.ledgerly.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * UserRepository - 사용자 데이터 접근 인터페이스
 *
 * <p>[설계] Spring Data JPA의 메서드 이름 규칙(Method Name Query)을 활용합니다.
 * 별도의 JPQL을 작성하지 않아도 메서드명으로부터 쿼리가 자동 생성됩니다.</p>
 */
public interface UserRepository extends JpaRepository<User, Long> {

    /** 이메일로 사용자를 조회합니다. 로그인 및 사용자 정보 조회에 사용됩니다. */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 존재 여부를 확인합니다.
     *
     * <p>[설계] {@code findByEmail}으로도 가능하지만, 존재 여부만 확인할 때는
     * 엔티티 전체를 로드하지 않는 {@code EXISTS} 쿼리가 생성되어 성능상 효율적입니다.</p>
     */
    boolean existsByEmail(String email);
}
