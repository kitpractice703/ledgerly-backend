package com.ledgerly.config;

import com.ledgerly.security.JwtFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * SecurityConfig - Spring Security 전체 보안 정책을 정의하는 설정 클래스
 *
 * <p>[설계] JWT 기반 Stateless 인증 방식을 채택하여 서버가 세션 상태를 유지하지 않습니다.
 * 이를 통해 수평 확장(Scale-out) 시 세션 공유 문제 없이 운영할 수 있습니다.</p>
 *
 * <p>[보안] 미인증 요청에 대해 Spring Security 기본 동작(로그인 페이지로 302 리다이렉트) 대신
 * 401 상태코드를 반환합니다. React SPA는 페이지 리다이렉트가 아닌 API 응답 코드로
 * 인증 상태를 판단하기 때문입니다.</p>
 */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    /**
     * 비밀번호 단방향 해시 인코더를 Bean으로 등록합니다.
     *
     * <p>[보안] BCrypt는 솔팅(salting)과 비용 인수(cost factor)를 내장하여
     * 레인보우 테이블 공격과 브루트포스 공격에 강합니다.</p>
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Spring Security의 AuthenticationManager를 Bean으로 노출합니다.
     * AuthController에서 로그인 시 이메일/비밀번호 인증 위임에 사용됩니다.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * HTTP 보안 필터 체인을 구성합니다.
     *
     * <ul>
     *   <li>[보안] CSRF 비활성화: JWT를 Authorization 헤더로 전달하는 Stateless 방식에서는
     *       쿠키 기반 세션을 사용하지 않으므로 CSRF 공격이 성립하지 않습니다.</li>
     *   <li>[설계] STATELESS 세션 정책: 서버가 HttpSession을 생성하지 않아 메모리를 절약하고
     *       로드밸런서 환경에서 세션 어피니티가 불필요합니다.</li>
     *   <li>[보안] /api/auth/** 와 /error 만 인증 없이 허용하고 나머지는 모두 인증 필요.</li>
     *   <li>[설계] JwtFilter를 UsernamePasswordAuthenticationFilter 앞에 배치하여
     *       토큰 기반 인증이 폼 로그인보다 먼저 처리되도록 합니다.</li>
     * </ul>
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/error").permitAll()
                .anyRequest().authenticated()
            )
            // [보안] 미인증 요청 시 302 리다이렉트 대신 401 응답 반환 → React SPA의 axios 인터셉터가 처리
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS(Cross-Origin Resource Sharing) 정책을 설정합니다.
     *
     * <p>[보안] 허용 출처를 명시적으로 열거하여 출처를 모두 허용하는 와일드카드(*) 대신
     * 개발 환경(localhost), 운영 프론트엔드(Netlify), 운영 백엔드 도메인(DuckDNS)만 허용합니다.</p>
     *
     * <p>[설계] {@code setAllowedOriginPatterns}를 사용하면 {@code allowCredentials(true)}와
     * 함께 쓸 수 있습니다. {@code setAllowedOrigins("*")}는 자격증명 포함 요청과 함께 쓸 수 없습니다.</p>
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
                "http://localhost:5173",              // 로컬 개발 서버 (Vite 기본 포트)
                "https://ledgerly-kit.netlify.app",  // Netlify 운영 프론트엔드
                "https://ledgerly-kit.duckdns.org"   // Oracle Cloud 서버 도메인
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
