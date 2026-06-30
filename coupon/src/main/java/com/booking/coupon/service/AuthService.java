package com.booking.coupon.service;

import com.booking.coupon.domain.member.Member;
import com.booking.coupon.domain.member.MemberRepository;
import com.booking.coupon.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원가입
    @Transactional
    public void signup(String username, String password, String name) {
        // 아이디 중복 검사
        if (memberRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 비밀번호 암호화 후 저장
        String encodedPassword = passwordEncoder.encode(password);
        Member newMember = new Member(username, encodedPassword, name);
        memberRepository.save(newMember);
    }

    // 로그인 (성공 시 토큰 반환)
    public String login(String username, String password) {
        // 회원 조회 (계정 존재 여부 노출을 막기 위해 실패 메시지를 통일)
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다."));

        // 비밀번호 검증
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // JWT 토큰 발급
        return jwtUtil.generateToken(member.getId(), member.getUsername());
    }
}