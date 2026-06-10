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

    // 1. 회원 가입
    @Transactional
    public void signup(String username, String password, String name) {
        // 이미 있는 아이디인지 1차 검사
        if (memberRepository.findByUsername(username).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 비밀번호를 안전하게 암호화해서 저장!
        String encodedPassword = passwordEncoder.encode(password);
        Member newMember = new Member(username, encodedPassword, name);
        memberRepository.save(newMember);
    }

    // 2. 로그인 (성공 시 토큰 반환)
    public String login(String username, String password) {
        // DB에서 회원 찾기
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("가입되지 않은 아이디입니다."));

        // 암호화된 비밀번호가 일치하는지 확인
        if (!passwordEncoder.matches(password, member.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        // 로그인 성공! JWT 토큰을 찍어서 건네줍니다.
        return jwtUtil.generateToken(member.getId(), member.getUsername());
    }
}