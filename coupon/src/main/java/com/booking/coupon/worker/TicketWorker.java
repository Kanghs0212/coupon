package com.booking.coupon.worker;

import com.booking.coupon.service.TicketReservationProcessor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.RedisListCommands.Direction;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketWorker {

    private static final String QUEUE_KEY = "ticket_queue";
    private static final String PROCESSING_KEY = "ticket_processing"; // 처리 중 메시지 보관 (유실 방지)
    private static final String DLQ_KEY = "ticket_dlq";               // 처리 실패 메시지
    private static final int MAX_BATCH = 1000;                        // 한 틱에 처리할 최대 건수

    private final StringRedisTemplate redisTemplate;
    private final TicketReservationProcessor processor;

    // 앱 시작 시, 이전 실행에서 처리 중이던(중단된) 메시지를 큐로 되돌려 유실을 막는다.
    @PostConstruct
    public void recoverProcessing() {
        String message;
        while ((message = redisTemplate.opsForList().move(PROCESSING_KEY, Direction.LEFT, QUEUE_KEY, Direction.RIGHT)) != null) {
            log.warn("== [Worker] 미완료 메시지 복구: {} ==", message);
        }
    }

    @Scheduled(fixedDelayString = "${app.ticket.worker-delay-ms:500}")
    public void processTicketQueue() {
        for (int i = 0; i < MAX_BATCH; i++) {
            // 큐 head -> 처리중 리스트 tail 로 원자적 이동(LMOVE). 처리 완료(ack) 전까지 메시지를 보존한다.
            String message = redisTemplate.opsForList().move(QUEUE_KEY, Direction.LEFT, PROCESSING_KEY, Direction.RIGHT);
            if (message == null) break; // 큐가 비면 종료
            handle(message);
        }
    }

    private void handle(String message) {
        String[] tokens = message.split(":");
        Long concertId = Long.parseLong(tokens[0]);
        Long seatId = Long.parseLong(tokens[1]);
        Long memberId = Long.parseLong(tokens[2]);
        String seatLockKey = "ticket:lock:concert:" + concertId + ":seat:" + seatId;

        try {
            processor.hold(seatId, memberId);
            // 선점 단계에선 락 TTL을 유지한다 (결제 완료 시 영구화, 미결제 시 만료 스케줄러가 해제).
            log.info("== [Worker] 좌석 선점(결제 대기) -> 회원: {}, 좌석: {} ==", memberId, seatId);
        } catch (Exception e) {
            // 실패 시 좌석 락을 풀어 재선점이 가능하게 하고, 원인 분석을 위해 DLQ에 적재한다.
            redisTemplate.delete(seatLockKey);
            redisTemplate.opsForList().rightPush(DLQ_KEY, message);
            log.error("== [Worker] 예매 실패(DLQ 적재): {} / 원인: {} ==", message, e.getMessage());
        } finally {
            // 처리중 리스트에서 제거 (ack)
            redisTemplate.opsForList().remove(PROCESSING_KEY, 1, message);
        }
    }
}
