package com.ccnu.military.service;

import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.ExpertAhpIndividualWeights;
import com.ccnu.military.repository.ExpertAhpIndividualWeightsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * 持久化专家 AHP 层次计算结果（与比较打分联动）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertAhpIndividualWeightsService {

    private final ExpertAhpIndividualWeightsRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional
    public void upsert(Long expertId, String expertName, MatrixCalculationResult result) {
        if (expertId == null) {
            throw new IllegalArgumentException("expertId 不能为空");
        }
        String json = null;
        try {
            json = objectMapper.writeValueAsString(result);
        } catch (Exception e) {
            throw new IllegalStateException("序列化 AHP 结果失败: " + e.getMessage(), e);
        }

        ExpertAhpIndividualWeights row = repository.findByExpertId(expertId)
                .orElseGet(ExpertAhpIndividualWeights::new);
        row.setExpertId(expertId);
        row.setExpertName(expertName);
        row.setResultJson(json);
        row.setUpdatedAt(LocalDateTime.now());
        applyFlattenedWeights(row, result);
        repository.save(row);
        log.debug("已写入专家 AHP 权重快照 expertId={}（含分列权重）", expertId);
    }

    /**
     * 查询某专家的权重快照。
     */
    public MatrixCalculationResult findResult(Long expertId) {
        if (expertId == null) {
            return null;
        }
        return repository.findByExpertId(expertId)
                .map(this::parseJson)
                .orElse(null);
    }

    private MatrixCalculationResult parseJson(ExpertAhpIndividualWeights row) {
        if (row.getResultJson() == null || row.getResultJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(row.getResultJson(), MatrixCalculationResult.class);
        } catch (Exception e) {
            log.warn("反序列化 AHP 权重快照失败 expertId={}", row.getExpertId(), e);
            return null;
        }
    }

    /**
     * 将一级维度权重、各二级指标对总目标的综合权重写入与 Navicat 可见的分列字段。
     */
    private void applyFlattenedWeights(ExpertAhpIndividualWeights row, MatrixCalculationResult result) {
        clearIndicatorColumns(row);

        MatrixCalculationResult.MatrixResult dim = result.getDimensionResult();
        if (dim == null || dim.getWeightMap() == null) {
            return;
        }
        Map<String, Double> dw = dim.getWeightMap();
        row.setSecurityWeight(toBd(dw.get("安全性")));
        row.setReliabilityWeight(toBd(dw.get("可靠性")));
        row.setTransmissionWeight(toBd(dw.get("传输能力")));
        row.setAntiJammingWeight(toBd(dw.get("抗干扰能力")));
        row.setEffectWeight(toBd(dw.get("效能影响")));

        Map<String, MatrixCalculationResult.MatrixResult> ir = result.getIndicatorResults();
        if (ir == null) {
            return;
        }
        for (Map.Entry<String, MatrixCalculationResult.MatrixResult> e : ir.entrySet()) {
            String dimName = e.getKey();
            MatrixCalculationResult.MatrixResult mr = e.getValue();
            if (mr == null || mr.getWeightMap() == null) {
                continue;
            }
            double dWeight = dw.getOrDefault(dimName, 0.0);
            for (Map.Entry<String, Double> iw : mr.getWeightMap().entrySet()) {
                setIndicatorCombined(row, iw.getKey(), dWeight * iw.getValue());
            }
        }
    }

    private void clearIndicatorColumns(ExpertAhpIndividualWeights row) {
        row.setSecurityKeyLeakageWeight(null);
        row.setSecurityDetectedProbabilityWeight(null);
        row.setSecurityInterceptionResistanceWeight(null);
        row.setReliabilityCrashRateWeight(null);
        row.setReliabilityRecoveryCapabilityWeight(null);
        row.setReliabilityCommunicationAvailabilityWeight(null);
        row.setTransmissionBandwidthWeight(null);
        row.setTransmissionCallSetupTimeWeight(null);
        row.setTransmissionTransmissionDelayWeight(null);
        row.setTransmissionBitErrorRateWeight(null);
        row.setTransmissionThroughputWeight(null);
        row.setTransmissionSpectralEfficiencyWeight(null);
        row.setAntiJammingSinrWeight(null);
        row.setAntiJammingAntiJammingMarginWeight(null);
        row.setAntiJammingCommunicationDistanceWeight(null);
        row.setEffectDamageRateWeight(null);
        row.setEffectMissionCompletionRateWeight(null);
        row.setEffectBlindRateWeight(null);
    }

    private void setIndicatorCombined(ExpertAhpIndividualWeights row, String indicatorName, double combined) {
        BigDecimal v = toBd(combined);
        if (v == null) {
            return;
        }
        switch (indicatorName) {
            case "密钥泄露得分":
                row.setSecurityKeyLeakageWeight(v);
                break;
            case "被侦察得分":
                row.setSecurityDetectedProbabilityWeight(v);
                break;
            case "抗拦截得分":
                row.setSecurityInterceptionResistanceWeight(v);
                break;
            case "崩溃比例得分":
                row.setReliabilityCrashRateWeight(v);
                break;
            case "恢复能力得分":
                row.setReliabilityRecoveryCapabilityWeight(v);
                break;
            case "通信可用得分":
                row.setReliabilityCommunicationAvailabilityWeight(v);
                break;
            case "带宽得分":
                row.setTransmissionBandwidthWeight(v);
                break;
            case "呼叫建立得分":
                row.setTransmissionCallSetupTimeWeight(v);
                break;
            case "传输时延得分":
                row.setTransmissionTransmissionDelayWeight(v);
                break;
            case "误码率得分":
                row.setTransmissionBitErrorRateWeight(v);
                break;
            case "吞吐量得分":
                row.setTransmissionThroughputWeight(v);
                break;
            case "频谱效率得分":
                row.setTransmissionSpectralEfficiencyWeight(v);
                break;
            case "信干噪比得分":
                row.setAntiJammingSinrWeight(v);
                break;
            case "抗干扰余量得分":
                row.setAntiJammingAntiJammingMarginWeight(v);
                break;
            case "通信距离得分":
                row.setAntiJammingCommunicationDistanceWeight(v);
                break;
            case "战损率得分":
                row.setEffectDamageRateWeight(v);
                break;
            case "任务完成率得分":
                row.setEffectMissionCompletionRateWeight(v);
                break;
            case "致盲率得分":
                row.setEffectBlindRateWeight(v);
                break;
            default:
                log.warn("未映射的指标名，跳过分列写入: {}", indicatorName);
        }
    }

    private static BigDecimal toBd(Double v) {
        if (v == null || !Double.isFinite(v)) {
            return null;
        }
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
    }
}
