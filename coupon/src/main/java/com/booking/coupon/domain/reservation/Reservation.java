package com.booking.coupon.domain.reservation;

import com.booking.coupon.domain.member.Member;
import com.booking.coupon.domain.seat.Seat;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member member; // 예매한 회원

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat; // 예매한 좌석 (좌석:예매 = 1:1)

    @Column(nullable = false)
    private LocalDateTime reservedAt; // 선점(예매 시작) 일시

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status; // PENDING, CONFIRMED

    @Column(nullable = false)
    private LocalDateTime expiresAt; // 결제 마감 시각 (이 시각 지나면 자동 해제)

    public Reservation(Member member, Seat seat, long holdMinutes) {
        this.member = member;
        this.seat = seat;
        this.reservedAt = LocalDateTime.now();
        this.status = ReservationStatus.PENDING;
        this.expiresAt = this.reservedAt.plusMinutes(holdMinutes);
    }

    // 결제 완료 처리
    public void confirm() {
        this.status = ReservationStatus.CONFIRMED;
    }
}