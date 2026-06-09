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

    // 1초(1000ms)마다 한 번씩 백그라운드에서 조용히 실행됩니다.
    @Scheduled(fixedDelay = 1000)
    @Transactional
    public void processQueue() {
        Long couponId = 1L; // 현재 테스트 중인 쿠폰 ID
        String queueKey = "coupon_queue:" + couponId;

        // 1. 대기열(List)에 쌓여있는 번호표의 개수를 확인합니다.
        Long queueSize = redisTemplate.opsForList().size(queueKey);

        if (queueSize != null && queueSize > 0) {
            System.out.println("====== 백그라운드 워커: 큐에서 " + queueSize + "개의 요청을 발견했습니다. DB 반영 시작! ======");

            // 2. DB에서 쿠폰을 딱 한 번만 조회합니다. (커넥션 풀 절약)
            Coupon coupon = couponRepository.findById(couponId).orElseThrow();

            // 3. 큐에 있는 개수만큼 메모리상에서 쿠폰을 차감합니다.
            for (int i = 0; i < queueSize; i++) {
                redisTemplate.opsForList().leftPop(queueKey); // 큐에서 번호표 빼기
                coupon.decrease(); // 수량 1 차감
            }

            // 트랜잭션(@Transactional)이 끝나는 이 시점에, JPA의 '변경 감지(Dirty Checking)' 덕분에
            // UPDATE 쿼리는 DB로 딱 **1번만** 날아갑니다! (핵심 최적화 포인트)
            System.out.println("====== 백그라운드 워커: DB 업데이트 완료. 현재 남은 수량: " + coupon.getQuantity() + " ======");
        }
    }
}