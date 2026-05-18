package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicCostBatch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DynamicCostBatchRepository extends JpaRepository<DynamicCostBatch, Long> {
    
    Optional<DynamicCostBatch> findByBatchCode(String batchCode);
    
    List<DynamicCostBatch> findByTemplateIdOrderByCreatedTimeDesc(Long templateId);
    
    List<DynamicCostBatch> findByStatusOrderByCreatedTimeDesc(Integer status);
}
