package com.booking.coupon.init;

import com.booking.coupon.domain.concert.Concert;
import com.booking.coupon.domain.concert.ConcertRepository;
import com.booking.coupon.domain.reservation.ReservationRepository;
import com.booking.coupon.domain.seat.Seat;
import com.booking.coupon.domain.seat.SeatRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 앱 시작 시 콘서트/좌석/예매 데이터를 초기화하고 데모 데이터를 시딩한다. 회원은 보존.
 * app.data-init.enabled = false 면 비활성화.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.data-init.enabled", havingValue = "true", matchIfMissing = true)
public class DataInitializer implements CommandLineRunner {

    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        log.info("== [DataInitializer] 콘서트/좌석 데이터 초기화 시작 (회원 정보는 유지) ==");

        // 기존 데이터 삭제 (FK 순서: 예매 -> 좌석 -> 콘서트). 회원은 제외
        reservationRepository.deleteAllInBatch();
        seatRepository.deleteAllInBatch();
        concertRepository.deleteAllInBatch();

        // DB와 상태를 맞추기 위해 Redis의 좌석 락/큐/대기열도 함께 정리
        Set<String> redisKeys = redisTemplate.keys("ticket:lock:*");
        if (redisKeys != null && !redisKeys.isEmpty()) redisTemplate.delete(redisKeys);
        Set<String> waitingKeys = redisTemplate.keys("waiting:*");
        if (waitingKeys != null && !waitingKeys.isEmpty()) redisTemplate.delete(waitingKeys);
        redisTemplate.delete(List.of("ticket_queue", "ticket_processing", "ticket_dlq"));

        // 콘서트 3개 생성
        Concert concert1 = concertRepository.save(new Concert(
                "2026 자바 스프링 락 페스티벌",
                "https://images.unsplash.com/photo-1540039155732-684735035726?auto=format&fit=crop&w=400&q=80",
                "KSPO DOME"));
        Concert concert2 = concertRepository.save(new Concert(
                "아이유 데뷔 18주년 콘서트",
                "https://images.unsplash.com/photo-1459749411175-04bf5292ceea?auto=format&fit=crop&w=400&q=80",
                "잠실 주경기장"));
        Concert concert3 = concertRepository.save(new Concert(
                "워터밤 서울 2026",
                "https://images.unsplash.com/photo-1533174000222-38bd0dbcc5fb?auto=format&fit=crop&w=400&q=80",
                "잠실 종합운동장"));

        // 콘서트별 좌석 생성
        List<Seat> seats = new ArrayList<>();

        // 1번: VIP석 20개 + R석 30개
        for (int i = 1; i <= 20; i++) seats.add(new Seat(concert1, "VIP석 A열 " + i + "번", 150000));
        for (int i = 1; i <= 30; i++) seats.add(new Seat(concert1, "R석 B열 " + i + "번", 120000));

        // 2번: VIP석 30개
        for (int i = 1; i <= 30; i++) seats.add(new Seat(concert2, "VIP석 " + i + "번", 165000));

        // 3번: 스탠딩 40개
        for (int i = 1; i <= 40; i++) seats.add(new Seat(concert3, "스탠딩 구역 " + i + "번", 110000));

        seatRepository.saveAll(seats);

        log.info("== [DataInitializer] 완료! 콘서트 3개, 좌석 {}개 생성 ==", seats.size());
    }
}
