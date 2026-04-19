package com.ccnu.military.service;

import com.ccnu.military.dto.AhpIndividualResult;
import com.ccnu.military.dto.ExpertAggregationResult;
import com.ccnu.military.dto.ExpertAggregationResult.CvDataResult;
import com.ccnu.military.dto.ExpertAggregationResult.DataSummary;
import com.ccnu.military.dto.ExpertAggregationResult.ExpertParticipant;
import com.ccnu.military.dto.ExpertAggregationResult.IndicatorCvItem;
import com.ccnu.military.entity.ExpertAhpIndividualWeights;
import com.ccnu.military.repository.ExpertAhpIndividualWeightsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
    private final ObjectMapper objectMapper;

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
        attachUnifiedLeafCvAnalysis(allResult, filteredExtremeResult);

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
     * 从 JSON 列读取效能维度权重和叶子指标权重
     */
    private List<ExpertWeightData> buildExpertDataList(List<ExpertAhpIndividualWeights> weightsList) {
        List<ExpertWeightData> dataList = new ArrayList<>();
        for (ExpertAhpIndividualWeights w : weightsList) {
            ExpertWeightData data = new ExpertWeightData();
            data.expertId = w.getExpertId();
            data.expertName = w.getExpertName();

            // 目标：这里构建“已包含域间一级权重”的全局权重数据，供 CV 直接对比
            // - 效能维度全局权重：w_eff * w_dim
            // - 效能叶子全局权重：已在 eff_leaf_weights_json 中存为 globalWeight（= w_eff*w_dim*w_ind）

            double wEff = w.getEffDomainWeight() != null ? w.getEffDomainWeight().doubleValue() : 0.5;
            double wEq = w.getEqDomainWeight() != null ? w.getEqDomainWeight().doubleValue() : 0.5;

            // 效能维度（JSON: {维度中文名: 权重}，这里乘以 w_eff 后落到固定 code 上，保证前端兼容）
            Map<String, Double> effDimWeights = parseJsonMap(w.getEffDimWeightsJson());
            data.dimensionWeights.put("security", wEff * effDimWeights.getOrDefault("安全性", 0.0));
            data.dimensionWeights.put("reliability", wEff * effDimWeights.getOrDefault("可靠性", 0.0));
            data.dimensionWeights.put("transmission", wEff * effDimWeights.getOrDefault("传输能力", 0.0));
            data.dimensionWeights.put("anti_jamming", wEff * effDimWeights.getOrDefault("抗干扰能力", 0.0));
            data.dimensionWeights.put("effect", wEff * effDimWeights.getOrDefault("效能影响", 0.0));

            // 效能叶子（JSON list 已是全局权重 globalWeight；这里按既有 indicatorCode 落表）
            Map<String, Double> effLeaf = parseLeafWeightsByCode(w.getEffLeafWeightsJson());
            data.indicatorWeights.putAll(effLeaf);

            dataList.add(data);
        }
        return dataList;
    }

    /**
     * 将 eff_leaf_weights_json 解析为：indicatorCode -> globalWeight（已包含 w_eff）
     * 兼容前端既有固定 code（security_key_leakage 等）。
     */
    private Map<String, Double> parseLeafWeightsByCode(String json) {
        Map<String, Double> out = new LinkedHashMap<>();
        if (json == null || json.isBlank()) return out;
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> item : list) {
                String dim = String.valueOf(item.get("dim"));
                String indicator = String.valueOf(item.get("indicator"));
                Object weightObj = item.get("globalWeight");
                if (dim == null || indicator == null || weightObj == null) continue;
                double w = toDouble(weightObj);
                String code = mapEfficiencyIndicatorToCode(dim, indicator);
                if (code != null) {
                    out.put(code, w);
                }
            }
        } catch (Exception e) {
            log.warn("解析效能叶子权重 JSON 失败: {}", e.getMessage());
        }
        return out;
    }

    private String mapEfficiencyIndicatorToCode(String dimCn, String indicatorCn) {
        // 与 INDICATOR_MAP 保持一致的编码
        for (Map.Entry<String, IndicatorInfo> e : INDICATOR_MAP.entrySet()) {
            if (Objects.equals(e.getValue().dimension, dimCn) && Objects.equals(e.getValue().name, indicatorCn)) {
                return e.getKey();
            }
        }
        return null;
    }

    /**
     * 解析 JSON 格式的 Map
     */
    private Map<String, Double> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return new HashMap<>();
        }
        try {
            return objectMapper.readValue(json, new com.fasterxml.jackson.core.type.TypeReference<Map<String, Double>>() {});
        } catch (Exception e) {
            log.warn("解析 JSON Map 失败: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * 解析叶子权重 JSON（格式：[{dim,indicator,globalWeight}, ...]）
     */
    private Map<String, Double> parseLeafWeights(String json) {
        Map<String, Double> result = new HashMap<>();
        if (json == null || json.isBlank()) {
            return result;
        }
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> item : list) {
                String dim = String.valueOf(item.get("dim"));
                String indicator = String.valueOf(item.get("indicator"));
                Object weightObj = item.get("globalWeight");
                if (dim != null && indicator != null && weightObj != null) {
                    double weight = toDouble(weightObj);
                    result.put(dim + ":" + indicator, weight);
                }
            }
        } catch (Exception e) {
            log.warn("解析叶子权重 JSON 失败: {}", e.getMessage());
        }
        return result;
    }

    private double toDouble(Object obj) {
        if (obj == null) return 0.0;
        if (obj instanceof Number) return ((Number) obj).doubleValue();
        try {
            return Double.parseDouble(String.valueOf(obj));
        } catch (Exception e) {
            return 0.0;
        }
    }

    /**
     * 读取旧列（兼容已有数据）
     * 如果 JSON 列没有数据，则尝试从固定列读取
     */
    private void readLegacyColumns(ExpertAhpIndividualWeights w, ExpertWeightData data) {
        // 只保留基本信息，其他从 JSON 获取
        // 如果 JSON 数据已填充，则跳过旧列读取
        boolean hasDimData = !data.dimensionWeights.isEmpty();
        boolean hasIndicatorData = !data.indicatorWeights.isEmpty();

        if (!hasDimData) {
            // 从 ahp_result_json 解析维度权重
            AhpIndividualResult result = parseAhpResultJson(w.getAhpResultJson());
            if (result != null && result.getEffectiveness() != null
                    && result.getEffectiveness().getDimensionWeights() != null) {
                for (Map.Entry<String, Double> e : result.getEffectiveness().getDimensionWeights().entrySet()) {
                    data.dimensionWeights.put("eff:" + e.getKey(), e.getValue());
                }
            }
        }

        if (!hasIndicatorData) {
            // 从 ahp_result_json 解析指标权重
            AhpIndividualResult result = parseAhpResultJson(w.getAhpResultJson());
            if (result != null && result.getEffectiveness() != null
                    && result.getEffectiveness().getIndicators() != null) {
                for (Map.Entry<String, Map<String, Double>> dimEntry : result.getEffectiveness().getIndicators().entrySet()) {
                    for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                        data.indicatorWeights.put("eff:" + dimEntry.getKey() + ":" + indEntry.getKey(), indEntry.getValue());
                    }
                }
            }
        }
    }

    private AhpIndividualResult parseAhpResultJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readValue(json, AhpIndividualResult.class);
        } catch (Exception e) {
            log.warn("解析 ahp_result_json 失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 去除离群点（动态检测维度）
     */
    private List<ExpertWeightData> removeOutliers(List<ExpertWeightData> dataList) {
        if (dataList.size() <= 2) {
            return new ArrayList<>(dataList);
        }

        // 从数据中动态获取维度键
        Set<String> dimKeys = new HashSet<>();
        for (ExpertWeightData d : dataList) {
            for (String key : d.dimensionWeights.keySet()) {
                if (key.startsWith("eff:")) {
                    dimKeys.add(key);
                }
            }
        }

        // 计算每个维度权重的中心点
        Map<String, Double> centers = new HashMap<>();
        for (String dim : dimKeys) {
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
     * 对给定数据计算CV（效能侧，已包含域间一级权重）
     * <p>
     * - 维度：security/reliability/...（值为 w_eff*w_dim）\n
     * - 指标：security_key_leakage/...（值为全局权重 w_eff*w_dim*w_ind）
     */
    private CvDataResult calculateCvForData(List<ExpertWeightData> dataList, String method) {
        List<Long> expertIds = dataList.stream().map(d -> d.expertId).collect(Collectors.toList());
        List<ExpertParticipant> participants = dataList.stream()
                .map(d -> new ExpertParticipant(d.expertId, d.expertName != null ? d.expertName : ""))
                .collect(Collectors.toList());

        // 维度层 CV（固定 5 维）
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

        // 指标层 CV（按当前定义的效能叶子集合）
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
     * 装备操作：直接从 expert_ahp_individual_weights 快照中读取（已包含域间一级权重）
     */
    private void attachEquipmentCvAnalysis(CvDataResult allResult, CvDataResult filteredResult) {
        List<ExpertWeightData> full = buildEquipmentExpertWeightDataListFromSnapshots();
        allResult.setEquipmentDimensionCvs(computeEquipmentDimensionCvsDynamic(full));
        allResult.setEquipmentIndicatorCvs(computeEquipmentIndicatorCvsDynamic(full));
        allResult.setEquipmentParticipatingExperts(toEquipmentParticipants(full));

        List<ExpertWeightData> fe = removeEquipmentOutliersDynamic(full);
        filteredResult.setEquipmentDimensionCvs(computeEquipmentDimensionCvsDynamic(fe));
        filteredResult.setEquipmentIndicatorCvs(computeEquipmentIndicatorCvsDynamic(fe));
        filteredResult.setEquipmentParticipatingExperts(toEquipmentParticipants(fe));
    }

    private List<ExpertParticipant> toEquipmentParticipants(List<ExpertWeightData> list) {
        return list.stream()
                .map(d -> new ExpertParticipant(d.expertId, d.expertName != null ? d.expertName : ""))
                .collect(Collectors.toList());
    }

    private List<ExpertWeightData> buildEquipmentExpertWeightDataListFromSnapshots() {
        List<ExpertAhpIndividualWeights> rows = weightsRepository.findAll();
        List<ExpertWeightData> out = new ArrayList<>();
        for (ExpertAhpIndividualWeights w : rows) {
            if ((w.getEqDimWeightsJson() == null || w.getEqDimWeightsJson().isBlank())
                    && (w.getEqLeafWeightsJson() == null || w.getEqLeafWeightsJson().isBlank())) {
                continue;
            }
            ExpertWeightData data = new ExpertWeightData();
            data.expertId = w.getExpertId();
            data.expertName = w.getExpertName();

            double wEq = w.getEqDomainWeight() != null ? w.getEqDomainWeight().doubleValue() : 0.5;

            // 维度全局权重：w_eq * w_dim
            Map<String, Double> dimWeights = parseJsonMap(w.getEqDimWeightsJson());
            for (Map.Entry<String, Double> e : dimWeights.entrySet()) {
                data.dimensionWeights.put("eq:" + e.getKey(), wEq * e.getValue());
            }

            // 叶子全局权重：已在 JSON 中保存 globalWeight
            if (w.getEqLeafWeightsJson() != null && !w.getEqLeafWeightsJson().isBlank()) {
                try {
                    List<Map<String, Object>> list = objectMapper.readValue(w.getEqLeafWeightsJson(),
                            new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
                    for (Map<String, Object> item : list) {
                        String dim = String.valueOf(item.get("dim"));
                        String indicator = String.valueOf(item.get("indicator"));
                        Object gw = item.get("globalWeight");
                        if (dim == null || indicator == null || gw == null) continue;
                        data.indicatorWeights.put("eq:" + dim + ":" + indicator, toDouble(gw));
                    }
                } catch (Exception ex) {
                    log.warn("解析装备叶子权重 JSON 失败 expertId={}: {}", w.getExpertId(), ex.getMessage());
                }
            }

            out.add(data);
        }
        return out;
    }

    private List<IndicatorCvItem> computeEquipmentDimensionCvsDynamic(List<ExpertWeightData> dataList) {
        if (dataList.isEmpty()) return Collections.emptyList();
        Set<String> keys = new HashSet<>();
        for (ExpertWeightData d : dataList) {
            for (String k : d.dimensionWeights.keySet()) {
                if (k.startsWith("eq:")) keys.add(k);
            }
        }
        List<IndicatorCvItem> out = new ArrayList<>();
        for (String code : keys) {
            String dimName = code.substring("eq:".length());
            List<Double> values = dataList.stream()
                    .map(d -> d.dimensionWeights.getOrDefault(code, 0.0))
                    .filter(v -> v > 0)
                    .collect(Collectors.toList());
            if (!values.isEmpty()) out.add(createCvItem(code, dimName, dimName, values));
        }
        return out;
    }

    private List<IndicatorCvItem> computeEquipmentIndicatorCvsDynamic(List<ExpertWeightData> dataList) {
        if (dataList.isEmpty()) return Collections.emptyList();
        Set<String> keys = new HashSet<>();
        for (ExpertWeightData d : dataList) {
            for (String k : d.indicatorWeights.keySet()) {
                if (k.startsWith("eq:")) keys.add(k);
            }
        }
        List<IndicatorCvItem> out = new ArrayList<>();
        for (String code : keys) {
            String rest = code.substring("eq:".length());
            String[] parts = rest.split(":", 2);
            if (parts.length < 2) continue;
            String dimName = parts[0];
            String indName = parts[1];
            List<Double> values = dataList.stream()
                    .map(d -> d.indicatorWeights.getOrDefault(code, 0.0))
                    .filter(v -> v > 0)
                    .collect(Collectors.toList());
            if (!values.isEmpty()) out.add(createCvItem(code, indName, dimName, values));
        }
        return out;
    }

    private List<ExpertWeightData> removeEquipmentOutliersDynamic(List<ExpertWeightData> dataList) {
        if (dataList.size() <= 2) return new ArrayList<>(dataList);
        Set<String> dimKeys = new HashSet<>();
        for (ExpertWeightData d : dataList) {
            for (String k : d.dimensionWeights.keySet()) {
                if (k.startsWith("eq:")) dimKeys.add(k);
            }
        }
        Map<String, Double> centers = new HashMap<>();
        for (String dk : dimKeys) {
            double mean = dataList.stream()
                    .mapToDouble(d -> d.dimensionWeights.getOrDefault(dk, 0.0))
                    .filter(v -> v > 0)
                    .average().orElse(0.0);
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
            distances.add(new ExpertDistance(data, Math.sqrt(dist)));
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

    // ── 综合叶子全局权重 CV ───────────────────────────────────────────────

    /**
     * 综合叶子 CV：把每个专家的效能+装备叶子权重合并后，一起算 CV 和均值。
     * 权重已各自包含域间一级（globalWeight = w_domain * w_dim * w_ind）。
     */
    private void attachUnifiedLeafCvAnalysis(CvDataResult allResult, CvDataResult filteredResult) {
        List<ExpertAhpIndividualWeights> rows = weightsRepository.findAll();
        if (rows.isEmpty()) {
            allResult.setUnifiedLeafCvs(Collections.emptyList());
            filteredResult.setUnifiedLeafCvs(Collections.emptyList());
            return;
        }

        // 一次性把所有专家的叶子收集起来（按叶子 key 聚合）
        // 结构：Map<String leafKey, List<Double>>  →  key = "域:维度:指标"
        Map<String, List<Double>> allLeafValues = new LinkedHashMap<>();
        for (ExpertAhpIndividualWeights w : rows) {
            collectLeafValues(w.getEffLeafWeightsJson(), "效能", allLeafValues);
            collectLeafValues(w.getEqLeafWeightsJson(), "装备", allLeafValues);
        }

        // 计算每个叶子的 CV
        List<IndicatorCvItem> unifiedCvs = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : allLeafValues.entrySet()) {
            List<Double> values = e.getValue();
            if (values.size() < 1) continue;
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStdDev(values, mean);
            double cv = (mean > 0) ? (stdDev / mean) * 100 : 0.0;

            // 解析 key: "域:维度:指标"
            String[] parts = e.getKey().split(":", 3);
            String domain = parts[0];
            String dimName = parts.length > 1 ? parts[1] : "";
            String indName = parts.length > 2 ? parts[2] : parts[parts.length - 1];

            IndicatorCvItem item = new IndicatorCvItem();
            item.setIndicatorCode(e.getKey());
            item.setIndicatorName(indName);
            item.setDimension(dimName);
            item.setMean(round6(mean));
            item.setStdDev(round6(stdDev));
            item.setCv(round6(cv));
            item.setConsistencyLevel(getConsistencyLevel(cv));
            item.setLevelType(getLevelType(cv));
            item.setWeightValues(values);

            // 额外标注域
            item.setDomainTag(domain);

            unifiedCvs.add(item);
        }

        // 过滤极端值：用全局权重均值排序，删除距离最远的10%
        List<ExpertAhpIndividualWeights> filteredRows = filterRowsByUnifiedLeaf(rows, allLeafValues);
        Map<String, List<Double>> filteredLeafValues = new LinkedHashMap<>();
        for (ExpertAhpIndividualWeights w : filteredRows) {
            collectLeafValues(w.getEffLeafWeightsJson(), "效能", filteredLeafValues);
            collectLeafValues(w.getEqLeafWeightsJson(), "装备", filteredLeafValues);
        }

        List<IndicatorCvItem> filteredCvs = new ArrayList<>();
        for (Map.Entry<String, List<Double>> e : filteredLeafValues.entrySet()) {
            List<Double> values = e.getValue();
            if (values.size() < 1) continue;
            double mean = values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            double stdDev = calculateStdDev(values, mean);
            double cv = (mean > 0) ? (stdDev / mean) * 100 : 0.0;

            String[] parts = e.getKey().split(":", 3);
            String domain = parts[0];
            String dimName = parts.length > 1 ? parts[1] : "";
            String indName = parts.length > 2 ? parts[2] : parts[parts.length - 1];

            IndicatorCvItem item = new IndicatorCvItem();
            item.setIndicatorCode(e.getKey());
            item.setIndicatorName(indName);
            item.setDimension(dimName);
            item.setMean(round6(mean));
            item.setStdDev(round6(stdDev));
            item.setCv(round6(cv));
            item.setConsistencyLevel(getConsistencyLevel(cv));
            item.setLevelType(getLevelType(cv));
            item.setWeightValues(values);
            item.setDomainTag(domain);

            filteredCvs.add(item);
        }

        // 按均值降序排列
        unifiedCvs.sort((a, b) -> Double.compare(b.getMean(), a.getMean()));
        filteredCvs.sort((a, b) -> Double.compare(b.getMean(), a.getMean()));

        allResult.setUnifiedLeafCvs(unifiedCvs);
        filteredResult.setUnifiedLeafCvs(filteredCvs);
    }

    /**
     * 收集叶子权重值到 allLeafValues 中（key 格式："域:维度:指标"）
     */
    private void collectLeafValues(String json, String domain, Map<String, List<Double>> allLeafValues) {
        if (json == null || json.isBlank()) return;
        try {
            List<Map<String, Object>> list = objectMapper.readValue(json,
                    new com.fasterxml.jackson.core.type.TypeReference<List<Map<String, Object>>>() {});
            for (Map<String, Object> item : list) {
                String dim = String.valueOf(item.get("dim"));
                String indicator = String.valueOf(item.get("indicator"));
                Object gw = item.get("globalWeight");
                if (dim == null || indicator == null || gw == null) continue;
                String key = domain + ":" + dim + ":" + indicator;
                allLeafValues.computeIfAbsent(key, k -> new ArrayList<>()).add(toDouble(gw));
            }
        } catch (Exception ex) {
            log.warn("解析叶子权重 JSON 失败: {}", ex.getMessage());
        }
    }

    /**
     * 基于综合叶子均值，过滤离群专家（删除距离最远的10%）
     */
    private List<ExpertAhpIndividualWeights> filterRowsByUnifiedLeaf(List<ExpertAhpIndividualWeights> rows,
                                                                     Map<String, List<Double>> leafValues) {
        if (rows.size() <= 2) return new ArrayList<>(rows);

        // 计算每个专家的综合叶子均值向量（简化：所有叶子均值之和 / count 代表）
        List<String> leafKeys = new ArrayList<>(leafValues.keySet());
        Map<Long, Double> expertTotals = new HashMap<>();

        for (ExpertAhpIndividualWeights w : rows) {
            double sum = 0.0;
            int cnt = 0;
            for (String key : leafKeys) {
                List<Double> vals = leafValues.get(key);
                if (vals != null && !vals.isEmpty()) {
                    sum += vals.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
                    cnt++;
                }
            }
            expertTotals.put(w.getExpertId(), cnt > 0 ? sum / cnt : 0.0);
        }

        // 计算中心
        double center = expertTotals.values().stream()
                .filter(v -> v > 0)
                .mapToDouble(Double::doubleValue)
                .average().orElse(0.0);

        // 计算距离并排序
        List<Map.Entry<Long, Double>> distList = new ArrayList<>();
        for (Map.Entry<Long, Double> e : expertTotals.entrySet()) {
            distList.add(new AbstractMap.SimpleEntry<>(e.getKey(), Math.abs(e.getValue() - center)));
        }
        distList.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        // 删除最远的10%
        int removeCount = Math.max(1, (int) Math.ceil(rows.size() * 0.1));
        Set<Long> removeIds = new HashSet<>();
        for (int i = 0; i < removeCount && i < distList.size(); i++) {
            removeIds.add(distList.get(i).getKey());
        }

        return rows.stream().filter(w -> !removeIds.contains(w.getExpertId())).collect(Collectors.toList());
    }

    private double round6(double v) {
        if (!Double.isFinite(v)) return 0.0;
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 专家距离（复用）
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
