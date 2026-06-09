package com.booking.coupon.worker;

import com.booking.coupon.domain.member.Member;
import com.booking.coupon.domain.member.MemberRepository;
import com.booking.coupon.domain.seat.Seat;
import com.booking.coupon.domain.seat.SeatRepository;
import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketWorker {

    private final StringRedisTemplate redisTemplate;
    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    // 0.5초(500ms)마다 백그라운드에서 실행되며 큐를 처리합니다.
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void processTicketQueue() {
        String queueKey = "ticket_queue";

        // 큐에서 가장 먼저 들어온 예약 요청 메시지를 하나 꺼냅니다. (왼쪽에서 Pop)
        String queueMessage = redisTemplate.opsForList().leftPop(queueKey);

        if (queueMessage != null) {
            try {
                // 메시지 파싱 (concertId:seatId:memberId)
                String[] tokens = queueMessage.split(":");
                Long concertId = Long.parseLong(tokens[0]);
                Long seatId = Long.parseLong(tokens[1]);
                Long memberId = Long.parseLong(tokens[2]);

                log.info("== [Worker] 예매 요청 처리 시작 -> 회원: {}, 좌석: {} ==", memberId, seatId);

                // 1. DB에서 관련 엔티티들을 안전하게 조회합니다.
                Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

                // 2. 좌석 상태를 AVAILABLE -> RESERVED로 변경합니다. (내부 캡슐화 로직 작동)
                seat.reserve();

                // 3. 최종 영수증(Reservation)을 발행하여 DB에 저장합니다.
                Reservation reservation = new Reservation(member, seat);
                reservationRepository.save(reservation);

                log.info("== [Worker] 최종 예매 완료 및 영수증 발행 성공! 예매 ID: {} ==", reservation.getId());

            } catch (Exception e) {
                log.error("== [Worker] 예매 처리 중 에러 발생: {} ==", e.getMessage());
                // 필요 시 실패 대기열(DLQ) 적재 또는 Redis 락 해제 로직을 구현할 수 있습니다.
            }
        }
    }
}