package com.ccnu.military.service;

import com.ccnu.military.entity.MilitaryEffectivenessEvaluation;
import com.ccnu.military.dto.EvaluationResultDTO;
import com.ccnu.military.repository.MilitaryEffectivenessRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 军事效能评估服务层
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MilitaryEffectivenessService {

    private final MilitaryEffectivenessRepository repository;

    /**
     * 查询所有评估记录
     */
    @Transactional(readOnly = true)
    public List<MilitaryEffectivenessEvaluation> findAll() {
        log.info("查询所有评估记录");
        return repository.findAll();
    }

    /**
     * 根据测试批次ID查询
     */
    @Transactional(readOnly = true)
    public Optional<MilitaryEffectivenessEvaluation> findByTestId(String testId) {
        log.info("查询测试批次: {}", testId);
        return repository.findByTestId(testId);
    }

    /**
     * 根据场景ID查询
     */
    @Transactional(readOnly = true)
    public List<MilitaryEffectivenessEvaluation> findByScenarioId(Integer scenarioId) {
        log.info("查询场景ID: {}", scenarioId);
        return repository.findByScenarioId(scenarioId);
    }

    /**
     * 查询有崩溃记录的评估
     */
    @Transactional(readOnly = true)
    public List<MilitaryEffectivenessEvaluation> findWithCrashes() {
        log.info("查询有崩溃记录的评估");
        return repository.findByCrashesGreaterThanZero();
    }

    /**
     * 查询成功率低于阈值的评估
     */
    @Transactional(readOnly = true)
    public List<MilitaryEffectivenessEvaluation> findLowSuccessRate(Double threshold) {
        log.info("查询成功率低于{}的评估", threshold);
        return repository.findBySuccessRateLessThan(threshold);
    }

    /**
     * 计算综合评估结果（包含得分和排名）
     */
    @Transactional(readOnly = true)
    public List<EvaluationResultDTO> calculateEvaluationResults() {
        log.info("开始计算综合评估结果");
        
        List<MilitaryEffectivenessEvaluation> evaluations = repository.findAll();
        List<EvaluationResultDTO> results = new ArrayList<>();

        // 计算每个评估的得分
        for (MilitaryEffectivenessEvaluation eval : evaluations) {
            EvaluationResultDTO dto = new EvaluationResultDTO();
            dto.setTestId(eval.getTestId());
            dto.setScenarioId(eval.getScenarioId());
            
            // 计算各维度得分（简化版，实际应使用AHP和熵权法）
            dto.setResponseScore(calculateResponseScore(eval));
            dto.setProcessingScore(calculateProcessingScore(eval));
            dto.setEffectivenessScore(calculateEffectivenessScore(eval));
            dto.setReliabilityScore(calculateReliabilityScore(eval));
            dto.setAntiJammingScore(calculateAntiJammingScore(eval));
            dto.setOperationScore(calculateOperationScore(eval));
            dto.setNetworkingScore(calculateNetworkingScore(eval));
            dto.setSecurityScore(calculateSecurityScore(eval));
            
            // 计算综合得分（加权平均，权重可调整）
            BigDecimal totalScore = dto.getReliabilityScore().multiply(new BigDecimal("0.25"))
                .add(dto.getSecurityScore().multiply(new BigDecimal("0.20")))
                .add(dto.getAntiJammingScore().multiply(new BigDecimal("0.15")))
                .add(dto.getEffectivenessScore().multiply(new BigDecimal("0.15")))
                .add(dto.getProcessingScore().multiply(new BigDecimal("0.10")))
                .add(dto.getNetworkingScore().multiply(new BigDecimal("0.07")))
                .add(dto.getOperationScore().multiply(new BigDecimal("0.05")))
                .add(dto.getResponseScore().multiply(new BigDecimal("0.03")));
            
            dto.setTotalScore(totalScore.setScale(2, RoundingMode.HALF_UP));
            dto.setGrade(getGrade(totalScore));
            
            // 设置关键指标
            dto.setTaskSuccessRate(eval.getTaskSuccessRate());
            dto.setCommunicationAvailabilityRate(eval.getCommunicationAvailabilityRate());
            dto.setTotalNetworkCrashes(eval.getTotalNetworkCrashes());
            dto.setTotalCommunications(eval.getTotalCommunications());
            
            results.add(dto);
        }

        // 排序并设置排名
        results.sort(Comparator.comparing(EvaluationResultDTO::getTotalScore).reversed());
        for (int i = 0; i < results.size(); i++) {
            results.get(i).setRank(i + 1);
        }

        log.info("评估结果计算完成，共{}条记录", results.size());
        return results;
    }

    /**
     * 计算响应能力得分
     */
    private BigDecimal calculateResponseScore(MilitaryEffectivenessEvaluation eval) {
        // 简化计算：基于呼叫建立时长和传输时延
        BigDecimal score = new BigDecimal("100");
        
        if (eval.getAvgCallSetupDurationMs() != null) {
            // 呼叫建立时长越短越好，假设2000ms为基准
            BigDecimal penalty = eval.getAvgCallSetupDurationMs().divide(new BigDecimal("20"), 2, RoundingMode.HALF_UP);
            score = score.subtract(penalty);
        }
        
        if (eval.getAvgTransmissionDelayMs() != null) {
            // 传输时延越短越好，假设500ms为基准
            BigDecimal penalty = eval.getAvgTransmissionDelayMs().divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
            score = score.subtract(penalty);
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算处理能力得分
     */
    private BigDecimal calculateProcessingScore(MilitaryEffectivenessEvaluation eval) {
        BigDecimal score = new BigDecimal("50");
        
        if (eval.getEffectiveThroughput() != null) {
            // 吞吐量越高越好
            score = score.add(eval.getEffectiveThroughput().divide(new BigDecimal("1000"), 2, RoundingMode.HALF_UP));
        }
        
        if (eval.getSpectralEfficiency() != null) {
            // 频谱效率越高越好
            score = score.add(eval.getSpectralEfficiency().multiply(new BigDecimal("10")));
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算有效性得分
     */
    private BigDecimal calculateEffectivenessScore(MilitaryEffectivenessEvaluation eval) {
        BigDecimal score = new BigDecimal("100");
        
        if (eval.getAvgBer() != null && eval.getAvgBer().compareTo(BigDecimal.ZERO) > 0) {
            // 误码率越低越好
            score = score.subtract(eval.getAvgBer().multiply(new BigDecimal("1000000")));
        }
        
        if (eval.getAvgPlr() != null) {
            // 丢包率越低越好
            score = score.subtract(eval.getAvgPlr().multiply(new BigDecimal("100")));
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算可靠性得分
     */
    private BigDecimal calculateReliabilityScore(MilitaryEffectivenessEvaluation eval) {
        BigDecimal score = new BigDecimal("100");
        
        // 崩溃次数影响
        if (eval.getTotalNetworkCrashes() != null && eval.getTotalNetworkCrashes() > 0) {
            score = score.subtract(new BigDecimal(eval.getTotalNetworkCrashes() * 10));
        }
        
        // 成功率影响
        if (eval.getTaskSuccessRate() != null) {
            score = eval.getTaskSuccessRate().multiply(new BigDecimal("100"));
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算抗干扰性得分
     */
    private BigDecimal calculateAntiJammingScore(MilitaryEffectivenessEvaluation eval) {
        BigDecimal score = new BigDecimal("50");
        
        if (eval.getAvgSinr() != null) {
            // SINR越高越好
            score = score.add(eval.getAvgSinr().multiply(new BigDecimal("2")));
        }
        
        if (eval.getAvgJammingMargin() != null) {
            // 抗干扰余量越高越好
            score = score.add(eval.getAvgJammingMargin().multiply(new BigDecimal("2")));
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算人为操作得分
     */
    private BigDecimal calculateOperationScore(MilitaryEffectivenessEvaluation eval) {
        BigDecimal score = new BigDecimal("100");
        
        if (eval.getOperationSuccessRate() != null) {
            score = eval.getOperationSuccessRate().multiply(new BigDecimal("100"));
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算组网能力得分
     */
    private BigDecimal calculateNetworkingScore(MilitaryEffectivenessEvaluation eval) {
        BigDecimal score = new BigDecimal("100");
        
        if (eval.getAvgConnectivityRate() != null) {
            score = eval.getAvgConnectivityRate().multiply(new BigDecimal("100"));
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 计算安全性得分
     */
    private BigDecimal calculateSecurityScore(MilitaryEffectivenessEvaluation eval) {
        BigDecimal score = new BigDecimal("100");
        
        if (eval.getDetectionProbability() != null) {
            // 被侦察概率越低越好
            score = score.subtract(eval.getDetectionProbability().multiply(new BigDecimal("100")));
        }
        
        if (eval.getInterceptionResistance() != null) {
            // 抗拦截能力越高越好
            score = eval.getInterceptionResistance();
        }
        
        return score.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }

    /**
     * 根据分数获取等级
     */
    private String getGrade(BigDecimal score) {
        if (score.compareTo(new BigDecimal("90")) >= 0) {
            return "优秀";
        } else if (score.compareTo(new BigDecimal("80")) >= 0) {
            return "良好";
        } else if (score.compareTo(new BigDecimal("70")) >= 0) {
            return "中等";
        } else if (score.compareTo(new BigDecimal("60")) >= 0) {
            return "及格";
        } else {
            return "较差";
        }
    }

    /**
     * 获取统计信息
     */
    @Transactional(readOnly = true)
    public Long getTotalCount() {
        return repository.countTotalEvaluations();
    }
}
