package com.www.plan.cache;

import com.www.plan.dto.PlanParams;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * 캐시 키 정규화.
 *
 * 프롬프트 원문을 해시하면 "도쿄 3박4일 여행"과 "도쿄로 3박4일 다녀오려고 해"가
 * 서로 다른 키가 되어 히트율이 사실상 0이 된다. 의미가 같은 요청을 한 칸에 모으는 것이
 * 이 클래스의 전부다.
 *
 * 축을 늘리면 결과는 정확해지지만 조합이 곱으로 늘어 히트율이 떨어진다.
 * HIT 는 0.3원이고 MISS 는 128원이라, 축 하나를 더하는 건 비용 결정이기도 하다.
 * 그래서 결과를 실제로 바꾸는 것(동행, 관심사, 여행 시기)만 넣고
 * 고유값이 많은 것(숙소 위치, 자유 텍스트)은 키에 넣지 않고 캐시를 우회시킨다.
 *
 * 키는 요청 내용만으로 정해진다. 오늘 날짜 같은 외부 상태가 섞이면
 * 같은 요청의 키가 날짜에 따라 바뀌어 시드와 캐시가 한꺼번에 무효가 된다.
 */
public record CacheKey(String destination, String airport, int nights, String budgetBand,
                       String companion, String interestKey, String season) {

    private static final int BAND_UNIT = 500_000;

    private static final String UNSPECIFIED = "미지정";

    private static final Set<String> KNOWN_SEASONS = Set.of("봄", "여름", "가을", "겨울");

    private static final Set<String> KNOWN_THEMES = Set.of(
            "겨울", "봄", "여름", "가을", "따뜻한곳", "시원한곳",
            "휴양", "도시", "자연", "미식", "무관");

    private static final Set<String> KNOWN_COMPANIONS = Set.of("혼자", "연인", "친구", "가족");

    public static final List<String> KNOWN_INTERESTS = List.of(
            "관광", "맛집", "쇼핑", "카페", "야경", "애니메이션", "자연", "테마파크");

    /**
     * 일정 캐시 키. 목적지가 없으면 만들 수 없다 - 목적지 없는 요청이 전부
     * 한 칸을 공유해서 서로 남의 답을 받게 된다.
     */
    public static CacheKey from(PlanParams params) {
        if (!params.hasDestination()) {
            throw new IllegalArgumentException("목적지 없이는 일정 캐시 키를 만들 수 없습니다.");
        }
        return new CacheKey(
                params.destination().trim(),
                airport(params.airport()),
                params.nights(),
                budgetBand(params.budgetKrw()),
                companion(params.companion()),
                interestKey(params.interests()),
                season(params.travelSeason()));
    }

    /**
     * 목적지 추천 캐시 키. 추천은 "무엇을 기준으로 고르는가"와 시기, 예산대만으로 갈린다.
     * destination 자리에 고정 문자열을 넣어 일정 키와 섞이지 않게 한다.
     */
    public static CacheKey forDiscovery(PlanParams params) {
        return new CacheKey(
                "@추천",
                UNSPECIFIED,
                0,
                budgetBand(params.budgetKrw()),
                companion(params.companion()),
                normalizeTheme(params.discoveryTheme()),
                season(params.travelSeason()));
    }

    /**
     * 예산은 50만원 구간으로 뭉갠다. 80만원과 85만원에 서로 다른 일정을 만들 이유가 없고,
     * 구간으로 묶어야 히트율이 올라간다.
     */
    private static String budgetBand(int budgetKrw) {
        if (budgetKrw <= 0) {
            return UNSPECIFIED;
        }
        int band = (budgetKrw - 1) / BAND_UNIT;
        int lower = band * BAND_UNIT / 10_000;
        int upper = (band + 1) * BAND_UNIT / 10_000;
        return lower + "-" + upper + "만원";
    }

    /**
     * 도착 공항. 나리타와 하네다는 도심까지 60분 차이라 첫날·마지막날 일정이 달라진다.
     * 공항이 하나뿐인 도시는 항상 미지정이라 조합이 늘지 않는다.
     */
    private static String airport(String airport) {
        return airport == null || airport.isBlank() ? UNSPECIFIED : airport.trim();
    }

    /** 가족 여행과 친구 여행은 장소 구성이 실제로 달라져서 키에 넣는다. */
    private static String companion(String companion) {
        if (companion == null || !KNOWN_COMPANIONS.contains(companion.trim())) {
            return UNSPECIFIED;
        }
        return companion.trim();
    }

    /**
     * 관심사는 정렬해 전부 키에 넣는다. 일부만 넣으면 "맛집+쇼핑"으로 만든 일정을
     * "맛집"만 고른 사람에게 주게 되어, 요청과 다른 결과를 캐시로 돌려주는 셈이 된다.
     * 대신 선택 개수를 UI 에서 제한해 조합 폭발을 막는다.
     */
    private static String interestKey(List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return UNSPECIFIED;
        }
        Set<String> sorted = new TreeSet<>();
        for (String interest : interests) {
            if (interest != null && KNOWN_INTERESTS.contains(interest.trim())) {
                sorted.add(interest.trim());
            }
        }
        return sorted.isEmpty() ? UNSPECIFIED : String.join("+", sorted);
    }

    /** 모델이 목록 밖 표현을 내놓으면 키가 흩어져 캐시가 안 맞는다. 알려진 값만 통과시킨다. */
    private static String normalizeTheme(String theme) {
        if (theme == null || theme.isBlank()) {
            return "무관";
        }
        String trimmed = theme.trim();
        return KNOWN_THEMES.contains(trimmed) ? trimmed : "무관";
    }

    /**
     * 여행 시기. 봄 교토와 겨울 교토는 다른 일정이어야 하므로 키에 넣는다.
     * 월 단위로 넣으면 조합이 12배가 되므로 예산처럼 계절로 뭉갠다.
     * 시기를 밝히지 않은 요청은 계절을 가리지 않는 일정을 받으므로 미지정 한 칸을 공유한다.
     */
    private static String season(String travelSeason) {
        if (travelSeason == null || !KNOWN_SEASONS.contains(travelSeason.trim())) {
            return UNSPECIFIED;
        }
        return travelSeason.trim();
    }

    /** 여행 월을 계절로 옮긴다. 월이 없거나 범위를 벗어나면 미지정. */
    public static String seasonOf(Integer month) {
        if (month == null) {
            return UNSPECIFIED;
        }
        return switch (month) {
            case 3, 4, 5 -> "봄";
            case 6, 7, 8 -> "여름";
            case 9, 10, 11 -> "가을";
            case 12, 1, 2 -> "겨울";
            default -> UNSPECIFIED;
        };
    }

    /** DB 유니크 컬럼에 그대로 넣는 값. 해시가 아니라 사람이 읽을 수 있게 둔다 (디버깅·통계용). */
    public String asString() {
        return String.join("|", destination, airport, String.valueOf(nights),
                budgetBand, companion, interestKey, season);
    }
}
