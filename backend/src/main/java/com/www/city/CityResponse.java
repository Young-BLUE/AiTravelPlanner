package com.www.city;

import java.util.List;

/** 자동완성 목록 한 줄. 공항이 둘 이상인 도시는 선택지를 함께 내려준다. */
public record CityResponse(String city, String country, String nameEn, List<AirportView> airports) {

    public record AirportView(String code, String name, String note) {
    }

    public static CityResponse from(CityEntity entity) {
        return new CityResponse(
                entity.getNameKo(),
                entity.getCountry(),
                entity.getNameEn(),
                entity.getAirports().stream()
                        .map(a -> new AirportView(a.getCode(), a.getNameKo(), a.getNote()))
                        .toList());
    }
}
