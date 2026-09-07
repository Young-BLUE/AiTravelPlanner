package com.wayfarer.plan;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.OutputConfig;
import com.anthropic.models.messages.StructuredMessageCreateParams;
import com.anthropic.models.messages.StructuredOutputConfig;
import com.anthropic.models.messages.TextBlockParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.plan.cache.CacheKey;
import com.wayfarer.plan.cache.ItineraryCacheEntity;
import com.wayfarer.plan.cache.ItineraryCacheRepository;
import com.wayfarer.plan.dto.Itinerary;
import com.wayfarer.plan.dto.PlanParams;
import com.wayfarer.plan.dto.PlanRequest;
import com.wayfarer.plan.dto.PlanResponse;
import com.wayfarer.plan.dto.PlanResult;
import com.wayfarer.plan.dto.Suggestions;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlanService {

    /**
     * 매 요청마다 동일하게 들어가는 고정 프롬프트.
     * 내용이 1바이트라도 바뀌면 프롬프트 캐시가 무효화되므로 함부로 문자열을 조립하지 말 것.
     */
    private static final String SYSTEM_PROMPT = """
            당신은 한국인 여행자를 위한 여행 일정 플래너입니다.
            사용자가 자유롭게 쓴 문장에서 목적지, 기간, 예산, 동행, 여행 스타일을 추론해
            바로 실행할 수 있는 일자별 일정을 만듭니다.

            원칙:
            - 실제로 존재하는 장소만 넣습니다. 확신이 없으면 그 지역의 검증된 대표 장소로 대체합니다.
            - 하루 일정은 지리적으로 가까운 곳끼리 묶어 동선 낭비가 없게 합니다.
            - 장소 간 이동 방법과 소요시간을 moveFromPrev에 구체적으로 적습니다. 첫 장소는 빈 문자열입니다.
            - 하루 3~5곳으로 현실적인 밀도를 유지하고, 식사와 휴식을 반드시 포함합니다.
            - 장소의 운영시간과 휴무일을 고려해 배치합니다. 정기 휴무나 야간 운영처럼
              방문 시각에 영향을 주는 조건은 description 에 함께 적습니다.
            - 마지막 날은 공항까지 가는 시간을 확보하고, 마지막 일정을 공항 도착으로 끝냅니다.
            - 모든 금액은 한국 원화 정수로 적습니다. 최근 시세 기준의 대략적인 추정치입니다.
            - 사용자가 기간을 밝히지 않았다면 3박 4일로 가정합니다.
            - 예산을 밝혔다면 그 범위 안에서 장소와 숙소 등급을 고릅니다.
            - 모든 텍스트는 한국어로 작성합니다.
            """;

    /**
     * 캐시 payload 직렬화용.
     *
     * Spring Boot 4가 등록하는 ObjectMapper는 Jackson 3(tools.jackson)인데,
     * DTO와 Anthropic SDK는 Jackson 2(com.fasterxml) 애노테이션을 쓴다.
     * 주입받으면 버전이 어긋나므로 여기서 직접 만든다.
     */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    /** 목적지 추천용 고정 프롬프트. 일정용과 마찬가지로 캐싱되므로 조립하지 말 것. */
    private static final String DISCOVER_PROMPT = """
            당신은 한국인 여행자에게 목적지를 추천하는 여행 상담사입니다.
            사용자가 아직 갈 곳을 정하지 못했을 때, 조건에 맞는 도시를 골라 줍니다.

            원칙:
            - 한국에서 직항이나 1회 경유로 갈 수 있는 도시를 우선합니다.
            - 4~6곳을 추천하되 성격이 겹치지 않게 고릅니다.
              (예: 눈 오는 곳만 다섯 곳이 아니라 설경, 온천, 따뜻한 휴양지를 섞습니다)
            - 왜 그 시기에 그곳이 좋은지 날씨나 행사 같은 근거를 답니다.
            - 예상 경비는 항공과 숙박을 포함한 1인 기준 원화 정수입니다.
            - 예산이 주어졌다면 그 범위 안의 도시만 고릅니다.
            - planPrompt 는 반드시 '<도시> <숙박>박 <숙박+1>일 여행' 형식으로만 씁니다.
            - 모든 텍스트는 한국어로 작성합니다.
            """;

    private final AnthropicClient anthropicClient;
    private final PromptExtractor promptExtractor;
    private final ItineraryCacheRepository cacheRepository;

    @Value("${wayfarer.claude.model}")
    private String model;

    @Value("${wayfarer.claude.max-tokens}")
    private long maxTokens;

    /** low / medium / high / xhigh / max. 낮출수록 빠르고 저렴하지만 일정 완성도가 떨어질 수 있다. */
    @Value("${wayfarer.claude.effort}")
    private String effort;

    @Value("${wayfarer.cache.ttl-days}")
    private int ttlDays;

    @Transactional
    public PlanResult generate(PlanRequest request) {
        // 조건을 직접 고른 요청은 추출할 게 없다. Haiku 호출을 건너뛰어
        // 캐시 히트여도 무조건 나가던 1초와 0.3원을 없앤다.
        PlanParams params = request.isStructured()
                ? fromStructured(request)
                : promptExtractor.extract(request.prompt());
        String userPrompt = request.isStructured()
                ? composePrompt(request)
                : request.prompt();

        // 갈 곳을 아직 안 정했으면 일정이 아니라 목적지 추천을 돌려준다.
        // 여기서 걸러내지 않으면 모델이 임의로 도시 하나를 골라 일정을 짜버린다.
        if (params.isDiscovery()) {
            return discover(userPrompt, params);
        }

        // 4개 축으로 표현되지 않는 요구("미술관 위주로")가 있으면 캐시를 건너뛴다.
        // 캐시 키가 그 요구를 담지 못하므로, 재사용하면 엉뚱한 일정을 주게 된다.
        if (params.hasExtraRequirements()) {
            log.debug("추가 요구가 있어 캐시를 사용하지 않음");
            return new PlanResult(PlanResponse.of(callClaude(userPrompt)), false);
        }

        CacheKey key = CacheKey.from(params, LocalDate.now());
        Optional<ItineraryCacheEntity> cached = cacheRepository.findByCacheKey(key.asString());

        if (cached.isPresent() && !cached.get().isExpired(ttlDays)) {
            ItineraryCacheEntity entity = cached.get();
            entity.recordHit();
            log.info("캐시 HIT [{}] - 누적 {}회", key.asString(), entity.getHitCount());
            return new PlanResult(
                    PlanResponse.of(deserialize(entity.getPayload(), Itinerary.class)), true);
        }

        log.info("캐시 MISS [{}] - 생성 시작", key.asString());
        Itinerary itinerary = callClaude(userPrompt);

        String payload = serialize(itinerary);
        cached.ifPresentOrElse(
                entity -> entity.refresh(payload),
                () -> cacheRepository.save(new ItineraryCacheEntity(key, payload)));

        return new PlanResult(PlanResponse.of(itinerary), false);
    }

    /** 선택한 조건을 그대로 파라미터로 옮긴다. 모델을 부르지 않으므로 표기가 흔들릴 일이 없다. */
    private PlanParams fromStructured(PlanRequest request) {
        // 숙소 위치와 자유 텍스트는 고유값이 많아 키에 넣으면 캐시가 무의미해진다.
        // 대신 있으면 캐시를 건너뛰어, 요청과 다른 결과를 돌려주는 일이 없게 한다.
        boolean hasExtra = notBlank(request.hotelArea()) || notBlank(request.extra());

        return new PlanParams(
                PlanParams.Intent.PLAN,
                request.destination().trim(),
                request.nights() == null || request.nights() <= 0 ? 3 : request.nights(),
                resolveBudget(request),
                request.companion(),
                request.interests() == null ? List.of() : request.interests(),
                "무관",
                hasExtra);
    }

    /** 직접 입력한 금액이 있으면 그걸 쓰고, 없으면 등급을 금액으로 옮긴다. */
    private int resolveBudget(PlanRequest request) {
        if (request.budgetKrw() != null && request.budgetKrw() > 0) {
            return request.budgetKrw();
        }
        return budgetTierToKrw(request.budget());
    }

    /**
     * 예산 등급을 대표 금액으로 옮긴다. 캐시 키는 어차피 50만원 구간으로 뭉개므로
     * 등급별로 구간이 갈리기만 하면 된다.
     */
    private int budgetTierToKrw(String tier) {
        if (tier == null) {
            return 0;
        }
        return switch (tier.trim()) {
            case "가성비" -> 400_000;
            case "보통" -> 900_000;
            case "프리미엄" -> 1_600_000;
            default -> 0;
        };
    }

    /** 선택한 조건을 모델이 읽을 문장으로 조립한다. 규칙 기반이라 호출 비용이 없다. */
    private String composePrompt(PlanRequest request) {
        StringBuilder sb = new StringBuilder();
        int nights = request.nights() == null || request.nights() <= 0 ? 3 : request.nights();
        sb.append(request.destination().trim())
                .append(' ').append(nights).append("박 ").append(nights + 1).append("일 여행");

        if (notBlank(request.companion())) {
            sb.append(", 동행: ").append(request.companion().trim());
        }
        if (request.budgetKrw() != null && request.budgetKrw() > 0) {
            sb.append(", 예산: 1인 ").append(request.budgetKrw() / 10_000).append("만원");
        } else if (notBlank(request.budget())) {
            sb.append(", 예산: ").append(request.budget().trim());
        }
        if (request.interests() != null && !request.interests().isEmpty()) {
            sb.append(", 관심사: ").append(String.join(", ", request.interests()));
        }
        if (notBlank(request.hotelArea())) {
            sb.append(", 숙소: ").append(request.hotelArea().trim()).append(" 인근");
        }
        if (notBlank(request.extra())) {
            sb.append("\n추가 요청: ").append(request.extra().trim());
        }
        return sb.toString();
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    /** 목적지 추천. 일정보다 출력이 짧아 더 싸고 빠르며, 조합이 적어 캐시가 잘 맞는다. */
    private PlanResult discover(String userPrompt, PlanParams params) {
        CacheKey key = CacheKey.forDiscovery(params, LocalDate.now());
        Optional<ItineraryCacheEntity> cached = cacheRepository.findByCacheKey(key.asString());

        if (cached.isPresent() && !cached.get().isExpired(ttlDays)) {
            ItineraryCacheEntity entity = cached.get();
            entity.recordHit();
            log.info("추천 캐시 HIT [{}] - 누적 {}회", key.asString(), entity.getHitCount());
            return new PlanResult(
                    PlanResponse.of(deserialize(entity.getPayload(), Suggestions.class)), true);
        }

        log.info("추천 캐시 MISS [{}] - 생성 시작", key.asString());
        Suggestions suggestions = callClaudeForSuggestions(userPrompt);

        String payload = serialize(suggestions);
        cached.ifPresentOrElse(
                entity -> entity.refresh(payload),
                () -> cacheRepository.save(new ItineraryCacheEntity(key, payload)));

        return new PlanResult(PlanResponse.of(suggestions), false);
    }

    private Suggestions callClaudeForSuggestions(String userPrompt) {
        StructuredMessageCreateParams<Suggestions> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(DISCOVER_PROMPT)
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()
                ))
                .outputConfig(StructuredOutputConfig.<Suggestions>builder()
                        .format(Suggestions.class)
                        .effort(OutputConfig.Effort.of(effort))
                        .build())
                .addUserMessage(userPrompt)
                .build();

        var message = anthropicClient.messages().create(params);

        log.debug("추천 생성 완료 (effort={}) - 입력 {} / 캐시읽기 {} / 출력 {} 토큰",
                effort,
                message.usage().inputTokens(),
                message.usage().cacheReadInputTokens().orElse(0L),
                message.usage().outputTokens());

        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("모델이 추천을 반환하지 않았습니다."));
    }

    /** 캐시를 거치지 않고 항상 새로 생성한다. 시드 생성기가 쓴다. */
    public Itinerary generateFresh(String userPrompt) {
        return callClaude(userPrompt);
    }

    private Itinerary callClaude(String userPrompt) {
        StructuredMessageCreateParams<Itinerary> params = MessageCreateParams.builder()
                .model(model)
                .maxTokens(maxTokens)
                .systemOfTextBlockParams(List.of(
                        TextBlockParam.builder()
                                .text(SYSTEM_PROMPT)
                                // 고정 프리픽스를 캐싱해 반복 요청의 입력 비용을 줄인다
                                .cacheControl(CacheControlEphemeral.builder().build())
                                .build()
                ))
                .outputConfig(StructuredOutputConfig.<Itinerary>builder()
                        .format(Itinerary.class)
                        .effort(OutputConfig.Effort.of(effort))
                        .build())
                .addUserMessage(userPrompt)
                .build();

        var message = anthropicClient.messages().create(params);

        log.debug("일정 생성 완료 (effort={}) - 입력 {} / 캐시쓰기 {} / 캐시읽기 {} / 출력 {} 토큰",
                effort,
                message.usage().inputTokens(),
                message.usage().cacheCreationInputTokens().orElse(0L),
                message.usage().cacheReadInputTokens().orElse(0L),
                message.usage().outputTokens());

        return message.content().stream()
                .flatMap(block -> block.text().stream())
                .map(text -> text.text())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("모델이 일정을 반환하지 않았습니다."));
    }

    private String serialize(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("결과를 저장용으로 변환하지 못했습니다.", e);
        }
    }

    private <T> T deserialize(String payload, Class<T> type) {
        try {
            return OBJECT_MAPPER.readValue(payload, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("저장된 결과를 읽지 못했습니다.", e);
        }
    }
}
