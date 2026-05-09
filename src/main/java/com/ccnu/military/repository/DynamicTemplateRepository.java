package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DynamicTemplateRepository extends JpaRepository<DynamicTemplate, Long> {
    
    Optional<DynamicTemplate> findByTemplateCode(String templateCode);
    
    List<DynamicTemplate> findByStatusOrderByCreatedAtDesc(DynamicTemplate.Status status);
    
    List<DynamicTemplate> findAllByOrderByCreatedAtDesc();
}
