package com.booking.coupon.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CouponAsyncService {

    private final StringRedisTemplate redisTemplate;

    public void issue(Long couponId) {
        String countKey = "coupon_count:" + couponId;
        String queueKey = "coupon_queue:" + couponId;

        // Redis 카운터 1 증가 (싱글 스레드라 원자적)
        Long count = redisTemplate.opsForValue().increment(countKey);

        // 수량 초과 시 예외 (DB 미접근)
        if (count != null && count > 10000) {
            throw new IllegalArgumentException("선착순 마감되었습니다.");
        }

        // 대기열에 적재
        redisTemplate.opsForList().rightPush(queueKey, couponId.toString());
    }
}