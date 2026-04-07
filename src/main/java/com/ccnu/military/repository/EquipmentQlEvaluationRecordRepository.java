package com.ccnu.military.repository;

import com.ccnu.military.entity.EquipmentQlEvaluationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentQlEvaluationRecordRepository extends JpaRepository<EquipmentQlEvaluationRecord, Long> {

    List<EquipmentQlEvaluationRecord> findByEvaluationBatchIdAndOperationIdOrderByExpertId(String evaluationBatchId, String operationId);

    Optional<EquipmentQlEvaluationRecord> findByEvaluationBatchIdAndOperationIdAndExpertId(String evaluationBatchId, String operationId, Long expertId);

    List<EquipmentQlEvaluationRecord> findByEvaluationBatchIdOrderByOperationId(String evaluationBatchId);

    void deleteByEvaluationBatchId(String evaluationBatchId);
}
