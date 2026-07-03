package com.booking.coupon.domain.seat;

import com.booking.coupon.domain.concert.Concert;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @PrimaryKeyJoinColumn
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "concert_id", nullable = false)
    private Concert concert; // 소속 콘서트

    @Column(nullable = false)
    private String seatName; // 예: "A열 1번"

    @Column(nullable = false)
    private int price; // 좌석 가격

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status; // AVAILABLE, HELD, RESERVED

    @Version
    private Long version; // 낙관적 락 버전

    // 시딩용 생성자 (상태는 AVAILABLE)
    public Seat(Concert concert, String seatName, int price) {
        this.concert = concert;
        this.seatName = seatName;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    // 선점: 예매 가능한 좌석만 결제 대기(HELD)로
    public void hold() {
        if (this.status != SeatStatus.AVAILABLE) {
            throw new IllegalStateException("이미 선택되었거나 예매된 좌석입니다.");
        }
        this.status = SeatStatus.HELD;
    }

    // 결제 확정: 선점(HELD) 상태만 예매 완료(RESERVED)로
    public void confirm() {
        if (this.status != SeatStatus.HELD) {
            throw new IllegalStateException("선점 상태가 아닌 좌석입니다.");
        }
        this.status = SeatStatus.RESERVED;
    }

    // 해제: 결제 미완료/취소 시 다시 예매 가능(AVAILABLE)하게
    public void release() {
        this.status = SeatStatus.AVAILABLE;
    }
}