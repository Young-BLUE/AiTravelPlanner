package com.wayfarer.plan;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.wayfarer.plan.dto.PlanParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 자유 문장에서 캐시 키 재료를 뽑는다.
 *
 * Haiku 를 쓰는 이유는 이 단계가 캐시 조회보다 먼저 오기 때문이다.
 * 캐시 히트여도 이 비용과 지연은 항상 발생하므로 가장 싸고 빠른 모델이어야 한다.
 * (실측 약 1초 / 건당 0.3원)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PromptExtractor {

    private static final String SYSTEM_PROMPT = """
            여행 요청 문장에서 일정 캐시 키에 쓸 항목만 뽑아낸다.
            일정을 만들지 말고, 문장에 드러난 정보만 정확히 추출한다.

            - destination: 도시명을 한국어로. '도쿄 여행'이면 '도쿄'
            - nights: 숙박 일수. '3박 4일'이면 3, '당일치기'면 0. 언급이 없으면 3
            - budgetKrw: 1인 예산을 원화 정수로. '80만원'이면 800000. 언급이 없으면 0
            - style: 배낭 / 가성비 / 일반 / 호캉스 / 가족 중 가장 가까운 하나.
              단서가 없으면 '일반', 예산만 언급하면 '가성비', 아이·부모 동반이면 '가족'
            - hasExtraRequirements: 위 4개로 표현되지 않는 요구가 있으면 true
            """;

    private final AnthropicClient anthropicClient;

    @Value("${wayfarer.claude.extract-model}")
    private String extractModel;

    public PlanParams extract(String userPrompt) {
        StructuredMessageCreateParams<PlanParams> params = MessageCreateParams.builder()
                .model(extractModel)
                .maxTokens(300L)
                .system(SYSTEM_PROMPT)
                .outputConfig(PlanParams.class)
                .addUserMessage(userPrompt)
                .build();

        PlanParams extracted = anthropicClient.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("프롬프트에서 여행 정보를 읽지 못했습니다."));

        log.debug("파라미터 추출 - {} / {}박 / {}원 / {} / 추가요구={}",
                extracted.destination(), extracted.nights(), extracted.budgetKrw(),
                extracted.style(), extracted.hasExtraRequirements());
        return extracted;
    }
}
