package com.ccnu.military.repository;

import com.ccnu.military.entity.AhpCommunicationScoringResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AhpCommunicationScoringResultRepository extends JpaRepository<AhpCommunicationScoringResult, Long> {

    List<AhpCommunicationScoringResult> findByEvaluationBatchIdOrderByOperationIdAsc(String evaluationBatchId);

    Optional<AhpCommunicationScoringResult> findByEvaluationBatchIdAndOperationId(String evaluationBatchId, String operationId);

    void deleteByEvaluationBatchId(String evaluationBatchId);

    void deleteByEvaluationBatchIdAndOperationId(String evaluationBatchId, String operationId);
}