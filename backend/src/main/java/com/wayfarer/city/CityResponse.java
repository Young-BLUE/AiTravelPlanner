package com.wayfarer.city;

/** 자동완성 목록 한 줄. */
public record CityResponse(String city, String country, String nameEn) {

    public static CityResponse from(CityEntity entity) {
        return new CityResponse(entity.getNameKo(), entity.getCountry(), entity.getNameEn());
    }
}
