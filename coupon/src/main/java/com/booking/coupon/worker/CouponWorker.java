package com.booking.coupon.worker;

import com.booking.coupon.domain.Coupon;
import com.booking.coupon.domain.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class CouponWorker {

    private final StringRedisTemplate redisTemplate;
    private final CouponRepository couponRepository;

    // 1초마다 큐 처리
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processQueue() {
        Long couponId = 1L; // 테스트용 쿠폰 ID
        String queueKey = "coupon_queue:" + couponId;

        // 대기열 크기 확인
        Long queueSize = redisTemplate.opsForList().size(queueKey);

        if (queueSize != null && queueSize > 0) {
            System.out.println("====== 백그라운드 워커: 큐에서 " + queueSize + "개의 요청을 발견했습니다. DB 반영 시작! ======");

            // 쿠폰 1회만 조회 (커넥션 절약)
            Coupon coupon = couponRepository.findById(couponId).orElseThrow();

            // 큐 개수만큼 메모리에서 차감
            for (int i = 0; i < queueSize; i++) {
                redisTemplate.opsForList().leftPop(queueKey); // 큐에서 제거
                coupon.decrease(); // 수량 차감
            }

            // 트랜잭션 커밋 시 Dirty Checking으로 UPDATE 1회만 실행
            System.out.println("====== 백그라운드 워커: DB 업데이트 완료. 현재 남은 수량: " + coupon.getQuantity() + " ======");
        }
    }
}