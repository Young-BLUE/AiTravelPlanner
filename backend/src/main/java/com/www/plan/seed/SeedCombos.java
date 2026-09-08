package com.www.plan.seed;

import java.util.ArrayList;
import java.util.List;

/**
 * 사전 생성할 조합 목록.
 *
 * 한국인 단거리 여행은 3박 4일이 압도적이라 기간은 3박으로 고정한다.
 * 예산은 목적지 물가에 맞는 구간만 넣는다 (다낭에 250만원 조합을 만들 이유가 없다).
 *
 * 스타일은 PromptExtractor 가 실제로 뱉는 값에 맞춘다.
 * 예산 언급이 없으면 '일반', 있으면 '가성비' - 이게 어긋나면 시드가 영원히 안 맞는다.
 */
public final class SeedCombos {

    /** 목적지와 그 지역에서 현실적인 예산 2개 (원). */
    private record Destination(String city, int budgetLow, int budgetHigh) {
    }

    private static final List<Destination> DESTINATIONS = List.of(
            // 일본 - 한국인 해외여행 1위 권역
            new Destination("도쿄", 800_000, 1_200_000),
            new Destination("오사카", 700_000, 1_100_000),
            new Destination("후쿠오카", 600_000, 900_000),
            new Destination("삿포로", 800_000, 1_200_000),
            new Destination("교토", 800_000, 1_200_000),
            // 동남아 - 가성비 장기 체류
            new Destination("다낭", 500_000, 800_000),
            new Destination("방콕", 600_000, 900_000),
            new Destination("나트랑", 500_000, 800_000),
            new Destination("세부", 600_000, 900_000),
            new Destination("보라카이", 700_000, 1_000_000),
            new Destination("발리", 900_000, 1_300_000),
            new Destination("코타키나발루", 700_000, 1_000_000),
            new Destination("하노이", 500_000, 800_000),
            new Destination("싱가포르", 1_000_000, 1_400_000),
            // 중화권
            new Destination("타이베이", 600_000, 900_000),
            new Destination("홍콩", 800_000, 1_200_000),
            new Destination("상하이", 700_000, 1_000_000),
            // 장거리
            new Destination("파리", 2_200_000, 2_700_000),
            new Destination("뉴욕", 2_400_000, 2_900_000),
            new Destination("로마", 2_200_000, 2_700_000));

    private static final int NIGHTS = 3;

    /** 시드 한 건. prompt 로 생성하고, 나머지로 캐시 키를 만든다. */
    public record Combo(String destination, int nights, int budgetKrw, String style, String prompt) {
    }

    public static List<Combo> all() {
        List<Combo> combos = new ArrayList<>();
        for (Destination d : DESTINATIONS) {
            // 예산 언급 없음 - 가장 흔한 입력 형태
            combos.add(new Combo(d.city(), NIGHTS, 0, "일반",
                    "%s %d박 %d일 여행".formatted(d.city(), NIGHTS, NIGHTS + 1)));
            for (int budget : List.of(d.budgetLow(), d.budgetHigh())) {
                combos.add(new Combo(d.city(), NIGHTS, budget, "가성비",
                        "%s %d박 %d일 여행, 예산 %d만원"
                                .formatted(d.city(), NIGHTS, NIGHTS + 1, budget / 10_000)));
            }
        }
        return combos;
    }

    private SeedCombos() {
    }
}
