package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicQlRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态定性评估记录Repository
 * 完全动态化：基于 dynamic_dimension 表中的定性指标
 */
@Repository
public interface DynamicQlRecordRepository extends JpaRepository<DynamicQlRecord, Long> {

    // ==================== 按维度查询 ====================

    /**
     * 根据批次ID查询所有记录
     */
    List<DynamicQlRecord> findByBatchId(String batchId);

    /**
     * 根据批次ID和专家ID查询
     */
    List<DynamicQlRecord> findByBatchIdAndExpertId(String batchId, Long expertId);

    /**
     * 根据批次ID和作战ID查询
     */
    List<DynamicQlRecord> findByBatchIdAndOperationId(String batchId, String operationId);

    /**
     * 根据批次ID、专家ID、作战ID查询
     */
    List<DynamicQlRecord> findByBatchIdAndExpertIdAndOperationId(
            String batchId, Long expertId, String operationId);

    /**
     * 根据批次ID和指标编码查询
     */
    List<DynamicQlRecord> findByBatchIdAndSecondaryCode(String batchId, String secondaryCode);

    /**
     * 根据批次ID和层级名称查询
     */
    List<DynamicQlRecord> findByBatchIdAndLevelName(String batchId, String levelName);

    // ==================== 单条查询 ====================

    /**
     * 根据批次ID、专家ID、作战ID、指标编码查询（唯一约束）
     */
    Optional<DynamicQlRecord> findByBatchIdAndExpertIdAndOperationIdAndSecondaryCode(
            String batchId, Long expertId, String operationId, String secondaryCode);

    // ==================== 统计查询 ====================

    /**
     * 统计某批次某专家的评估数量
     */
    @Query("SELECT COUNT(r) FROM DynamicQlRecord r WHERE r.batchId = :batchId AND r.expertId = :expertId")
    long countByBatchIdAndExpertId(@Param("batchId") String batchId, @Param("expertId") Long expertId);

    /**
     * 统计某批次的评估记录总数
     */
    @Query("SELECT COUNT(r) FROM DynamicQlRecord r WHERE r.batchId = :batchId")
    long countByBatchId(@Param("batchId") String batchId);

    /**
     * 统计某批次某作战的评估数量
     */
    @Query("SELECT COUNT(r) FROM DynamicQlRecord r WHERE r.batchId = :batchId AND r.operationId = :operationId")
    long countByBatchIdAndOperationId(@Param("batchId") String batchId, @Param("operationId") String operationId);

    /**
     * 获取某批次的不同专家数量
     */
    @Query("SELECT COUNT(DISTINCT r.expertId) FROM DynamicQlRecord r WHERE r.batchId = :batchId")
    int countDistinctExpertsByBatchId(@Param("batchId") String batchId);

    /**
     * 获取某批次某作战的不同专家数量
     */
    @Query("SELECT COUNT(DISTINCT r.expertId) FROM DynamicQlRecord r WHERE r.batchId = :batchId AND r.operationId = :operationId")
    int countDistinctExpertsByBatchIdAndOperationId(@Param("batchId") String batchId, @Param("operationId") String operationId);

    // ==================== 删除操作 ====================

    /**
     * 根据批次ID删除所有记录
     */
    void deleteByBatchId(String batchId);

    /**
     * 根据批次ID和专家ID删除
     */
    void deleteByBatchIdAndExpertId(String batchId, Long expertId);

    /**
     * 根据批次ID和作战ID删除
     */
    void deleteByBatchIdAndOperationId(String batchId, String operationId);

    // ==================== 动态查询（根据条件组合） ====================

    /**
     * 通用查询：根据可选条件动态构建
     */
    @Query("SELECT r FROM DynamicQlRecord r WHERE " +
           "(:batchId IS NULL OR r.batchId = :batchId) AND " +
           "(:expertId IS NULL OR r.expertId = :expertId) AND " +
           "(:operationId IS NULL OR r.operationId = :operationId) AND " +
           "(:levelName IS NULL OR r.levelName = :levelName) AND " +
           "(:secondaryCode IS NULL OR r.secondaryCode = :secondaryCode)")
    List<DynamicQlRecord> findByConditions(
            @Param("batchId") String batchId,
            @Param("expertId") Long expertId,
            @Param("operationId") String operationId,
            @Param("levelName") String levelName,
            @Param("secondaryCode") String secondaryCode);

    // ==================== 汇总查询 ====================

    /**
     * 按评分等级统计某批次的分布
     */
    @Query("SELECT r.scoreLevel, COUNT(r) FROM DynamicQlRecord r WHERE r.batchId = :batchId GROUP BY r.scoreLevel")
    List<Object[]> countByBatchIdGroupByScoreLevel(@Param("batchId") String batchId);

    /**
     * 按专家统计某批次的评估数量
     */
    @Query("SELECT r.expertId, r.expertName, COUNT(r) FROM DynamicQlRecord r WHERE r.batchId = :batchId GROUP BY r.expertId, r.expertName")
    List<Object[]> countByBatchIdGroupByExpert(@Param("batchId") String batchId);

    /**
     * 按作战统计某批次的评估数量
     */
    @Query("SELECT r.operationId, COUNT(r) FROM DynamicQlRecord r WHERE r.batchId = :batchId GROUP BY r.operationId")
    List<Object[]> countByBatchIdGroupByOperation(@Param("batchId") String batchId);
}
