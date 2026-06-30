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
        // 쿠폰별 분산 락 키 생성
        RLock lock = redissonClient.getLock("coupon:" + couponId);

        try {
            // 락 획득 시도 (대기 10초, 점유 1초 후 자동 해제)
            boolean available = lock.tryLock(10, 1, TimeUnit.SECONDS);

            if (!available) {
                System.out.println("락 획득 실패 (동시 접속이 너무 많습니다.)");
                return;
            }

            // 락 획득 성공 시 쿠폰 차감 실행
            couponService.decrease(couponId);

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 락 해제
            lock.unlock();
        }
    }
}