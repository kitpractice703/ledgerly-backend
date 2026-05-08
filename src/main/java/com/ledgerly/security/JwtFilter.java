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
 * 모든 HTTP 요청에서 JWT를 추출하고 인증 정보를 SecurityContext에 등록하는 필터입니다.
 *
 * OncePerRequestFilter를 상속하여 포워드·인클루드로 요청이 내부 재처리될 때도
 * 필터가 정확히 1회만 실행됩니다. 토큰이 없거나 유효하지 않으면 컨텍스트를
 * 비워두고 다음 필터로 넘기며, 이후 Spring Security가 401을 반환합니다.
 */
@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtProvider jwtProvider;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7); // "Bearer " 7자 제거

            if (jwtProvider.isValid(token)) {
                String email = jwtProvider.extractEmail(token);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                // 토큰으로 인증이 완료된 상태이므로 credentials(비밀번호)는 null로 넘깁니다.
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        filterChain.doFilter(request, response);
    }
}
