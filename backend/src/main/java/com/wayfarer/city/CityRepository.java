package com.wayfarer.city;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CityRepository extends JpaRepository<CityEntity, Long> {

    /**
     * 한글명·영문명·초성 어디로든 찾는다.
     * 입력으로 시작하는 도시를 먼저 보여준다. "도"를 쳤을 때 삿포로보다 도쿄가 위여야 한다.
     */
    @Query("""
            select c from CityEntity c
            where lower(c.nameKo) like :like
               or lower(c.nameEn) like :like
               or c.chosung like :like
            order by case when lower(c.nameKo) like :prefix
                            or lower(c.nameEn) like :prefix
                            or c.chosung like :prefix then 0 else 1 end,
                     c.popularity desc, c.nameKo asc
            """)
    List<CityEntity> search(@Param("like") String like,
                            @Param("prefix") String prefix,
                            Pageable pageable);

    List<CityEntity> findAllByOrderByPopularityDescNameKoAsc(Pageable pageable);
}
