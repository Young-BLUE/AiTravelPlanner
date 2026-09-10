package com.www.plan.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * 일정/추천 요청.
 *
 * 두 가지 경로를 하나의 엔드포인트로 받는다.
 * - 자유 입력: prompt 만 채워서 보낸다. 서버가 Haiku 로 의도와 조건을 추출한다.
 * - 조건 선택: destination 을 채워서 보낸다. 추출 호출이 필요 없어 1초와 0.3원을 아낀다.
 */
public record PlanRequest(
        @Size(max = 500, message = "500자 이내로 입력해 주세요.")
        String prompt,

        @Size(max = 40, message = "목적지가 너무 깁니다.")
        String destination,

        Integer nights,

        /** 여행 월 (1~12). 비우면 계절을 가리지 않는 일정을 만든다 */
        @Min(value = 1, message = "여행 월은 1~12 사이여야 합니다.")
        @Max(value = 12, message = "여행 월은 1~12 사이여야 합니다.")
        Integer travelMonth,

        /** 도착 공항 이름. 공항이 둘 이상인 도시에서만 의미가 있다 (나리타 / 하네다) */
        @Size(max = 30, message = "공항명이 너무 깁니다.")
        String airport,

        /** 혼자 / 연인 / 친구 / 가족. 비우면 미지정 */
        String companion,

        /** 가성비 / 보통 / 프리미엄. 비우면 미지정 */
        String budget,

        /** 직접 입력한 1인 예산 (원). 값이 있으면 budget 등급보다 우선한다 */
        @Min(value = 0, message = "예산은 0원 이상이어야 합니다.")
        @Max(value = 100_000_000, message = "예산이 너무 큽니다.")
        Integer budgetKrw,

        /** 관광 / 맛집 / 쇼핑 / 카페 / 야경 / 애니메이션 / 자연 / 테마파크 */
        List<String> interests,

        @Size(max = 40, message = "숙소 위치가 너무 깁니다.")
        String hotelArea,

        /** 조건으로 표현되지 않는 요청. "디즈니랜드는 꼭", "걷는 거 싫음" 등 */
        @Size(max = 300, message = "추가 요청은 300자 이내로 입력해 주세요.")
        String extra
) {

    public boolean isStructured() {
        return destination != null && !destination.isBlank();
    }

    @AssertTrue(message = "여행 계획을 입력하거나 목적지를 선택해 주세요.")
    public boolean isFilled() {
        return isStructured() || (prompt != null && !prompt.isBlank());
    }
}
