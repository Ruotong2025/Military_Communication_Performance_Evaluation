package com.ccnu.military.repository;

import com.ccnu.military.entity.EquipmentQlIndicatorDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentQlIndicatorDefRepository extends JpaRepository<EquipmentQlIndicatorDef, String> {

    List<EquipmentQlIndicatorDef> findByEnabledTrueOrderByDisplayOrderAsc();

    List<EquipmentQlIndicatorDef> findByPhaseOrderByDisplayOrderAsc(String phase);

    List<EquipmentQlIndicatorDef> findByDimensionOrderByDisplayOrderAsc(String dimension);
}
