package com.ccnu.military.repository;

import com.ccnu.military.entity.CostIndicatorConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 成本指标配置 Repository
 */
@Repository
public interface CostIndicatorConfigRepository extends JpaRepository<CostIndicatorConfig, Long> {

    /** 根据指标键查找 */
    Optional<CostIndicatorConfig> findByIndicatorKey(String indicatorKey);

    /** 查找所有启用的指标 */
    List<CostIndicatorConfig> findByIsActiveTrueOrderBySortOrderAsc();

    /** 按类别查找启用的指标 */
    List<CostIndicatorConfig> findByIsActiveTrueAndCategoryOrderBySortOrderAsc(String category);

    /** 查找所有类别 */
    @Query("SELECT DISTINCT c.category FROM CostIndicatorConfig c WHERE c.isActive = true ORDER BY c.category")
    List<String> findAllCategories();

    /** 根据指标键列表批量查找 */
    List<CostIndicatorConfig> findByIndicatorKeyIn(List<String> indicatorKeys);

    /** 检查指标键是否存在 */
    boolean existsByIndicatorKey(String indicatorKey);
}
