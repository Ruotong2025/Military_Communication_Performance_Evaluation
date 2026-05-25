package com.ccnu.military.repository;

import com.ccnu.military.entity.IndicatorSourceData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 指标可测得数据 Repository
 */
@Repository
public interface IndicatorSourceDataRepository extends JpaRepository<IndicatorSourceData, Long> {

    List<IndicatorSourceData> findByIndicatorId(Long indicatorId);

    List<IndicatorSourceData> findByIndicatorIdOrderByPriorityAsc(Long indicatorId);

    List<IndicatorSourceData> findByIndicatorIdAndIsFormulaRelatedTrue(Long indicatorId);

    List<IndicatorSourceData> findByIndicatorIdIn(List<Long> indicatorIds);

    void deleteByIndicatorId(Long indicatorId);

    void deleteByIndicatorIdIn(List<Long> indicatorIds);
}
