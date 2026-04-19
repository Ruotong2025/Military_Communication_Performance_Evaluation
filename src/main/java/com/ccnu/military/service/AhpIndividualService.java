package com.ccnu.military.service;

import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.dto.AhpIndividualResult;
import com.ccnu.military.dto.AhpIndividualResult.*;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.entity.ExpertAhpIndividualWeights;
import com.ccnu.military.repository.ExpertAhpComparisonScoreRepository;
import com.ccnu.military.repository.ExpertAhpIndividualWeightsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 专家 AHP 个体层次计算服务：读取域间 + 效能 + 装备三组比较打分，
 * 计算所有叶子指标的全局权重（效能+装备各叶子之和=1），
 * 写入 expert_ahp_individual_weights。
 */
@Slf4j
@Service
public class AhpIndividualService {

    private static final String CROSS_DOMAIN_KEY = "域间一级_效能指标_装备操作";

    private final ExpertAhpComparisonScoreRepository scoreRepository;
    private final ExpertAhpIndividualWeightsRepository repository;
    private final ExpertAHPService expertAHPService;
    private final EquipmentAhpService equipmentAhpService;
    private final EquipmentAhpScoreService equipmentAhpScoreService;
    private final ObjectMapper objectMapper;

    @Autowired
    public AhpIndividualService(ExpertAhpComparisonScoreRepository scoreRepository,
                             ExpertAhpIndividualWeightsRepository repository,
                             ExpertAHPService expertAHPService,
                             EquipmentAhpService equipmentAhpService,
                             @Lazy EquipmentAhpScoreService equipmentAhpScoreService,
                             ObjectMapper objectMapper) {
        this.scoreRepository = scoreRepository;
        this.repository = repository;
        this.expertAHPService = expertAHPService;
        this.equipmentAhpService = equipmentAhpService;
        this.equipmentAhpScoreService = equipmentAhpScoreService;
        this.objectMapper = objectMapper;
    }

    /**
     * 为指定专家计算并持久化统一 AHP 快照（域间 + 效能 + 装备）。
     * <p>
     * 调用时机：
     * <ul>
     *   <li>效能保存成功后（ExpertAhpComparisonScoreService）</li>
     *   <li>装备保存成功后（EquipmentAhpScoreService）</li>
     *   <li>域间保存成功后（ExpertAhpComparisonScoreService）</li>
     *   <li>综合计算/批量模拟后</li>
     * </ul>
     * 若任一侧数据缺失则跳过对应域（效能侧或装备侧），
     * 若域间缺失则 w_eff = w_eq = 0.5 兜底。
     */
    @Transactional
    public void persistUnified(Long expertId, String expertName) {
        if (expertId == null) {
            log.warn("persistUnified 跳过：expertId 为空");
            return;
        }

        // 1. 读取域间一级
        CrossDomainResult crossDomain = readCrossDomain(expertId);

        // 2. 读取效能矩阵并计算
        EffectivenessResult effectiveness = computeEffectiveness(expertId);

        // 3. 读取装备矩阵并计算
        EquipmentResult equipment = computeEquipment(expertId);

        // 4. 组装统一结果（含叶子权重展开）
        AhpIndividualResult unified = buildUnifiedResult(crossDomain, effectiveness, equipment);

        // 5. 写入数据库
        ExpertAhpIndividualWeights entity = toEntity(expertId, expertName, crossDomain,
                effectiveness, equipment, unified);
        repository.findByExpertId(expertId)
                .ifPresent(existing -> entity.setId(existing.getId()));
        repository.save(entity);
        log.info("统一 AHP 快照已写入 expertId={} 叶子数={}", expertId, unified.getTotalLeafCount());
    }

    // ── 读取 ────────────────────────────────────────────────────────────────

    private CrossDomainResult readCrossDomain(Long expertId) {
        CrossDomainResult r = new CrossDomainResult();
        scoreRepository.findByExpertIdAndComparisonKey(expertId, CROSS_DOMAIN_KEY)
                .ifPresent(row -> {
                    double a = row.getScore() != null ? row.getScore().doubleValue() : 1.0;
                    a = Math.max(1.0 / 9.0, Math.min(9.0, a));
                    r.setScore(a);
                    r.setConfidence(row.getConfidence() != null ? row.getConfidence().doubleValue() : 0.8);
                    double wEff = a / (1.0 + a);
                    r.setEffWeight(round6(wEff));
                    r.setEqWeight(round6(1.0 / (1.0 + a)));
                });
        if (r.getScore() == 0) {
            // 域间缺失时兜底
            r.setScore(1.0);
            r.setConfidence(0.8);
            r.setEffWeight(0.5);
            r.setEqWeight(0.5);
        }
        return r;
    }

