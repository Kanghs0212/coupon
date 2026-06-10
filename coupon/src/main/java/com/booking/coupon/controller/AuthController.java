package com.booking.coupon.controller;

import com.booking.coupon.service.AuthService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@CrossOrigin("*") // CORS 허용
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // 회원가입 API
    @PostMapping("/signup")
    public ResponseEntity<String> signup(@RequestBody SignupRequest request) {
        try {
            authService.signup(request.getUsername(), request.getPassword(), request.getName());
            return ResponseEntity.ok("회원가입 성공!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());
            // 로그인에 성공하면, 만들어진 "JWT 토큰" 글자를 그대로 반환해 줍니다!
            return ResponseEntity.ok(token);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // --- 프론트에서 데이터를 받을 때 사용할 DTO (Data Transfer Object) ---
    @Data
    public static class SignupRequest {
        private String username;
        private String password;
        private String name;
    }

    @Data
    public static class LoginRequest {
        private String username;
        private String password;
    }
}