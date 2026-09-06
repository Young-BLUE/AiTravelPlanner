package com.wayfarer.plan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 랜딩 페이지 프롬프트 입력창에서 넘어오는 요청. */
public record PlanRequest(
        @NotBlank(message = "여행 계획을 입력해 주세요.")
        @Size(max = 500, message = "500자 이내로 입력해 주세요.")
        String prompt
) {
}
