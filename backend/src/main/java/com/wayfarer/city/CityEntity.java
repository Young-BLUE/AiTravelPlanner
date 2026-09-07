package com.wayfarer.city;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    public CityEntity(String nameKo, String nameEn, String country, String chosung, int popularity) {
        this.nameKo = nameKo;
        this.nameEn = nameEn;
        this.country = country;
        this.chosung = chosung;
        this.popularity = popularity;
    }
}
