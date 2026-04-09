package com.ccnu.military.service;

import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.EquipmentQlIndicatorDef;
import com.ccnu.military.entity.EquipmentQtIndicatorDef;
import com.ccnu.military.repository.EquipmentQlIndicatorDefRepository;
import com.ccnu.military.repository.EquipmentQtIndicatorDefRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 装备操作 AHP Service
 * 从 equipment_qt_indicator_def / equipment_ql_indicator_def 读取维度与指标元数据，
 * 复用 ExpertAHPService 的矩阵计算逻辑，按 comparison_key 前缀 "装备操作_" 隔离数据。
 */
@Slf4j
@Service
public class EquipmentAhpService {

    public static final String PREFIX = "装备操作_";

    private final EquipmentQtIndicatorDefRepository qtRepo;
    private final EquipmentQlIndicatorDefRepository qlRepo;
    private final ExpertAHPService expertAHPService;

    public EquipmentAhpService(
            EquipmentQtIndicatorDefRepository qtRepo,
            EquipmentQlIndicatorDefRepository qlRepo,
            ExpertAHPService expertAHPService) {
        this.qtRepo = qtRepo;
        this.qlRepo = qlRepo;
        this.expertAHPService = expertAHPService;
    }

    // ─── 元数据读取 ───────────────────────────────────────────────

    /**
     * 获取装备操作的全部维度（去重，按定义顺序）
     */
    public List<String> getDimensions() {
        Set<String> dims = new LinkedHashSet<>();
        qtRepo.findAll().forEach(e -> dims.add(e.getDimension()));
        qlRepo.findAll().forEach(e -> dims.add(e.getDimension()));
        return new ArrayList<>(dims);
    }

    /**
     * 获取维度下的指标列表（合并定性与定量）
     * @param type 可选 "ql" / "qt" / null（both）
     */
    public Map<String, List<Map<String, Object>>> getDimensionIndicators(String type) {
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();

        // 初始化所有维度
        for (String dim : getDimensions()) {
            result.put(dim, new ArrayList<>());
        }

        if (type == null || type.equals("qt") || type.equals("both")) {
            qtRepo.findAll().forEach(e -> addIndicator(result, e.getDimension(), e));
        }

        if (type == null || type.equals("ql") || type.equals("both")) {
            qlRepo.findAll().forEach(e -> addIndicator(result, e.getDimension(), e));
        }

        return result;
    }

    private void addIndicator(Map<String, List<Map<String, Object>>> result, String dimension, Object entity) {
        if (!result.containsKey(dimension)) {
            result.put(dimension, new ArrayList<>());
        }
        Map<String, Object> item = new LinkedHashMap<>();
        if (entity instanceof EquipmentQtIndicatorDef) {
            EquipmentQtIndicatorDef qt = (EquipmentQtIndicatorDef) entity;
            item.put("key", qt.getIndicatorKey());
            item.put("name", qt.getIndicatorName());
            item.put("description", qt.getDescription());
            item.put("type", "qt");
        } else if (entity instanceof EquipmentQlIndicatorDef) {
            EquipmentQlIndicatorDef ql = (EquipmentQlIndicatorDef) entity;
            item.put("key", ql.getIndicatorKey());
            item.put("name", ql.getIndicatorName());
            item.put("description", ql.getDescription());
            item.put("type", "ql");
        }
        result.get(dimension).add(item);
    }

    /**
     * 获取完整 meta（含维度层矩阵配对数、指标层配对数统计）
     */
    public Map<String, Object> getMeta(String type) {
        Map<String, Object> meta = new LinkedHashMap<>();
        List<String> dims = getDimensions();
        Map<String, List<Map<String, Object>>> indicators = getDimensionIndicators(type);

        meta.put("dimensions", dims);
        meta.put("dimensionOrder", dims);
        meta.put("indicators", indicators);
        meta.put("indicatorCount", indicators.values().stream().mapToInt(List::size).sum());

        // 维度层配对数 n*(n-1)/2
        int n = dims.size();
        meta.put("dimensionMatrixPairs", n * (n - 1) / 2);

        // 各维度指标层配对数
        Map<String, Integer> pairsPerDim = new LinkedHashMap<>();
        for (Map.Entry<String, List<Map<String, Object>>> entry : indicators.entrySet()) {
            int m = entry.getValue().size();
            pairsPerDim.put(entry.getKey(), m * (m - 1) / 2);
        }
        meta.put("indicatorMatrixPairs", pairsPerDim);

        return meta;
    }

