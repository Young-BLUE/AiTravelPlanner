package com.www.plan.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * 목적지 추천 결과. 사용자가 아직 갈 곳을 정하지 않았을 때 돌려준다.
 * 카드를 누르면 그 도시로 일정 생성이 이어지도록 planPrompt 를 함께 만든다.
 */
public record Suggestions(
        @JsonPropertyDescription("추천 기준을 한 줄로. 예: 겨울에 가기 좋은 여행지")
        String theme,

        @JsonPropertyDescription("추천 목적지 4~6곳. 성격이 겹치지 않게 고른다")
        List<Suggestion> destinations
) {

    public record Suggestion(
            @JsonPropertyDescription("도시명 (한국어). 예: 삿포로")
            String city,

            @JsonPropertyDescription("국가명 (한국어). 예: 일본")
            String country,

            @JsonPropertyDescription("왜 이 조건에 맞는지 1~2문장. 날씨나 시기 근거를 넣는다")
            String reason,

            @JsonPropertyDescription("가볼 만한 대표 장소 3곳")
            List<String> highlights,

            @JsonPropertyDescription("추천 숙박 일수. 보통 3")
            int nights,

            @JsonPropertyDescription("항공·숙박 포함 1인 예상 경비 (원화 정수)")
            int estBudgetKrw,

            @JsonPropertyDescription("가기 좋은 시기. 예: 12~2월")
            String bestSeason,

            @JsonPropertyDescription("""
                    이 도시를 골랐을 때 일정 생성에 쓸 문장.
                    반드시 '<도시> <숙박>박 <숙박+1>일 여행' 형식으로만 쓴다.
                    예: '삿포로 3박 4일 여행'""")
            String planPrompt
    ) {
    }
}
