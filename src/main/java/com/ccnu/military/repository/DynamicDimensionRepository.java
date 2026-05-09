package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicDimension;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicDimensionRepository extends JpaRepository<DynamicDimension, Long> {
    
    /**
     * 查询指定模板的所有维度
     */
    List<DynamicDimension> findByTemplateIdOrderBySortOrder(Long templateId);
    
    /**
     * 查询指定模板的指定层级的维度
     */
    List<DynamicDimension> findByTemplateIdAndDimensionLevelOrderBySortOrder(
            Long templateId, 
            DynamicDimension.DimensionLevel dimensionLevel);
    
    /**
     * 查询指定父节点的所有子维度
     */
    List<DynamicDimension> findByParentIdOrderBySortOrder(Long parentId);
    
    /**
     * 根据模板ID和层级删除所有维度
     */
    void deleteByTemplateIdAndDimensionLevel(Long templateId, DynamicDimension.DimensionLevel dimensionLevel);
    
    /**
     * 统计指定模板指定层级的维度数量
     */
    long countByTemplateIdAndDimensionLevel(Long templateId, DynamicDimension.DimensionLevel dimensionLevel);
    
    /**
     * 查询所有二级维度（定量指标）
     */
    @Query("SELECT d FROM DynamicDimension d WHERE d.templateId = :templateId " +
           "AND d.dimensionLevel = 'SECONDARY' " +
           "AND d.metricType = 'QUANTITATIVE' " +
           "ORDER BY d.sortOrder")
    List<DynamicDimension> findQuantitativeIndicators(@Param("templateId") Long templateId);

    /**
     * 查询所有二级维度（定性指标）
     */
    @Query("SELECT d FROM DynamicDimension d WHERE d.templateId = :templateId " +
           "AND d.dimensionLevel = 'SECONDARY' " +
           "AND d.metricType = 'QUALITATIVE' " +
           "ORDER BY d.sortOrder")
    List<DynamicDimension> findQualitativeIndicators(@Param("templateId") Long templateId);

    /**
     * 查询指定模板指定层级的所有维度
     */
    @Query("SELECT d FROM DynamicDimension d WHERE d.templateId = :templateId " +
           "AND d.dimensionLevel = :dimensionLevel " +
           "ORDER BY d.sortOrder")
    List<DynamicDimension> findByTemplateIdAndDimensionLevel(
            @Param("templateId") Long templateId,
            @Param("dimensionLevel") DynamicDimension.DimensionLevel dimensionLevel);
    
    /**
     * 根据code查找（返回列表，可能有多个）
     */
    List<DynamicDimension> findByTemplateIdAndCode(Long templateId, String code);

    /**
     * 检查指定模板、层级、code的维度是否存在
     */
    boolean existsByTemplateIdAndDimensionLevelAndCode(Long templateId, DynamicDimension.DimensionLevel dimensionLevel, String code);

    /**
     * 根据模板ID、层级和名称查询
     */
    List<DynamicDimension> findByTemplateIdAndDimensionLevelAndName(
            Long templateId, DynamicDimension.DimensionLevel dimensionLevel, String name);
}
