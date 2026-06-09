package com.booking.coupon.facade;

import com.booking.coupon.service.CouponService;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RedissonLockCouponFacade {

    private final RedissonClient redissonClient;
    private final CouponService couponService;

    public void decrease(Long couponId) {
        // 1. 쿠폰 ID를 기반으로 Redis에 고유한 자물쇠(Key)를 생성합니다.
        RLock lock = redissonClient.getLock("coupon:" + couponId);

        try {
            // 2. 자물쇠 획득 시도 (최대 10초까지 기다리고, 획득하면 1초 뒤에 자동 해제)
            // Redisson은 Pub/Sub 방식이라 Redis에 부하를 주지 않고 우아하게 대기합니다.
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("락 획득 실패 (동시 접속이 너무 많습니다.)");
                return;
            }

            // 3. 자물쇠를 획득했다면, 실제 쿠폰 차감 비즈니스 로직(트랜잭션)을 실행합니다.
            couponService.decrease(couponId);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 4. 로직이 모두 끝나면(DB 커밋 완료) 자물쇠를 풀어줍니다.
            lock.unlock();
        }
    }
}