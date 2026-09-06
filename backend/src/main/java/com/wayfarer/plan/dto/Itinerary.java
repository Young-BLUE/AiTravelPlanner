package com.wayfarer.plan.dto;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;

import java.util.List;

/**
 * Claude가 생성하는 여행 일정. 이 record 트리에서 JSON 스키마가 자동 도출되므로
 * 필드를 바꾸면 모델 출력 형식도 함께 바뀐다.
 */
public record Itinerary(
        @JsonPropertyDescription("여행 도시명 (한국어). 예: 도쿄")
        String destination,

        @JsonPropertyDescription("국가명 (한국어). 예: 일본")
        String country,

        @JsonPropertyDescription("숙박 일수. 3박 4일이면 3")
        int nights,

        @JsonPropertyDescription("여행 스타일 요약. 예: 미식 중심 도심 여행")
        String style,

        @JsonPropertyDescription("일자별 일정. 길이는 nights + 1")
        List<DayPlan> days,

        @JsonPropertyDescription("1인 기준 예상 경비 (원화)")
        Budget budget,

        @JsonPropertyDescription("이 일정에 대한 실용적인 팁 2~4개")
        List<String> tips
) {

    public record DayPlan(
            @JsonPropertyDescription("여행 n일차. 1부터 시작")
            int day,

            @JsonPropertyDescription("그날의 테마 한 줄. 예: 시부야 도심 산책")
            String theme,

            @JsonPropertyDescription("그날 방문하는 장소들. 시간 순서대로 3~5개")
            List<Place> places,

            @JsonPropertyDescription("그날 예상 지출 (원화, 숙박·항공 제외)")
            int estCostKrw
    ) {
    }

    public record Place(
            @JsonPropertyDescription("장소명. 실제로 존재하는 곳만")
            String name,

            @JsonPropertyDescription("분류. 관광/식사/카페/쇼핑/휴식 중 하나")
            String category,

            @JsonPropertyDescription("방문 시각. HH:mm 형식")
            String time,

            @JsonPropertyDescription("머무는 시간 (분)")
            int durationMin,

            @JsonPropertyDescription("무엇을 하는 곳인지 1~2문장")
            String description,

            @JsonPropertyDescription("직전 장소에서 오는 방법과 소요시간. 첫 장소는 빈 문자열")
            String moveFromPrev
    ) {
    }

    public record Budget(
            @JsonPropertyDescription("왕복 항공권 예상액 (원화)")
            int flightKrw,

            @JsonPropertyDescription("전체 숙박비 예상액 (원화)")
            int stayKrw,

            @JsonPropertyDescription("전체 식비 예상액 (원화)")
            int foodKrw,

            @JsonPropertyDescription("입장료·교통 등 활동비 예상액 (원화)")
            int activityKrw,

            @JsonPropertyDescription("위 항목들의 합계 (원화)")
            int totalKrw
    ) {
    }
}
