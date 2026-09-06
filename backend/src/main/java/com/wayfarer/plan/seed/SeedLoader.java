package com.wayfarer.plan.seed;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.wayfarer.plan.cache.ItineraryCacheEntity;
import com.wayfarer.plan.cache.ItineraryCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;

/**
 * 기동 시 사전 생성된 일정을 캐시 테이블에 주입한다.
 *
 * DB를 새로 만들거나 새 환경에 배포해도 인기 조합은 처음부터 즉시 응답한다.
 * 없는 키만 넣는다 - 이미 있는 행을 덮어쓰면 운영 중 쌓인 hitCount 와
 * 더 최신일 수 있는 일정이 시드 버전으로 되돌아간다.
 */
@Slf4j
@Component
@Profile("!seed")   // 시드 생성 모드에서는 주입할 이유가 없다
@RequiredArgsConstructor
public class SeedLoader implements ApplicationRunner {

    private static final String SEED_PATH = "seed/itineraries.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ItineraryCacheRepository cacheRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ClassPathResource resource = new ClassPathResource(SEED_PATH);
        if (!resource.exists()) {
            log.info("시드 파일이 없어 주입을 건너뜁니다 ({})", SEED_PATH);
            return;
        }

        List<SeedEntry> entries;
        try (InputStream in = resource.getInputStream()) {
            entries = OBJECT_MAPPER.readValue(in, new TypeReference<>() {
            });
        } catch (Exception e) {
            // 시드가 깨졌다고 서버를 못 뜨게 할 이유는 없다. 캐시가 비어 있을 뿐이다
            log.error("시드 파일을 읽지 못해 주입을 건너뜁니다", e);
            return;
        }

        int inserted = 0;
        int skipped = 0;
        for (SeedEntry entry : entries) {
            if (cacheRepository.findByCacheKey(entry.cacheKey()).isPresent()) {
                skipped++;
                continue;
            }
            cacheRepository.save(ItineraryCacheEntity.fromSeed(entry));
            inserted++;
        }

        log.info("시드 주입 완료 - 신규 {}건 / 기존 유지 {}건 (전체 {}건)",
                inserted, skipped, entries.size());
    }
}
