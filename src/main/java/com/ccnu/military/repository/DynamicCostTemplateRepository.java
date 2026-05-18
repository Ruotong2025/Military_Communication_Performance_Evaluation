package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicCostTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DynamicCostTemplateRepository extends JpaRepository<DynamicCostTemplate, Long> {
    
    Optional<DynamicCostTemplate> findByTemplateCode(String templateCode);
    
    List<DynamicCostTemplate> findByTemplateNameAndStatus(String templateName, Integer status);
    
    List<DynamicCostTemplate> findByStatusOrderByCreatedTimeDesc(Integer status);
    
    List<DynamicCostTemplate> findAllByOrderByCreatedTimeDesc();
}
