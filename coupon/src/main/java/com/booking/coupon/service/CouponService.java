package com.booking.coupon.service;

import com.booking.coupon.domain.Coupon;
import com.booking.coupon.domain.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    @Transactional
    public void decrease(Long couponId) {
        // 🔥 일반 findById 대신 새로 만든 락(Lock) 쿼리 메서드로 변경합니다.
        Coupon coupon = couponRepository.findByIdWithPessimisticLock(couponId)
                .orElseThrow(() -> new IllegalArgumentException("쿠폰을 찾을 수 없습니다."));

        coupon.decrease();
    }
}