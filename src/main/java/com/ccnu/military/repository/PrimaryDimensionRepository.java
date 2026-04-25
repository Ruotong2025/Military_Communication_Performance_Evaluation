package com.ccnu.military.repository;

import com.ccnu.military.entity.PrimaryDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PrimaryDimensionRepository extends JpaRepository<PrimaryDimension, Long> {

    @Query("SELECT p FROM PrimaryDimension p WHERE p.level.id = :levelId ORDER BY p.sortOrder")
    List<PrimaryDimension> findByLevelIdOrderBySortOrder(Long levelId);

    Optional<PrimaryDimension> findByLevelIdAndDimensionCode(Long levelId, String dimensionCode);

    List<PrimaryDimension> findByLevelId(Long levelId);
}
