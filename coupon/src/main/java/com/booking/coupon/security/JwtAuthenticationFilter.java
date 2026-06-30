package com.booking.coupon.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        // Authorization 헤더 추출 ("Bearer <token>" 형태)
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // "Bearer " 제거

            // 토큰 유효성 검증 (위변조/만료)
            if (jwtUtil.validateToken(token)) {
                // 토큰에서 username과 memberId 추출
                String username = jwtUtil.extractUsername(token);
                Long memberId = jwtUtil.extractMemberId(token);

                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                // memberId를 인증 정보에 저장 (예매/마이페이지에서 사용)
                auth.setDetails(memberId);
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 다음 필터로 전달
        filterChain.doFilter(request, response);
    }
}