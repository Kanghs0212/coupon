package com.booking.coupon.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    // 비밀번호를 암호화해주는 도구 (DB에 비밀번호가 생얼로 저장되는 것을 막아줍니다)
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // REST API이므로 CSRF 방어 비활성화
                .cors(cors -> cors.configure(http)) // 프론트엔드 연동을 위한 CORS 허용
                // 서버에 세션(상태)을 저장하지 않겠다! (JWT를 쓰기 위한 필수 설정)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // API별 접근 권한 설정
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/**").permitAll() // 회원가입, 로그인은 누구나 가능
                        .requestMatchers("/concerts/**").permitAll() // 콘서트 구경(예매 페이지 접근 등) 누구나 가능
                        .anyRequest().authenticated() // 그 외의 모든 요청(마이페이지 등)은 로그인해야 가능!
                )
                // 우리가 만든 JWT 검사기를 기본 필터보다 먼저 실행되게 끼워 넣습니다.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}