package com.booking.coupon.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;

@Component
public class JwtUtil {

    // JWT 서명에 사용할 비밀키
    // 실제 운영 환경에서는 application.yml에서 길고 복잡한 값으로 주입
    private final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    // 토큰 만료 시간 (예: 1시간 = 1000ms * 60초 * 60분)
    private final long EXPIRATION_TIME = 1000 * 60 * 60;

    /**
     * 1. 토큰 생성 로직 (로그인 성공 시 호출됨)
     * 예매 시 빠르게 DB를 찌르기 위해, 회원 PK인 memberId도 토큰 내부에 숨겨서(Claim) 발급합니다.
     */
    public String generateToken(Long memberId, String username) {
        return Jwts.builder()
                .setSubject(username) // 토큰의 주인 ID
                .claim("memberId", memberId) // 커스텀 정보: 회원의 DB PK
                .setIssuedAt(new Date()) // 발급 시간
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // 만료 시간
                .signWith(key) // 서버의 비밀키로 암호화(서명) 도장 쾅!
                .compact();
    }

    /**
     * 2. 토큰에서 클레임(숨겨둔 정보) 추출
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * 3. 토큰에서 사용자 ID(username) 꺼내기
     */
    public String extractUsername(String token) {
        return extractAllClaims(token).getSubject();
    }

    /**
     * 4. 토큰에서 회원 PK(memberId) 꺼내기
     * 프론트엔드가 예매 요청을 보낼 때, 파라미터가 아닌 이 토큰 안의 memberId를 꺼내서 안전하게 예매시킵니다.
     */
    public Long extractMemberId(String token) {
        return extractAllClaims(token).get("memberId", Long.class);
    }

    /**
     * 5. 토큰 유효성 검증
     * 유효기간이 지났거나, 해커가 임의로 글자를 조작했다면 false를 반환합니다.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}