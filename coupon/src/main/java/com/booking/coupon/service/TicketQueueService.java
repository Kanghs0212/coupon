package com.booking.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TicketQueueService {

    private final StringRedisTemplate redisTemplate;

    /**
     * Redis를 이용해 특정 좌석을 초고속으로 선점합니다.
     */
    public void queueTicketRequest(Long concertId, Long seatId, Long memberId) {
        // 좌석별로 고유한 Redis Key를 생성합니다. (예: ticket:lock:concert:1:seat:5)
        String seatLockKey = "ticket:lock:concert:" + concertId + ":seat:" + seatId;
        String queueKey = "ticket_queue";

        // 1. Redis의 SETNX 연산을 통해 해당 좌석 키에 회원 ID를 등록 시도합니다.
        // 이 연산은 원자적(Atomic)이므로 여러 명이 동시에 찔러도 단 한 명만 true를 반환받습니다.
        Boolean isOccupied = redisTemplate.opsForValue().setIfAbsent(seatLockKey, memberId.toString(), Duration.ofMinutes(10));

        // 2. 만약 이미 선점된 좌석이라면 즉시 예외를 던져 사용자에게 알립니다. (DB 부하 0%)
        if (isOccupied == null || !isOccupied) {
            throw new IllegalArgumentException("이미 선택되었거나 예매 중인 좌석입니다.");
        }

        // 3. 선점에 성공한 행운의 유저의 정보(콘서트ID, 좌석ID, 회원ID)를 문자열로 결합하여 큐에 적재합니다.
        String queueMessage = concertId + ":" + seatId + ":" + memberId;
        redisTemplate.opsForList().rightPush(queueKey, queueMessage);
    }
}