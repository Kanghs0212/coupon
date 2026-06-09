package com.booking.coupon.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 마이페이지에서 로그인한 회원의 예매 내역을 조회하는 메서드
    List<Reservation> findByMemberId(Long memberId);
}