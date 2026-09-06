package com.wayfarer.plan.dto;

/** 일정과 그것이 캐시에서 나왔는지 여부. 캐시 여부는 응답 헤더로만 나가고 본문은 건드리지 않는다. */
public record PlanResult(Itinerary itinerary, boolean cacheHit) {
}
