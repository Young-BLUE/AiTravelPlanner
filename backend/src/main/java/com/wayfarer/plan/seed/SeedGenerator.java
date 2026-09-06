package com.wayfarer.plan.seed;

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

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<SeedCombos.Combo> combos = SeedCombos.all();
        if (limit > 0 && limit < combos.size()) {
            combos = combos.subList(0, limit);
        }

        String season = CacheKey.from(
                new PlanParams("x", 0, 0, "일반", false), LocalDate.now()).season();
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
                        CacheKey key = CacheKey.from(
                                new PlanParams(combo.destination(), combo.nights(),
                                        combo.budgetKrw(), combo.style(), false),
                                LocalDate.now());
                        results.add(new SeedEntry(key.asString(), key.destination(), key.nights(),
                                key.budgetBand(), key.style(), key.season(),
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

        results.sort((a, b) -> a.cacheKey().compareTo(b.cacheKey()));

        Path out = Path.of(outputPath);
        Files.createDirectories(out.getParent());
        Files.writeString(out, OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(results));

        log.info("시드 생성 완료 - 성공 {}건 / 실패 {}건 -> {} ({} KB)",
                results.size(), failed.get(), out.toAbsolutePath(),
                Files.size(out) / 1024);
    }
}
