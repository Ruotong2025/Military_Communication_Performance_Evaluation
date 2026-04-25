package com.ccnu.military.repository;

import com.ccnu.military.entity.LevelDefinition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LevelDefinitionRepository extends JpaRepository<LevelDefinition, Long> {

    @Query("SELECT l FROM LevelDefinition l WHERE l.template.id = :templateId ORDER BY l.sortOrder")
    List<LevelDefinition> findByTemplateIdOrderBySortOrder(Long templateId);

    Optional<LevelDefinition> findByLevelCode(String levelCode);

    List<LevelDefinition> findByTemplateId(Long templateId);
}
