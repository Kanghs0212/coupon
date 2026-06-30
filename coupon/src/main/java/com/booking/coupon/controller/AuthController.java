package com.booking.coupon.controller;

import com.booking.coupon.service.AuthService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
    public ResponseEntity<String> signup(@Valid @RequestBody SignupRequest request) {
        try {
            authService.signup(request.getUsername(), request.getPassword(), request.getName());
            return ResponseEntity.ok("회원가입 성공!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 로그인 API
    @PostMapping("/login")
    public ResponseEntity<String> login(@Valid @RequestBody LoginRequest request) {
        try {
            String token = authService.login(request.getUsername(), request.getPassword());
            // 성공 시 JWT 토큰 반환
            return ResponseEntity.ok(token);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 요청 DTO
    @Data
    public static class SignupRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        @Size(min = 4, max = 20, message = "아이디는 4~20자여야 합니다.")
        private String username;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        @Size(min = 4, max = 64, message = "비밀번호는 4자 이상이어야 합니다.")
        private String password;

        @NotBlank(message = "이름을 입력해주세요.")
        private String name;
    }

    @Data
    public static class LoginRequest {
        @NotBlank(message = "아이디를 입력해주세요.")
        private String username;

        @NotBlank(message = "비밀번호를 입력해주세요.")
        private String password;
    }
}