    // ─── AHP 计算（委托给 ExpertAHPService） ─────────────────────

    /**
     * 计算 AHP 权重与一致性比率
     * @param request 请求中 comparison_key 已带 "装备操作_" 前缀（与入库一致）。
     *                委托 {@link ExpertAHPService#calculateAllDynamic} 前会去掉前缀，使 key 变为「维度A_维度B」「指标A_指标B」供 {@link ExpertAHPService#buildMatrix} 解析。
     */
    public MatrixCalculationResult calculate(MatrixCalculationRequest request) {
        String[] dims = getDimensions().toArray(new String[0]);
        Map<String, String[]> dimIndicators = buildDimensionIndicators();
        MatrixCalculationRequest normalized = normalizeKeysForCalculation(request);
        return expertAHPService.calculateAllDynamic(dims, dimIndicators, normalized);
    }

    /**
     * 维度层：去掉 {@link #PREFIX}，得到「维度i_维度j」。
     * 指标层：去掉 PREFIX + 维度名 + "_"，得到「指标i_指标j」（与 ExpertAHPService.buildMatrix 的 split 一致）。
     */
    private MatrixCalculationRequest normalizeKeysForCalculation(MatrixCalculationRequest request) {
        if (request == null) {
            return new MatrixCalculationRequest();
        }
        MatrixCalculationRequest out = new MatrixCalculationRequest();
        out.setDimensionMatrix(stripEntryKeyPrefix(request.getDimensionMatrix(), PREFIX));
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> inMap = request.getIndicatorMatrices();
        if (inMap != null) {
            Map<String, List<MatrixCalculationRequest.MatrixEntry>> outMap = new LinkedHashMap<>();
            for (Map.Entry<String, List<MatrixCalculationRequest.MatrixEntry>> e : inMap.entrySet()) {
                String dimName = e.getKey();
                String perDimPrefix = PREFIX + dimName + "_";
                outMap.put(dimName, stripEntryKeyPrefix(e.getValue(), perDimPrefix));
            }
            out.setIndicatorMatrices(outMap);
        }
        return out;
    }

    private static List<MatrixCalculationRequest.MatrixEntry> stripEntryKeyPrefix(
            List<MatrixCalculationRequest.MatrixEntry> entries, String prefix) {
        if (entries == null) {
            return null;
        }
        List<MatrixCalculationRequest.MatrixEntry> list = new ArrayList<>(entries.size());
        for (MatrixCalculationRequest.MatrixEntry e : entries) {
            if (e == null || e.getKey() == null) {
                continue;
            }
            String k = e.getKey();
            if (prefix != null && k.startsWith(prefix)) {
                k = k.substring(prefix.length());
            }
            list.add(new MatrixCalculationRequest.MatrixEntry(k, e.getScore(), e.getConfidence()));
        }
        return list;
    }

    private Map<String, String[]> buildDimensionIndicators() {
        Map<String, String[]> result = new LinkedHashMap<>();
        for (String dim : getDimensions()) {
            List<Map<String, Object>> inds = getDimensionIndicators(null).getOrDefault(dim, Collections.emptyList());
            String[] names = inds.stream()
                    .map(m -> (String) m.get("name"))
                    .toArray(String[]::new);
            result.put(dim, names);
        }
        return result;
    }

    // ─── comparison_key 构造 ────────────────────────────────────

    /**
     * 构造维度层比较对 key
     * @param dimA 维度 A（不含前缀）
     * @param dimB 维度 B（不含前缀）
     */
    public static String dimensionKey(String dimA, String dimB) {
        return PREFIX + dimA + "_" + dimB;
    }

    /**
     * 构造指标层比较对 key
     * @param dimension   维度（不含前缀）
     * @param indicatorA 指标 A 名称（中文 name）
     * @param indicatorB 指标 B 名称（中文 name）
     */
    public static String indicatorKey(String dimension, String indicatorA, String indicatorB) {
        return PREFIX + dimension + "_" + indicatorA + "_" + indicatorB;
    }
}
