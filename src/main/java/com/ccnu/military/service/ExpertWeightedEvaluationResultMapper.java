package com.ccnu.military.service;

import com.ccnu.military.dto.CollectiveCalculateResponse;
import com.ccnu.military.entity.ExpertWeightedEvaluationResult;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 集结加权评估结果实体 ↔ DTO 映射（写入 expert_weighted_evaluation_result）
 */
public final class ExpertWeightedEvaluationResultMapper {

    private ExpertWeightedEvaluationResultMapper() {}

    public static ExpertWeightedEvaluationResult toRow(
            CollectiveCalculateResponse response,
            CollectiveCalculateResponse.OperationResult op,
            String expertWeightsJson) {
        ExpertWeightedEvaluationResult e = new ExpertWeightedEvaluationResult();
        e.setEvaluationId(response.getEvaluationId());
        e.setOperationId(op.getOperationId());
        e.setExpertCount(response.getExpertCount());
        if (response.getExpertIds() != null && !response.getExpertIds().isEmpty()) {
            e.setExpertIds(
                    response.getExpertIds().stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
        e.setExpertWeightsJson(expertWeightsJson);

        Map<String, BigDecimal> cr = response.getCrResults();
        if (cr != null) {
            e.setCrDim(cr.get("dim"));
            e.setCrSecurity(cr.get("安全性"));
            e.setCrReliability(cr.get("可靠性"));
            e.setCrTransmission(cr.get("传输能力"));
            e.setCrAntiJamming(cr.get("抗干扰能力"));
            e.setCrEffect(cr.get("效能影响"));
        }
        Map<String, BigDecimal> dw = response.getDimensionWeights();
        if (dw != null) {
            e.setDimWeightSecurity(dw.get("安全性"));
            e.setDimWeightReliability(dw.get("可靠性"));
            e.setDimWeightTransmission(dw.get("传输能力"));
            e.setDimWeightAntiJamming(dw.get("抗干扰能力"));
            e.setDimWeightEffect(dw.get("效能影响"));
        }
        Map<String, BigDecimal> iw = response.getIndicatorWeights();
        if (iw != null) {
            e.setIndWeightKeyLeakage(iw.get("密钥泄露得分"));
            e.setIndWeightDetectedProbability(iw.get("被侦察得分"));
            e.setIndWeightInterceptionResistance(iw.get("抗拦截得分"));
            e.setIndWeightCrashRate(iw.get("崩溃比例得分"));
            e.setIndWeightRecoveryCapability(iw.get("恢复能力得分"));
            e.setIndWeightCommunicationAvailability(iw.get("通信可用得分"));
            e.setIndWeightBandwidth(iw.get("带宽得分"));
            e.setIndWeightCallSetupTime(iw.get("呼叫建立得分"));
            e.setIndWeightTransmissionDelay(iw.get("传输时延得分"));
            e.setIndWeightBitErrorRate(iw.get("误码率得分"));
            e.setIndWeightThroughput(iw.get("吞吐量得分"));
            e.setIndWeightSpectralEfficiency(iw.get("频谱效率得分"));
            e.setIndWeightSinr(iw.get("信干噪比得分"));
            e.setIndWeightAntiJammingMargin(iw.get("抗干扰余量得分"));
            e.setIndWeightCommunicationDistance(iw.get("通信距离得分"));
            e.setIndWeightDamageRate(iw.get("战损率得分"));
            e.setIndWeightMissionCompletionRate(iw.get("任务完成率得分"));
            e.setIndWeightBlindRate(iw.get("致盲率得分"));
        }

        if (op.getTotalScore() != null) {
            e.setTotalScore(op.getTotalScore());
        }
        Map<String, BigDecimal> ds = op.getDimensionScores();
        if (ds != null) {
            e.setScoreSecurity(ds.get("安全性"));
            e.setScoreReliability(ds.get("可靠性"));
            e.setScoreTransmission(ds.get("传输能力"));
            e.setScoreAntiJamming(ds.get("抗干扰能力"));
            e.setScoreEffect(ds.get("效能影响"));
        }
        Map<String, BigDecimal> ind = op.getIndicatorScores();
        if (ind != null) {
            e.setScoreKeyLeakage(ind.get("密钥泄露得分"));
            e.setScoreDetectedProbability(ind.get("被侦察得分"));
            e.setScoreInterceptionResistance(ind.get("抗拦截得分"));
            e.setScoreCrashRate(ind.get("崩溃比例得分"));
            e.setScoreRecoveryCapability(ind.get("恢复能力得分"));
            e.setScoreCommunicationAvailability(ind.get("通信可用得分"));
            e.setScoreBandwidth(ind.get("带宽得分"));
            e.setScoreCallSetupTime(ind.get("呼叫建立得分"));
            e.setScoreTransmissionDelay(ind.get("传输时延得分"));
            e.setScoreBitErrorRate(ind.get("误码率得分"));
            e.setScoreThroughput(ind.get("吞吐量得分"));
            e.setScoreSpectralEfficiency(ind.get("频谱效率得分"));
            e.setScoreSinr(ind.get("信干噪比得分"));
            e.setScoreAntiJammingMargin(ind.get("抗干扰余量得分"));
            e.setScoreCommunicationDistance(ind.get("通信距离得分"));
            e.setScoreDamageRate(ind.get("战损率得分"));
            e.setScoreMissionCompletionRate(ind.get("任务完成率得分"));
            e.setScoreBlindRate(ind.get("致盲率得分"));
        }
        return e;
    }

    public static CollectiveCalculateResponse toResponse(List<ExpertWeightedEvaluationResult> rows) {
        if (rows == null || rows.isEmpty()) {
            return null;
        }
        ExpertWeightedEvaluationResult first = rows.get(0);
        CollectiveCalculateResponse out = new CollectiveCalculateResponse();
        out.setEvaluationId(first.getEvaluationId());
        out.setExpertCount(first.getExpertCount());
        if (first.getExpertIds() != null && !first.getExpertIds().isBlank()) {
            List<Long> ids = Arrays.stream(first.getExpertIds().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::valueOf)
                    .collect(Collectors.toList());
            out.setExpertIds(ids);
        }

        Map<String, BigDecimal> cr = new LinkedHashMap<>();
        cr.put("dim", first.getCrDim());
        cr.put("安全性", first.getCrSecurity());
        cr.put("可靠性", first.getCrReliability());
        cr.put("传输能力", first.getCrTransmission());
        cr.put("抗干扰能力", first.getCrAntiJamming());
        cr.put("效能影响", first.getCrEffect());
        out.setCrResults(cr);

        Map<String, BigDecimal> dw = new LinkedHashMap<>();
        dw.put("安全性", first.getDimWeightSecurity());
        dw.put("可靠性", first.getDimWeightReliability());
        dw.put("传输能力", first.getDimWeightTransmission());
        dw.put("抗干扰能力", first.getDimWeightAntiJamming());
        dw.put("效能影响", first.getDimWeightEffect());
        out.setDimensionWeights(dw);

        Map<String, BigDecimal> iw = new LinkedHashMap<>();
        iw.put("密钥泄露得分", first.getIndWeightKeyLeakage());
        iw.put("被侦察得分", first.getIndWeightDetectedProbability());
        iw.put("抗拦截得分", first.getIndWeightInterceptionResistance());
        iw.put("崩溃比例得分", first.getIndWeightCrashRate());
        iw.put("恢复能力得分", first.getIndWeightRecoveryCapability());
        iw.put("通信可用得分", first.getIndWeightCommunicationAvailability());
        iw.put("带宽得分", first.getIndWeightBandwidth());
        iw.put("呼叫建立得分", first.getIndWeightCallSetupTime());
        iw.put("传输时延得分", first.getIndWeightTransmissionDelay());
        iw.put("误码率得分", first.getIndWeightBitErrorRate());
        iw.put("吞吐量得分", first.getIndWeightThroughput());
        iw.put("频谱效率得分", first.getIndWeightSpectralEfficiency());
        iw.put("信干噪比得分", first.getIndWeightSinr());
        iw.put("抗干扰余量得分", first.getIndWeightAntiJammingMargin());
        iw.put("通信距离得分", first.getIndWeightCommunicationDistance());
        iw.put("战损率得分", first.getIndWeightDamageRate());
        iw.put("任务完成率得分", first.getIndWeightMissionCompletionRate());
        iw.put("致盲率得分", first.getIndWeightBlindRate());
        out.setIndicatorWeights(iw);
        out.setIndicatorWeightsByDimension(new LinkedHashMap<>());
        out.setCollectiveScores(new LinkedHashMap<>());

        List<CollectiveCalculateResponse.OperationResult> results = new ArrayList<>();
        for (ExpertWeightedEvaluationResult r : rows) {
            results.add(rowToOperationResult(r));
        }
        out.setResults(results);
        return out;
    }

    private static CollectiveCalculateResponse.OperationResult rowToOperationResult(ExpertWeightedEvaluationResult r) {
        CollectiveCalculateResponse.OperationResult op = new CollectiveCalculateResponse.OperationResult();
        op.setOperationId(r.getOperationId());
        op.setTotalScore(r.getTotalScore());
        Map<String, BigDecimal> ds = new LinkedHashMap<>();
        ds.put("安全性", r.getScoreSecurity());
        ds.put("可靠性", r.getScoreReliability());
        ds.put("传输能力", r.getScoreTransmission());
        ds.put("抗干扰能力", r.getScoreAntiJamming());
        ds.put("效能影响", r.getScoreEffect());
        op.setDimensionScores(ds);
        Map<String, BigDecimal> ind = new LinkedHashMap<>();
        ind.put("密钥泄露得分", r.getScoreKeyLeakage());
        ind.put("被侦察得分", r.getScoreDetectedProbability());
        ind.put("抗拦截得分", r.getScoreInterceptionResistance());
        ind.put("崩溃比例得分", r.getScoreCrashRate());
        ind.put("恢复能力得分", r.getScoreRecoveryCapability());
        ind.put("通信可用得分", r.getScoreCommunicationAvailability());
        ind.put("带宽得分", r.getScoreBandwidth());
        ind.put("呼叫建立得分", r.getScoreCallSetupTime());
        ind.put("传输时延得分", r.getScoreTransmissionDelay());
        ind.put("误码率得分", r.getScoreBitErrorRate());
        ind.put("吞吐量得分", r.getScoreThroughput());
        ind.put("频谱效率得分", r.getScoreSpectralEfficiency());
        ind.put("信干噪比得分", r.getScoreSinr());
        ind.put("抗干扰余量得分", r.getScoreAntiJammingMargin());
        ind.put("通信距离得分", r.getScoreCommunicationDistance());
        ind.put("战损率得分", r.getScoreDamageRate());
        ind.put("任务完成率得分", r.getScoreMissionCompletionRate());
        ind.put("致盲率得分", r.getScoreBlindRate());
        op.setIndicatorScores(ind);
        return op;
    }
}
