package com.booking.coupon.controller;

import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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

    // 내 예매 내역 조회
    // memberId는 파라미터가 아닌 로그인 토큰에서 추출 (타인 내역 조회 방지)
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {

        Long memberId = (Long) authentication.getDetails();

        // 회원의 예매 목록 조회
        List<Reservation> reservations = reservationRepository.findByMemberId(memberId);

        // 응답 DTO로 변환
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

    // 응답 DTO
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