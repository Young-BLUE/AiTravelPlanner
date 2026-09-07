package com.wayfarer.city;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 도시에 딸린 공항. 도착 공항에 따라 첫날·마지막날 동선이 달라진다. */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Airport {

    @Column(name = "airport_code", length = 8)
    private String code;

    @Column(name = "airport_name", length = 40)
    private String nameKo;

    /** 도심까지 걸리는 시간 등. 사용자 선택을 돕고 프롬프트에도 그대로 넘긴다. */
    @Column(name = "airport_note", length = 80)
    private String note;

    public Airport(String code, String nameKo, String note) {
        this.code = code;
        this.nameKo = nameKo;
        this.note = note;
    }
}
