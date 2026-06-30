package com.booking.coupon.controller;

import com.booking.coupon.service.WaitingQueueService;
import com.booking.coupon.service.WaitingQueueService.WaitingStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/concerts/{concertId}/waiting")
@CrossOrigin("*")
@RequiredArgsConstructor
public class WaitingQueueController {

    private final WaitingQueueService waitingQueueService;

    // 대기열 진입
    @PostMapping
    public ResponseEntity<WaitingStatus> enter(@PathVariable("concertId") Long concertId,
                                               Authentication authentication) {
        Long memberId = (Long) authentication.getDetails();
        return ResponseEntity.ok(waitingQueueService.enter(concertId, memberId));
    }

    // 내 대기 순번 조회 (폴링)
    @GetMapping
    public ResponseEntity<WaitingStatus> status(@PathVariable("concertId") Long concertId,
                                                Authentication authentication) {
        Long memberId = (Long) authentication.getDetails();
        return ResponseEntity.ok(waitingQueueService.status(concertId, memberId));
    }
}
