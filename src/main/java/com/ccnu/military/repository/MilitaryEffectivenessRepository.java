package com.ccnu.military.repository;

import com.ccnu.military.entity.MilitaryEffectivenessEvaluation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 军事效能评估数据访问层
 */
@Repository
public interface MilitaryEffectivenessRepository extends JpaRepository<MilitaryEffectivenessEvaluation, Long> {

    /**
     * 根据测试批次ID查询
     */
    Optional<MilitaryEffectivenessEvaluation> findByTestId(String testId);

    /**
     * 根据场景ID查询所有评估记录
     */
    List<MilitaryEffectivenessEvaluation> findByScenarioId(Integer scenarioId);

    /**
     * 查询所有评估记录，按综合得分排序（需要在Service层计算）
     */
    List<MilitaryEffectivenessEvaluation> findAllByOrderByTestIdAsc();

    /**
     * 查询崩溃次数大于0的记录
     */
    @Query("SELECT e FROM MilitaryEffectivenessEvaluation e WHERE e.totalNetworkCrashes > 0 ORDER BY e.totalNetworkCrashes DESC")
    List<MilitaryEffectivenessEvaluation> findByCrashesGreaterThanZero();

    /**
     * 查询通信成功率低于指定值的记录
     */
    @Query("SELECT e FROM MilitaryEffectivenessEvaluation e WHERE e.taskSuccessRate < :threshold ORDER BY e.taskSuccessRate ASC")
    List<MilitaryEffectivenessEvaluation> findBySuccessRateLessThan(@Param("threshold") Double threshold);

    /**
     * 统计总评估记录数
     */
    @Query("SELECT COUNT(e) FROM MilitaryEffectivenessEvaluation e")
    Long countTotalEvaluations();

    /**
     * 检查测试批次ID是否存在
     */
    boolean existsByTestId(String testId);
}
