package com.booking.coupon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class TicketQueueService {

    private final StringRedisTemplate redisTemplate;
    private final long lockTtlMinutes; // 좌석 선점 락 유지 시간

    public TicketQueueService(StringRedisTemplate redisTemplate,
                              @Value("${app.ticket.lock-ttl-minutes:10}") long lockTtlMinutes) {
        this.redisTemplate = redisTemplate;
        this.lockTtlMinutes = lockTtlMinutes;
    }

    // Redis로 좌석을 선점한다.
    public void queueTicketRequest(Long concertId, Long seatId, Long memberId) {
        // 좌석별 락 키 (예: ticket:lock:concert:1:seat:5)
        String seatLockKey = "ticket:lock:concert:" + concertId + ":seat:" + seatId;
        String queueKey = "ticket_queue";

        // SETNX로 좌석 선점 시도 (원자적이라 한 명만 성공)
        Boolean isOccupied = redisTemplate.opsForValue().setIfAbsent(seatLockKey, memberId.toString(), Duration.ofMinutes(lockTtlMinutes));

        // 이미 선점된 좌석이면 예외
        if (isOccupied == null || !isOccupied) {
            throw new IllegalArgumentException("이미 선택되었거나 예매 중인 좌석입니다.");
        }

        // 선점 성공 시 큐에 적재 (concertId:seatId:memberId)
        String queueMessage = concertId + ":" + seatId + ":" + memberId;
        redisTemplate.opsForList().rightPush(queueKey, queueMessage);
    }
}
