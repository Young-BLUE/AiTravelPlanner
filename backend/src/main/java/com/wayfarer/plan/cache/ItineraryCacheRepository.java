package com.wayfarer.plan.cache;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ItineraryCacheRepository extends JpaRepository<ItineraryCacheEntity, Long> {

    Optional<ItineraryCacheEntity> findByCacheKey(String cacheKey);
}
