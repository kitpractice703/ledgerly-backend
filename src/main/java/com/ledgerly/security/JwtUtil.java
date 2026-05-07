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
 * JwtUtil - JWT 생성·검증·파싱을 담당하는 유틸리티 컴포넌트
 *
 * <p>[설계] JJWT 라이브러리(0.12.x)를 사용하여 JWT를 구현합니다.
 * 서명 알고리즘은 HMAC-SHA256(HS256)을 선택했습니다. 비대칭키(RS256)는
 * 서버 간 토큰 검증이 필요한 MSA 환경에 적합하지만, 단일 서버 구성에서는
 * 관리가 단순한 대칭키 방식이 충분합니다.</p>
 *
 * <p>[보안] 서명 키({@code jwt.secret})는 환경 변수로 주입받아 소스코드에 노출되지 않습니다.
 * 운영 환경의 시크릿은 Oracle Cloud 서버의 환경 변수 또는 Docker Compose의 env 파일로 관리합니다.</p>
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    // [설계] 토큰 유효 기간을 24시간으로 설정합니다. Refresh Token 없이 단순성을 우선하되,
    //        만료 시 프론트엔드 401 인터셉터가 자동 로그아웃 처리합니다.
    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24시간

    /**
     * 이메일을 Subject로 하는 JWT를 생성합니다.
     *
     * @param email 토큰의 주체가 될 사용자 이메일
     * @return 서명된 JWT 문자열
     */
    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION_MS))
                .signWith(getKey())
                .compact();
    }

    /**
     * 토큰에서 이메일(Subject)을 추출합니다.
     *
     * @param token 검증된 JWT 문자열
     * @return 사용자 이메일
     */
    public String extractEmail(String token) {
        return getClaims(token).getSubject();
    }

    /**
     * 토큰의 유효성을 검증합니다.
     *
     * <p>[보안] 서명 위변조({@link JwtException})와 만료({@code ExpiredJwtException})를
     * 모두 포괄합니다. 예외 발생 시 {@code false}를 반환하여 호출 측에서 별도의
     * 예외 처리 없이 분기할 수 있도록 합니다.</p>
     *
     * @param token 검증할 JWT 문자열
     * @return 유효하면 {@code true}, 서명 오류·만료 등이면 {@code false}
     */
    public boolean isValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * 토큰을 파싱하여 Claims(페이로드)를 반환합니다.
     * 서명 검증 및 만료 체크가 이 시점에 함께 수행됩니다.
     */
    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 환경 변수로 주입받은 비밀 문자열로 HMAC-SHA 서명 키를 생성합니다.
     *
     * <p>[보안] UTF-8 인코딩으로 바이트 변환하여 플랫폼 간 일관성을 보장합니다.
     * 키 길이가 짧으면 jjwt가 WeakKeyException을 발생시키므로 최소 32자 이상을 권장합니다.</p>
     */
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
