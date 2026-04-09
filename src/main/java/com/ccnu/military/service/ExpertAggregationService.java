package com.ccnu.military.service;

import com.ccnu.military.dto.ExpertAggregationResult;
import com.ccnu.military.dto.ExpertAggregationResult.CvDataResult;
import com.ccnu.military.dto.ExpertAggregationResult.DataSummary;
import com.ccnu.military.dto.ExpertAggregationResult.ExpertParticipant;
import com.ccnu.military.dto.ExpertAggregationResult.IndicatorCvItem;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.entity.ExpertAhpIndividualWeights;
import com.ccnu.military.repository.ExpertAhpIndividualWeightsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 专家集结 AHP 聚合服务 - 计算各专家权重的变异系数 CV
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertAggregationService {

    private final ExpertAhpIndividualWeightsRepository weightsRepository;
    private final EquipmentAhpScoreService equipmentAhpScoreService;
    private final EquipmentAhpService equipmentAhpService;

    /** 一级维度编码映射 */
    private static final Map<String, String> DIMENSION_CODE_MAP = Map.of(
            "安全性", "security",
            "可靠性", "reliability",
            "传输能力", "transmission",
            "抗干扰能力", "anti_jamming",
            "效能影响", "effect"
    );

    /** 二级指标定义（indicatorCode -> IndicatorInfo） */
    private static final Map<String, IndicatorInfo> INDICATOR_MAP;

    static {
        Map<String, IndicatorInfo> map = new LinkedHashMap<>();
        // 安全性
        map.put("security_key_leakage", new IndicatorInfo("密钥泄露得分", "安全性"));
        map.put("security_detected_probability", new IndicatorInfo("被侦察得分", "安全性"));
        map.put("security_interception_resistance", new IndicatorInfo("抗拦截得分", "安全性"));
        // 可靠性
        map.put("reliability_crash_rate", new IndicatorInfo("崩溃比例得分", "可靠性"));
        map.put("reliability_recovery_capability", new IndicatorInfo("恢复能力得分", "可靠性"));
        map.put("reliability_communication_availability", new IndicatorInfo("通信可用得分", "可靠性"));
        // 传输能力
        map.put("transmission_bandwidth", new IndicatorInfo("带宽得分", "传输能力"));
        map.put("transmission_call_setup_time", new IndicatorInfo("呼叫建立得分", "传输能力"));
        map.put("transmission_transmission_delay", new IndicatorInfo("传输时延得分", "传输能力"));
        map.put("transmission_bit_error_rate", new IndicatorInfo("误码率得分", "传输能力"));
        map.put("transmission_throughput", new IndicatorInfo("吞吐量得分", "传输能力"));
        map.put("transmission_spectral_efficiency", new IndicatorInfo("频谱效率得分", "传输能力"));
        // 抗干扰能力
        map.put("anti_jamming_sinr", new IndicatorInfo("信干噪比得分", "抗干扰能力"));
        map.put("anti_jamming_anti_jamming_margin", new IndicatorInfo("抗干扰余量得分", "抗干扰能力"));
        map.put("anti_jamming_communication_distance", new IndicatorInfo("通信距离得分", "抗干扰能力"));
        // 效能影响
        map.put("effect_damage_rate", new IndicatorInfo("战损率得分", "效能影响"));
        map.put("effect_mission_completion_rate", new IndicatorInfo("任务完成率得分", "效能影响"));
        map.put("effect_blind_rate", new IndicatorInfo("致盲率得分", "效能影响"));
        INDICATOR_MAP = Collections.unmodifiableMap(map);
    }

    /** 一级维度权重字段映射（fieldName -> getter method） */
    private static final Map<String, String> DIMENSION_FIELD_MAP = Map.of(
            "security", "securityWeight",
            "reliability", "reliabilityWeight",
            "transmission", "transmissionWeight",
            "anti_jamming", "antiJammingWeight",
            "effect", "effectWeight"
    );

    /**
     * 获取完整的聚合结果
     */
    public ExpertAggregationResult getAggregationResult() {
        // 1. 获取所有专家权重数据
        List<ExpertAhpIndividualWeights> allWeights = weightsRepository.findAll();
        if (allWeights.isEmpty()) {
            log.warn("expert_ahp_individual_weights 表为空");
            return createEmptyResult();
        }

        // 2. 构建专家权重数据列表
        List<ExpertWeightData> expertDataList = buildExpertDataList(allWeights);

        // 3. 计算所有数据的CV
        CvDataResult allResult = calculateCvForData(expertDataList, "所有数据");

        // 4. 去除离群点后计算CV
        List<ExpertWeightData> filteredExtremeList = removeOutliers(expertDataList);
        CvDataResult filteredExtremeResult = calculateCvForData(filteredExtremeList, "去除极端值");

        // 5. 构建摘要
        DataSummary summary = buildSummary(allWeights.size(), allResult, filteredExtremeResult);

        ExpertAggregationResult result = new ExpertAggregationResult();
        result.setAllDataResult(allResult);
        result.setFilteredExtremeResult(filteredExtremeResult);
        result.setSummary(summary);

        attachEquipmentCvAnalysis(allResult, filteredExtremeResult);

        log.info("专家集结CV计算完成: 总专家数={}, 所有数据量={}, 去除极端值后数据量={}",
                allWeights.size(), allResult.getDataCount(), filteredExtremeResult.getDataCount());

        return result;
    }

    /**
     * 获取所有专家权重数据（不含筛选）
     */
    public List<ExpertAhpIndividualWeights> getAllExpertWeights() {
        return weightsRepository.findAll();
    }

    /**
     * 构建专家权重数据列表
     */
    private List<ExpertWeightData> buildExpertDataList(List<ExpertAhpIndividualWeights> weightsList) {

        List<ExpertWeightData> dataList = new ArrayList<>();
        for (ExpertAhpIndividualWeights w : weightsList) {
            ExpertWeightData data = new ExpertWeightData();
            data.expertId = w.getExpertId();
            data.expertName = w.getExpertName();

            // 一级维度权重
            data.dimensionWeights.put("security", getDoubleValue(w.getSecurityWeight()));
            data.dimensionWeights.put("reliability", getDoubleValue(w.getReliabilityWeight()));
            data.dimensionWeights.put("transmission", getDoubleValue(w.getTransmissionWeight()));
            data.dimensionWeights.put("anti_jamming", getDoubleValue(w.getAntiJammingWeight()));
            data.dimensionWeights.put("effect", getDoubleValue(w.getEffectWeight()));

            // 二级指标权重
            data.indicatorWeights.put("security_key_leakage", getDoubleValue(w.getSecurityKeyLeakageWeight()));
            data.indicatorWeights.put("security_detected_probability", getDoubleValue(w.getSecurityDetectedProbabilityWeight()));
            data.indicatorWeights.put("security_interception_resistance", getDoubleValue(w.getSecurityInterceptionResistanceWeight()));
            data.indicatorWeights.put("reliability_crash_rate", getDoubleValue(w.getReliabilityCrashRateWeight()));
            data.indicatorWeights.put("reliability_recovery_capability", getDoubleValue(w.getReliabilityRecoveryCapabilityWeight()));
            data.indicatorWeights.put("reliability_communication_availability", getDoubleValue(w.getReliabilityCommunicationAvailabilityWeight()));
            data.indicatorWeights.put("transmission_bandwidth", getDoubleValue(w.getTransmissionBandwidthWeight()));
            data.indicatorWeights.put("transmission_call_setup_time", getDoubleValue(w.getTransmissionCallSetupTimeWeight()));
            data.indicatorWeights.put("transmission_transmission_delay", getDoubleValue(w.getTransmissionTransmissionDelayWeight()));
            data.indicatorWeights.put("transmission_bit_error_rate", getDoubleValue(w.getTransmissionBitErrorRateWeight()));
            data.indicatorWeights.put("transmission_throughput", getDoubleValue(w.getTransmissionThroughputWeight()));
            data.indicatorWeights.put("transmission_spectral_efficiency", getDoubleValue(w.getTransmissionSpectralEfficiencyWeight()));
            data.indicatorWeights.put("anti_jamming_sinr", getDoubleValue(w.getAntiJammingSinrWeight()));
            data.indicatorWeights.put("anti_jamming_anti_jamming_margin", getDoubleValue(w.getAntiJammingAntiJammingMarginWeight()));
            data.indicatorWeights.put("anti_jamming_communication_distance", getDoubleValue(w.getAntiJammingCommunicationDistanceWeight()));
            data.indicatorWeights.put("effect_damage_rate", getDoubleValue(w.getEffectDamageRateWeight()));
            data.indicatorWeights.put("effect_mission_completion_rate", getDoubleValue(w.getEffectMissionCompletionRateWeight()));
            data.indicatorWeights.put("effect_blind_rate", getDoubleValue(w.getEffectBlindRateWeight()));

            dataList.add(data);
        }
        return dataList;
    }

    /**
     * 去除离群点（对每个指标，计算当前集合的中心点距离，删除距离最远的专家）
     * 简化实现：对于整体离群判断，计算每个专家到中心点的欧氏距离，删除距离最大的
     */
    private List<ExpertWeightData> removeOutliers(List<ExpertWeightData> dataList) {
        if (dataList.size() <= 2) {
            return new ArrayList<>(dataList);
        }

        // 计算每个维度权重的中心点
        Map<String, Double> centers = new HashMap<>();
        for (String dim : DIMENSION_FIELD_MAP.keySet()) {
            double mean = dataList.stream()
                    .mapToDouble(d -> d.dimensionWeights.getOrDefault(dim, 0.0))
                    .filter(v -> v > 0)
                    .average().orElse(0.0);
            centers.put(dim, mean);
        }

        // 计算每个专家到中心点的距离
        List<ExpertDistance> distances = new ArrayList<>();
        for (ExpertWeightData data : dataList) {
            double dist = 0;
            for (Map.Entry<String, Double> entry : centers.entrySet()) {
                double v = data.dimensionWeights.getOrDefault(entry.getKey(), 0.0);
                double diff = v - entry.getValue();
                dist += diff * diff;
            }
            dist = Math.sqrt(dist);
            distances.add(new ExpertDistance(data, dist));
        }

        // 按距离降序排序
        distances.sort((a, b) -> Double.compare(b.distance, a.distance));

        // 删除距离最远的10%（至少删除1个）
        int removeCount = Math.max(1, (int) Math.ceil(dataList.size() * 0.1));
        Set<Long> removeIds = new HashSet<>();
        for (int i = 0; i < removeCount && i < distances.size(); i++) {
            removeIds.add(distances.get(i).data.expertId);
        }

        log.debug("去除离群点，删除专家IDs: {}", removeIds);
        return dataList.stream()
                .filter(d -> !removeIds.contains(d.expertId))
                .collect(Collectors.toList());
    }

    /**
     * 对给定数据计算CV
     */
    private CvDataResult calculateCvForData(List<ExpertWeightData> dataList, String method) {
        List<Long> expertIds = dataList.stream().map(d -> d.expertId).collect(Collectors.toList());
        List<ExpertParticipant> participants = dataList.stream()
                .map(d -> new ExpertParticipant(d.expertId, d.expertName != null ? d.expertName : ""))
                .collect(Collectors.toList());

        // 计算一级维度CV
        List<IndicatorCvItem> dimensionCvs = new ArrayList<>();
        for (Map.Entry<String, String> entry : DIMENSION_FIELD_MAP.entrySet()) {
            String code = entry.getKey();
            String dimName = getDimensionNameByCode(code);
            List<Double> values = dataList.stream()
                    .map(d -> d.dimensionWeights.getOrDefault(code, 0.0))
                    .filter(v -> v > 0)
                    .collect(Collectors.toList());

            if (!values.isEmpty()) {
                dimensionCvs.add(createCvItem(code, dimName, dimName, values));
            }
        }

        // 计算二级指标CV
        List<IndicatorCvItem> indicatorCvs = new ArrayList<>();
        for (Map.Entry<String, IndicatorInfo> entry : INDICATOR_MAP.entrySet()) {
            String code = entry.getKey();
            IndicatorInfo info = entry.getValue();
            List<Double> values = dataList.stream()
                    .map(d -> d.indicatorWeights.getOrDefault(code, 0.0))
                    .filter(v -> v > 0)
                    .collect(Collectors.toList());

            if (!values.isEmpty()) {
                indicatorCvs.add(createCvItem(code, info.name, info.dimension, values));
            }
        }

        CvDataResult result = new CvDataResult();
        result.setDimensionCvs(dimensionCvs);
        result.setIndicatorCvs(indicatorCvs);
        result.setDataCount(dataList.size());
        result.setMethod(method);
        result.setExpertIds(expertIds);
        result.setParticipatingExperts(participants);
        return result;
    }

    /**
     * 创建CV分析项
     */
    private IndicatorCvItem createCvItem(String code, String name, String dimension, List<Double> values) {
        double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double stdDev = calculateStdDev(values, mean);
        double cv = (mean > 0) ? (stdDev / mean) * 100 : 0.0;

        IndicatorCvItem item = new IndicatorCvItem();
        item.setIndicatorCode(code);
        item.setIndicatorName(name);
        item.setDimension(dimension);
        item.setWeightValues(values);
        item.setMean(mean);
        item.setStdDev(stdDev);
        item.setCv(cv);
        item.setConsistencyLevel(getConsistencyLevel(cv));
        item.setLevelType(getLevelType(cv));
        return item;
    }

    /**
     * 计算标准差
     */
    private double calculateStdDev(List<Double> values, double mean) {
        if (values.size() <= 1) {
            return 0.0;
        }
        double sumSquaredDiff = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .sum();
        return Math.sqrt(sumSquaredDiff / (values.size() - 1));
    }

    /**
     * 获取一致性等级
     */
    private String getConsistencyLevel(double cv) {
        if (cv <= 15) {
            return "高度一致";
        } else if (cv <= 25) {
            return "较为一致";
        } else if (cv <= 35) {
            return "轻度分歧";
        } else {
            return "严重分歧";
        }
    }

    /**
     * 获取等级标签类型
     */
    private String getLevelType(double cv) {
        if (cv <= 15) {
            return "success";
        } else if (cv <= 25) {
            return "warning";
        } else if (cv <= 35) {
            return "warning";
        } else {
            return "danger";
        }
    }

    /**
     * 构建摘要
     */
    private DataSummary buildSummary(int totalExperts, CvDataResult allResult, CvDataResult extremeResult) {
        DataSummary summary = new DataSummary();
        summary.setTotalExperts(totalExperts);
        summary.setAllDataCount(allResult.getDataCount());
        summary.setFilteredExtremeCount(extremeResult.getDataCount());

        summary.setAllHighConsistency(countByLevel(allResult, "高度一致"));
        summary.setAllSevereDisagreement(countByLevel(allResult, "严重分歧"));
        summary.setExtremeHighConsistency(countByLevel(extremeResult, "高度一致"));
        summary.setExtremeSevereDisagreement(countByLevel(extremeResult, "严重分歧"));

        return summary;
    }

    private int countByLevel(CvDataResult result, String level) {
        int count = 0;
        for (IndicatorCvItem item : result.getDimensionCvs()) {
            if (level.equals(item.getConsistencyLevel())) count++;
        }
        for (IndicatorCvItem item : result.getIndicatorCvs()) {
            if (level.equals(item.getConsistencyLevel())) count++;
        }
        return count;
    }

    /**
     * 根据维度编码获取维度名称
     */
    private String getDimensionNameByCode(String code) {
        return DIMENSION_CODE_MAP.entrySet().stream()
                .filter(e -> e.getValue().equals(code))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(code);
    }

    private Double getDoubleValue(BigDecimal bd) {
        return bd != null ? bd.doubleValue() : 0.0;
    }

    private ExpertAggregationResult createEmptyResult() {
        ExpertAggregationResult result = new ExpertAggregationResult();
        CvDataResult empty = new CvDataResult();
        empty.setEquipmentDimensionCvs(Collections.emptyList());
        empty.setEquipmentIndicatorCvs(Collections.emptyList());
        empty.setEquipmentParticipatingExperts(Collections.emptyList());
        result.setAllDataResult(empty);
        CvDataResult empty2 = new CvDataResult();
        empty2.setEquipmentDimensionCvs(Collections.emptyList());
        empty2.setEquipmentIndicatorCvs(Collections.emptyList());
        empty2.setEquipmentParticipatingExperts(Collections.emptyList());
        result.setFilteredExtremeResult(empty2);
        result.setSummary(new DataSummary());
        return result;
    }

    /**
     * 装备操作：从 expert_ahp_comparison_score（装备操作_ 前缀）现场算权重，再算二级指标 CV
     */
    private void attachEquipmentCvAnalysis(CvDataResult allResult, CvDataResult filteredResult) {
        List<ExpertWeightData> full = buildEquipmentExpertWeightDataList();
        allResult.setEquipmentDimensionCvs(computeEquipmentDimensionCvs(full));
        allResult.setEquipmentIndicatorCvs(computeEquipmentIndicatorCvs(full));
        allResult.setEquipmentParticipatingExperts(toEquipmentParticipants(full));
        List<ExpertWeightData> fe = removeEquipmentOutliers(full);
        filteredResult.setEquipmentDimensionCvs(computeEquipmentDimensionCvs(fe));
        filteredResult.setEquipmentIndicatorCvs(computeEquipmentIndicatorCvs(fe));
        filteredResult.setEquipmentParticipatingExperts(toEquipmentParticipants(fe));
    }

    private List<ExpertParticipant> toEquipmentParticipants(List<ExpertWeightData> list) {
        return list.stream()
                .map(d -> new ExpertParticipant(d.expertId, d.expertName != null ? d.expertName : ""))
                .collect(Collectors.toList());
    }

    private List<ExpertWeightData> buildEquipmentExpertWeightDataList() {
        List<Long> ids = equipmentAhpScoreService.listExpertIdsWithEquipmentScores();
        List<ExpertWeightData> out = new ArrayList<>();
        for (Long id : ids) {
            MatrixCalculationResult calc = equipmentAhpScoreService.calculateFromStoredScores(id);
            if (calc == null || calc.getCombinedWeights() == null || calc.getCombinedWeights().isEmpty()) {
                continue;
            }
            ExpertWeightData data = new ExpertWeightData();
            data.expertId = id;
            List<ExpertAhpComparisonScore> rows = equipmentAhpScoreService.listByExpert(id);
            data.expertName = rows.isEmpty() ? "" : Optional.ofNullable(rows.get(0).getExpertName()).orElse("");
            if (calc.getDimensionResult() != null && calc.getDimensionResult().getWeightMap() != null) {
                for (Map.Entry<String, Double> e : calc.getDimensionResult().getWeightMap().entrySet()) {
                    data.dimensionWeights.put("eq:" + e.getKey(), e.getValue());
                }
            }
            for (MatrixCalculationResult.CombinedWeight cw : calc.getCombinedWeights()) {
                String code = "eq:" + cw.getDimension() + ":" + cw.getIndicator();
                data.indicatorWeights.put(code, cw.getCombinedWeight());
            }
            out.add(data);
        }
        return out;
    }

    private List<IndicatorCvItem> computeEquipmentDimensionCvs(List<ExpertWeightData> dataList) {
        if (dataList.isEmpty()) {
            return Collections.emptyList();
        }
        List<String> dims = equipmentAhpService.getDimensions();
        List<IndicatorCvItem> out = new ArrayList<>();
        for (String dim : dims) {
            String code = "eq:" + dim;
            List<Double> values = dataList.stream()
                    .map(d -> d.dimensionWeights.getOrDefault(code, 0.0))
                    .filter(v -> v > 0)
                    .collect(Collectors.toList());
            if (!values.isEmpty()) {
                out.add(createCvItem(code, dim, dim, values));
            }
        }
        return out;
    }

    private List<IndicatorCvItem> computeEquipmentIndicatorCvs(List<ExpertWeightData> dataList) {
        if (dataList.isEmpty()) {
            return Collections.emptyList();
        }
        List<IndicatorCvItem> indicatorCvs = new ArrayList<>();
        List<String> dims = equipmentAhpService.getDimensions();
        Map<String, List<Map<String, Object>>> dimInds = equipmentAhpService.getDimensionIndicators(null);
        for (String dim : dims) {
            List<Map<String, Object>> inds = dimInds.getOrDefault(dim, Collections.emptyList());
            for (Map<String, Object> ind : inds) {
                String name = (String) ind.get("name");
                if (name == null) {
                    continue;
                }
                String code = "eq:" + dim + ":" + name;
                List<Double> values = dataList.stream()
                        .map(d -> d.indicatorWeights.getOrDefault(code, 0.0))
                        .filter(v -> v > 0)
                        .collect(Collectors.toList());
                if (!values.isEmpty()) {
                    indicatorCvs.add(createCvItem(code, name, dim, values));
                }
            }
        }
        return indicatorCvs;
    }

    private List<ExpertWeightData> removeEquipmentOutliers(List<ExpertWeightData> dataList) {
        if (dataList.size() <= 2) {
            return new ArrayList<>(dataList);
        }
        List<String> eqDimKeys = equipmentAhpService.getDimensions().stream()
                .map(d -> "eq:" + d)
                .collect(Collectors.toList());
        Map<String, Double> centers = new HashMap<>();
        for (String dk : eqDimKeys) {
            double mean = dataList.stream()
                    .mapToDouble(d -> d.dimensionWeights.getOrDefault(dk, 0.0))
                    .filter(v -> v > 0)
                    .average()
                    .orElse(0.0);
            centers.put(dk, mean);
        }
        List<ExpertDistance> distances = new ArrayList<>();
        for (ExpertWeightData data : dataList) {
            double dist = 0;
            for (Map.Entry<String, Double> entry : centers.entrySet()) {
                double v = data.dimensionWeights.getOrDefault(entry.getKey(), 0.0);
                double diff = v - entry.getValue();
                dist += diff * diff;
            }
            dist = Math.sqrt(dist);
            distances.add(new ExpertDistance(data, dist));
        }
        distances.sort((a, b) -> Double.compare(b.distance, a.distance));
        int removeCount = Math.max(1, (int) Math.ceil(dataList.size() * 0.1));
        Set<Long> removeIds = new HashSet<>();
        for (int i = 0; i < removeCount && i < distances.size(); i++) {
            removeIds.add(distances.get(i).data.expertId);
        }
        return dataList.stream().filter(d -> !removeIds.contains(d.expertId)).collect(Collectors.toList());
    }

    // ============ 内部类 ============

    /**
     * 专家权重数据
     */
    private static class ExpertWeightData {
        Long expertId;
        String expertName;
        Map<String, Double> dimensionWeights = new HashMap<>();
        Map<String, Double> indicatorWeights = new HashMap<>();
    }

    /**
     * 专家距离
     */
    private static class ExpertDistance {
        ExpertWeightData data;
        double distance;

        ExpertDistance(ExpertWeightData data, double distance) {
            this.data = data;
            this.distance = distance;
        }
    }

    /**
     * 指标信息
     */
    private static class IndicatorInfo {
        String name;
        String dimension;

        IndicatorInfo(String name, String dimension) {
            this.name = name;
            this.dimension = dimension;
        }
    }
}
