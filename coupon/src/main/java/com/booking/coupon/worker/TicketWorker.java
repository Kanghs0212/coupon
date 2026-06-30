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

    // 0.5초마다 큐 처리
    @Scheduled(fixedDelay = 500)
    @Transactional
    public void processTicketQueue() {
        String queueKey = "ticket_queue";

        // 큐에서 요청 하나 꺼냄 (FIFO)
        String queueMessage = redisTemplate.opsForList().leftPop(queueKey);

        if (queueMessage != null) {
            try {
                // 메시지 파싱 (concertId:seatId:memberId)
                String[] tokens = queueMessage.split(":");
                Long concertId = Long.parseLong(tokens[0]);
                Long seatId = Long.parseLong(tokens[1]);
                Long memberId = Long.parseLong(tokens[2]);

                log.info("== [Worker] 예매 요청 처리 시작 -> 회원: {}, 좌석: {} ==", memberId, seatId);

                // 엔티티 조회
                Member member = memberRepository.findById(memberId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
                Seat seat = seatRepository.findById(seatId)
                        .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

                // 좌석 상태 AVAILABLE -> RESERVED
                seat.reserve();

                // 예매 내역(Reservation) 저장
                Reservation reservation = new Reservation(member, seat);
                reservationRepository.save(reservation);

                log.info("== [Worker] 최종 예매 완료 및 영수증 발행 성공! 예매 ID: {} ==", reservation.getId());

            } catch (Exception e) {
                log.error("== [Worker] 예매 처리 중 에러 발생: {} ==", e.getMessage());
                // 실패 시 DLQ 적재나 락 해제 등 처리 가능
            }
        }
    }
}