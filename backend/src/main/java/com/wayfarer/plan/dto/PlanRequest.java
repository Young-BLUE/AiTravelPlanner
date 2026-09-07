package com.wayfarer.plan.dto;

import jakarta.validation.constraints.AssertTrue;
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

        /** 혼자 / 연인 / 친구 / 가족. 비우면 미지정 */
        String companion,

        /** 가성비 / 보통 / 프리미엄. 비우면 미지정 */
        String budget,

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
