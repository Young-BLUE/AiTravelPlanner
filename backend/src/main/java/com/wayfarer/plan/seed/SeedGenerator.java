package com.wayfarer.plan.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.plan.PlanService;
import com.wayfarer.plan.cache.CacheKey;
import com.wayfarer.plan.dto.Itinerary;
import com.wayfarer.plan.dto.PlanParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 인기 조합 일정을 미리 생성해 시드 파일로 떨군다. 개발 시 1회성 작업이다.
 *
 *   ./gradlew bootRun -PskipFrontend --args='--spring.profiles.active=seed'
 *
 * 생성된 파일을 src/main/resources/seed/ 에 커밋하면 이후 모든 배포가 따뜻하게 시작한다.
 * 유료 호출이 조합 수만큼 발생하므로 --wayfarer.seed.limit 으로 먼저 소량 확인할 것.
 */
@Slf4j
@Component
@Profile("seed")
@RequiredArgsConstructor
public class SeedGenerator implements ApplicationRunner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final PlanService planService;

    @Value("${wayfarer.seed.output:src/main/resources/seed/itineraries.json}")
    private String outputPath;

    /** 0이면 전체. 소량으로 먼저 확인할 때 쓴다. */
    @Value("${wayfarer.seed.limit:0}")
    private int limit;

    /** 동시 실행 수. 너무 올리면 429가 난다. */
    @Value("${wayfarer.seed.concurrency:4}")
    private int concurrency;

    private static CacheKey keyOf(SeedCombos.Combo combo) {
        return CacheKey.from(new PlanParams(PlanParams.Intent.PLAN, combo.destination(),
                combo.nights(), combo.budgetKrw(), "미지정", java.util.List.of(),
                "무관", false), LocalDate.now());
    }

    /** 기존 시드를 읽는다. 없거나 깨졌으면 빈 목록으로 시작한다. */
    private List<SeedEntry> readExisting(Path path) {
        if (!Files.exists(path)) {
            return new ArrayList<>();
        }
        try {
            return new ArrayList<>(OBJECT_MAPPER.readValue(Files.readString(path),
                    new TypeReference<List<SeedEntry>>() {
                    }));
        } catch (Exception e) {
            log.warn("기존 시드를 읽지 못해 처음부터 생성합니다: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        Path out = Path.of(outputPath);

        // 이미 만들어 둔 항목은 다시 만들지 않는다.
        // 60건을 40분간 돌리는 작업이라 중간에 죽었을 때 처음부터 다시 하면 그대로 돈이다.
        List<SeedEntry> existing = readExisting(out);
        Set<String> existingKeys = existing.stream()
                .map(SeedEntry::cacheKey)
                .collect(java.util.stream.Collectors.toSet());

        List<SeedCombos.Combo> combos = new ArrayList<>();
        for (SeedCombos.Combo combo : SeedCombos.all()) {
            CacheKey key = keyOf(combo);
            if (!existingKeys.contains(key.asString())) {
                combos.add(combo);
            }
        }
        if (limit > 0 && limit < combos.size()) {
            combos = combos.subList(0, limit);
        }
        if (combos.isEmpty()) {
            log.info("이미 {}건이 모두 생성돼 있어 할 일이 없습니다", existing.size());
            return;
        }
        log.info("기존 {}건 유지, 신규 {}건 생성", existing.size(), combos.size());

        String season = CacheKey.season(LocalDate.now().getMonthValue());
        log.info("시드 생성 시작 - {}건 / 동시 {} / 계절 {}", combos.size(), concurrency, season);

        List<SeedEntry> results = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger done = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        int total = combos.size();

        try (ExecutorService pool = Executors.newFixedThreadPool(concurrency)) {
            List<Future<?>> futures = new ArrayList<>();
            for (SeedCombos.Combo combo : combos) {
                futures.add(pool.submit((Callable<Void>) () -> {
                    try {
                        Itinerary itinerary = planService.generateFresh(combo.prompt());
                        CacheKey key = keyOf(combo);
                        results.add(new SeedEntry(key.asString(), key.destination(), key.nights(),
                                key.budgetBand(), key.companion(), key.interestKey(), key.season(),
                                OBJECT_MAPPER.writeValueAsString(itinerary)));
                        log.info("[{}/{}] 완료 - {}", done.incrementAndGet(), total, key.asString());
                    } catch (Exception e) {
                        // 한 건 실패로 전체를 버리지 않는다. 실패분은 다시 돌리면 된다
                        failed.incrementAndGet();
                        log.error("[{}/{}] 실패 - {} : {}",
                                done.incrementAndGet(), total, combo.prompt(), e.getMessage());
                    }
                    return null;
                }));
            }
            for (Future<?> f : futures) {
                f.get();
            }
        }

        results.addAll(existing);
        results.sort((a, b) -> a.cacheKey().compareTo(b.cacheKey()));

        Files.createDirectories(out.getParent());
        Files.writeString(out, OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results));

        log.info("시드 생성 완료 - 성공 {}건 / 실패 {}건 -> {} ({} KB)",
                results.size(), failed.get(), out.toAbsolutePath(),
                Files.size(out) / 1024);
    }
}
