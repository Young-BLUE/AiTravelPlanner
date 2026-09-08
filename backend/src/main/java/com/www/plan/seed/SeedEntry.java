package com.www.plan.seed;

/** 시드 파일 한 줄. payload 는 Itinerary 를 직렬화한 JSON 문자열. */
public record SeedEntry(
        String cacheKey,
        String destination,
        String airport,
        int nights,
        String budgetBand,
        String companion,
        String interestKey,
        String season,
        String payload
) {
}
