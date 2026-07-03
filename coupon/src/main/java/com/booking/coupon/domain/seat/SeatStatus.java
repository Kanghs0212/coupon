package com.booking.coupon.domain.seat;

public enum SeatStatus {
    AVAILABLE, // 예매 가능
    HELD,      // 선점됨 (결제 대기)
    RESERVED   // 예매 완료 (결제됨)
}
