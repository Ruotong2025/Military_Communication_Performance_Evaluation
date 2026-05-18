package com.ccnu.military.service;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.DynamicCostBatch;
import com.ccnu.military.entity.DynamicCostSimulationDetail;
import com.ccnu.military.entity.DynamicCostTemplate;
import com.ccnu.military.repository.DynamicCostBatchRepository;
import com.ccnu.military.repository.DynamicCostSimulationDetailRepository;
import com.ccnu.military.repository.DynamicCostTemplateRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class DynamicCostEffectivenessService {

    private final DynamicCostTemplateRepository templateRepository;
    private final DynamicCostBatchRepository batchRepository;
    private final DynamicCostSimulationDetailRepository simulationDetailRepository;
    private final ObjectMapper objectMapper;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    // ==================== 模板管理 ====================

    public DynamicCostTemplate saveTemplate(DynamicCostTemplateSaveRequest request) {
        DynamicCostTemplate template;
        
        if (request.getId() != null) {
            template = templateRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("模板不存在"));
        } else {
            // 检查名称是否重复（数据库中可能已有重复数据，只需检查是否存在）
            List<DynamicCostTemplate> existing = templateRepository.findByTemplateNameAndStatus(
                    request.getTemplateName(), 1);
            if (!existing.isEmpty()) {
                throw new RuntimeException("模板名称已存在，请使用其他名称");
            }
            template = new DynamicCostTemplate();
            template.setTemplateCode(generateTemplateCode());
        }
        
        template.setTemplateName(request.getTemplateName());
        template.setStatus(1);
        
        try {
            template.setCostConfig(objectMapper.writeValueAsString(request.getCostIndicators()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException("配置序列化失败", e);
        }
        
        template.setTotalCostCount(
                request.getCostIndicators() != null ? request.getCostIndicators().size() : 0);
        
        return templateRepository.save(template);
    }

    public List<DynamicCostTemplateVO> getTemplateList() {
        return templateRepository.findAllByOrderByCreatedTimeDesc().stream()
                .filter(t -> t.getStatus() == 1)
                .map(this::toTemplateVO)
                .collect(Collectors.toList());
    }

    public DynamicCostTemplate getTemplateById(Long id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
    }

    public void deleteTemplate(Long id) {
        DynamicCostTemplate template = templateRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        template.setStatus(0);
        templateRepository.save(template);
    }

    // ==================== 批次管理 ====================

    public List<DynamicCostBatch> getBatchList(Long templateId) {
        return batchRepository.findByTemplateIdOrderByCreatedTimeDesc(templateId).stream()
                .filter(b -> b.getStatus() == 1)
                .collect(Collectors.toList());
    }

    public DynamicCostBatchDetailVO getBatchDetail(Long batchId) {
        log.info("获取批次详情 - Batch ID: {}", batchId);
        
        DynamicCostBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("批次不存在"));
        
        DynamicCostTemplate template = templateRepository.findById(batch.getTemplateId())
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        
        List<DynamicCostSimulationDetail> details = 
                simulationDetailRepository.findByBatchIdOrderByIterationNumberAsc(batchId);
        
        log.info("查询到 {} 条模拟详情", details.size());
        
        DynamicCostBatchDetailVO vo = new DynamicCostBatchDetailVO();
        vo.setBatchId(batch.getId());
        vo.setBatchCode(batch.getBatchCode());
        vo.setBatchName(batch.getBatchName());
        vo.setTemplateId(batch.getTemplateId());
        vo.setTemplateName(template.getTemplateName());
        vo.setSimulationCount(batch.getSimulationCount());
        vo.setRandomSeed(batch.getRandomSeed());
        vo.setCostScore(batch.getCostScore());
        vo.setEffectivenessCostRatio(batch.getEffectivenessCostRatio());
        vo.setCreatedTime(batch.getCreatedTime());
        
        if (batch.getResultSummary() != null) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, DynamicCostSimulationResult.DistributionStats> distribution = 
                        objectMapper.readValue(batch.getResultSummary(), 
                                objectMapper.getTypeFactory().constructMapType(Map.class, String.class, 
                                        DynamicCostSimulationResult.DistributionStats.class));
                vo.setDistribution(distribution);
            } catch (JsonProcessingException e) {
                log.error("解析resultSummary失败", e);
            }
        }
        
        vo.setDetailList(details.stream()
                .map(d -> {
                    DynamicCostBatchDetailVO.SimulationDetail detail = new DynamicCostBatchDetailVO.SimulationDetail();
                    detail.setIterationNumber(d.getIterationNumber());
                    detail.setIndicatorName(d.getIndicatorName());
                    detail.setIndicatorCode(d.getIndicatorCode());
                    detail.setLevel1Name(d.getLevel1Name());
                    detail.setLevel2Name(d.getLevel2Name());
                    detail.setSimulatedValue(d.getSimulatedValue());
                    detail.setNormalizedValue(d.getNormalizedValue());
                    return detail;
                })
                .collect(Collectors.toList()));
        
        return vo;
    }

    public void deleteBatch(Long batchId) {
        DynamicCostBatch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("批次不存在"));
        batch.setStatus(0);
        batchRepository.save(batch);
        simulationDetailRepository.deleteByBatchId(batchId);
    }

    // ==================== 模拟计算 ====================

    @Transactional
    public DynamicCostSimulationResult runSimulation(DynamicCostSimulationRequest request) {
        DynamicCostTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new RuntimeException("模板不存在"));
        
        log.info("开始模拟计算 - 模板ID: {}, 模拟次数: {}", request.getTemplateId(), request.getSimulationCount());
        
        int simulationCount = request.getSimulationCount() != null ? request.getSimulationCount() : 1000;
        long seed = request.getRandomSeed() != null ? request.getRandomSeed() : System.currentTimeMillis();
        Random random = new Random(seed);
        
        // 只获取成本指标
        List<DynamicCostTemplateSaveRequest.IndicatorConfig> costIndicators = parseIndicators(template.getCostConfig());
        
        log.info("解析到 {} 个成本指标", costIndicators.size());
        
        if (costIndicators.isEmpty()) {
            throw new RuntimeException("成本指标配置为空");
        }
        
        List<double[]> costSimulations = new ArrayList<>();
        
        for (int i = 0; i < simulationCount; i++) {
            double[] costValues = simulateIndicators(costIndicators, random);
            costSimulations.add(costValues);
        }
        
        double[] costScores = calculateScores(costSimulations, costIndicators);
        
        // 创建批次
        DynamicCostBatch batch = new DynamicCostBatch();
        batch.setTemplateId(request.getTemplateId());
        batch.setBatchCode(generateBatchCode());
        batch.setBatchName(request.getBatchName());
        batch.setSimulationCount(simulationCount);
        batch.setRandomSeed(seed);
        batch.setCostScore(BigDecimal.valueOf(calculateMean(costScores)));
        
        // 计算效费比（假设效费比 = 100 / 成本得分）
        double avgCostScore = calculateMean(costScores);
        batch.setEffectivenessCostRatio(BigDecimal.valueOf(avgCostScore > 0 ? 100 / avgCostScore : 0));
        
        Map<String, DynamicCostSimulationResult.DistributionStats> distribution = new HashMap<>();
        distribution.put("cost", calculateDistributionStats(costScores));
        
        try {
            batch.setResultSummary(objectMapper.writeValueAsString(distribution));
        } catch (JsonProcessingException e) {
            log.error("序列化distribution失败", e);
        }
        
        batch = batchRepository.save(batch);
        log.info("批次保存成功 - Batch ID: {}, Batch Code: {}", batch.getId(), batch.getBatchCode());
        
        // 保存详细数据
        List<DynamicCostSimulationDetail> details = new ArrayList<>();
        for (int i = 0; i < simulationCount; i++) {
            for (int j = 0; j < costIndicators.size(); j++) {
                DynamicCostSimulationDetail detail = createDetail(batch.getId(), i + 1, 
                        costIndicators.get(j), costSimulations.get(i)[j]);
                details.add(detail);
            }
        }
        simulationDetailRepository.saveAll(details);
        log.info("保存 {} 条模拟详情记录", details.size());
        
        // 构建返回结果
        DynamicCostSimulationResult result = new DynamicCostSimulationResult();
        result.setBatchId(batch.getId());
        result.setBatchCode(batch.getBatchCode());
        result.setCostScore(batch.getCostScore());
        result.setEffectivenessCostRatio(batch.getEffectivenessCostRatio());
        result.setDistribution(distribution);
        
        return result;
    }

    // ==================== 原数据来源（dynamic_qt_record）====================

    /**
     * 获取 dynamic_qt_record 中的模板列表
     */
    public List<Map<String, Object>> getQtTemplateList() {
        String sql = "SELECT DISTINCT template_id, template_name, COUNT(DISTINCT batch_id) as batch_count " +
                "FROM dynamic_qt_record WHERE template_name IS NOT NULL GROUP BY template_id, template_name ORDER BY template_id";
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 根据模板ID获取 dynamic_qt_record 中的批次列表
     */
    public List<Map<String, Object>> getQtBatchListByTemplate(Long templateId) {
        String sql = "SELECT DISTINCT batch_id, template_name, MAX(created_at) as created_at, " +
                "COUNT(DISTINCT operation_id) as operation_count " +
                "FROM dynamic_qt_record WHERE template_id = ? GROUP BY batch_id, template_name ORDER BY created_at DESC";
        return jdbcTemplate.queryForList(sql, templateId);
    }

    /**
     * 根据批次ID获取作战ID列表
     */
    public List<String> getQtOperationIds(String batchId) {
        String sql = "SELECT DISTINCT operation_id FROM dynamic_qt_record " +
                "WHERE batch_id = ? AND normalization_name IS NULL ORDER BY operation_id";
        return jdbcTemplate.queryForList(sql, String.class, batchId);
    }

    /**
     * 根据模板和批次获取效费分析数据
     * 思路：
     * 1. 从 dynamic_qt_record 获取作战ID列表
     * 2. 根据成本模板的配置（最大值、最小值、分布类型）为每个作战ID生成模拟数据
     * 3. 计算加权成本
     */
    public Map<String, Object> getEffectivenessData(Long templateId, String batchId) {
        log.info("【效费分析Service】getEffectivenessData 开始, templateId={}, batchId={}", templateId, batchId);

        DynamicCostTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> {
                    log.error("【效费分析Service】成本模板不存在, templateId={}", templateId);
                    return new RuntimeException("成本模板不存在");
                });
        log.info("【效费分析Service】找到模板: {}, 成本指标配置长度={}", template.getTemplateName(),
                template.getCostConfig() != null ? template.getCostConfig().length() : 0);

        // 获取作战ID列表
        List<String> operationIds = getQtOperationIds(batchId);
        log.info("【效费分析Service】获取到作战ID列表: {}, 数量={}", operationIds, operationIds.size());

        // 解析成本指标配置
        @SuppressWarnings("unchecked")
        List<DynamicCostTemplateSaveRequest.IndicatorConfig> costIndicators =
                parseIndicators(template.getCostConfig());
        log.info("【效费分析Service】解析成本指标数量: {}", costIndicators.size());

        // 生成模拟数据
        List<Map<String, Object>> simulatedData = new ArrayList<>();
        Random random = new Random();

        StringBuilder debugInfo = new StringBuilder();
        debugInfo.append("\n========== 效费分析数据调试 ==========\n");
        debugInfo.append(String.format("作战ID列表: %s\n", operationIds));
        debugInfo.append(String.format("成本指标数量: %d\n", costIndicators.size()));
        debugInfo.append("成本指标名称列表:\n");
        for (int i = 0; i < costIndicators.size(); i++) {
            DynamicCostTemplateSaveRequest.IndicatorConfig ind = costIndicators.get(i);
            debugInfo.append(String.format("  [%d] %s (level2Name: %s, indicatorCode: %s)\n",
                    i, ind.getLevel1Name() + "-" + ind.getLevel2Name(), ind.getLevel2Name(), ind.getIndicatorCode()));
        }
        debugInfo.append("\n生成的模拟数据 (operation_id + secondary_name -> normalized_value):\n");

        for (String operationId : operationIds) {
            // 对每个作战ID和每个成本指标生成一条记录
            for (DynamicCostTemplateSaveRequest.IndicatorConfig ind : costIndicators) {
                Map<String, Object> row = new HashMap<>();
                row.put("operation_id", operationId);
                // 使用 col_indicatorCode 作为 secondary_name（前端 level1Groups 生成的 prop 格式）
                row.put("secondary_name", "col_" + ind.getIndicatorCode());
                // 生成模拟值
                double value = generateSimulatedValue(ind, random);
                row.put("normalized_value", value);
                simulatedData.add(row);

                // 记录匹配键
                String matchKey = operationId + "_col_" + ind.getIndicatorCode();
                debugInfo.append(String.format("  '%s' -> %.4f\n", matchKey, value));
            }
        }

        debugInfo.append(String.format("总计生成 %d 条模拟数据\n", simulatedData.size()));
        debugInfo.append("=========================================\n");

        log.info(debugInfo.toString());

        // 构建结果
        Map<String, Object> result = new HashMap<>();
        result.put("operationIds", operationIds);
        result.put("costIndicators", costIndicators);
        result.put("normalizedData", simulatedData);
        result.put("templateId", templateId);
        result.put("batchId", batchId);

        log.info("【效费分析Service】getEffectivenessData 完成, operationIds={}, costIndicators={}, normalizedData={}",
                operationIds.size(), costIndicators.size(), simulatedData.size());
        return result;
    }

    /**
     * 根据指标配置生成模拟值
     */
    private double generateSimulatedValue(DynamicCostTemplateSaveRequest.IndicatorConfig ind, Random random) {
        double min = ind.getMinValue() != null ? ind.getMinValue() : 0;
        double max = ind.getMaxValue() != null ? ind.getMaxValue() : 100;

        if ("normal".equalsIgnoreCase(ind.getDistributionType())) {
            // 正态分布：均值=中点，标准差=(max-min)/6
            double mean = (max + min) / 2;
            double std = (max - min) / 6;
            return mean + std * nextGaussian(random);
        } else {
            // 均匀分布
            return min + random.nextDouble() * (max - min);
        }
    }

    /**
     * 生成标准正态分布随机数（Box-Muller方法）
     */
    private double nextGaussian(Random random) {
        return random.nextGaussian();
    }

    // ==================== 私有方法 ====================

    private DynamicCostTemplateVO toTemplateVO(DynamicCostTemplate template) {
        DynamicCostTemplateVO vo = new DynamicCostTemplateVO();
        vo.setId(template.getId());
        vo.setTemplateName(template.getTemplateName());
        vo.setTemplateCode(template.getTemplateCode());
        vo.setTotalCostCount(template.getTotalCostCount());
        vo.setCreatedTime(template.getCreatedTime());
        return vo;
    }

    private String generateTemplateCode() {
        return "DCT_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    private String generateBatchCode() {
        return "DCB_" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    }

    @SuppressWarnings("unchecked")
    private List<DynamicCostTemplateSaveRequest.IndicatorConfig> parseIndicators(String json) {
        if (json == null || json.isEmpty() || "[]".equals(json)) {
            return new ArrayList<>();
        }
        try {
            return objectMapper.readValue(json, 
                    objectMapper.getTypeFactory().constructCollectionType(List.class, 
                            DynamicCostTemplateSaveRequest.IndicatorConfig.class));
        } catch (JsonProcessingException e) {
            log.error("解析indicators失败", e);
            return new ArrayList<>();
        }
    }

    private double[] simulateIndicators(List<DynamicCostTemplateSaveRequest.IndicatorConfig> indicators, Random random) {
        double[] values = new double[indicators.size()];
        for (int i = 0; i < indicators.size(); i++) {
            DynamicCostTemplateSaveRequest.IndicatorConfig config = indicators.get(i);
            double min = config.getMinValue() != null ? config.getMinValue() : 0;
            double max = config.getMaxValue() != null ? config.getMaxValue() : 100;
            
            if ("normal".equals(config.getDistributionType())) {
                double mean = (min + max) / 2;
                double stdDev = (max - min) / 6;
                values[i] = mean + stdDev * random.nextGaussian();
                values[i] = Math.max(min, Math.min(max, values[i]));
            } else {
                values[i] = min + (max - min) * random.nextDouble();
            }
        }
        return values;
    }

    private double[] calculateScores(List<double[]> simulations, 
            List<DynamicCostTemplateSaveRequest.IndicatorConfig> indicators) {
        double[] scores = new double[simulations.size()];
        double totalWeight = indicators.stream()
                .mapToDouble(i -> i.getWeight() != null ? i.getWeight() : 0)
                .sum();
        
        for (int i = 0; i < simulations.size(); i++) {
            double weightedSum = 0;
            for (int j = 0; j < indicators.size(); j++) {
                double weight = indicators.get(j).getWeight() != null ? indicators.get(j).getWeight() : 0;
                weightedSum += simulations.get(i)[j] * weight;
            }
            scores[i] = totalWeight > 0 ? weightedSum / totalWeight : 0;
        }
        return scores;
    }

    private double calculateMean(double[] values) {
        return Arrays.stream(values).average().orElse(0);
    }

    private DynamicCostSimulationResult.DistributionStats calculateDistributionStats(double[] values) {
        double mean = calculateMean(values);
        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        double std = Math.sqrt(variance);
        
        double[] sorted = Arrays.stream(values).sorted().toArray();
        
        DynamicCostSimulationResult.DistributionStats stats = new DynamicCostSimulationResult.DistributionStats();
        stats.setMin(round(sorted[0], 4));
        stats.setMax(round(sorted[sorted.length - 1], 4));
        stats.setMean(round(mean, 4));
        stats.setStd(round(std, 4));
        stats.setPercentile25(round(percentile(sorted, 0.25), 4));
        stats.setPercentile50(round(percentile(sorted, 0.5), 4));
        stats.setPercentile75(round(percentile(sorted, 0.75), 4));
        stats.setPercentile90(round(percentile(sorted, 0.9), 4));
        stats.setPercentile95(round(percentile(sorted, 0.95), 4));
        
        return stats;
    }

    private double percentile(double[] sorted, double p) {
        if (sorted.length == 0) return 0;
        double index = p * (sorted.length - 1);
        int lower = (int) Math.floor(index);
        int upper = (int) Math.ceil(index);
        if (lower == upper) return sorted[lower];
        return sorted[lower] * (upper - index) + sorted[upper] * (index - lower);
    }

    private DynamicCostSimulationDetail createDetail(Long batchId, int iteration, 
            DynamicCostTemplateSaveRequest.IndicatorConfig config, double simulatedValue) {
        DynamicCostSimulationDetail detail = new DynamicCostSimulationDetail();
        detail.setBatchId(batchId);
        detail.setIterationNumber(iteration);
        detail.setIndicatorName(config.getLevel2Name());
        detail.setIndicatorCode(config.getIndicatorCode());
        detail.setLevel1Name(config.getLevel1Name());
        detail.setLevel2Name(config.getLevel2Name());
        detail.setSimulatedValue(BigDecimal.valueOf(round(simulatedValue, 4)));
        detail.setNormalizedValue(BigDecimal.valueOf(round(simulatedValue / 100, 4)));
        detail.setWeight(BigDecimal.valueOf(config.getWeight() != null ? config.getWeight() : 0));
        return detail;
    }

    private double round(double value, int places) {
        if (Double.isNaN(value) || Double.isInfinite(value)) return 0;
        return BigDecimal.valueOf(value)
                .setScale(places, RoundingMode.HALF_UP)
                .doubleValue();
    }
}
