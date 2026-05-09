package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicAhpWeights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态AHP权重Repository
 */
@Repository
public interface DynamicAhpWeightsRepository extends JpaRepository<DynamicAhpWeights, Long> {

    /**
     * 根据专家ID和模板ID查询所有权重记录
     */
    List<DynamicAhpWeights> findByExpertIdAndTemplateIdOrderByLevelNameAscSortOrderAsc(Long expertId, Long templateId);

    /**
     * 根据专家ID、模板ID和层级查询权重记录
     */
    List<DynamicAhpWeights> findByExpertIdAndTemplateIdAndLevelNameOrderBySortOrderAsc(
            Long expertId, Long templateId, String levelName);

    /**
     * 根据专家ID、模板ID、层级和维度类型查询权重记录
     */
    List<DynamicAhpWeights> findByExpertIdAndTemplateIdAndLevelNameAndDimensionTypeOrderBySortOrderAsc(
            Long expertId, Long templateId, String levelName, DynamicAhpWeights.DimensionType dimensionType);

    /**
     * 根据专家ID、模板ID、层级和父级维度查询权重记录
     */
    List<DynamicAhpWeights> findByExpertIdAndTemplateIdAndLevelNameAndParentCodeOrderBySortOrderAsc(
            Long expertId, Long templateId, String levelName, String parentCode);

    /**
     * 查询特定权重记录
     */
    Optional<DynamicAhpWeights> findByExpertIdAndTemplateIdAndLevelNameAndDimensionCode(
            Long expertId, Long templateId, String levelName, String dimensionCode);

    /**
     * 查询某专家在某模板下的所有唯一层级名称
     */
    @Query("SELECT DISTINCT d.levelName FROM DynamicAhpWeights d WHERE d.expertId = :expertId AND d.templateId = :templateId ORDER BY d.levelName")
    List<String> findDistinctLevelsByExpertAndTemplate(@Param("expertId") Long expertId, @Param("templateId") Long templateId);

    /**
     * 删除某专家在某模板下的所有权重记录
     */
    @Modifying
    @Query("DELETE FROM DynamicAhpWeights d WHERE d.expertId = :expertId AND d.templateId = :templateId")
    void deleteByExpertIdAndTemplateId(@Param("expertId") Long expertId, @Param("templateId") Long templateId);

    /**
     * 删除某专家在某模板某层级下的所有权重记录
     */
    @Modifying
    @Query("DELETE FROM DynamicAhpWeights d WHERE d.expertId = :expertId AND d.templateId = :templateId AND d.levelName = :levelName")
    void deleteByExpertIdAndTemplateIdAndLevelName(
            @Param("expertId") Long expertId,
            @Param("templateId") Long templateId,
            @Param("levelName") String levelName);

    /**
     * 查询某专家某模板某层级的所有一级维度权重
     */
    @Query("SELECT d FROM DynamicAhpWeights d WHERE d.expertId = :expertId AND d.templateId = :templateId " +
           "AND d.levelName = :levelName AND d.dimensionType = 'PRIMARY' ORDER BY d.sortOrder")
    List<DynamicAhpWeights> findPrimaryWeights(
            @Param("expertId") Long expertId,
            @Param("templateId") Long templateId,
            @Param("levelName") String levelName);

    /**
     * 查询某专家某模板某层级某父级维度下的所有二级指标权重
     */
    @Query("SELECT d FROM DynamicAhpWeights d WHERE d.expertId = :expertId AND d.templateId = :templateId " +
           "AND d.levelName = :levelName AND d.dimensionType = 'SECONDARY' AND d.parentCode = :parentCode ORDER BY d.sortOrder")
    List<DynamicAhpWeights> findSecondaryWeights(
            @Param("expertId") Long expertId,
            @Param("templateId") Long templateId,
            @Param("levelName") String levelName,
            @Param("parentCode") String parentCode);

    /**
     * 检查是否存在指定专家的权重记录
     */
    boolean existsByExpertIdAndTemplateId(Long expertId, Long templateId);

    /**
     * 统计某专家某模板的权重记录数
     */
    long countByExpertIdAndTemplateId(Long expertId, Long templateId);

    /**
     * 统计某专家某模板某层级的权重记录数
     */
    long countByExpertIdAndTemplateIdAndLevelName(Long expertId, Long templateId, String levelName);
}
