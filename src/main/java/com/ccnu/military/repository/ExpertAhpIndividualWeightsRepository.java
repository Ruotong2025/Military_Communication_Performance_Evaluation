package com.ccnu.military.repository;

import com.ccnu.military.entity.ExpertAhpIndividualWeights;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExpertAhpIndividualWeightsRepository extends JpaRepository<ExpertAhpIndividualWeights, Long> {

    Optional<ExpertAhpIndividualWeights> findByExpertId(Long expertId);
}
