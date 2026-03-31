package com.ccnu.military.repository;

import com.ccnu.military.entity.ExpertAhpComparisonScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExpertAhpComparisonScoreRepository extends JpaRepository<ExpertAhpComparisonScore, Long> {

    void deleteByExpertId(Long expertId);

    List<ExpertAhpComparisonScore> findByExpertIdOrderByIdAsc(Long expertId);

    List<ExpertAhpComparisonScore> findAllByOrderByIdAsc();

    boolean existsByExpertId(Long expertId);
}
