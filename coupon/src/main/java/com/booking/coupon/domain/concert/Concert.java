package com.booking.coupon.domain.concert;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class Concert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title; // 콘서트 제목

    private String posterUrl; // 포스터 이미지 링크
    private String venue;     // 공연 장소

    public Concert(String title, String posterUrl, String venue) {
        this.title = title;
        this.posterUrl = posterUrl;
        this.venue = venue;
    }
}