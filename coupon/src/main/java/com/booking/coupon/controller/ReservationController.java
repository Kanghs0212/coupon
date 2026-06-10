package com.booking.coupon.controller;

import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/reservations")
@CrossOrigin("*")
@RequiredArgsConstructor
public class ReservationController {

    private final ReservationRepository reservationRepository;

    // 내 예매 내역을 조회하는 API
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(@RequestParam("memberId") Long memberId) {

        // 1. DB에서 해당 회원의 영수증(Reservation) 목록을 전부 가져옵니다.
        List<Reservation> reservations = reservationRepository.findByMemberId(memberId);

        // 2. 프론트엔드가 화면에 그리기 좋게 꼭 필요한 정보만 담은 DTO로 변환합니다.
        List<ReservationResponse> response = reservations.stream()
                .map(r -> new ReservationResponse(
                        r.getId(),
                        r.getSeat().getConcert().getTitle(),
                        r.getSeat().getSeatName(),
                        r.getSeat().getPrice(),
                        r.getReservedAt()
                ))
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    // --- 프론트엔드로 응답을 보낼 DTO 클래스 ---
    @Data
    public static class ReservationResponse {
        private Long reservationId;
        private String concertTitle;
        private String seatName;
        private int price;
        private LocalDateTime reservedAt;

        public ReservationResponse(Long id, String title, String seat, int price, LocalDateTime time) {
            this.reservationId = id;
            this.concertTitle = title;
            this.seatName = seat;
            this.price = price;
            this.reservedAt = time;
        }
    }
}