package com.booking.coupon.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// 스프링 컨텍스트 없이 동작하는 순수 단위 테스트 (인프라 불필요)
class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("test-secret-key-for-jwt-unit-test-32bytes-minimum!!");

    @Test
    @DisplayName("토큰 생성 후 검증과 클레임(username, memberId) 추출이 정상 동작한다")
    void generateAndParseToken() {
        String token = jwtUtil.generateToken(42L, "alice");

        assertThat(jwtUtil.validateToken(token)).isTrue();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.extractMemberId(token)).isEqualTo(42L);
    }

    @Test
    @DisplayName("위조된 토큰은 검증에 실패한다")
    void tamperedTokenIsRejected() {
        String token = jwtUtil.generateToken(1L, "bob");
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThat(jwtUtil.validateToken(tampered)).isFalse();
    }

    @Test
    @DisplayName("다른 비밀키로 서명된 토큰은 검증에 실패한다")
    void tokenSignedWithDifferentKeyIsRejected() {
        JwtUtil other = new JwtUtil("another-totally-different-secret-key-32bytes!!");
        String token = other.generateToken(1L, "bob");

        assertThat(jwtUtil.validateToken(token)).isFalse();
    }
}
