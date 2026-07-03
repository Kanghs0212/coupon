package com.booking.coupon.controller;

import com.booking.coupon.domain.reservation.Reservation;
import com.booking.coupon.domain.reservation.ReservationRepository;
import com.booking.coupon.domain.reservation.ReservationStatus;
import com.booking.coupon.service.ReservationService;
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
    private final ReservationService reservationService;

    // 내 예매 내역(결제 완료분) 조회. memberId는 로그인 토큰에서 추출 (타인 내역 조회 방지)
    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> getMyReservations(Authentication authentication) {
        Long memberId = (Long) authentication.getDetails();
        List<ReservationResponse> response =
                reservationRepository.findByMemberIdAndStatus(memberId, ReservationStatus.CONFIRMED).stream()
                        .map(r -> new ReservationResponse(
                                r.getId(),
                                r.getSeat().getConcert().getTitle(),
                                r.getSeat().getSeatName(),
                                r.getSeat().getPrice(),
                                r.getReservedAt()))
                        .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 결제 대기 중인 선점 내역 조회 (결제 화면 카운트다운용)
    @GetMapping("/pending")
    public ResponseEntity<List<PendingResponse>> getPending(Authentication authentication) {
        Long memberId = (Long) authentication.getDetails();
        List<PendingResponse> response =
                reservationRepository.findByMemberIdAndStatus(memberId, ReservationStatus.PENDING).stream()
                        .map(r -> new PendingResponse(
                                r.getId(),
                                r.getSeat().getConcert().getId(),
                                r.getSeat().getConcert().getTitle(),
                                r.getSeat().getSeatName(),
                                r.getSeat().getPrice(),
                                r.getExpiresAt()))
                        .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 결제 처리
    @PostMapping("/{reservationId}/pay")
    public ResponseEntity<String> pay(@PathVariable("reservationId") Long reservationId,
                                      Authentication authentication) {
        try {
            Long memberId = (Long) authentication.getDetails();
            reservationService.pay(reservationId, memberId);
            return ResponseEntity.ok("결제가 완료되었습니다!");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 마이페이지 응답 DTO
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

    // 결제 대기 응답 DTO
    @Data
    public static class PendingResponse {
        private Long reservationId;
        private Long concertId;
        private String concertTitle;
        private String seatName;
        private int price;
        private LocalDateTime expiresAt;

        public PendingResponse(Long id, Long concertId, String title, String seat, int price, LocalDateTime expiresAt) {
            this.reservationId = id;
            this.concertId = concertId;
            this.concertTitle = title;
            this.seatName = seat;
            this.price = price;
            this.expiresAt = expiresAt;
        }
    }
}
