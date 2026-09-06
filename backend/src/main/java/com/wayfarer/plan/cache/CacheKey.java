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

    public static CacheKey from(PlanParams params, LocalDate today) {
        return new CacheKey(
                params.destination().trim(),
                params.nights(),
                budgetBand(params.budgetKrw()),
                params.style(),
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

    /** 봄 교토와 겨울 교토는 다른 일정이어야 하므로 계절을 키에 넣는다. */
    private static String season(int month) {
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
