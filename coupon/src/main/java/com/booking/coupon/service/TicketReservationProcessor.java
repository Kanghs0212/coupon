package com.booking.coupon.service;

import com.booking.coupon.domain.member.Member;
import com.booking.coupon.domain.member.MemberRepository;
import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import com.booking.coupon.domain.seat.Seat;
import com.booking.coupon.domain.seat.SeatRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketReservationProcessor {

    private final MemberRepository memberRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final long holdMinutes; // 결제 대기(선점 유지) 시간

    public TicketReservationProcessor(MemberRepository memberRepository,
                                      SeatRepository seatRepository,
                                      ReservationRepository reservationRepository,
                                      @Value("${app.ticket.hold-minutes:5}") long holdMinutes) {
        this.memberRepository = memberRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.holdMinutes = holdMinutes;
    }

    // 좌석 1건을 선점(HELD)하고 결제 대기 예매(PENDING)를 생성한다.
    // 호출마다 독립 트랜잭션이라 한 건의 실패가 다른 건에 영향을 주지 않는다.
    @Transactional
    public void hold(Long seatId, Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 좌석입니다."));

        seat.hold(); // AVAILABLE -> HELD (이미 선점/예매된 좌석이면 예외)
        reservationRepository.save(new Reservation(member, seat, holdMinutes));
    }
}
