package com.booking.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponAsyncService {

    // RedissonLock 대신 더 가볍고 빠른 기본 RedisTemplate을 사용합니다.
    private final StringRedisTemplate redisTemplate;

    public void issue(Long couponId) {
        String countKey = "coupon_count:" + couponId;
        String queueKey = "coupon_queue:" + couponId;

        // 1. Redis 카운터를 1 증가시킵니다. (Redis는 싱글 스레드라 이 동작 자체가 완벽히 스레드 세이프합니다!)
        Long count = redisTemplate.opsForValue().increment(countKey);

        // 2. 만약 10,000번을 넘어가면 즉시 예외를 던져 튕겨냅니다. (DB 접근조차 하지 않음)
        if (count != null && count > 10000) {
            throw new IllegalArgumentException("선착순 마감되었습니다.");
        }

        // 3. 10,000번 이내의 사람이라면, 대기열(List)에 쿠폰 ID를 쓱 밀어 넣고 로직을 끝냅니다.
        redisTemplate.opsForList().rightPush(queueKey, couponId.toString());
    }
}