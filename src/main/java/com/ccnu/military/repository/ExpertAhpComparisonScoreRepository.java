package com.ccnu.military.repository;

import com.ccnu.military.entity.ExpertAhpComparisonScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertAhpComparisonScoreRepository extends JpaRepository<ExpertAhpComparisonScore, Long> {

    void deleteByExpertId(Long expertId);

    List<ExpertAhpComparisonScore> findByExpertIdOrderByIdAsc(Long expertId);

    List<ExpertAhpComparisonScore> findAllByOrderByIdAsc();

    boolean existsByExpertId(Long expertId);

    /**
     * 按 comparison_key 前缀精确查询（用于装备操作域）
     */
    List<ExpertAhpComparisonScore> findByExpertIdAndComparisonKeyStartingWith(Long expertId, String prefix);

    /**
     * 按 comparison_key 前缀删除（装备操作域）
     */
    void deleteByExpertIdAndComparisonKeyStartingWith(Long expertId, String prefix);

    /**
     * 效能指标域：排除 comparison_key 以指定前缀开头的记录（保留装备操作_前缀）
     */
    @Query("SELECT e FROM ExpertAhpComparisonScore e WHERE e.expertId = :expertId AND e.comparisonKey NOT LIKE :prefix ORDER BY e.id ASC")
    List<ExpertAhpComparisonScore> findEffectivenessByExpertId(@Param("expertId") Long expertId, @Param("prefix") String prefix);

    /**
     * 仅删除 comparison_key 不以指定前缀开头的行（效能指标域），保留装备操作_前缀记录
     */
    @Modifying
    @Query("DELETE FROM ExpertAhpComparisonScore e WHERE e.expertId = :expertId AND e.comparisonKey NOT LIKE :prefix")
    void deleteEffectivenessByExpertId(@Param("expertId") Long expertId, @Param("prefix") String prefix);

    @Query("SELECT DISTINCT e.expertId FROM ExpertAhpComparisonScore e WHERE e.comparisonKey LIKE CONCAT(:prefix, '%')")
    List<Long> findDistinctExpertIdsByComparisonKeyStartingWith(@Param("prefix") String prefix);
}
