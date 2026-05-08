package com.ledgerly.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 생성·검증·파싱을 담당하는 유틸리티 클래스입니다.
 *
 * HS256(대칭키) 서명 방식을 사용합니다. RS256(비대칭키)은 서버 간 토큰 검증이
 * 필요한 MSA 환경에 적합하지만, 단일 서버 구성에서는 관리가 단순한 대칭키로 충분합니다.
 * 서명 키는 환경 변수로 주입받아 소스코드에 노출되지 않습니다.
 */
@Component
public class JwtProvider {

    @Value("${jwt.secret}")
    private String secret;

    // Refresh Token 없이 단순하게 유지하되, 만료 시 프론트엔드 401 인터셉터가 로그아웃을 처리합니다.
    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24시간

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getKey())
                .compact();
    }

    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 서명 위변조와 만료를 모두 검사합니다.
     * 예외를 직접 던지지 않고 boolean으로 반환하여 호출부를 단순하게 유지합니다.
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        // jjwt는 키 길이가 짧으면 WeakKeyException을 던지므로 secret은 32자 이상을 권장합니다.
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
