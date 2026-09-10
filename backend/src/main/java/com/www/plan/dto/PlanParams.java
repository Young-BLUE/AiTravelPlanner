package com.www.plan.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * 요청을 정규화한 결과. 자유 입력이면 Haiku 가 채우고, 조건 선택이면 서버가 직접 만든다.
 * intent 가 PLAN 이면 destination 이하가 캐시 키 재료가 되고,
 * DISCOVER 면 목적지가 아직 없으므로 추천 조건으로만 쓰인다.
 */
public record PlanParams(
        @JsonPropertyDescription("""
                PLAN: 갈 곳을 이미 정했고 일정을 원한다. 예: '도쿄 3박4일', '오사카 여행 짜줘'
                DISCOVER: 어디로 갈지 고르는 중이다. 예: '겨울 여행지 추천', '100만원으로 갈 만한 곳'""")
        Intent intent,

        @JsonPropertyDescription("여행 도시명 (한국어). 예: 도쿄. 목적지를 정하지 않았으면 빈 문자열")
        String destination,

        @JsonPropertyDescription("숙박 일수. '3박 4일'이면 3. 언급이 없으면 3")
        int nights,

        @JsonPropertyDescription("""
                도착 공항이 문장에 드러나면 그 이름. 예: '나리타로 들어가서'면 '나리타'.
                언급이 없으면 빈 문자열""")
        String airport,

        @JsonPropertyDescription("1인 예산 (원화 정수). 언급이 없으면 0")
        int budgetKrw,

        @JsonPropertyDescription("혼자 / 연인 / 친구 / 가족 중 하나. 동행이 드러나지 않으면 '미지정'")
        String companion,

        @JsonPropertyDescription("""
                관심사. 관광/맛집/쇼핑/카페/야경/애니메이션/자연/테마파크 중에서만 고른다.
                드러나지 않으면 빈 목록""")
        List<String> interests,

        @JsonPropertyDescription("""
                여행 시기의 계절. 봄(3~5월) / 여름(6~8월) / 가을(9~11월) / 겨울(12~2월) 중 하나.
                '1월에', '벚꽃 시즌', '겨울 여행'처럼 시기가 드러나면 해당 계절, 드러나지 않으면 '미지정'""")
        String travelSeason,

        @JsonPropertyDescription("""
                DISCOVER 일 때 목적지를 고르는 기준. 아래 중 가장 가까운 하나만 고른다.
                겨울 / 봄 / 여름 / 가을 / 따뜻한곳 / 시원한곳 / 휴양 / 도시 / 자연 / 미식 / 무관.
                시기나 조건이 드러나지 않으면 '무관'. PLAN 이면 '무관'""")
        String discoveryTheme,

        @JsonPropertyDescription("""
                PLAN 일 때, 위 항목들로 표현되지 않는 구체적 요구가 있으면 true.
                예: '미술관 위주로', '차 없이 대중교통만', '비건 식당 위주'.
                단순히 도시·기간·예산·동행·관심사만 말했다면 false""")
        boolean hasExtraRequirements
) {

    public enum Intent {
        PLAN, DISCOVER
    }

    /** 목적지를 실제로 알아냈는지. 모델이 null 이나 공백을 줄 수 있어 여기서 막는다. */
    public boolean hasDestination() {
        return destination != null && !destination.isBlank();
    }

    /** intent 가 비어 있어도(구형 응답) 목적지 유무로 판단할 수 있게 한다. */
    public boolean isDiscovery() {
        return intent == Intent.DISCOVER || !hasDestination();
    }
}
