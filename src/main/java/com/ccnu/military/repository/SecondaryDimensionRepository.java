package com.ccnu.military.repository;

import com.ccnu.military.entity.SecondaryDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SecondaryDimensionRepository extends JpaRepository<SecondaryDimension, Long> {

    List<SecondaryDimension> findByPrimaryDimensionIdOrderBySortOrder(Long primaryDimensionId);

    Optional<SecondaryDimension> findByPrimaryDimensionIdAndDimensionCode(Long primaryDimensionId, String dimensionCode);

    List<SecondaryDimension> findByPrimaryDimensionId(Long primaryDimensionId);
}
