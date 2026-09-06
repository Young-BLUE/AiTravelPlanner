package com.wayfarer.plan.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

/** 자유 문장에서 뽑아낸 캐시 키 재료. 이 4개 축이 같으면 같은 일정으로 본다. */
public record PlanParams(
        @JsonPropertyDescription("여행 도시명 (한국어). 예: 도쿄")
        String destination,

        @JsonPropertyDescription("숙박 일수. '3박 4일'이면 3. 언급이 없으면 3")
        int nights,

        @JsonPropertyDescription("1인 예산 (원화 정수). 언급이 없으면 0")
        int budgetKrw,

        @JsonPropertyDescription("배낭 / 가성비 / 일반 / 호캉스 / 가족 중 하나")
        String style,

        @JsonPropertyDescription("""
                위 4개 항목으로 표현되지 않는 구체적 요구가 있으면 true.
                예: '미술관 위주로', '차 없이 대중교통만', '비건 식당 위주'.
                단순히 도시·기간·예산·동행만 말했다면 false""")
        boolean hasExtraRequirements
) {
}
