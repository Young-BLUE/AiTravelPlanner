package com.wayfarer.plan.cache;

import com.wayfarer.plan.dto.PlanParams;

import java.time.LocalDate;

/**
 * 캐시 키 정규화.
 *
 * 프롬프트 원문을 해시하면 "도쿄 3박4일 여행"과 "도쿄로 3박4일 다녀오려고 해"가
 * 서로 다른 키가 되어 히트율이 사실상 0이 된다. 의미가 같은 요청을 한 칸에 모으는 것이
 * 이 클래스의 전부다.
 */
public record CacheKey(String destination, int nights, String budgetBand, String style, String season) {

    private static final int BAND_UNIT = 500_000;

    /**
     * 일정 캐시 키. 목적지가 없으면 만들 수 없다 - 목적지 없는 요청이 전부
     * "|3|미지정|일반|가을" 이라는 한 칸을 공유해서 서로 남의 답을 받게 된다.
     */
    public static CacheKey from(PlanParams params, LocalDate today) {
        if (!params.hasDestination()) {
            throw new IllegalArgumentException("목적지 없이는 일정 캐시 키를 만들 수 없습니다.");
        }
        return new CacheKey(
                params.destination().trim(),
                params.nights(),
                budgetBand(params.budgetKrw()),
                params.style(),
                season(today.getMonthValue()));
    }

    /**
     * 목적지 추천 캐시 키. 추천은 "무엇을 기준으로 고르는가"와 계절, 예산대만으로 갈린다.
     * destination 자리에 고정 문자열을 넣어 일정 키와 섞이지 않게 한다.
     */
    public static CacheKey forDiscovery(PlanParams params, LocalDate today) {
        // 모델이 목록 밖 표현을 내놓으면 키가 흩어져 캐시가 안 맞는다. 알려진 값만 통과시킨다
        String theme = normalizeTheme(params.discoveryTheme());
        return new CacheKey(
                "@추천",
                0,
                budgetBand(params.budgetKrw()),
                theme + "/" + params.style(),
                season(today.getMonthValue()));
    }

    /**
     * 예산은 50만원 구간으로 뭉갠다. 80만원과 85만원에 서로 다른 일정을 만들 이유가 없고,
     * 구간으로 묶어야 히트율이 올라간다.
     */
    private static String budgetBand(int budgetKrw) {
        if (budgetKrw <= 0) {
            return "미지정";
        }
        int band = (budgetKrw - 1) / BAND_UNIT;
        int lower = band * BAND_UNIT / 10_000;
        int upper = (band + 1) * BAND_UNIT / 10_000;
        return lower + "-" + upper + "만원";
    }

    private static final java.util.Set<String> KNOWN_THEMES = java.util.Set.of(
            "겨울", "봄", "여름", "가을", "따뜻한곳", "시원한곳",
            "휴양", "도시", "자연", "미식", "무관");

    private static String normalizeTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return "무관";
        }
        String trimmed = theme.trim();
        return KNOWN_THEMES.contains(trimmed) ? trimmed : "무관";
    }

    /** 봄 교토와 겨울 교토는 다른 일정이어야 하므로 계절을 키에 넣는다. */
    public static String season(int month) {
        return switch (month) {
            case 3, 4, 5 -> "봄";
            case 6, 7, 8 -> "여름";
            case 9, 10, 11 -> "가을";
            default -> "겨울";
        };
    }

    /** DB 유니크 컬럼에 그대로 넣는 값. 해시가 아니라 사람이 읽을 수 있게 둔다 (디버깅·통계용). */
    public String asString() {
        return String.join("|", destination, String.valueOf(nights), budgetBand, style, season);
    }
}
