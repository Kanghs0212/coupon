package com.booking.coupon.service;

import com.booking.coupon.domain.Coupon;
import com.booking.coupon.domain.CouponRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
public class CouponServiceTest {

    @Autowired
    private CouponService couponService;

    @Autowired
    private CouponRepository couponRepository;

    @BeforeEach
    public void setUp() {
        // 테스트 시작 전 쿠폰 100개 저장
        couponRepository.save(new Coupon("오픈 기념 특가 쿠폰", 100));
    }

    @AfterEach
    public void tearDown() {
        // 테스트 종료 후 DB 초기화
        couponRepository.deleteAll();
    }

    @Test
    @DisplayName("Phase 0.5: 스프링 부트 환경과 H2 DB가 정상적으로 로드되고 저장된다.")
    public void contextLoadsAndDbWorks() {
        // given & when
        long count = couponRepository.count();
        Coupon savedCoupon = couponRepository.findAll().get(0);

        // then
        assertThat(count).isEqualTo(1);
        assertThat(savedCoupon.getName()).isEqualTo("오픈 기념 특가 쿠폰");
        assertThat(savedCoupon.getQuantity()).isEqualTo(100);

        System.out.println("✅ Phase 0.5 테스트 성공! 스프링과 DB가 완벽하게 연결되었습니다.");
    }

    @Test
    @DisplayName("Phase 1: 100명의 사용자가 동시에 쿠폰 발급을 요청하면 동시성 문제가 발생한다.")
    public void issueCouponConcurrently() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executorService = Executors.newFixedThreadPool(32);
        CountDownLatch latch = new CountDownLatch(threadCount);

        Coupon coupon = couponRepository.findAll().get(0);
        Long couponId = coupon.getId();

        for (int i = 0; i < threadCount; i++) {
            executorService.submit(() -> {
                try {
                    couponService.decrease(couponId);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();

        Coupon findCoupon = couponRepository.findById(couponId).orElseThrow();

        System.out.println("====== 최종 남은 쿠폰 수량: " + findCoupon.getQuantity() + " ======");
        assertThat(findCoupon.getQuantity()).isEqualTo(0);
    }
}