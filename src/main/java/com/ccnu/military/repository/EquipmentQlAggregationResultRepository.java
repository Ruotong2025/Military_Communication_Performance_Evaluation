package com.ccnu.military.repository;

import com.ccnu.military.entity.EquipmentQlAggregationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentQlAggregationResultRepository extends JpaRepository<EquipmentQlAggregationResult, Long> {

    Optional<EquipmentQlAggregationResult> findByEvaluationBatchIdAndOperationIdAndIndicatorKey(
            String evaluationBatchId, String operationId, String indicatorKey);

    List<EquipmentQlAggregationResult> findByEvaluationBatchIdAndOperationId(
            String evaluationBatchId, String operationId);

    List<EquipmentQlAggregationResult> findByEvaluationBatchId(String evaluationBatchId);

    void deleteByEvaluationBatchId(String evaluationBatchId);

    void deleteByEvaluationBatchIdAndOperationId(String evaluationBatchId, String operationId);
}