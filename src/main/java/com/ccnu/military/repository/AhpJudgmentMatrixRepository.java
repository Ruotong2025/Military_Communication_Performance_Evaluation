package com.ccnu.military.repository;

import com.ccnu.military.entity.AhpJudgmentMatrix;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AhpJudgmentMatrixRepository extends JpaRepository<AhpJudgmentMatrix, Long> {

    Optional<AhpJudgmentMatrix> findByLevelId(Long levelId);

    void deleteByLevelId(Long levelId);
}
