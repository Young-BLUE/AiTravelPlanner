package com.wayfarer.city;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "city",
        indexes = @Index(name = "ux_city_name_ko", columnList = "nameKo", unique = true))
public class CityEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 60)
    private String nameKo;

    @Column(nullable = false, length = 80)
    private String nameEn;

    @Column(nullable = false, length = 60)
    private String country;

    /** 한글 초성. "도쿄" -> "ㄷㅋ". 시드 적재 시 미리 계산해 LIKE 로 찾는다. */
    @Column(nullable = false, length = 60)
    private String chosung;

    /** 한국인 여행 수요 기준 정렬 가중치. 클수록 위에 노출된다. */
    private int popularity;

    /** 공항이 둘 이상인 도시만 채운다. 하나뿐이면 고르게 할 이유가 없다. */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "city_airport", joinColumns = @JoinColumn(name = "city_id"))
    @OrderColumn(name = "sort_order")
    private List<Airport> airports = new ArrayList<>();

    public CityEntity(String nameKo, String nameEn, String country, String chosung,
                      int popularity, List<Airport> airports) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.country = country;
        this.chosung = chosung;
        this.popularity = popularity;
        this.airports = airports == null ? new ArrayList<>() : new ArrayList<>(airports);
    }
}
