package com.ccnu.military.repository;

import com.ccnu.military.entity.EquipmentQtEvaluationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EquipmentQtEvaluationRecordRepository extends JpaRepository<EquipmentQtEvaluationRecord, Long> {

    List<EquipmentQtEvaluationRecord> findByEvaluationBatchIdOrderByOperationId(String evaluationBatchId);

    Optional<EquipmentQtEvaluationRecord> findByEvaluationBatchIdAndOperationId(String evaluationBatchId, String operationId);

    void deleteByEvaluationBatchId(String evaluationBatchId);

    List<EquipmentQtEvaluationRecord> findDistinctEvaluationBatchIdBy();
}
