package com.booking.coupon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class CouponAsyncService {

    private final StringRedisTemplate redisTemplate;
    private final long limit; // 선착순 발급 한도

    public CouponAsyncService(StringRedisTemplate redisTemplate,
                              @Value("${app.coupon.limit:10000}") long limit) {
        this.redisTemplate = redisTemplate;
        this.limit = limit;
    }

    public void issue(Long couponId) {
        String countKey = "coupon_count:" + couponId;
        String queueKey = "coupon_queue:" + couponId;

        // Redis 카운터 1 증가 (싱글 스레드라 원자적)
        Long count = redisTemplate.opsForValue().increment(countKey);

        // 수량 초과 시 예외 (DB 미접근)
        if (count != null && count > limit) {
            throw new IllegalArgumentException("선착순 마감되었습니다.");
        }

        // 대기열에 적재
        redisTemplate.opsForList().rightPush(queueKey, couponId.toString());
    }
}
