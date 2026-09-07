package com.wayfarer.city;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
@RequiredArgsConstructor
public class CityController {

    private static final int MAX_LIMIT = 20;

    private final CityRepository cityRepository;

    /**
     * 도시 자동완성. q 가 비어 있으면 인기 도시를 돌려줘
     * 입력 전에도 후보를 보여줄 수 있게 한다.
     */
    @GetMapping
    public List<CityResponse> search(@RequestParam(required = false) String q,
                                     @RequestParam(defaultValue = "8") int limit) {
        var page = PageRequest.of(0, Math.clamp(limit, 1, MAX_LIMIT));

        if (q == null || q.isBlank()) {
            return cityRepository.findAllByOrderByPopularityDescNameKoAsc(page).stream()
                    .map(CityResponse::from)
                    .toList();
        }

        String term = q.trim().toLowerCase();
        return cityRepository.search("%" + term + "%", term + "%", page).stream()
                .map(CityResponse::from)
                .toList();
    }
}
