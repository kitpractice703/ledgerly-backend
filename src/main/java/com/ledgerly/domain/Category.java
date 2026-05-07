package com.ledgerly.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/**
 * Category - 거래 내역을 분류하는 카테고리 엔티티
 *
 * <p>[설계] 카테고리는 사용자별로 완전히 격리됩니다. 모든 카테고리는 특정 사용자에 귀속되어
 * 다른 사용자의 카테고리를 볼 수 없습니다. 회원가입 시 기본 카테고리가 자동 생성됩니다.</p>
 *
 * <p>[설계] {@code @JsonIgnoreProperties}로 Hibernate 프록시 객체의 내부 속성이
 * JSON에 노출되는 것을 방지합니다. Lazy Loading 프록시는 {@code hibernateLazyInitializer}와
 * {@code handler} 필드를 가지는데, 이를 직렬화하면 오류가 발생합니다.</p>
 */
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
@Entity
@Table(name = "category")
@Getter
@Setter
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // [보안] 카테고리 소유자를 나타냅니다. @JsonIgnore로 응답에서 제외하여
    //        다른 사용자의 ID가 노출되지 않도록 합니다.
    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private String name;

    // 거래 타입을 나타냅니다. "INCOME"(수입) 또는 "EXPENSE"(지출) 두 가지 값을 가집니다.
    @Column(nullable = false)
    private String type;
}
