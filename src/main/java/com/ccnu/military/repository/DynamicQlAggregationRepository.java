package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicQlAggregation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态定性评估集结结果Repository
 */
@Repository
public interface DynamicQlAggregationRepository extends JpaRepository<DynamicQlAggregation, Long> {

    /**
     * 根据批次ID查询所有集结结果
     */
    List<DynamicQlAggregation> findByBatchId(String batchId);

    /**
     * 根据批次ID和作战ID查询
     */
    List<DynamicQlAggregation> findByBatchIdAndOperationId(String batchId, String operationId);

    /**
     * 根据批次ID、作战ID、指标编码查询
     */
    Optional<DynamicQlAggregation> findByBatchIdAndOperationIdAndSecondaryCode(
            String batchId, String operationId, String secondaryCode);

    /**
     * 删除某批次的所有集结结果
     */
    void deleteByBatchId(String batchId);

    /**
     * 删除某批次某作战的集结结果
     */
    void deleteByBatchIdAndOperationId(String batchId, String operationId);
}
