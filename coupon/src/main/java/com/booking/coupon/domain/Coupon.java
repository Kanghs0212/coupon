package com.booking.coupon.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int quantity;

    public Coupon(String name, int quantity) {
        this.name = name;
        this.quantity = quantity;
    }

    // 쿠폰 수량 감소 로직
    public void decrease() {
        if (this.quantity <= 0) {
            throw new IllegalArgumentException("쿠폰이 모두 소진되었습니다.");
        }
        this.quantity -= 1;
    }
}