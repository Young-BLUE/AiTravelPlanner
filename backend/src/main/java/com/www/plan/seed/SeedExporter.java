package com.www.plan.seed;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.www.plan.cache.ItineraryCacheEntity;
import com.www.plan.cache.ItineraryCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;

/**
 * 운영 중 쌓인 캐시를 시드 파일로 내보낸다. 개발 시 1회성 작업이다.
 *
 *   ./gradlew bootRun -PskipFrontend --args='--spring.profiles.active=export'
 *
 * 실제 요청으로 만들어진 결과를 그대로 담으므로, 손으로 작성할 때와 달리
 * 운영 경로와 결과물이 어긋날 일이 없다.
 * H2 파일을 열어야 하므로 서버를 내린 뒤에 실행할 것.
 */
@Slf4j
@Component
@Profile("export")
@RequiredArgsConstructor
public class SeedExporter implements ApplicationRunner {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final ItineraryCacheRepository cacheRepository;

    @Value("${www.seed.output:src/main/resources/seed/itineraries.json}")
    private String outputPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<SeedEntry> entries = cacheRepository.findAll().stream()
                .map(e -> new SeedEntry(e.getCacheKey(), e.getDestination(), e.getAirport(),
                        e.getNights(), e.getBudgetBand(), e.getCompanion(),
                        e.getInterestKey(), e.getSeason(), e.getPayload()))
                .sorted(Comparator.comparing(SeedEntry::cacheKey))
                .toList();

        Path out = Path.of(outputPath);
        Files.createDirectories(out.getParent());
        Files.writeString(out, OBJECT_MAPPER.writerWithDefaultPrettyPrinter()
                .writeValueAsString(entries));

        log.info("시드 내보내기 완료 - {}건 -> {} ({} KB)",
                entries.size(), out.toAbsolutePath(), Files.size(out) / 1024);
        entries.forEach(e -> log.info("  {}", e.cacheKey()));
    }
}
