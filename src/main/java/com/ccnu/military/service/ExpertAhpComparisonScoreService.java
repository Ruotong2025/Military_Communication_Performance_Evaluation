package com.ccnu.military.service;

import com.ccnu.military.dto.ExpertAhpScoresPersistRequest;
import com.ccnu.military.dto.ExpertAhpSimulateRequest;
import com.ccnu.military.dto.MatrixCalculationRequest;
import com.ccnu.military.dto.MatrixCalculationResult;
import com.ccnu.military.entity.ExpertAhpComparisonScore;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.repository.ExpertAhpComparisonScoreRepository;
import com.ccnu.military.repository.ExpertBaseInfoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 专家 AHP 比较打分持久化与模拟生成
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertAhpComparisonScoreService {

    /** 与 {@link EquipmentAhpScoreService} 中装备操作域前缀一致，用于同表域隔离 */
    private static final String EQUIPMENT_KEY_PREFIX = "装备操作_";

    private static final double CR_LIMIT = 0.1;
    private static final int WEIGHT_PAYLOAD_MAX_TRIES = 8;

    private final ExpertAhpComparisonScoreRepository scoreRepository;
    private final ExpertBaseInfoRepository expertBaseInfoRepository;
    private final ExpertAHPService expertAHPService;
    private final ExpertAhpIndividualWeightsService individualWeightsService;

    /**
     * 查询某专家的比较打分。
     */
    public List<ExpertAhpComparisonScore> listByExpert(Long expertId) {
        if (expertId == null) {
            return Collections.emptyList();
        }
        return scoreRepository.findEffectivenessByExpertId(expertId, EQUIPMENT_KEY_PREFIX + "%");
    }

    @Transactional
    public int persist(ExpertAhpScoresPersistRequest req) {
        if (req.getExpertId() == null) {
            throw new IllegalArgumentException("expertId 不能为空");
        }
        ExpertBaseInfo expert = expertBaseInfoRepository.findById(req.getExpertId())
                .orElseThrow(() -> new IllegalArgumentException("专家不存在: " + req.getExpertId()));
        String name = req.getExpertName() != null && !req.getExpertName().isBlank()
                ? req.getExpertName()
                : expert.getExpertName();

        List<ExpertAhpComparisonScore> rows = buildRowsFromMatrixPayload(
                req.getExpertId(), name, req.getDimensionMatrix(), req.getIndicatorMatrices());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("没有可保存的比较项");
        }

        scoreRepository.deleteEffectivenessByExpertId(req.getExpertId(), EQUIPMENT_KEY_PREFIX + "%");
        scoreRepository.saveAll(rows);
        MatrixCalculationRequest matrixReq = new MatrixCalculationRequest();
        matrixReq.setDimensionMatrix(req.getDimensionMatrix());
        matrixReq.setIndicatorMatrices(req.getIndicatorMatrices());
        saveSnapshot(req.getExpertId(), name, matrixReq);
        log.info("已保存专家 AHP 打分 expertId={} 条数={}", req.getExpertId(), rows.size());
        return rows.size();
    }

    /**
     * 批量模拟：为每名专家生成随机矩阵后覆盖写入。
     */
    @Transactional
    public Map<String, Object> simulate(ExpertAhpSimulateRequest req) {
        if (req.getExpertIds() == null || req.getExpertIds().isEmpty()) {
            throw new IllegalArgumentException("请至少选择一名专家");
        }
        List<Long> inserted = new ArrayList<>();
        List<Long> skipped = new ArrayList<>();
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        log.info("开始批量模拟 AHP 打分，专家数={}", req.getExpertIds().size());

        for (Long expertId : req.getExpertIds()) {
            if (expertId == null) continue;
            ExpertBaseInfo expert = expertBaseInfoRepository.findById(expertId)
                    .orElseThrow(() -> new IllegalArgumentException("专家不存在: " + expertId));
            MatrixCalculationRequest matrix = randomConsistentMatrixPayload(rnd);
            List<ExpertAhpComparisonScore> rows = buildRowsFromMatrixPayload(
                    expertId, expert.getExpertName(),
                    matrix.getDimensionMatrix(), matrix.getIndicatorMatrices());
            scoreRepository.deleteEffectivenessByExpertId(expertId, EQUIPMENT_KEY_PREFIX + "%");
            scoreRepository.saveAll(rows);
            saveSnapshot(expertId, expert.getExpertName(), matrix);
            inserted.add(expertId);
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("insertedExpertIds", inserted);
        out.put("skippedExpertIds", skipped);
        out.put("insertedCount", inserted.size());
        out.put("skippedCount", skipped.size());
        log.info("批量模拟 AHP 打分结束，写入={}", inserted.size());
        return out;
    }

    /**
     * 权重反推判断矩阵：先在 [1,9] 上独立抽样正数 a_i（可视为未归一化权重），
     * 再令上三角标度 a_ij = a_i / a_j（与先归一化再取 w_i/w_j 等价）。理论为完全一致矩阵，CI=0；
     * 两位小数舍入后一般 CR 仍 < 0.1；极少数不满足则少量重试。
     */
    private MatrixCalculationRequest randomConsistentMatrixPayload(ThreadLocalRandom rnd) {
        for (int t = 0; t < WEIGHT_PAYLOAD_MAX_TRIES; t++) {
            MatrixCalculationRequest candidate = buildWeightProportionalPayload(rnd);
            if (candidate != null && allMatricesPassCr(candidate)) {
                return candidate;
            }
        }
        log.warn("权重反推矩阵多次尝试后仍有 CR≥{}，使用全 1 标度兜底", CR_LIMIT);
        return buildAllOnesPayload(rnd);
    }

    private MatrixCalculationRequest buildWeightProportionalPayload(ThreadLocalRandom rnd) {
        String[] dims = ExpertAHPService.getDimensions();
        List<MatrixCalculationRequest.MatrixEntry> dimEntries = buildEntriesFromRandomPositiveWeights(dims, rnd);
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> indMap = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : ExpertAHPService.getDimensionIndicators().entrySet()) {
            indMap.put(e.getKey(), buildEntriesFromRandomPositiveWeights(e.getValue(), rnd));
        }
        MatrixCalculationRequest req = new MatrixCalculationRequest();
        req.setDimensionMatrix(dimEntries);
        req.setIndicatorMatrices(indMap);
        return req;
    }

    /**
     * a_i ~ U(1,9)，标度 a_ij = a_i/a_j ∈ [1/9,9]；归一化权重 w_i = a_i/Σa 时 w_i/w_j = a_i/a_j。
     * 三层波动策略：20% 指数极端 / 40% 均匀 / 40% 局部放大
     */
    private List<MatrixCalculationRequest.MatrixEntry> buildEntriesFromRandomPositiveWeights(String[] elements, ThreadLocalRandom rnd) {
        int n = elements.length;
        double[] a = new double[n];

        double roll = rnd.nextDouble();
        if (roll < 0.20) {
            // Dirichlet 指数风格：少数极大、多数极小，波动最剧烈
            double[] raw = new double[n];
            double sum = 0;
            for (int i = 0; i < n; i++) {
                raw[i] = -Math.log(rnd.nextDouble(1e-6, 1.0));
                sum += raw[i];
            }
            double base = rnd.nextDouble(0.5, 3.0);
            for (int i = 0; i < n; i++) {
                a[i] = base * (raw[i] / sum) * n * 3.5;
                a[i] = Math.max(0.11, Math.min(9.0, a[i]));
            }
        } else if (roll < 0.60) {
            // 均匀分布 [1, 9]，中等波动（原有逻辑）
            for (int i = 0; i < n; i++) {
                a[i] = rnd.nextDouble(1.0, 9.0);
            }
        } else {
            // 局部放大风格：挑选 1～2 个优势元素乘以 3～6 倍，其余 [1, 3]
            for (int i = 0; i < n; i++) {
                a[i] = rnd.nextDouble(1.0, 3.0);
            }
            int advantageCount = rnd.nextInt(1, Math.min(3, n));
            Set<Integer> chosen = new HashSet<>();
            while (chosen.size() < advantageCount) {
                chosen.add(rnd.nextInt(n));
            }
            for (int idx : chosen) {
                a[idx] = rnd.nextDouble(3.5, 7.0);
            }
        }

        List<MatrixCalculationRequest.MatrixEntry> list = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double score = a[i] / a[j];
                score = round2(score);
                score = Math.max(1.0 / 9.0, Math.min(9.0, score));
                list.add(new MatrixCalculationRequest.MatrixEntry(
                        elements[i] + "_" + elements[j],
                        score,
                        randomConfidence(rnd)));
            }
        }
        return list;
    }

    private boolean allMatricesPassCr(MatrixCalculationRequest req) {
        MatrixCalculationResult.MatrixResult dr =
                expertAHPService.calculateSingleMatrix(ExpertAHPService.getDimensions(), req.getDimensionMatrix());
        if (dr.getCr() >= CR_LIMIT) {
            return false;
        }
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> ind = req.getIndicatorMatrices();
        if (ind == null) {
            return false;
        }
        for (Map.Entry<String, String[]> e : ExpertAHPService.getDimensionIndicators().entrySet()) {
            List<MatrixCalculationRequest.MatrixEntry> entries = ind.get(e.getKey());
            if (entries == null) {
                return false;
            }
            MatrixCalculationResult.MatrixResult ir = expertAHPService.calculateSingleMatrix(e.getValue(), entries);
            if (ir.getCr() >= CR_LIMIT) {
                return false;
            }
        }
        return true;
    }

    private MatrixCalculationRequest buildAllOnesPayload(ThreadLocalRandom rnd) {
        String[] dims = ExpertAHPService.getDimensions();
        List<MatrixCalculationRequest.MatrixEntry> dimEntries = new ArrayList<>();
        for (int i = 0; i < dims.length; i++) {
            for (int j = i + 1; j < dims.length; j++) {
                dimEntries.add(new MatrixCalculationRequest.MatrixEntry(
                        dims[i] + "_" + dims[j], 1.0, randomConfidence(rnd)));
            }
        }
        Map<String, List<MatrixCalculationRequest.MatrixEntry>> indMap = new LinkedHashMap<>();
        for (Map.Entry<String, String[]> e : ExpertAHPService.getDimensionIndicators().entrySet()) {
            String[] inds = e.getValue();
            List<MatrixCalculationRequest.MatrixEntry> list = new ArrayList<>();
            for (int i = 0; i < inds.length; i++) {
                for (int j = i + 1; j < inds.length; j++) {
                    list.add(new MatrixCalculationRequest.MatrixEntry(
                            inds[i] + "_" + inds[j], 1.0, randomConfidence(rnd)));
                }
            }
            indMap.put(e.getKey(), list);
        }
        MatrixCalculationRequest req = new MatrixCalculationRequest();
        req.setDimensionMatrix(dimEntries);
        req.setIndicatorMatrices(indMap);
        return req;
    }

    /**
     * 约 20% 把握度 < 0.6（模拟"没把握"），其余在 [0.62, 0.95]
     */
    private double randomConfidence(ThreadLocalRandom rnd) {
        if (rnd.nextDouble() < 0.20) {
            return round2(rnd.nextDouble(0.35, 0.59));
        }
        return round2(rnd.nextDouble(0.62, 0.96));
    }

    private static double round2(double v) {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 根据当前判断矩阵计算 AHP 层次结果并写入 expert_ahp_individual_weights
     */
    private void saveSnapshot(Long expertId, String expertName, MatrixCalculationRequest matrixReq) {
        MatrixCalculationResult calc = expertAHPService.calculateAll(matrixReq);
        individualWeightsService.upsert(expertId, expertName, calc);
    }

    private List<ExpertAhpComparisonScore> buildRowsFromMatrixPayload(
            Long expertId,
            String expertName,
            List<MatrixCalculationRequest.MatrixEntry> dimensionMatrix,
            Map<String, List<MatrixCalculationRequest.MatrixEntry>> indicatorMatrices) {

        List<ExpertAhpComparisonScore> rows = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (dimensionMatrix != null) {
            for (MatrixCalculationRequest.MatrixEntry e : dimensionMatrix) {
                if (e == null || e.getKey() == null || e.getScore() == null) continue;
                rows.add(row(expertId, expertName, e.getKey(), e.getScore(), e.getConfidence(), now));
            }
        }
        if (indicatorMatrices != null) {
            for (List<MatrixCalculationRequest.MatrixEntry> list : indicatorMatrices.values()) {
                if (list == null) continue;
                for (MatrixCalculationRequest.MatrixEntry e : list) {
                    if (e == null || e.getKey() == null || e.getScore() == null) continue;
                    rows.add(row(expertId, expertName, e.getKey(), e.getScore(), e.getConfidence(), now));
                }
            }
        }
        return rows;
    }

    private ExpertAhpComparisonScore row(
            Long expertId,
            String expertName,
            String key,
            double score,
            Double confidence,
            LocalDateTime now) {

        ExpertAhpComparisonScore s = new ExpertAhpComparisonScore();
        s.setExpertId(expertId);
        s.setExpertName(expertName);
        s.setComparisonKey(key);
        double sc = score;
        if (!Double.isFinite(sc)) {
            sc = 1.0;
        }
        double lo = 1.0 / 9.0;
        double hi = 9.0;
        sc = Math.max(lo, Math.min(hi, sc));
        s.setScore(BigDecimal.valueOf(sc).setScale(2, RoundingMode.HALF_UP));
        double c = confidence != null ? confidence : 0.55;
        c = Math.min(1.0, Math.max(0.0, c));
        s.setConfidence(BigDecimal.valueOf(c).setScale(2, RoundingMode.HALF_UP));
        s.setCreateTime(now);
        return s;
    }
}
