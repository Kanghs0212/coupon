package com.booking.coupon.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;

/**
 * 콘서트별 입장 대기열을 관리한다.
 * - 대기열: Redis Sorted Set (score = 도착 순서). 순번 = ZRANK + 1
 * - 통과자: Redis Set (이 집합에 든 회원만 좌석 선점 가능)
 */
@Service
public class WaitingQueueService {

    public record WaitingStatus(String status, long position, long total) {
        public static WaitingStatus admitted() { return new WaitingStatus("ADMITTED", 0, 0); }
        public static WaitingStatus waiting(long position, long total) { return new WaitingStatus("WAITING", position, total); }
    }

    private final StringRedisTemplate redis;
    private final Duration passedTtl; // 입장 권한 유지 시간

    public WaitingQueueService(StringRedisTemplate redis,
                               @Value("${app.waiting.passed-ttl-minutes:30}") long passedTtlMinutes) {
        this.redis = redis;
        this.passedTtl = Duration.ofMinutes(passedTtlMinutes);
    }

    private String queueKey(Long concertId) { return "waiting:queue:" + concertId; }
    private String passedKey(Long concertId) { return "waiting:passed:" + concertId; }
    private String seqKey(Long concertId) { return "waiting:seq:" + concertId; }

    // 대기열 진입 (이미 통과했거나 대기 중이면 현재 상태를 그대로 반환)
    public WaitingStatus enter(Long concertId, Long memberId) {
        String member = memberId.toString();
        if (isAdmitted(concertId, memberId)) {
            return WaitingStatus.admitted();
        }
        Long rank = redis.opsForZSet().rank(queueKey(concertId), member);
        if (rank == null) {
            // 처음 진입: 도착 순서(seq)를 점수로 부여
            Long seq = redis.opsForValue().increment(seqKey(concertId));
            redis.opsForZSet().add(queueKey(concertId), member, seq == null ? 0 : seq);
        }
        return status(concertId, memberId);
    }

    // 현재 대기 상태 조회 (폴링용)
    public WaitingStatus status(Long concertId, Long memberId) {
        if (isAdmitted(concertId, memberId)) {
            return WaitingStatus.admitted();
        }
        Long rank = redis.opsForZSet().rank(queueKey(concertId), memberId.toString());
        if (rank == null) {
            // 대기열에 없으면 입장 처리된 것으로 본다 (실제 예매 가능 여부는 passed 집합으로 최종 판정)
            return WaitingStatus.admitted();
        }
        Long total = redis.opsForZSet().size(queueKey(concertId));
        return WaitingStatus.waiting(rank + 1, total == null ? rank + 1 : total);
    }

    // 좌석 선점 가능 여부 (대기열 통과자만 true)
    public boolean isAdmitted(Long concertId, Long memberId) {
        return Boolean.TRUE.equals(redis.opsForSet().isMember(passedKey(concertId), memberId.toString()));
    }

    // 스케줄러가 호출: 앞에서부터 count명을 입장시킨다.
    public void admitFront(Long concertId, long count) {
        Set<ZSetOperations.TypedTuple<String>> popped = redis.opsForZSet().popMin(queueKey(concertId), count);
        if (popped == null || popped.isEmpty()) return;
        for (ZSetOperations.TypedTuple<String> tuple : popped) {
            if (tuple.getValue() != null) {
                redis.opsForSet().add(passedKey(concertId), tuple.getValue());
            }
        }
        redis.expire(passedKey(concertId), passedTtl);
    }
}
