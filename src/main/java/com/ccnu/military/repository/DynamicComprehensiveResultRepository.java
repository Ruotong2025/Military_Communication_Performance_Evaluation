package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicComprehensiveResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态综合评分结果Repository
 */
@Repository
public interface DynamicComprehensiveResultRepository extends JpaRepository<DynamicComprehensiveResult, Long> {

    /**
     * 根据批次ID查询所有记录
     */
    List<DynamicComprehensiveResult> findByBatchIdOrderByOperationId(String batchId);

    /**
     * 根据批次ID和作战ID查询
     */
    Optional<DynamicComprehensiveResult> findByBatchIdAndOperationId(String batchId, String operationId);

    /**
     * 检查是否存在记录
     */
    boolean existsByBatchId(String batchId);

    /**
     * 删除批次的所有记录
     */
    void deleteByBatchId(String batchId);
}
