package com.ccnu.military.repository;

import com.ccnu.military.entity.CostEvaluationBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 成本评估批次配置 Repository
 */
@Repository
public interface CostEvaluationBatchRepository extends JpaRepository<CostEvaluationBatch, Long> {

    /** 根据评估批次ID查找 */
    Optional<CostEvaluationBatch> findByEvaluationId(String evaluationId);

    /** 根据评估批次ID删除 */
    void deleteByEvaluationId(String evaluationId);

    /** 检查是否存在 */
    boolean existsByEvaluationId(String evaluationId);
}
