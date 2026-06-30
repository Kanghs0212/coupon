package com.booking.coupon.service;

import com.booking.coupon.domain.concert.Concert;
import com.booking.coupon.domain.concert.ConcertRepository;
import com.booking.coupon.domain.member.Member;
import com.booking.coupon.domain.member.MemberRepository;
import com.booking.coupon.domain.reservation.ReservationRepository;
import com.booking.coupon.domain.seat.Seat;
import com.booking.coupon.domain.seat.SeatRepository;
import com.booking.coupon.domain.seat.SeatStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

// DB 정합성 가드(좌석 상태 + 유니크 제약) 검증. PostgreSQL/Redis 컨텍스트가 필요하다(docker-compose up).
@SpringBootTest
public class TicketReservationConcurrencyTest {

    @Autowired private TicketReservationProcessor processor;
    @Autowired private MemberRepository memberRepository;
    @Autowired private ConcertRepository concertRepository;
    @Autowired private SeatRepository seatRepository;
    @Autowired private ReservationRepository reservationRepository;

    private Long seatId;
    private Long memberId;

    @BeforeEach
    void setUp() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        concertRepository.deleteAll();

        memberId = memberRepository.save(new Member("tester", "pw", "테스터")).getId();
        Concert concert = concertRepository.save(new Concert("테스트 콘서트", null, "테스트홀"));
        seatId = seatRepository.save(new Seat(concert, "A열 1번", 100000)).getId();
    }

    @AfterEach
    void tearDown() {
        reservationRepository.deleteAll();
        seatRepository.deleteAll();
        concertRepository.deleteAll();
        memberRepository.deleteAll();
    }

    @Test
    @DisplayName("같은 좌석에 동시 예매가 몰려도 정확히 1건만 성공한다")
    void onlyOneReservationSucceedsUnderConcurrency() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger successCount = new AtomicInteger();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    processor.reserve(seatId, memberId);
                    successCount.incrementAndGet();
                } catch (Exception ignored) {
                    // 좌석 상태 가드/낙관적 락/유니크 제약으로 패배한 요청은 예외 발생 (정상)
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executorService.shutdown();

        assertThat(successCount.get()).isEqualTo(1);
        assertThat(reservationRepository.count()).isEqualTo(1);
        assertThat(seatRepository.findById(seatId).orElseThrow().getStatus()).isEqualTo(SeatStatus.RESERVED);
    }
}
