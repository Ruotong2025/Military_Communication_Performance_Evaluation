package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicAhpMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态AHP判断矩阵Repository
 */
@Repository
public interface DynamicAhpMatrixRepository extends JpaRepository<DynamicAhpMatrix, Long> {

    /**
     * 根据专家ID和模板ID查询所有矩阵记录
     */
    List<DynamicAhpMatrix> findByExpertIdAndTemplateId(Long expertId, Long templateId);

    /**
     * 根据专家ID和模板ID查询指定层级的矩阵记录
     */
    List<DynamicAhpMatrix> findByExpertIdAndTemplateIdAndLevelName(Long expertId, Long templateId, String levelName);

    /**
     * 根据专家ID、模板ID和比较类型查询记录
     */
    List<DynamicAhpMatrix> findByExpertIdAndTemplateIdAndComparisonType(
            Long expertId, Long templateId, DynamicAhpMatrix.ComparisonType comparisonType);

    /**
     * 根据专家ID、模板ID和比较类型删除记录
     */
    @Modifying
    @Query("DELETE FROM DynamicAhpMatrix d WHERE d.expertId = :expertId AND d.templateId = :templateId AND d.comparisonType = :comparisonType")
    void deleteByExpertIdAndTemplateIdAndComparisonType(
            @Param("expertId") Long expertId,
            @Param("templateId") Long templateId,
            @Param("comparisonType") DynamicAhpMatrix.ComparisonType comparisonType);

    /**
     * 根据专家ID、模板ID、层级和比较类型查询记录
     */
    List<DynamicAhpMatrix> findByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
            Long expertId, Long templateId, String levelName, DynamicAhpMatrix.ComparisonType comparisonType);

    /**
     * 根据专家ID、模板ID、层级和父级维度查询记录
     */
    List<DynamicAhpMatrix> findByExpertIdAndTemplateIdAndLevelNameAndParentCode(
            Long expertId, Long templateId, String levelName, String parentCode);

    /**
     * 根据比较key查询记录
     */
    Optional<DynamicAhpMatrix> findByExpertIdAndTemplateIdAndComparisonTypeAndRowCodeAndColCode(
            Long expertId, Long templateId, DynamicAhpMatrix.ComparisonType comparisonType,
            String rowCode, String colCode);

    /**
     * 查询某专家在某模板下的所有唯一层级名称
     */
    @Query("SELECT DISTINCT d.levelName FROM DynamicAhpMatrix d WHERE d.expertId = :expertId AND d.templateId = :templateId ORDER BY d.levelName")
    List<String> findDistinctLevelsByExpertAndTemplate(@Param("expertId") Long expertId, @Param("templateId") Long templateId);

    /**
     * 删除某专家在某模板下的所有记录
     */
    @Modifying
    @Query("DELETE FROM DynamicAhpMatrix d WHERE d.expertId = :expertId AND d.templateId = :templateId")
    void deleteByExpertIdAndTemplateId(@Param("expertId") Long expertId, @Param("templateId") Long templateId);

    /**
     * 删除某专家在某模板某层级下的所有记录
     */
    @Modifying
    @Query("DELETE FROM DynamicAhpMatrix d WHERE d.expertId = :expertId AND d.templateId = :templateId AND d.levelName = :levelName")
    void deleteByExpertIdAndTemplateIdAndLevelName(
            @Param("expertId") Long expertId,
            @Param("templateId") Long templateId,
            @Param("levelName") String levelName);

    /**
     * 删除某专家在某模板某层级下指定比较类型的记录
     */
    @Modifying
    @Query("DELETE FROM DynamicAhpMatrix d WHERE d.expertId = :expertId AND d.templateId = :templateId AND d.levelName = :levelName AND d.comparisonType = :comparisonType")
    void deleteByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
            @Param("expertId") Long expertId,
            @Param("templateId") Long templateId,
            @Param("levelName") String levelName,
            @Param("comparisonType") DynamicAhpMatrix.ComparisonType comparisonType);

    /**
     * 检查是否存在指定专家的矩阵记录
     */
    boolean existsByExpertIdAndTemplateId(Long expertId, Long templateId);

    /**
     * 查询某模板的所有记录数
     */
    long countByTemplateId(Long templateId);

    /**
     * 查询某专家在某模板下的记录数
     */
    long countByExpertIdAndTemplateId(Long expertId, Long templateId);

    /**
     * 根据模板ID查询所有矩阵记录
     */
    List<DynamicAhpMatrix> findByTemplateId(Long templateId);

    /**
     * 根据模板ID和专家ID列表查询矩阵记录
     */
    List<DynamicAhpMatrix> findByTemplateIdAndExpertIdIn(Long templateId, List<Long> expertIds);

    /**
     * 根据模板ID和比较类型列表查询矩阵记录
     */
    List<DynamicAhpMatrix> findByTemplateIdAndComparisonTypeIn(Long templateId, List<DynamicAhpMatrix.ComparisonType> types);
}
