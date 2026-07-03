package com.booking.coupon.domain.reservation;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {
    // 마이페이지: 회원의 특정 상태(예: 결제 완료) 예매 내역 조회
    List<Reservation> findByMemberIdAndStatus(Long memberId, ReservationStatus status);

    // 만료 홀드 정리: 결제 마감이 지난 대기(PENDING) 예매 조회
    List<Reservation> findByStatusAndExpiresAtBefore(ReservationStatus status, LocalDateTime time);
}
