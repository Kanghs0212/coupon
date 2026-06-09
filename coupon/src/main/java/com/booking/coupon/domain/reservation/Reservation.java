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
    private Seat seat; // 예매한 좌석 (1개 좌석은 1개의 예매 내역만 가짐)

    @Column(nullable = false)
    private LocalDateTime reservedAt; // 예매 일시

    public Reservation(Member member, Seat seat) {
        this.member = member;
        this.seat = seat;
        this.reservedAt = LocalDateTime.now();
    }
}