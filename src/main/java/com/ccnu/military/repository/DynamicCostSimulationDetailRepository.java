package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicCostSimulationDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DynamicCostSimulationDetailRepository extends JpaRepository<DynamicCostSimulationDetail, Long> {
    
    List<DynamicCostSimulationDetail> findByBatchIdOrderByIterationNumberAsc(Long batchId);
    
    void deleteByBatchId(Long batchId);
}
