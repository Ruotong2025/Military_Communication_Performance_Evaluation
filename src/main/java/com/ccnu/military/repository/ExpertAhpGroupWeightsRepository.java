package com.ccnu.military.repository;

import com.ccnu.military.entity.ExpertAhpGroupWeights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExpertAhpGroupWeightsRepository extends JpaRepository<ExpertAhpGroupWeights, Long> {

    /**
     * 最近一次更新的集结权重（用于「加载综合结果」时与 score 表相乘）
     */
    Optional<ExpertAhpGroupWeights> findFirstByOrderByUpdatedAtDesc();

    /**
     * 根据专家ID集合查询集结权重（排序后的expert_ids作为唯一标识）
     */
    Optional<ExpertAhpGroupWeights> findByExpertIds(String expertIds);

    /**
     * 根据groupId查询
     */
    Optional<ExpertAhpGroupWeights> findByGroupId(String groupId);

    /**
     * 查询所有专家组权重（按创建时间倒序）
     */
    List<ExpertAhpGroupWeights> findAllByOrderByCreatedAtDesc();

    /**
     * 根据专家ID模糊查询（查找包含该专家的所有专家组）
     */
    @Query("SELECT g FROM ExpertAhpGroupWeights g WHERE g.expertIds LIKE %:expertId%")
    List<ExpertAhpGroupWeights> findByExpertIdContaining(@Param("expertId") String expertId);

    /**
     * 删除指定的专家组
     */
    void deleteByGroupId(String groupId);

    /**
     * 批量删除
     */
    void deleteAllByGroupIdIn(List<String> groupIds);
}
