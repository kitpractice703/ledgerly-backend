package com.ledgerly.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JwtFilter - HTTP 요청에서 JWT를 추출하고 인증 컨텍스트를 설정하는 보안 필터
 *
 * <p>[설계] {@link OncePerRequestFilter}를 상속하여 포워드·인클루드 등으로 요청이
 * 내부에서 재처리될 때도 필터가 정확히 1회만 실행됨을 보장합니다.</p>
 *
 * <p>[보안] 이 필터가 SecurityContext에 인증 객체를 주입해야 이후 컨트롤러에서
 * {@code @AuthenticationPrincipal UserDetails}로 현재 사용자를 조회할 수 있습니다.
 * 토큰이 유효하지 않으면 컨텍스트를 비워두고 필터 체인을 통과시키며,
 * 이후 Spring Security가 401을 반환합니다.</p>
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final CustomUserDetailsService userDetailsService;

    /**
     * 요청마다 JWT를 검증하고 인증 정보를 SecurityContext에 등록합니다.
     *
     * <p>처리 흐름:</p>
     * <ol>
     *   <li>Authorization 헤더에서 "Bearer " 접두사 이후의 토큰 문자열 추출</li>
     *   <li>서명·만료 검증 ({@link JwtUtil#isValid})</li>
     *   <li>토큰에서 이메일 추출 후 DB에서 사용자 정보 로드</li>
     *   <li>인증 객체를 SecurityContext에 저장 → 이후 필터·컨트롤러에서 인증된 사용자로 처리</li>
     * </ol>
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            // "Bearer " 접두사(7자)를 제거하여 순수 JWT 문자열만 추출
            String token = header.substring(7);

            if (jwtUtil.isValid(token)) {
                String email = jwtUtil.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // [설계] credentials 자리를 null로 전달: 이미 토큰으로 인증이 완료된 상태이므로
                //        비밀번호를 메모리에 유지할 필요가 없습니다.
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                // 요청 IP·세션 정보를 인증 객체에 첨부하여 감사 로그 등에 활용 가능하도록 설정
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 토큰이 없거나 유효하지 않아도 필터 체인을 진행시킵니다.
        // SecurityContext가 비어있으면 Spring Security가 인증 실패로 처리합니다.
        filterChain.doFilter(request, response);
    }
}
