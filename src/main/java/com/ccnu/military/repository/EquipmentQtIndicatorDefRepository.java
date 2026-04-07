package com.ccnu.military.repository;

import com.ccnu.military.entity.EquipmentQtIndicatorDef;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EquipmentQtIndicatorDefRepository extends JpaRepository<EquipmentQtIndicatorDef, String> {

    List<EquipmentQtIndicatorDef> findByEnabledTrueOrderByDisplayOrderAsc();

    List<EquipmentQtIndicatorDef> findByPhaseOrderByDisplayOrderAsc(String phase);

    List<EquipmentQtIndicatorDef> findByDimensionOrderByDisplayOrderAsc(String dimension);
}
