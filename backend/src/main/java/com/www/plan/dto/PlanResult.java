package com.www.plan.dto;

/** 응답과 그것이 캐시에서 나왔는지 여부. 캐시 여부는 헤더로만 나가고 본문은 건드리지 않는다. */
public record PlanResult(PlanResponse response, boolean cacheHit) {
}
