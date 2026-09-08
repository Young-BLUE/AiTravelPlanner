package com.www.plan;

import com.www.plan.dto.PlanRequest;
import com.www.plan.dto.PlanResponse;
import com.www.plan.dto.PlanResult;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PlanController {

    /** 캐시 적중 여부. 응답 본문(=LLM 스키마)을 건드리지 않으려고 헤더로 내보낸다. */
    private static final String CACHE_HEADER = "X-WWW-Cache";

    private final PlanService planService;

    /**
     * 목적지가 정해졌으면 일정을, 아직 고르는 중이면 목적지 추천을 돌려준다.
     * 어느 쪽인지는 응답의 type 필드로 구분한다.
     */
    @PostMapping("/plan")
    public ResponseEntity<PlanResponse> plan(@Valid @RequestBody PlanRequest request) {
        PlanResult result = planService.generate(request);
        return ResponseEntity.ok()
                .header(CACHE_HEADER, result.cacheHit() ? "HIT" : "MISS")
                .body(result.response());
    }
}