    private EffectivenessResult computeEffectiveness(Long expertId) {
        EffectivenessResult r = new EffectivenessResult();
        r.setDimensionWeights(new LinkedHashMap<>());
        r.setIndicators(new LinkedHashMap<>());
        List<ExpertAhpComparisonScore> rows = scoreRepository.findByExpertIdOrderByIdAsc(expertId);
        // 过滤：排除装备操作_前缀和域间一级_前缀，只保留效能指标打分
        if (rows.isEmpty()) {
            log.debug("expertId={} 无打分数据", expertId);
            return r;
        }
        rows = rows.stream()
                .filter(row -> {
                    String k = row.getComparisonKey();
                    if (k == null) return false;
                    return !k.startsWith("装备操作_") && !k.startsWith("域间一级_");
                })
                .collect(java.util.stream.Collectors.toList());
        MatrixCalculationRequest req = buildEffectivenessRequest(rows);
        if (req == null) {
            return r;
        }
        MatrixCalculationResult calc = expertAHPService.calculateAll(req);
        if (calc == null) {
            return r;
        }

        // 维度层权重（动态 Map）
        MatrixCalculationResult.MatrixResult dimResult = calc.getDimensionResult();
        if (dimResult != null && dimResult.getWeightMap() != null) {
            r.getDimensionWeights().putAll(dimResult.getWeightMap());
        }
        r.setCr(dimResult != null ? safeCr(dimResult) : 0.0);

        // 各维度指标层：MatrixResult → Map<String,Double>
        Map<String, Map<String, Double>> flatInd = new LinkedHashMap<>();
        Map<String, MatrixCalculationResult.MatrixResult> ir = calc.getIndicatorResults();
        if (ir != null) {
            for (Map.Entry<String, MatrixCalculationResult.MatrixResult> e : ir.entrySet()) {
                flatInd.put(e.getKey(), e.getValue().getWeightMap());
            }
        }
        r.setIndicators(flatInd);
        return r;
    }

    private EquipmentResult computeEquipment(Long expertId) {
        EquipmentResult r = new EquipmentResult();
        MatrixCalculationRequest req = equipmentAhpScoreService.buildMatrixRequestFromStoredRows(expertId);
        if (req == null) {
            log.debug("expertId={} 无装备打分数据", expertId);
            return r;
        }
        MatrixCalculationResult calc = equipmentAhpService.calculate(req);
        if (calc == null) {
            return r;
        }

        r.setDimensionWeights(new LinkedHashMap<>());
        r.setIndicators(new LinkedHashMap<>());
        r.setCrByDimension(new LinkedHashMap<>());

        MatrixCalculationResult.MatrixResult dimResult = calc.getDimensionResult();
        if (dimResult != null && dimResult.getWeightMap() != null) {
            r.getDimensionWeights().putAll(dimResult.getWeightMap());
            r.getCrByDimension().put("dimension", safeCr(dimResult));
        }
        Map<String, MatrixCalculationResult.MatrixResult> ir = calc.getIndicatorResults();
        if (ir != null) {
            for (Map.Entry<String, MatrixCalculationResult.MatrixResult> e : ir.entrySet()) {
                r.getIndicators().put(e.getKey(), e.getValue().getWeightMap());
                r.getCrByDimension().put(e.getKey(), safeCr(e.getValue()));
            }
        }
        return r;
    }

    // ── 组装统一结果 ────────────────────────────────────────────────────────

    /**
     * 将域间 + 效能 + 装备三层 AHP 结果合并为统一快照（与 expert_ahp_individual_weights 中 JSON 一致），供集结层复用。
     */
    public AhpIndividualResult composeUnifiedFromLayers(
            CrossDomainResult crossDomain,
            EffectivenessResult effectiveness,
            EquipmentResult equipment) {
        if (crossDomain == null) {
            crossDomain = new CrossDomainResult();
            crossDomain.setScore(1.0);
            crossDomain.setConfidence(0.8);
            crossDomain.setEffWeight(0.5);
            crossDomain.setEqWeight(0.5);
        }
        if (effectiveness == null) {
            effectiveness = new EffectivenessResult();
            effectiveness.setDimensionWeights(new LinkedHashMap<>());
            effectiveness.setIndicators(new LinkedHashMap<>());
            effectiveness.setCr(0.0);
        }
        if (equipment == null) {
            equipment = new EquipmentResult();
            equipment.setDimensionWeights(new LinkedHashMap<>());
            equipment.setIndicators(new LinkedHashMap<>());
            equipment.setCrByDimension(new LinkedHashMap<>());
        }
        return buildUnifiedResult(crossDomain, effectiveness, equipment);
    }

