package com.booking.coupon.worker;

import com.booking.coupon.service.WaitingQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@RequiredArgsConstructor
public class WaitingQueueAdmissionWorker {

    private static final String QUEUE_PREFIX = "waiting:queue:";

    private final StringRedisTemplate redisTemplate;
    private final WaitingQueueService waitingQueueService;

    @Value("${app.waiting.admit-per-tick:5}")
    private long admitPerTick; // 한 틱에 입장시킬 인원

    // 일정 주기로 각 콘서트 대기열의 앞쪽 인원을 입장시킨다.
    @Scheduled(fixedDelayString = "${app.waiting.tick-ms:2000}")
    public void admit() {
        Set<String> queueKeys = redisTemplate.keys(QUEUE_PREFIX + "*");
        if (queueKeys == null) return;
        for (String key : queueKeys) {
            Long concertId = Long.parseLong(key.substring(QUEUE_PREFIX.length()));
            waitingQueueService.admitFront(concertId, admitPerTick);
        }
    }
}
