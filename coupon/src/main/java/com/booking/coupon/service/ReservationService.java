package com.booking.coupon.service;

import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import com.booking.coupon.domain.reservation.ReservationStatus;
import com.booking.coupon.domain.seat.Seat;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ReservationService {

    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;

    // 결제 완료 처리: 선점(PENDING/HELD)을 예매 확정(CONFIRMED/RESERVED)으로 전환한다.
    @Transactional
    public void pay(Long reservationId, Long memberId) {
        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 예매입니다."));

        if (!reservation.getMember().getId().equals(memberId)) {
            throw new IllegalArgumentException("본인 예매만 결제할 수 있습니다.");
        }
        if (reservation.getStatus() != ReservationStatus.PENDING) {
            throw new IllegalArgumentException("결제할 수 있는 상태가 아닙니다.");
        }
        if (reservation.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("결제 시간이 만료되었습니다.");
        }

        Seat seat = reservation.getSeat();
        seat.confirm();          // HELD -> RESERVED
        reservation.confirm();   // PENDING -> CONFIRMED

        // 판매 확정된 좌석은 Redis 락 TTL을 제거해 영구 점유로 둔다 (만료 후 거짓 선점 방지).
        redisTemplate.persist(lockKey(seat));
    }

    private String lockKey(Seat seat) {
        return "ticket:lock:concert:" + seat.getConcert().getId() + ":seat:" + seat.getId();
    }
}
