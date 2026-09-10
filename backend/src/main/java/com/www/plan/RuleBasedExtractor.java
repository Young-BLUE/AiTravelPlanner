package com.www.plan;

import com.www.city.CityEntity;
import com.www.city.CityRepository;
import com.www.plan.cache.CacheKey;
import com.www.plan.dto.PlanParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 모델 없이 문장에서 조건을 뽑는다. 데모 모드(API 키 없음)에서만 쓴다.
 *
 * Haiku 추출만큼 유연하지는 않지만, 캐시에 이미 있는 일정을 찾아 주는 데는 충분하다.
 * 도시명은 DB에 있는 164곳과 대조하므로 표기가 흔들릴 일이 없다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RuleBasedExtractor {

    private static final Pattern NIGHTS = Pattern.compile("(\\d+)\\s*박");
    private static final Pattern BUDGET_MAN = Pattern.compile("(\\d+)\\s*만\\s*원");
    private static final Pattern MONTH = Pattern.compile("(\\d{1,2})\\s*월");
    private static final List<String> SEASONS = List.of("봄", "여름", "가을", "겨울");

    private static final Map<String, String> COMPANION_WORDS = new LinkedHashMap<>(Map.of(
            "혼자", "혼자", "나홀로", "혼자",
            "연인", "연인", "커플", "연인", "여자친구", "연인", "남자친구", "연인",
            "친구", "친구",
            "가족", "가족", "아이", "가족"));

    private static final Map<String, String> INTEREST_WORDS = new LinkedHashMap<>();
    private static final Map<String, String> THEME_WORDS = new LinkedHashMap<>();

    static {
        INTEREST_WORDS.put("맛집", "맛집");
        INTEREST_WORDS.put("먹거리", "맛집");
        INTEREST_WORDS.put("미식", "맛집");
        INTEREST_WORDS.put("쇼핑", "쇼핑");
        INTEREST_WORDS.put("카페", "카페");
        INTEREST_WORDS.put("야경", "야경");
        INTEREST_WORDS.put("관광", "관광");
        INTEREST_WORDS.put("자연", "자연");
        INTEREST_WORDS.put("테마파크", "테마파크");
        INTEREST_WORDS.put("애니", "애니메이션");

        THEME_WORDS.put("겨울", "겨울");
        THEME_WORDS.put("봄", "봄");
        THEME_WORDS.put("여름", "여름");
        THEME_WORDS.put("가을", "가을");
        THEME_WORDS.put("따뜻", "따뜻한곳");
        THEME_WORDS.put("더운", "따뜻한곳");
        THEME_WORDS.put("시원", "시원한곳");
        THEME_WORDS.put("추운", "겨울");
        THEME_WORDS.put("휴양", "휴양");
        THEME_WORDS.put("자연", "자연");
        THEME_WORDS.put("미식", "미식");
    }

    private final CityRepository cityRepository;

    public PlanParams extract(String prompt) {
        String text = prompt == null ? "" : prompt.trim();

        String destination = findCity(text);
        PlanParams.Intent intent = destination.isBlank()
                ? PlanParams.Intent.DISCOVER
                : PlanParams.Intent.PLAN;

        PlanParams params = new PlanParams(
                intent,
                destination,
                findNights(text),
                "",
                findBudget(text),
                findFirst(text, COMPANION_WORDS, "미지정"),
                findInterests(text),
                findSeason(text),
                intent == PlanParams.Intent.DISCOVER
                        ? findFirst(text, THEME_WORDS, "무관")
                        : "무관",
                false);

        log.debug("규칙 기반 추출 - {} / {} / {}박 / {}원 / {} / 관심사{} / 시기={}",
                params.intent(),
                params.hasDestination() ? params.destination() : "(목적지 미정)",
                params.nights(), params.budgetKrw(), params.companion(), params.interests(),
                params.travelSeason());
        return params;
    }

    /** 월이 있으면 월로, 없으면 계절 단어로 판단한다. '월'이 붙은 숫자만 보므로 "3박"이 월로 잡히지 않는다. */
    private String findSeason(String text) {
        Matcher m = MONTH.matcher(text);
        while (m.find()) {
            String season = CacheKey.seasonOf(Integer.parseInt(m.group(1)));
            if (SEASONS.contains(season)) {
                return season;
            }
        }
        return SEASONS.stream().filter(text::contains).findFirst().orElse("미지정");
    }

    /** 가장 긴 도시명이 이긴다. "뉴욕"과 "욕"이 함께 걸리는 상황을 피하기 위함. */
    private String findCity(String text) {
        return cityRepository.findAll().stream()
                .map(CityEntity::getNameKo)
                .filter(text::contains)
                .max(Comparator.comparingInt(String::length))
                .orElse("");
    }

    private int findNights(String text) {
        if (text.contains("당일치기")) {
            return 0;
        }
        Matcher m = NIGHTS.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) : 3;
    }

    private int findBudget(String text) {
        Matcher m = BUDGET_MAN.matcher(text);
        return m.find() ? Integer.parseInt(m.group(1)) * 10_000 : 0;
    }

    private String findFirst(String text, Map<String, String> words, String fallback) {
        return words.entrySet().stream()
                .filter(e -> text.contains(e.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(fallback);
    }

    private List<String> findInterests(String text) {
        List<String> found = new ArrayList<>();
        INTEREST_WORDS.forEach((word, value) -> {
            if (text.contains(word) && !found.contains(value)) {
                found.add(value);
            }
        });
        return found;
    }
}
