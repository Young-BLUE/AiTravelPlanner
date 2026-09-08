package com.www.plan.cache;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "itinerary_cache",
        indexes = @Index(name = "ux_itinerary_cache_key", columnList = "cacheKey", unique = true))
public class ItineraryCacheEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 200)
    private String cacheKey;

    // 키를 쪼갠 컬럼들. 조회에는 안 쓰지만 "어떤 조합이 인기인지" 통계를 내려면 필요하다
    private String destination;
    private String airport;
    private int nights;
    private String budgetBand;
    private String companion;
    private String interestKey;
    private String season;

    /** Itinerary 를 직렬화한 JSON 원본. */
    @Lob
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    private int hitCount;
    private Instant createdAt;
    private Instant lastHitAt;

    public ItineraryCacheEntity(CacheKey key, String payload) {
        this.cacheKey = key.asString();
        this.destination = key.destination();
        this.airport = key.airport();
        this.nights = key.nights();
        this.budgetBand = key.budgetBand();
        this.companion = key.companion();
        this.interestKey = key.interestKey();
        this.season = key.season();
        this.payload = payload;
        this.hitCount = 0;
        this.createdAt = Instant.now();
    }

    /** 시드 파일에서 그대로 복원한다. 키는 이미 정규화된 상태로 저장돼 있다. */
    public static ItineraryCacheEntity fromSeed(com.www.plan.seed.SeedEntry entry) {
        ItineraryCacheEntity e = new ItineraryCacheEntity();
        e.cacheKey = entry.cacheKey();
        e.destination = entry.destination();
        e.airport = entry.airport();
        e.nights = entry.nights();
        e.budgetBand = entry.budgetBand();
        e.companion = entry.companion();
        e.interestKey = entry.interestKey();
        e.season = entry.season();
        e.payload = entry.payload();
        e.hitCount = 0;
        e.createdAt = Instant.now();
        return e;
    }

    public void recordHit() {
        this.hitCount++;
        this.lastHitAt = Instant.now();
    }

    public boolean isExpired(int ttlDays) {
        return createdAt.isBefore(Instant.now().minusSeconds((long) ttlDays * 86_400));
    }

    public void refresh(String newPayload) {
        this.payload = newPayload;
        this.createdAt = Instant.now();
        this.hitCount = 0;
        this.lastHitAt = null;
    }
}
