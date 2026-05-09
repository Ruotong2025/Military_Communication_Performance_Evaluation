package com.ccnu.military.repository;

import com.ccnu.military.entity.DynamicAhpAggregationResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 动态AHP集结结果数据访问接口
 */
@Repository
public interface DynamicAhpAggregationResultRepository extends JpaRepository<DynamicAhpAggregationResult, Long> {

    /**
     * 根据专家组ID查询
     */
    Optional<DynamicAhpAggregationResult> findByGroupId(String groupId);

    /**
     * 根据模板ID查询所有集结结果
     */
    List<DynamicAhpAggregationResult> findByTemplateIdOrderByUpdatedAtDesc(Long templateId);

    /**
     * 根据专家ID列表查询集结结果
     */
    @Query("SELECT r FROM DynamicAhpAggregationResult r WHERE r.expertIds = :expertIdsStr")
    Optional<DynamicAhpAggregationResult> findByExpertIds(@Param("expertIdsStr") String expertIdsStr);

    /**
     * 查询某模板的最新集结结果
     */
    Optional<DynamicAhpAggregationResult> findFirstByTemplateIdOrderByUpdatedAtDesc(Long templateId);

    /**
     * 根据模板ID删除所有集结结果
     */
    void deleteByTemplateId(Long templateId);

    /**
     * 查询所有模板ID
     */
    @Query("SELECT DISTINCT r.templateId FROM DynamicAhpAggregationResult r ORDER BY r.updatedAt DESC")
    List<Long> findDistinctTemplateIds();
}
