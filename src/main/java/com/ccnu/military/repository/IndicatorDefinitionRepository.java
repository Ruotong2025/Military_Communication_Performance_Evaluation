package com.ccnu.military.repository;

import com.ccnu.military.entity.IndicatorDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 指标定义 Repository
 */
@Repository
public interface IndicatorDefinitionRepository extends JpaRepository<IndicatorDefinition, Long> {

    Optional<IndicatorDefinition> findByIndicatorNameAndCategory(String indicatorName, String category);

    Optional<IndicatorDefinition> findByIndicatorName(String indicatorName);

    boolean existsByIndicatorNameAndCategory(String indicatorName, String category);

    boolean existsByIndicatorName(String indicatorName);

    List<IndicatorDefinition> findByIndicatorType(IndicatorDefinition.IndicatorType indicatorType);

    List<IndicatorDefinition> findByIsActiveTrue();

    List<IndicatorDefinition> findByCategory(String category);

    List<IndicatorDefinition> findByIsActiveTrueOrderByCategoryAscIndicatorNameAsc();

    List<IndicatorDefinition> findByCategoryAndIndicatorType(String category, IndicatorDefinition.IndicatorType indicatorType);

    @Query("SELECT DISTINCT d.category FROM IndicatorDefinition d WHERE d.category IS NOT NULL ORDER BY d.category")
    List<String> findAllCategories();

    /**
     * 批量查询指标（用于预匹配）
     */
    List<IndicatorDefinition> findByIndicatorNameIn(List<String> indicatorNames);

    /**
     * 批量查询指标（用于预匹配），指定类别
     */
    List<IndicatorDefinition> findByIndicatorNameInAndCategory(List<String> indicatorNames, String category);
}
