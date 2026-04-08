package com.ccnu.military.repository;

import com.ccnu.military.entity.QlAggregationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QlAggregationResultRepository extends JpaRepository<QlAggregationResult, Long> {

    Optional<QlAggregationResult> findByEvaluationBatchIdAndOperationIdAndIndicatorKey(
            String evaluationBatchId, String operationId, String indicatorKey);

    List<QlAggregationResult> findByEvaluationBatchIdAndOperationId(
            String evaluationBatchId, String operationId);

    List<QlAggregationResult> findByEvaluationBatchId(String evaluationBatchId);

    void deleteByEvaluationBatchId(String evaluationBatchId);

    void deleteByEvaluationBatchIdAndOperationId(String evaluationBatchId, String operationId);
}
