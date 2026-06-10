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

        // 1. 요청 헤더에서 "Authorization" 값을 꺼냅니다. (보통 "Bearer eyJhbG..." 형태로 들어옵니다)
        String authHeader = request.getHeader("Authorization");

        // 2. 헤더에 토큰이 제대로 들어있다면 검사를 시작합니다.
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7); // "Bearer " 글자 떼어내고 순수 토큰만 추출

            // 3. 토큰이 진짜인지(위조되지 않고 만료되지 않았는지) 확인합니다.
            if (jwtUtil.validateToken(token)) {
                // 4. 진짜라면 토큰 안에서 사용자 아이디를 꺼냅니다.
                String username = jwtUtil.extractUsername(token);

                // 5. 스프링 시큐리티에게 "이 사용자 인증 성공했어! 통과시켜줘" 하고 알려줍니다.
                UsernamePasswordAuthenticationToken auth =
                        new UsernamePasswordAuthenticationToken(username, null, new ArrayList<>());
                SecurityContextHolder.getContext().setAuthentication(auth);
            }
        }

        // 6. 검사가 끝났으면 다음 단계(컨트롤러 등)로 요청을 넘겨줍니다.
        filterChain.doFilter(request, response);
    }
}