package com.booking.coupon.domain.seat;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    // 특정 콘서트의 모든 좌석을 조회하는 메서드 (추후 프론트엔드 화면 렌더링용)
    List<Seat> findByConcertId(Long concertId);
}