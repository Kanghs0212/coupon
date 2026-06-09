package com.booking.coupon.controller;

import com.booking.coupon.service.CouponAsyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponAsyncService couponAsyncService;
    private final StringRedisTemplate redisTemplate;

    @PostMapping("/coupons/{couponId}/decrease")
    public ResponseEntity<String> decrease(@PathVariable("couponId") Long couponId) {
        try {
            // 이제 자물쇠(Facade)가 아닌, 초고속 비동기 대기열 서비스를 호출합니다!
            couponAsyncService.issue(couponId);
            return ResponseEntity.ok("쿠폰 발급 접수 완료");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body("쿠폰 소진: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 에러");
        }
    }

    // 💡 JMeter 테스트 전 Redis에 남아있는 찌꺼기를 청소하기 위한 초기화 API
    @GetMapping("/coupons/reset")
    public ResponseEntity<String> resetRedis() {
        redisTemplate.delete("coupon_count:1");
        redisTemplate.delete("coupon_queue:1");
        return ResponseEntity.ok("Redis 카운터 및 대기열 초기화 완료!");
    }
}