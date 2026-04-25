package com.ccnu.military.repository;

import com.ccnu.military.entity.IndicatorTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface IndicatorTemplateRepository extends JpaRepository<IndicatorTemplate, Long> {

    Optional<IndicatorTemplate> findByTemplateCode(String templateCode);

    List<IndicatorTemplate> findByStatus(IndicatorTemplate.Status status);

    List<IndicatorTemplate> findAllByOrderByCreatedAtDesc();
}
