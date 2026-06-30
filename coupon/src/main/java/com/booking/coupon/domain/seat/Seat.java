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
    private SeatStatus status; // AVAILABLE, RESERVED

    @Version
    private Long version; // 낙관적 락 버전

    // 시딩용 생성자 (상태는 AVAILABLE)
    public Seat(Concert concert, String seatName, int price) {
        this.concert = concert;
        this.seatName = seatName;
        this.price = price;
        this.status = SeatStatus.AVAILABLE;
    }

    public void reserve() {
        if (this.status == SeatStatus.RESERVED) {
            throw new IllegalStateException("이미 예매 완료된 좌석입니다.");
        }
        this.status = SeatStatus.RESERVED;
    }

    // 상태 초기화 (테스트용)
    public void cancel() {
        this.status = SeatStatus.AVAILABLE;
    }
}