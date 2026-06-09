package com.booking.coupon.controller;

import com.booking.coupon.service.TicketQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Set;

@RestController
@CrossOrigin("*") // 프론트엔드 연동을 위한 CORS 전체 허용 주문
@RequiredArgsConstructor
public class TicketController {

    private final TicketQueueService ticketQueueService;
    private final StringRedisTemplate redisTemplate;

    /**
     * 특정 콘서트의 특정 좌석을 예매 요청하는 API
     */
    @PostMapping("/concerts/{concertId}/reserve")
    public ResponseEntity<String> reserveTicket(
            @PathVariable("concertId") Long concertId,
            @RequestParam("seatId") Long seatId,
            @RequestParam("memberId") Long memberId) {
        try {
            // 초고속 Redis 대기열 서비스로 요청을 이관합니다.
            ticketQueueService.queueTicketRequest(concertId, seatId, memberId);
            return ResponseEntity.ok("좌석 선점 완료! 예매 결제가 진행됩니다.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("서버 내부 오류가 발생했습니다.");
        }
    }

    /**
     * 차후 테스트 및 화면 리셋을 위해 티켓팅 관련 Redis 락 데이터를 싹 청소하는 API
     */
    @GetMapping("/concerts/reset")
    public ResponseEntity<String> resetTicketData() {
        // ticket:lock으로 시작하는 모든 Redis 키를 찾아 삭제합니다.
        Set<String> keys = redisTemplate.keys("ticket:lock:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete("ticket_queue");
        return ResponseEntity.ok("티켓팅 관련 Redis 락 및 대기열 데이터 초기화 완료!");
    }
}