package com.booking.coupon.service;

import com.booking.coupon.domain.member.Member;
import com.booking.coupon.domain.member.MemberRepository;
import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import com.booking.coupon.domain.seat.Seat;
import com.booking.coupon.domain.seat.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketReservationProcessor {

    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;

    // 좌석 1건을 예매 확정한다. 호출마다 독립 트랜잭션이라 한 건의 실패가 다른 건에 영향을 주지 않는다.
    @Transactional
    public void reserve(Long seatId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        seat.reserve(); // AVAILABLE -> RESERVED (이미 RESERVED면 예외)
        reservationRepository.save(new Reservation(member, seat));
    }
}
