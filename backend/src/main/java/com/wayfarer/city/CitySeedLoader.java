package com.wayfarer.city;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ApplicationArguments;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 기동 시 도시 목록을 적재한다. 이미 있는 도시는 건드리지 않는다. */
@Slf4j
@Component
@Profile("!seed")
@RequiredArgsConstructor
public class CitySeedLoader implements ApplicationRunner {

    private static final String SEED_PATH = "seed/cities.json";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final CityRepository cityRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ClassPathResource resource = new ClassPathResource(SEED_PATH);
        if (!resource.exists()) {
            log.warn("도시 목록 파일이 없습니다 ({})", SEED_PATH);
            return;
        }

        List<Map<String, Object>> rows;
        try (InputStream in = resource.getInputStream()) {
            rows = OBJECT_MAPPER.readValue(in, new TypeReference<>() {
            });
        } catch (Exception e) {
            log.error("도시 목록을 읽지 못했습니다", e);
            return;
        }

        Set<String> existing = cityRepository.findAll().stream()
                .map(CityEntity::getNameKo)
                .collect(Collectors.toSet());

        List<CityEntity> toSave = rows.stream()
                .filter(row -> !existing.contains((String) row.get("nameKo")))
                .map(row -> new CityEntity(
                        (String) row.get("nameKo"),
                        (String) row.get("nameEn"),
                        (String) row.get("country"),
                        (String) row.get("chosung"),
                        ((Number) row.get("popularity")).intValue()))
                .toList();

        if (!toSave.isEmpty()) {
            cityRepository.saveAll(toSave);
        }
        log.info("도시 목록 적재 완료 - 신규 {}건 / 기존 {}건", toSave.size(), existing.size());
    }
}
