package com.ccnu.military.repository;

import com.ccnu.military.entity.PenaltyModelResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 惩罚模型计算结果 Repository
 */
@Repository
public interface PenaltyModelResultRepository extends JpaRepository<PenaltyModelResult, Long> {

    /**
     * 根据评估批次ID和作战ID查询
     */
    Optional<PenaltyModelResult> findByEvaluationIdAndOperationId(String evaluationId, String operationId);

    /**
     * 查询指定评估批次的所有惩罚结果
     */
    List<PenaltyModelResult> findByEvaluationId(String evaluationId);

    /**
     * 删除指定评估批次的所有惩罚结果
     */
    void deleteByEvaluationId(String evaluationId);

    /**
     * 检查是否存在指定评估批次的惩罚结果
     */
    boolean existsByEvaluationId(String evaluationId);
}
