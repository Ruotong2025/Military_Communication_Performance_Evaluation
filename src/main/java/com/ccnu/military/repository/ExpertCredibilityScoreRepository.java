package com.ccnu.military.repository;

import com.ccnu.military.entity.ExpertCredibilityScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 专家可信度评估得分数据访问层
 */
@Repository
public interface ExpertCredibilityScoreRepository extends JpaRepository<ExpertCredibilityScore, Long> {

    /**
     * 根据专家ID查找评估记录
     */
    Optional<ExpertCredibilityScore> findByExpertId(Long expertId);

    /**
     * 根据专家姓名查找最新评估记录
     */
    Optional<ExpertCredibilityScore> findTopByExpertNameOrderByEvaluationDateDesc(String expertName);

    /**
     * 根据专家ID删除评估记录
     */
    void deleteByExpertId(Long expertId);

    /**
     * 查询所有评估记录，按综合得分降序
     */
    List<ExpertCredibilityScore> findAllByOrderByTotalScoreDesc();

    /**
     * 查询所有评估记录，按评估日期降序
     */
    List<ExpertCredibilityScore> findAllByOrderByEvaluationDateDesc();

    /**
     * 根据可信度等级查询
     */
    List<ExpertCredibilityScore> findByCredibilityLevelOrderByTotalScoreDesc(String credibilityLevel);

    /**
     * 查询综合得分大于指定值的记录
     */
    List<ExpertCredibilityScore> findByTotalScoreGreaterThanOrderByTotalScoreDesc(java.math.BigDecimal score);

    /**
     * 批量根据专家ID查询
     */
    List<ExpertCredibilityScore> findByExpertIdIn(List<Long> expertIds);

    /**
     * 获取所有评估记录（包含专家姓名）
     */
    @Query("SELECT s FROM ExpertCredibilityScore s ORDER BY s.totalScore DESC")
    List<ExpertCredibilityScore> findAllScoresOrderByTotalScore();
}
