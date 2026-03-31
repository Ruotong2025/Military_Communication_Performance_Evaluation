package com.ccnu.military.repository;

import com.ccnu.military.entity.ExpertWeightedEvaluationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertWeightedEvaluationResultRepository extends JpaRepository<ExpertWeightedEvaluationResult, Long> {

    /**
     * 根据评估批次ID查询所有结果
     */
    List<ExpertWeightedEvaluationResult> findByEvaluationIdOrderByOperationIdAsc(String evaluationId);

    /**
     * 删除指定评估批次的所有结果
     */
    void deleteByEvaluationId(String evaluationId);

    /**
     * 检查指定评估批次是否存在结果
     */
    boolean existsByEvaluationId(String evaluationId);
}
