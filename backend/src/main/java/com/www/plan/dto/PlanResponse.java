package com.www.plan.dto;

/**
 * /api/plan 의 단일 응답 형태.
 * 프론트는 type 을 보고 일정 화면과 추천 화면을 나눠 그린다.
 */
public record PlanResponse(
        Type type,
        Itinerary itinerary,
        Suggestions suggestions
) {

    public enum Type {
        ITINERARY, SUGGESTIONS
    }

    public static PlanResponse of(Itinerary itinerary) {
        return new PlanResponse(Type.ITINERARY, itinerary, null);
    }

    public static PlanResponse of(Suggestions suggestions) {
        return new PlanResponse(Type.SUGGESTIONS, null, suggestions);
    }
}
