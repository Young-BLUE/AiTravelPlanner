package com.wayfarer.plan;

import com.wayfarer.plan.dto.Itinerary;
import com.wayfarer.plan.dto.PlanRequest;
import com.wayfarer.plan.dto.PlanResult;
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
    private static final String CACHE_HEADER = "X-Wayfarer-Cache";

    private final PlanService planService;

    @PostMapping("/plan")
    public ResponseEntity<Itinerary> plan(@Valid @RequestBody PlanRequest request) {
        PlanResult result = planService.generate(request.prompt());
        return ResponseEntity.ok()
                .header(CACHE_HEADER, result.cacheHit() ? "HIT" : "MISS")
                .body(result.itinerary());
    }
}
