package com.booking.coupon.controller;

import com.booking.coupon.domain.concert.ConcertRepository;
import com.booking.coupon.domain.seat.SeatRepository;
import com.booking.coupon.service.TicketQueueService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@CrossOrigin("*")
@RequiredArgsConstructor
public class TicketController {

    private final TicketQueueService ticketQueueService;
    private final StringRedisTemplate redisTemplate;

    // 🔥 동적 조회를 위해 Repository 추가
    private final ConcertRepository concertRepository;
    private final SeatRepository seatRepository;

    // 1. 전체 콘서트 목록 조회 API
    @GetMapping("/concerts")
    public ResponseEntity<List<ConcertDto>> getConcerts() {
        List<ConcertDto> response = concertRepository.findAll().stream()
                .map(c -> new ConcertDto(c.getId(), c.getTitle(), c.getPosterUrl(), c.getVenue()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 2. 특정 콘서트의 좌석 목록 조회 API
    @GetMapping("/concerts/{concertId}/seats")
    public ResponseEntity<List<SeatDto>> getSeats(@PathVariable("concertId") Long concertId) {
        List<SeatDto> response = seatRepository.findByConcertId(concertId).stream()
                .map(s -> new SeatDto(s.getId(), s.getSeatName(), s.getPrice(), s.getStatus().name()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    // 3. 기존 티켓 예매 API
    @PostMapping("/concerts/{concertId}/reserve")
    public ResponseEntity<String> reserveTicket(
            @PathVariable("concertId") Long concertId,
            @RequestParam("seatId") Long seatId,
            @RequestParam("memberId") Long memberId) {
        try {
            ticketQueueService.queueTicketRequest(concertId, seatId, memberId);
            return ResponseEntity.ok("좌석 선점 완료! 예매 결제가 진행됩니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
        }
    }

    // 4. Redis 초기화 API
    @PostMapping("/concerts/reset")
    public ResponseEntity<String> resetTicketData() {
        Set<String> keys = redisTemplate.keys("ticket:lock:*");
        if (keys != null && !keys.isEmpty()) redisTemplate.delete(keys);
        redisTemplate.delete("ticket_queue");
        return ResponseEntity.ok("Redis 초기화 완료!");
    }

    // --- DTO 클래스들 ---
    @Data
    public static class ConcertDto {
        private Long id; private String title; private String posterUrl; private String venue;
        public ConcertDto(Long id, String title, String posterUrl, String venue) {
            this.id = id; this.title = title; this.posterUrl = posterUrl; this.venue = venue;
        }
    }

    @Data
    public static class SeatDto {
        private Long id; private String seatName; private int price; private String status;
        public SeatDto(Long id, String seatName, int price, String status) {
            this.id = id; this.seatName = seatName; this.price = price; this.status = status;
        }
    }
}