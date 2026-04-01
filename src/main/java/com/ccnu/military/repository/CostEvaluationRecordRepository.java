package com.ccnu.military.repository;

import com.ccnu.military.entity.CostEvaluationRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 成本评估记录 Repository
 */
@Repository
public interface CostEvaluationRecordRepository extends JpaRepository<CostEvaluationRecord, Long> {

    /** 根据评估批次ID查找 */
    List<CostEvaluationRecord> findByEvaluationId(String evaluationId);

    /** 根据作战任务ID查找 */
    List<CostEvaluationRecord> findByOperationId(String operationId);

    /** 根据评估批次ID和作战任务ID查找 */
    Optional<CostEvaluationRecord> findByEvaluationIdAndOperationId(String evaluationId, String operationId);

    /** 根据评估批次ID删除 */
    void deleteByEvaluationId(String evaluationId);

    /** 检查是否存在记录 */
    boolean existsByEvaluationIdAndOperationId(String evaluationId, String operationId);
}
