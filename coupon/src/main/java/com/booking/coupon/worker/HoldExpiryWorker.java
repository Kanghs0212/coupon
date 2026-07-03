package com.booking.coupon.worker;

import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import com.booking.coupon.domain.reservation.ReservationStatus;
import com.booking.coupon.domain.seat.Seat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class HoldExpiryWorker {

    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;

    // 결제 마감이 지난 선점(PENDING)을 주기적으로 해제한다.
    @Scheduled(fixedDelayString = "${app.ticket.hold-check-ms:10000}")
    @Transactional
    public void releaseExpiredHolds() {
        List<Reservation> expired =
                reservationRepository.findByStatusAndExpiresAtBefore(ReservationStatus.PENDING, LocalDateTime.now());

        for (Reservation reservation : expired) {
            Seat seat = reservation.getSeat();
            // Redis 좌석 락 해제 -> 좌석 상태 복구 -> 미결제 예매 삭제(유니크 제약 회복)
            redisTemplate.delete("ticket:lock:concert:" + seat.getConcert().getId() + ":seat:" + seat.getId());
            seat.release();
            reservationRepository.delete(reservation);
            log.info("== [HoldExpiry] 미결제 좌석 해제 -> 예매ID: {}, 좌석: {} ==", reservation.getId(), seat.getId());
        }
    }
}
