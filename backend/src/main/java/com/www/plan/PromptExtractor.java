package com.www.plan;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.www.plan.dto.PlanParams;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
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
            여행 요청 문장을 분석해 필요한 항목만 뽑아낸다.
            일정이나 추천을 직접 만들지 말고, 문장에 드러난 정보만 정확히 추출한다.

            먼저 intent 를 판단한다.
            - PLAN: 갈 도시를 이미 정했다. '도쿄 3박4일', '오사카 여행 짜줘'
            - DISCOVER: 어디로 갈지 고르는 중이다. '겨울 여행지 추천해줘',
              '100만원으로 갈 만한 해외', '가족이랑 갈 만한 따뜻한 나라'
            도시명이 없으면 거의 항상 DISCOVER 다.

            - destination: 도시명을 한국어로. '도쿄 여행'이면 '도쿄'.
              DISCOVER 라 아직 정해지지 않았으면 빈 문자열. null 을 쓰지 않는다
            - nights: 숙박 일수. '3박 4일'이면 3, '당일치기'면 0. 언급이 없으면 3
            - budgetKrw: 1인 예산을 원화 정수로. '80만원'이면 800000. 언급이 없으면 0
            - companion: 혼자 / 연인 / 친구 / 가족 중 하나. 드러나지 않으면 '미지정'.
              이 목록에 없는 표현을 새로 만들지 않는다
            - interests: 관광 / 맛집 / 쇼핑 / 카페 / 야경 / 애니메이션 / 자연 / 테마파크
              중에서만 고른다. 문장에 드러난 것만 담고, 없으면 빈 목록
            - travelSeason: 여행 시기를 봄 / 여름 / 가을 / 겨울 / 미지정 중 하나로.
              '1월 도쿄'면 '겨울', '벚꽃 보러'면 '봄', '여름휴가'면 '여름'.
              시기가 드러나지 않으면 '미지정'. 오늘 날짜로 짐작하지 않는다
            - discoveryTheme: DISCOVER 일 때 목적지를 고르는 기준을 아래 목록에서 하나만 고른다.
              겨울 / 봄 / 여름 / 가을 / 따뜻한곳 / 시원한곳 / 휴양 / 도시 / 자연 / 미식 / 무관
              이 목록에 없는 표현을 새로 만들지 않는다. 캐시 키에 쓰이므로 표기가 흔들리면 안 된다.
              '겨울 여행지 추천'이면 '겨울', '따뜻한 나라'면 '따뜻한곳',
              '맛집 위주로 갈 만한 곳'이면 '미식'. 조건이 없으면 '무관'. PLAN 이면 '무관'
            - hasExtraRequirements: PLAN 일 때 위 항목들로 표현되지 않는 요구가 있으면 true
            """;

    /** 데모 모드에서는 빈이 없다. 그때는 규칙 기반 추출로 대체한다. */
    private final ObjectProvider<AnthropicClient> anthropicClient;
    private final RuleBasedExtractor ruleBasedExtractor;

    @Value("${www.claude.extract-model}")
    private String extractModel;

    public PlanParams extract(String userPrompt) {
        AnthropicClient client = anthropicClient.getIfAvailable();
        if (client == null) {
            return ruleBasedExtractor.extract(userPrompt);
        }

        StructuredMessageCreateParams<PlanParams> params = MessageCreateParams.builder()
                .model(extractModel)
                .maxTokens(300L)
                .system(SYSTEM_PROMPT)
                .outputConfig(PlanParams.class)
                .addUserMessage(userPrompt)
                .build();

        PlanParams extracted = client.messages().create(params).content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("프롬프트에서 여행 정보를 읽지 못했습니다."));

        log.debug("파라미터 추출 - {} / {} / {}박 / {}원 / {} / 관심사{} / 시기={} / 주제='{}' / 추가요구={}",
                extracted.intent(),
                extracted.hasDestination() ? extracted.destination() : "(목적지 미정)",
                extracted.nights(), extracted.budgetKrw(), extracted.companion(),
                extracted.interests(), extracted.travelSeason(), extracted.discoveryTheme(),
                extracted.hasExtraRequirements());
        return extracted;
    }
}
