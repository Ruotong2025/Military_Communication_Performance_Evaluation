package com.ccnu.military.repository;

import com.ccnu.military.entity.IndicatorAiLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * AI识别历史记录 Repository
 */
@Repository
public interface IndicatorAiLogRepository extends JpaRepository<IndicatorAiLog, Long> {

    List<IndicatorAiLog> findByIsSuccessTrueOrderByCreatedAtDesc();

    List<IndicatorAiLog> findByIsSuccessFalseOrderByCreatedAtDesc();

    List<IndicatorAiLog> findByOrderByCreatedAtDesc();
}
