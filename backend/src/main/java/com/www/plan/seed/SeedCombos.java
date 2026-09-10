package com.www.plan.seed;

import com.www.plan.dto.PlanRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * 사전 생성할 조합 목록.
 *
 * 한국인 단거리 여행은 3박 4일이 압도적이라 기간은 3박으로 고정한다.
 * 예산은 목적지 물가에 맞는 구간만 넣는다 (다낭에 250만원 조합을 만들 이유가 없다).
 *
 * 기본 조합은 여행 시기를 비워 둔다. 시기를 고르지 않은 요청이 가장 흔하고,
 * 계절을 가리지 않는 일정이라 계절이 바뀌어도 버릴 필요가 없다.
 * 계절 조합은 랜딩의 '시기별 추천' 카드가 요청하는 것만 만든다.
 */
public final class SeedCombos {

    /** 목적지와 그 지역에서 현실적인 예산 2개 (원). */
    private record Destination(String city, int budgetLow, int budgetHigh) {
    }

    // 두 예산은 서로 다른 50만원 구간에 떨어져야 한다.
    // 같은 구간이면(60만·90만) 키가 같아 한 건은 돈만 쓰고 버려진다 - SeedCombosTest 가 막는다
    private static final List<Destination> DESTINATIONS = List.of(
            // 일본 - 한국인 해외여행 1위 권역
            new Destination("도쿄", 800_000, 1_200_000),
            new Destination("오사카", 700_000, 1_100_000),
            new Destination("후쿠오카", 450_000, 900_000),
            new Destination("삿포로", 800_000, 1_200_000),
            new Destination("교토", 800_000, 1_200_000),
            // 동남아 - 가성비 장기 체류
            new Destination("다낭", 500_000, 800_000),
            new Destination("방콕", 450_000, 900_000),
            new Destination("나트랑", 500_000, 800_000),
            new Destination("세부", 700_000, 1_200_000),
            new Destination("보라카이", 700_000, 1_200_000),
            new Destination("발리", 900_000, 1_300_000),
            new Destination("코타키나발루", 700_000, 1_200_000),
            new Destination("하노이", 500_000, 800_000),
            new Destination("싱가포르", 1_000_000, 1_400_000),
            // 중화권
            new Destination("타이베이", 450_000, 900_000),
            new Destination("홍콩", 800_000, 1_200_000),
            new Destination("상하이", 600_000, 1_100_000),
            // 장거리
            new Destination("파리", 2_200_000, 2_700_000),
            new Destination("뉴욕", 2_400_000, 2_900_000),
            new Destination("로마", 2_200_000, 2_700_000));

    /**
     * 랜딩 '시기별 추천'과 같은 조합. 키는 계절로 뭉개므로 월은 프롬프트에만 쓰인다 -
     * 그 계절의 대표 풍경이 나오는 달로 고른다 (교토 가을은 단풍 절정인 11월).
     */
    private static final List<Combo> SEASONAL = List.of(
            new Combo("교토", 3, 0, 4),
            new Combo("워싱턴DC", 4, 0, 4),
            new Combo("발리", 4, 0, 7),
            new Combo("산토리니", 4, 0, 7),
            new Combo("교토", 3, 0, 11),
            new Combo("뉴욕", 4, 0, 10),
            new Combo("삿포로", 3, 0, 2),
            new Combo("헬싱키", 4, 0, 12));

    private static final int NIGHTS = 3;

    /**
     * 시드 한 건. 조건 선택 요청과 똑같은 형태로 만들어,
     * 운영 경로와 같은 프롬프트·같은 캐시 키를 거치게 한다.
     */
    public record Combo(String destination, int nights, int budgetKrw, Integer travelMonth) {

        public PlanRequest toRequest() {
            return new PlanRequest(null, destination, nights, travelMonth, null, null, null,
                    budgetKrw > 0 ? budgetKrw : null, List.of(), null, null);
        }

        @Override
        public String toString() {
            return "%s %d박%s%s".formatted(destination, nights,
                    budgetKrw > 0 ? " " + budgetKrw / 10_000 + "만원" : "",
                    travelMonth != null ? " " + travelMonth + "월" : "");
        }
    }

    public static List<Combo> all() {
        List<Combo> combos = new ArrayList<>();
        for (Destination d : DESTINATIONS) {
            // 예산 언급 없음 - 가장 흔한 입력 형태
            combos.add(new Combo(d.city(), NIGHTS, 0, null));
            for (int budget : List.of(d.budgetLow(), d.budgetHigh())) {
                combos.add(new Combo(d.city(), NIGHTS, budget, null));
            }
        }
        combos.addAll(SEASONAL);
        return combos;
    }

    private SeedCombos() {
    }
}