    private AhpIndividualResult buildUnifiedResult(
            CrossDomainResult crossDomain,
            EffectivenessResult effectiveness,
            EquipmentResult equipment) {

        AhpIndividualResult unified = new AhpIndividualResult();
        unified.setCrossDomain(crossDomain);

        double wEff = crossDomain.getEffWeight();
        double wEq = crossDomain.getEqWeight();

        unified.setEffectiveness(effectiveness);
        unified.setEquipment(equipment);

        // 展开所有叶子
        List<LeafWeight> leaves = new ArrayList<>();
        expandEfficiencyLeaves(leaves, wEff, effectiveness);
        expandEquipmentLeaves(leaves, wEq, equipment);

        unified.setAllLeaves(leaves);
        unified.setTotalLeafCount(leaves.size());

        // 校验权重和（供调试）
        double sum = leaves.stream().mapToDouble(LeafWeight::getGlobalWeight).sum();
        log.debug("叶子权重总和={}（应为 ~1.0）", round6(sum));
        return unified;
    }

    private void expandEfficiencyLeaves(List<LeafWeight> leaves, double wEff,
                                        EffectivenessResult eff) {
        if (eff == null || eff.getIndicators() == null || eff.getDimensionWeights() == null) {
            return;
        }
        Map<String, Double> dimWeightMap = eff.getDimensionWeights();

        for (Map.Entry<String, Map<String, Double>> dimEntry : eff.getIndicators().entrySet()) {
            String dim = dimEntry.getKey();
            double wDim = dimWeightMap.getOrDefault(dim, 0.0);
            if (wDim == 0.0) continue;
            for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                double global = wEff * wDim * indEntry.getValue();
                leaves.add(new LeafWeight("efficiency", dim, indEntry.getKey(), round6(global)));
            }
        }
    }

    private void expandEquipmentLeaves(List<LeafWeight> leaves, double wEq,
                                        EquipmentResult eq) {
        if (eq == null || eq.getDimensionWeights() == null || eq.getIndicators() == null) {
            return;
        }
        for (Map.Entry<String, Map<String, Double>> dimEntry : eq.getIndicators().entrySet()) {
            String dim = dimEntry.getKey();
            double wDim = eq.getDimensionWeights().getOrDefault(dim, 0.0);
            if (wDim == 0.0) continue;
            for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                double global = wEq * wDim * indEntry.getValue();
                leaves.add(new LeafWeight("equipment", dim, indEntry.getKey(), round6(global)));
            }
        }
    }

    // ── 写入实体 ────────────────────────────────────────────────────────────

    private ExpertAhpIndividualWeights toEntity(
            Long expertId, String expertName,
            CrossDomainResult crossDomain,
            EffectivenessResult effectiveness,
            EquipmentResult equipment,
            AhpIndividualResult unified) {

        ExpertAhpIndividualWeights e = new ExpertAhpIndividualWeights();
        e.setExpertId(expertId);
        e.setExpertName(expertName);
        e.setUpdatedAt(LocalDateTime.now());

        // 域间
        if (crossDomain != null) {
            e.setEffDomainWeight(bd(crossDomain.getEffWeight()));
            e.setEqDomainWeight(bd(crossDomain.getEqWeight()));
            e.setCrossDomainScore(bd(crossDomain.getScore()));
            e.setCrossDomainConfidence(bd(crossDomain.getConfidence()));
        }

        // 效能维度层（JSON）
        if (effectiveness.getDimensionWeights() != null) {
            e.setEffDimWeightsJson(toJson(effectiveness.getDimensionWeights()));
            e.setEffDimCount(effectiveness.getDimensionWeights().size());
        }

        // 效能叶子全局权重（JSON）
        if (effectiveness.getIndicators() != null && effectiveness.getDimensionWeights() != null) {
            double wEff = crossDomain.getEffWeight();
            List<Map<String, Object>> effLeafList = new ArrayList<>();
            int leafCount = 0;
            for (Map.Entry<String, Map<String, Double>> dimEntry : effectiveness.getIndicators().entrySet()) {
                double wDim = effectiveness.getDimensionWeights().getOrDefault(dimEntry.getKey(), 0.0);
                for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                    double global = wEff * wDim * indEntry.getValue();
                    Map<String, Object> leaf = new LinkedHashMap<>();
                    leaf.put("dim", dimEntry.getKey());
                    leaf.put("indicator", indEntry.getKey());
                    leaf.put("globalWeight", round6(global));
                    effLeafList.add(leaf);
                    leafCount++;
                }
            }
            e.setEffLeafWeightsJson(toJson(effLeafList));
            e.setEffLeafCount(leafCount);
        }

        // 装备维度（JSON）
        if (equipment != null && equipment.getDimensionWeights() != null) {
            e.setEqDimWeightsJson(toJson(equipment.getDimensionWeights()));
            e.setEqDimCount(equipment.getDimensionWeights().size());
        }

        // 装备叶子（JSON）
        if (equipment != null && equipment.getIndicators() != null) {
            Map<String, Map<String, Double>> eqInd = equipment.getIndicators();
            double wEq = crossDomain.getEqWeight();
            List<Map<String, Object>> eqLeafList = new ArrayList<>();
            int leafCount = 0;
            for (Map.Entry<String, Map<String, Double>> dimEntry : eqInd.entrySet()) {
                double wDim = equipment.getDimensionWeights().getOrDefault(dimEntry.getKey(), 0.0);
                for (Map.Entry<String, Double> indEntry : dimEntry.getValue().entrySet()) {
                    double global = wEq * wDim * indEntry.getValue();
                    Map<String, Object> leaf = new LinkedHashMap<>();
                    leaf.put("dim", dimEntry.getKey());
                    leaf.put("indicator", indEntry.getKey());
                    leaf.put("globalWeight", round6(global));
                    eqLeafList.add(leaf);
                    leafCount++;
                }
            }
            e.setEqLeafWeightsJson(toJson(eqLeafList));
            e.setEqLeafCount(leafCount);
        }

        // CR
        e.setEffCr(bd(effectiveness.getCr()));
        e.setEqCrJson(toJson(equipment != null ? equipment.getCrByDimension() : null));

        // 完整 JSON
        e.setAhpResultJson(toJson(unified));

        return e;
    }

    // ── 工具方法 ────────────────────────────────────────────────────────────

    private double safeCr(MatrixCalculationResult.MatrixResult r) {
        return r != null ? r.getCr() : 0.0;
    }

    private double round6(double v) {
        if (!Double.isFinite(v)) return 0.0;
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP).doubleValue();
    }

    private BigDecimal bd(Double v) {
        if (v == null || !Double.isFinite(v)) return null;
        return BigDecimal.valueOf(v).setScale(6, RoundingMode.HALF_UP);
    }

    private String toJson(Object obj) {
        if (obj == null) return null;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException ex) {
            log.warn("JSON 序列化失败: {}", ex.getMessage());
            return null;
        }
    }

    private MatrixCalculationRequest buildEffectivenessRequest(List<ExpertAhpComparisonScore> rows) {
        Map<String, ExpertAhpComparisonScore> byKey = new HashMap<>();
        for (ExpertAhpComparisonScore r : rows) {
            if (r.getComparisonKey() != null) {
                byKey.put(r.getComparisonKey(), r);
            }
        }
        String[] dims = ExpertAHPService.getDimensions();
        List<MatrixCalculationRequest.MatrixEntry> dimEntries = new ArrayList<>();
        for (int i = 0; i < dims.length; i++) {
            for (int j = i + 1; j < dims.length; j++) {
                String key = dims[i] + "_" + dims[j];
                ExpertAhpComparisonScore s = byKey.get(key);
                if (s != null && s.getScore() != null) {
                    Double conf = s.getConfidence() != null ? s.getConfidence().doubleValue() : null;
                    dimEntries.add(new MatrixCalculationRequest.MatrixEntry(key, s.getScore().doubleValue(), conf));
                }
            }
        }
        Map<String, String[]> dimIndicators = ExpertAHPService.getDimensionIndicators();
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> indMap = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : dimIndicators.entrySet()) {
            String[] inds = e.getValue();
            List<MatrixCalculationRequest.MatrixEntry> entries = new ArrayList<>();
            for (int i = 0; i < inds.length; i++) {
                for (int j = i + 1; j < inds.length; j++) {
                    String key = inds[i] + "_" + inds[j];
                    ExpertAhpComparisonScore s = byKey.get(key);
                    if (s != null && s.getScore() != null) {
                        Double conf = s.getConfidence() != null ? s.getConfidence().doubleValue() : null;
                        entries.add(new MatrixCalculationRequest.MatrixEntry(key, s.getScore().doubleValue(), conf));
                    }
                }
            }
            indMap.put(e.getKey(), entries);
        }
        if (dimEntries.isEmpty() && indMap.values().stream().allMatch(List::isEmpty)) {
            return null;
        }
        MatrixCalculationRequest req = new MatrixCalculationRequest();
        req.setDimensionMatrix(dimEntries);
        req.setIndicatorMatrices(indMap);
        return req;
    }

    /**
     * 查询某专家的统一快照（供 Controller 返回）
     */
    public AhpIndividualResult findUnifiedResult(Long expertId) {
        if (expertId == null) return null;
        return repository.findByExpertId(expertId)
                .map(this::parseResultJson)
                .orElse(null);
    }

    private AhpIndividualResult parseResultJson(ExpertAhpIndividualWeights row) {
        if (row.getAhpResultJson() == null || row.getAhpResultJson().isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(row.getAhpResultJson(), AhpIndividualResult.class);
        } catch (Exception ex) {
            log.warn("反序列化统一 AHP 结果失败 expertId={}", row.getExpertId(), ex);
            return null;
        }
    }
}
