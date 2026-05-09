package com.ccnu.military.service;

import com.ccnu.military.dto.*;
import com.ccnu.military.entity.DynamicAhpMatrix;
import com.ccnu.military.entity.DynamicAhpWeights;
import com.ccnu.military.entity.DynamicDimension;
import com.ccnu.military.entity.DynamicTemplate;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.repository.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 动态AHP服务
 * 支持动态指标体系的层次分析法权重配置
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicAhpService {

    private final DynamicAhpMatrixRepository matrixRepository;
    private final DynamicAhpWeightsRepository weightsRepository;
    private final DynamicTemplateRepository templateRepository;
    private final DynamicDimensionRepository dimensionRepository;
    private final ExpertBaseInfoRepository expertBaseInfoRepository;
    private final ObjectMapper objectMapper;

    // AHP一致性指标RI值（根据矩阵阶数）
    private static final Map<Integer, Double> RI_VALUES = Map.ofEntries(
            Map.entry(1, 0.0),
            Map.entry(2, 0.0),
            Map.entry(3, 0.58),
            Map.entry(4, 0.90),
            Map.entry(5, 1.12),
            Map.entry(6, 1.24),
            Map.entry(7, 1.32),
            Map.entry(8, 1.41),
            Map.entry(9, 1.45),
            Map.entry(10, 1.49),
            Map.entry(11, 1.51),
            Map.entry(12, 1.54),
            Map.entry(13, 1.56),
            Map.entry(14, 1.57),
            Map.entry(15, 1.59)
    );

    /**
     * 获取指标模板列表
     */
    public List<Map<String, Object>> getIndicatorTemplates() {
        List<DynamicTemplate> templates = templateRepository.findAllByOrderByCreatedAtDesc();
        return templates.stream().map(tpl -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", tpl.getId());
            map.put("templateName", tpl.getTemplateName());
            map.put("templateCode", tpl.getTemplateCode());
            map.put("description", tpl.getDescription());
            map.put("status", tpl.getStatus());
            map.put("levelCount", tpl.getLevelCount());
            map.put("primaryCount", tpl.getPrimaryCount());
            map.put("secondaryCount", tpl.getSecondaryCount());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取指标树结构
     */
    public DynamicIndicatorTreeDTO getIndicatorTree(Long templateId) {
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + templateId));

        DynamicIndicatorTreeDTO dto = new DynamicIndicatorTreeDTO();
        dto.setId(template.getId());
        dto.setTemplateName(template.getTemplateName());
        dto.setTemplateCode(template.getTemplateCode());
        dto.setStatus(template.getStatus() != null ? template.getStatus().name() : "UNKNOWN");

        // 获取所有维度
        List<DynamicDimension> dimensions = dimensionRepository.findByTemplateIdOrderBySortOrder(templateId);

        // 按层级分组
        Map<String, List<DynamicDimension>> byLevel = dimensions.stream()
                .filter(d -> d.getDimensionLevel() == DynamicDimension.DimensionLevel.LEVEL)
                .collect(Collectors.groupingBy(DynamicDimension::getName));

        // 构建层级节点
        List<DynamicIndicatorTreeDTO.LevelNode> levelNodes = new ArrayList<>();
        for (Map.Entry<String, List<DynamicDimension>> entry : byLevel.entrySet()) {
            DynamicDimension levelDim = entry.getValue().get(0);
            DynamicIndicatorTreeDTO.LevelNode levelNode = new DynamicIndicatorTreeDTO.LevelNode();
            levelNode.setId(levelDim.getId());
            levelNode.setName(levelDim.getName());
            levelNode.setDescription(levelDim.getDescription());
            levelNode.setSortOrder(levelDim.getSortOrder());

            // 获取该层级下的一级维度
            List<DynamicDimension> primaries = dimensions.stream()
                    .filter(d -> d.getDimensionLevel() == DynamicDimension.DimensionLevel.PRIMARY
                            && d.getParentId() != null && d.getParentId().equals(levelDim.getId()))
                    .sorted(Comparator.comparingInt(DynamicDimension::getSortOrder))
                    .collect(Collectors.toList());

            // 构建一级维度节点
            List<DynamicIndicatorTreeDTO.PrimaryNode> primaryNodes = new ArrayList<>();
            for (DynamicDimension primary : primaries) {
                DynamicIndicatorTreeDTO.PrimaryNode primaryNode = new DynamicIndicatorTreeDTO.PrimaryNode();
                primaryNode.setId(primary.getId());
                primaryNode.setName(primary.getName());
                primaryNode.setCode(primary.getCode());
                primaryNode.setSortOrder(primary.getSortOrder());
                primaryNode.setWeight(primary.getWeight() != null ? primary.getWeight().doubleValue() : null);

                // 获取该一级维度下的二级指标
                List<DynamicDimension> secondaries = dimensions.stream()
                        .filter(d -> d.getDimensionLevel() == DynamicDimension.DimensionLevel.SECONDARY
                                && d.getParentId() != null && d.getParentId().equals(primary.getId()))
                        .sorted(Comparator.comparingInt(DynamicDimension::getSortOrder))
                        .collect(Collectors.toList());

                // 构建二级指标节点
                List<DynamicIndicatorTreeDTO.SecondaryNode> secondaryNodes = new ArrayList<>();
                for (DynamicDimension sec : secondaries) {
                    DynamicIndicatorTreeDTO.SecondaryNode secondaryNode = new DynamicIndicatorTreeDTO.SecondaryNode();
                    secondaryNode.setId(sec.getId());
                    secondaryNode.setName(sec.getName());
                    secondaryNode.setCode(sec.getCode());
                    secondaryNode.setMetricType(sec.getMetricType() != null ? sec.getMetricType().name() : null);
                    secondaryNode.setAggregationMethod(sec.getAggregationMethod());
                    secondaryNode.setScoreDirection(sec.getScoreDirection() != null ? sec.getScoreDirection().name() : null);
                    secondaryNode.setUnit(sec.getUnit());
                    secondaryNode.setWeight(sec.getWeight() != null ? sec.getWeight().doubleValue() : null);
                    secondaryNode.setBaselineValue(sec.getBaselineValue() != null ? sec.getBaselineValue().doubleValue() : null);
                    secondaryNode.setTargetValue(sec.getTargetValue() != null ? sec.getTargetValue().doubleValue() : null);
                    secondaryNode.setAverageValue(sec.getAverageValue() != null ? sec.getAverageValue().doubleValue() : null);
                    secondaryNode.setSortOrder(sec.getSortOrder());
                    secondaryNodes.add(secondaryNode);
                }

                primaryNode.setSecondaryDimensions(secondaryNodes);
                primaryNodes.add(primaryNode);
            }

            levelNode.setPrimaryDimensions(primaryNodes);
            levelNodes.add(levelNode);
        }

        levelNodes.sort(Comparator.comparingInt(DynamicIndicatorTreeDTO.LevelNode::getSortOrder));
        dto.setLevels(levelNodes);

        // 统计信息
        DynamicIndicatorTreeDTO.Statistics stats = new DynamicIndicatorTreeDTO.Statistics();
        stats.setLevelCount(levelNodes.size());
        stats.setTotalPrimaryDimensions(levelNodes.stream().mapToInt(l -> l.getPrimaryDimensions() != null ? l.getPrimaryDimensions().size() : 0).sum());
        stats.setTotalSecondaryDimensions((int) levelNodes.stream()
                .flatMap(l -> l.getPrimaryDimensions() != null ? l.getPrimaryDimensions().stream() : null)
                .filter(Objects::nonNull)
                .flatMap(p -> p.getSecondaryDimensions() != null ? p.getSecondaryDimensions().stream() : null)
                .filter(Objects::nonNull)
                .count());
        dto.setStatistics(stats);

        return dto;
    }

    /**
     * 获取专家列表
     */
    public List<Map<String, Object>> getExperts() {
        List<ExpertBaseInfo> experts = expertBaseInfoRepository.findAll();
        return experts.stream().map(e -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("expertId", e.getExpertId());
            map.put("expertName", e.getExpertName());
            map.put("workUnit", e.getWorkUnit());
            map.put("titleLevel", e.getTitleLevel());
            map.put("status", e.getStatus());
            return map;
        }).collect(Collectors.toList());
    }

    /**
     * 获取层级名称列表
     */
    public List<String> getLevelNames(Long templateId) {
        List<DynamicDimension> levelDimensions = dimensionRepository
                .findByTemplateIdAndDimensionLevelOrderBySortOrder(templateId, DynamicDimension.DimensionLevel.LEVEL);
        return levelDimensions.stream()
                .map(DynamicDimension::getName)
                .collect(Collectors.toList());
    }

    /**
     * 获取一级维度列表
     */
    public List<Map<String, Object>> getPrimaryDimensions(Long templateId, String levelName) {
        // 获取层级ID
        List<DynamicDimension> levelDims = dimensionRepository
                .findByTemplateIdAndDimensionLevelAndName(templateId, DynamicDimension.DimensionLevel.LEVEL, levelName);
        if (levelDims.isEmpty()) {
            return Collections.emptyList();
        }

        Long levelId = levelDims.get(0).getId();

        // 获取一级维度
        List<DynamicDimension> primaries = dimensionRepository.findByParentIdOrderBySortOrder(levelId);
        return primaries.stream().map(d -> toDimensionMap(d)).collect(Collectors.toList());
    }

    /**
     * 获取二级指标列表
     */
    public List<Map<String, Object>> getSecondaryDimensions(Long templateId, String levelName, String primaryCode) {
        // 获取一级维度
        List<DynamicDimension> primaries = dimensionRepository
                .findByTemplateIdAndCode(templateId, primaryCode);
        if (primaries.isEmpty()) {
            return Collections.emptyList();
        }

        Long primaryId = primaries.get(0).getId();

        // 获取二级指标
        List<DynamicDimension> secondaries = dimensionRepository.findByParentIdOrderBySortOrder(primaryId);
        return secondaries.stream().map(d -> toDimensionMap(d)).collect(Collectors.toList());
    }

    // ==================== 矩阵操作 ====================

    /**
     * 获取层级间比较矩阵（层级之间的比较）
     */
    public DynamicAhpMatrixDTO getLevelBetweenMatrix(Long templateId, Long expertId) {
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

        // 获取所有层级
        List<DynamicDimension> levels = dimensionRepository
                .findByTemplateIdAndDimensionLevelOrderBySortOrder(templateId, DynamicDimension.DimensionLevel.LEVEL);

        if (levels.isEmpty()) {
            return null;
        }

        List<DynamicAhpMatrixDTO.DimensionInfo> dimensions = levels.stream()
                .map(d -> DynamicAhpMatrixDTO.DimensionInfo.builder()
                        .code(d.getCode())
                        .name(d.getName())
                        .description(d.getDescription())
                        .sortOrder(d.getSortOrder())
                        .build())
                .collect(Collectors.toList());

        // 获取已保存的层级间矩阵数据（levelName 为 "LEVEL_BETWEEN"）
        List<DynamicAhpMatrix> savedMatrices = matrixRepository
                .findByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
                        expertId, templateId, "LEVEL_BETWEEN", DynamicAhpMatrix.ComparisonType.LEVEL_BETWEEN);

        Map<String, BigDecimal> scoreMap = new HashMap<>();
        Map<String, BigDecimal> confMap = new HashMap<>();
        for (DynamicAhpMatrix m : savedMatrices) {
            // 使用 rowName_colName 作为 key（层级间矩阵使用名称）
            scoreMap.put(m.getRowName() + "_" + m.getColName(), m.getScore());
            confMap.put(m.getRowName() + "_" + m.getColName(), m.getConfidence());
        }

        // 构建矩阵
        int n = levels.size();
        List<List<DynamicAhpCellDTO>> matrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<DynamicAhpCellDTO> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                DynamicAhpCellDTO cell = DynamicAhpCellDTO.builder()
                        .rowIndex(i)
                        .colIndex(j)
                        .rowCode(levels.get(i).getCode())
                        .rowName(levels.get(i).getName())
                        .colCode(levels.get(j).getCode())
                        .colName(levels.get(j).getName())
                        .diagonal(i == j)
                        .editable(i < j)
                        .build();

                if (i == j) {
                    cell.setScore(BigDecimal.ONE);
                    cell.setConfidence(BigDecimal.ONE);
                } else {
                    // 使用名称作为 key
                    String key = levels.get(i).getName() + "_" + levels.get(j).getName();
                    cell.setScore(scoreMap.get(key));
                    cell.setConfidence(confMap.get(key));
                }
                row.add(cell);
            }
            matrix.add(row);
        }

        return DynamicAhpMatrixDTO.builder()
                .templateId(templateId)
                .templateName(template.getTemplateName())
                .matrixType("LEVEL_BETWEEN")
                .dimensions(dimensions)
                .matrix(matrix)
                .rowCount(n)
                .colCount(n)
                .build();
    }

    /**
     * 获取一级维度间比较矩阵
     */
    public DynamicAhpMatrixDTO getPrimaryBetweenMatrix(Long templateId, String levelName, Long expertId) {
        List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, levelName);
        return buildMatrixDTO(templateId, null, levelName, "PRIMARY_BETWEEN",
                primaries, expertId, DynamicAhpMatrix.ComparisonType.PRIMARY_BETWEEN);
    }

    /**
     * 获取二级指标间比较矩阵
     */
    public DynamicAhpMatrixDTO getSecondaryBetweenMatrix(Long templateId, String levelName,
                                                        String primaryCode, Long expertId) {
        List<Map<String, Object>> secondaries = getSecondaryDimensions(templateId, levelName, primaryCode);
        return buildMatrixDTO(templateId, primaryCode, levelName, "SECONDARY_BETWEEN",
                secondaries, expertId, DynamicAhpMatrix.ComparisonType.SECONDARY_BETWEEN);
    }

    private DynamicAhpMatrixDTO buildMatrixDTO(Long templateId, String parentCode, String levelName,
                                               String matrixType, List<Map<String, Object>> dimensions,
                                               Long expertId, DynamicAhpMatrix.ComparisonType compType) {
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

        List<DynamicAhpMatrixDTO.DimensionInfo> dimInfos = dimensions.stream()
                .map(d -> DynamicAhpMatrixDTO.DimensionInfo.builder()
                        .code((String) d.get("code"))
                        .name((String) d.get("name"))
                        .description((String) d.get("description"))
                        .sortOrder((Integer) d.get("sortOrder"))
                        .metricType((String) d.get("metricType"))
                        .direction((String) d.get("direction"))
                        .build())
                .collect(Collectors.toList());

        // 获取已保存的矩阵数据
        List<DynamicAhpMatrix> savedMatrices;
        if (parentCode == null) {
            savedMatrices = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
                            expertId, templateId, levelName, compType);
        } else {
            savedMatrices = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndParentCode(
                            expertId, templateId, levelName, parentCode);
        }

        Map<String, BigDecimal> scoreMap = new HashMap<>();
        Map<String, BigDecimal> confMap = new HashMap<>();
        for (DynamicAhpMatrix m : savedMatrices) {
            scoreMap.put(m.getRowCode() + "_" + m.getColCode(), m.getScore());
            confMap.put(m.getRowCode() + "_" + m.getColCode(), m.getConfidence());
        }

        int n = dimensions.size();
        List<List<DynamicAhpCellDTO>> matrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<DynamicAhpCellDTO> row = new ArrayList<>();
            String rowCode = (String) dimensions.get(i).get("code");
            String rowName = (String) dimensions.get(i).get("name");
            for (int j = 0; j < n; j++) {
                String colCode = (String) dimensions.get(j).get("code");
                String colName = (String) dimensions.get(j).get("name");

                DynamicAhpCellDTO cell = DynamicAhpCellDTO.builder()
                        .rowIndex(i)
                        .colIndex(j)
                        .rowCode(rowCode)
                        .rowName(rowName)
                        .colCode(colCode)
                        .colName(colName)
                        .diagonal(i == j)
                        .editable(i < j)
                        .build();

                if (i == j) {
                    cell.setScore(BigDecimal.ONE);
                    cell.setConfidence(BigDecimal.ONE);
                } else {
                    String key = rowCode + "_" + colCode;
                    // 如果没有保存数据，返回 null
                    cell.setScore(scoreMap.get(key));
                    cell.setConfidence(confMap.get(key));
                }
                row.add(cell);
            }
            matrix.add(row);
        }

        String parentName = parentCode;
        if (parentCode != null) {
            List<DynamicDimension> primary = dimensionRepository.findByTemplateIdAndCode(templateId, parentCode);
            if (!primary.isEmpty()) {
                parentName = primary.get(0).getName();
            }
        }

        return DynamicAhpMatrixDTO.builder()
                .templateId(templateId)
                .templateName(template.getTemplateName())
                .levelName(levelName)
                .matrixType(matrixType)
                .parentCode(parentCode)
                .parentName(parentName)
                .dimensions(dimInfos)
                .matrix(matrix)
                .rowCount(n)
                .colCount(n)
                .build();
    }

    /**
     * 保存AHP矩阵打分
     */
    @Transactional
    public int saveMatrix(SaveDynamicAhpMatrixRequest request) {
        log.info("开始保存矩阵: expertId={}, templateId={}, levelName={}, matrixType={}, entries={}",
                request.getExpertId(), request.getTemplateId(), request.getLevelName(),
                request.getMatrixType(), request.getEntries() != null ? request.getEntries().size() : 0);

        // 打印entries内容
        if (request.getEntries() != null) {
            for (int i = 0; i < Math.min(3, request.getEntries().size()); i++) {
                SaveDynamicAhpMatrixRequest.MatrixEntry entry = request.getEntries().get(i);
                log.info("Entry {}: rowCode={}, colCode={}, score={}, confidence={}",
                        i, entry.getRowCode(), entry.getColCode(), entry.getScore(), entry.getConfidence());
            }
        }

        DynamicTemplate template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new IllegalArgumentException("模板不存在: " + request.getTemplateId()));

        DynamicAhpMatrix.ComparisonType compType = DynamicAhpMatrix.ComparisonType.valueOf(request.getMatrixType());
        log.info("comparisonType = {}", compType);

        List<DynamicAhpMatrix> matrices = new ArrayList<>();

        for (SaveDynamicAhpMatrixRequest.MatrixEntry entry : request.getEntries()) {
            DynamicAhpMatrix matrix = DynamicAhpMatrix.builder()
                    .expertId(request.getExpertId())
                    .expertName(request.getExpertName())
                    .templateId(request.getTemplateId())
                    .templateName(template.getTemplateName())
                    .levelName(request.getLevelName())
                    .comparisonType(compType)
                    .parentCode(request.getParentCode())
                    .rowCode(entry.getRowCode())
                    .rowName(entry.getRowName())
                    .colCode(entry.getColCode())
                    .colName(entry.getColName())
                    .score(entry.getScore())
                    .confidence(entry.getConfidence())
                    .dimensionCodes(request.getLevelName() + ":" + entry.getRowCode() + "," + entry.getColCode())
                    .dimensionNames(entry.getRowName() + "," + entry.getColName())
                    .dimensionCount(2)
                    .build();

            matrices.add(matrix);
        }

        // 先删除旧的，再保存新的
        if ("LEVEL_BETWEEN".equals(request.getMatrixType())) {
            // 层级间矩阵按 comparisonType 删除
            matrixRepository.deleteByExpertIdAndTemplateIdAndComparisonType(
                    request.getExpertId(), request.getTemplateId(), compType);
        } else if (request.getParentCode() == null) {
            // 一级维度矩阵按 levelName 和 comparisonType 删除
            matrixRepository.deleteByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
                    request.getExpertId(), request.getTemplateId(), request.getLevelName(), compType);
        } else {
            // 二级矩阵按parentCode删除
            List<DynamicAhpMatrix> old = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndParentCode(
                            request.getExpertId(), request.getTemplateId(), request.getLevelName(), request.getParentCode());
            matrixRepository.deleteAll(old);
        }

        matrixRepository.saveAll(matrices);
        log.info("保存矩阵成功, 共 {} 条", matrices.size());
        return matrices.size();
    }

    // ==================== 权重计算 ====================

    /**
     * 计算AHP权重
     */
    public DynamicAhpResultDTO calculateWeights(Long templateId, Long expertId,
                                                 String levelName, String matrixType, String parentCode) {
        // 获取矩阵数据
        List<List<BigDecimal>> matrix = new ArrayList<>();
        List<String> dimensionCodes = new ArrayList<>();
        List<String> dimensionNames = new ArrayList<>();
        List<String> dimensionDescriptions = new ArrayList<>();

        DynamicAhpMatrix.ComparisonType compType = DynamicAhpMatrix.ComparisonType.valueOf(matrixType);

        List<DynamicAhpMatrix> savedMatrices;

        // 层级间矩阵使用不同的查询方式（不按levelName过滤）
        if ("LEVEL_BETWEEN".equals(matrixType)) {
            savedMatrices = matrixRepository
                    .findByExpertIdAndTemplateIdAndComparisonType(expertId, templateId, compType);
            log.info("查询层级间矩阵: expertId={}, templateId={}, compType={}, 记录数={}",
                    expertId, templateId, compType, savedMatrices.size());
        } else if (parentCode == null) {
            // 一级维度间比较
            savedMatrices = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
                            expertId, templateId, levelName, compType);
            log.info("查询一级维度矩阵: expertId={}, templateId={}, levelName={}, compType={}, 记录数={}",
                    expertId, templateId, levelName, compType, savedMatrices.size());
        } else {
            // 二级指标间比较
            savedMatrices = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndParentCode(
                            expertId, templateId, levelName, parentCode);
            log.info("查询二级指标矩阵: expertId={}, templateId={}, levelName={}, parentCode={}, 记录数={}",
                    expertId, templateId, levelName, parentCode, savedMatrices.size());
        }

        if (savedMatrices.isEmpty()) {
            log.error("没有找到矩阵数据: matrixType={}, levelName={}, parentCode={}", matrixType, levelName, parentCode);
            throw new IllegalStateException("没有找到矩阵数据: matrixType=" + matrixType + ", levelName=" + levelName);
        }

        // 构建维度信息（去重）
        Set<String> dimCodesSet = new LinkedHashSet<>();
        Map<String, String> dimNamesMap = new LinkedHashMap<>();
        Map<String, String> dimDescMap = new LinkedHashMap<>();
        for (DynamicAhpMatrix m : savedMatrices) {
            // 对于层级间矩阵，使用 rowName/colName 作为 code
            if ("LEVEL_BETWEEN".equals(matrixType)) {
                dimCodesSet.add(m.getRowName());
                dimCodesSet.add(m.getColName());
                dimNamesMap.put(m.getRowName(), m.getRowName());
                dimNamesMap.put(m.getColName(), m.getColName());
            } else {
                dimCodesSet.add(m.getRowCode());
                dimCodesSet.add(m.getColCode());
                dimNamesMap.put(m.getRowCode(), m.getRowName());
                dimNamesMap.put(m.getColCode(), m.getColName());
            }
        }

        // 获取维度描述
        if (parentCode != null) {
            // 二级指标，从 dynamic_dimension 表获取
            List<Map<String, Object>> secondaries = getSecondaryDimensions(templateId, levelName, parentCode);
            for (Map<String, Object> sec : secondaries) {
                dimDescMap.put((String) sec.get("code"), (String) sec.get("description"));
            }
        } else if ("LEVEL_BETWEEN".equals(matrixType)) {
            // 层级间矩阵，不需要描述
            for (String code : dimCodesSet) {
                dimDescMap.put(code, "");
            }
        } else {
            // 一级维度
            List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, levelName);
            for (Map<String, Object> prim : primaries) {
                dimDescMap.put((String) prim.get("code"), (String) prim.get("description"));
            }
        }

        dimensionCodes.addAll(dimCodesSet);
        for (String code : dimensionCodes) {
            dimensionNames.add(dimNamesMap.getOrDefault(code, code));
            dimensionDescriptions.add(dimDescMap.getOrDefault(code, ""));
        }

        log.info("维度列表: {}", dimensionNames);

        int n = dimensionCodes.size();
        Map<String, Integer> dimIndex = new HashMap<>();
        for (int i = 0; i < dimensionCodes.size(); i++) {
            dimIndex.put(dimensionCodes.get(i), i);
        }

        // 构建矩阵
        matrix = new ArrayList<>();
        for (int i = 0; i < n; i++) {
            List<BigDecimal> row = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    row.add(BigDecimal.ONE);
                } else {
                    String rowCode = dimensionCodes.get(i);
                    String colCode = dimensionCodes.get(j);

                    // 查找保存的分数
                    BigDecimal score = null;
                    if ("LEVEL_BETWEEN".equals(matrixType)) {
                        // 层级间矩阵使用名称查找
                        final String finalRowCode = rowCode;
                        final String finalColCode = colCode;
                        score = savedMatrices.stream()
                                .filter(m -> m.getRowName().equals(finalRowCode) && m.getColName().equals(finalColCode))
                                .findFirst()
                                .map(DynamicAhpMatrix::getScore)
                                .orElse(null);
                        // 如果没有，找反向的并取倒数
                        if (score == null) {
                            final String finalRowCode2 = rowCode;
                            final String finalColCode2 = colCode;
                            BigDecimal reverseScore = savedMatrices.stream()
                                    .filter(m -> m.getRowName().equals(finalColCode2) && m.getColName().equals(finalRowCode2))
                                    .findFirst()
                                    .map(DynamicAhpMatrix::getScore)
                                    .orElse(null);
                            if (reverseScore != null && reverseScore.compareTo(BigDecimal.ZERO) > 0) {
                                score = BigDecimal.ONE.divide(reverseScore, 4, RoundingMode.HALF_UP);
                            }
                        }
                    } else {
                        final String finalRowCode = rowCode;
                        final String finalColCode = colCode;
                        score = savedMatrices.stream()
                                .filter(m -> m.getRowCode().equals(finalRowCode) && m.getColCode().equals(finalColCode))
                                .findFirst()
                                .map(DynamicAhpMatrix::getScore)
                                .orElse(null);
                        // 如果没有，找反向的并取倒数
                        if (score == null) {
                            final String finalRowCode2 = rowCode;
                            final String finalColCode2 = colCode;
                            BigDecimal reverseScore = savedMatrices.stream()
                                    .filter(m -> m.getRowCode().equals(finalColCode2) && m.getColCode().equals(finalRowCode2))
                                    .findFirst()
                                    .map(DynamicAhpMatrix::getScore)
                                    .orElse(null);
                            if (reverseScore != null && reverseScore.compareTo(BigDecimal.ZERO) > 0) {
                                score = BigDecimal.ONE.divide(reverseScore, 4, RoundingMode.HALF_UP);
                            }
                        }
                    }

                    // 如果还是没有，返回默认值1（仅用于计算，标记为未填写）
                    if (score == null) {
                        log.warn("矩阵数据缺失: row={}, col={}, 使用默认值1", rowCode, colCode);
                        score = BigDecimal.ONE;
                    }
                    row.add(score);
                }
            }
            matrix.add(row);
        }

        // 打印矩阵
        log.info("构建的判断矩阵:");
        for (int i = 0; i < n; i++) {
            StringBuilder sb = new StringBuilder();
            sb.append("  [");
            for (int j = 0; j < n; j++) {
                sb.append(matrix.get(i).get(j)).append(", ");
            }
            sb.append("]");
            log.info(sb.toString());
        }

        // 计算权重
        return calculateAhpWeights(templateId, expertId, levelName, matrixType, parentCode,
                dimensionCodes, dimensionNames, dimensionDescriptions, matrix);
    }

    /**
     * 计算AHP权重的核心算法
     */
    private DynamicAhpResultDTO calculateAhpWeights(Long templateId, Long expertId, String levelName,
                                                     String matrixType, String parentCode,
                                                     List<String> dimensionCodes, List<String> dimensionNames,
                                                     List<String> dimensionDescriptions,
                                                     List<List<BigDecimal>> matrix) {
        int n = dimensionCodes.size();

        // 计算权重向量（列和归一化法）
        List<BigDecimal> weights = new ArrayList<>();

        // 计算每列的和
        List<BigDecimal> colSums = new ArrayList<>();
        for (int j = 0; j < n; j++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int i = 0; i < n; i++) {
                sum = sum.add(matrix.get(i).get(j));
            }
            colSums.add(sum);
        }

        // 归一化并计算权重
        for (int i = 0; i < n; i++) {
            BigDecimal rowSum = BigDecimal.ZERO;
            for (int j = 0; j < n; j++) {
                rowSum = rowSum.add(matrix.get(i).get(j).divide(colSums.get(j), 10, RoundingMode.HALF_UP));
            }
            weights.add(rowSum.divide(new BigDecimal(n), 10, RoundingMode.HALF_UP));
        }

        // 计算 λmax
        BigDecimal lambdaMax = BigDecimal.ZERO;
        for (int i = 0; i < n; i++) {
            BigDecimal sum = BigDecimal.ZERO;
            for (int j = 0; j < n; j++) {
                sum = sum.add(matrix.get(i).get(j).multiply(weights.get(j)));
            }
            // (Aw)/w
            if (weights.get(i).compareTo(BigDecimal.ZERO) > 0) {
                lambdaMax = lambdaMax.add(sum.divide(weights.get(i), 10, RoundingMode.HALF_UP));
            }
        }
        lambdaMax = lambdaMax.divide(new BigDecimal(n), 10, RoundingMode.HALF_UP);

        // 计算CI和CR
        BigDecimal ci = BigDecimal.ZERO;
        if (n > 1) {
            ci = lambdaMax.subtract(new BigDecimal(n))
                    .divide(new BigDecimal(n - 1), 10, RoundingMode.HALF_UP);
        }

        double ri = RI_VALUES.getOrDefault(n, 1.59);
        BigDecimal cr = BigDecimal.ZERO;
        if (ri > 0) {
            cr = ci.divide(new BigDecimal(ri), 10, RoundingMode.HALF_UP);
        }

        boolean consistent = cr.compareTo(new BigDecimal("0.1")) <= 0;

        // 构建权重列表
        List<DynamicAhpResultDTO.DimensionWeight> dimWeights = new ArrayList<>();
        for (int i = 0; i < dimensionCodes.size(); i++) {
            dimWeights.add(DynamicAhpResultDTO.DimensionWeight.builder()
                    .code(dimensionCodes.get(i))
                    .name(dimensionNames.get(i))
                    .description(i < dimensionDescriptions.size() ? dimensionDescriptions.get(i) : "")
                    .weight(weights.get(i))
                    .sortOrder(i)
                    .build());
        }

        return DynamicAhpResultDTO.builder()
                .templateId(templateId)
                .levelName(levelName)
                .matrixType(matrixType)
                .parentCode(parentCode)
                .dimensionCount(n)
                .dimensionNames(dimensionNames)
                .weights(dimWeights)
                .lambdaMax(lambdaMax)
                .ci(ci)
                .ri(new BigDecimal(ri))
                .cr(cr)
                .consistent(consistent)
                .build();
    }

    /**
     * 计算所有AHP权重（跨层级综合）
     */
    @Transactional
    public DynamicAhpCompleteResultDTO calculateAllWeights(Long templateId, Long expertId) {
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("模板不存在"));

        // 获取所有层级
        List<String> levels = getLevelNames(templateId);

        DynamicAhpCompleteResultDTO result = DynamicAhpCompleteResultDTO.builder()
                .templateId(templateId)
                .templateName(template.getTemplateName())
                .expertId(expertId)
                .expertName("专家" + expertId)
                .allLeaves(new ArrayList<>())
                .inconsistentMatrices(new ArrayList<>())
                .build();

        BigDecimal totalWeight = BigDecimal.ZERO;
        boolean allConsistent = true;

        // 计算层级间的权重
        Map<String, BigDecimal> levelWeights = new HashMap<>();
        try {
            DynamicAhpResultDTO levelResult = calculateWeights(templateId, expertId, "LEVEL_BETWEEN", "LEVEL_BETWEEN", null);
            for (DynamicAhpResultDTO.DimensionWeight w : levelResult.getWeights()) {
                levelWeights.put(w.getName(), w.getWeight());
            }
            if (!levelResult.getConsistent()) {
                allConsistent = false;
                result.getInconsistentMatrices().add("层级间比较");
            }
            log.info("层级间权重计算成功: {}", levelWeights);
        } catch (Exception e) {
            log.warn("计算层级权重失败: {}, 使用平均权重", e.getMessage());
            // 如果没有层级间矩阵，使用平均权重
            BigDecimal avgWeight = BigDecimal.ONE.divide(new BigDecimal(levels.size()), 6, BigDecimal.ROUND_HALF_UP);
            for (String level : levels) {
                levelWeights.put(level, avgWeight);
            }
            log.info("使用平均层级权重: {}", levelWeights);
        }

        // 计算每个层级的权重
        for (String level : levels) {
            // 获取当前层级的权重
            BigDecimal levelWeight = levelWeights.getOrDefault(level, BigDecimal.ZERO);
            if (levelWeight.compareTo(BigDecimal.ZERO) == 0) {
                // 如果没有层级权重，使用平均权重
                levelWeight = BigDecimal.ONE.divide(new BigDecimal(levels.size()), 6, BigDecimal.ROUND_HALF_UP);
                log.warn("层级 {} 权重为0，使用平均权重: {}", level, levelWeight);
            }

            // 计算一级维度权重
            List<DynamicAhpResultDTO.DimensionWeight> primaryWeights = new ArrayList<>();
            try {
                DynamicAhpResultDTO primaryResult = calculateWeights(templateId, expertId, level, "PRIMARY_BETWEEN", null);
                primaryWeights = primaryResult.getWeights();
                if (!primaryResult.getConsistent()) {
                    allConsistent = false;
                    result.getInconsistentMatrices().add(level + "-一级维度");
                }
                log.info("层级 {} 一级维度权重: {}", level, primaryWeights.stream()
                        .map(w -> w.getName() + "=" + w.getWeight())
                        .collect(Collectors.joining(", ")));
            } catch (Exception e) {
                log.warn("计算一级维度权重失败: {}, 使用平均权重", e.getMessage());
                // 使用平均权重
                List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, level);
                if (!primaries.isEmpty()) {
                    BigDecimal avgPrimaryWeight = BigDecimal.ONE.divide(new BigDecimal(primaries.size()), 6, BigDecimal.ROUND_HALF_UP);
                    for (Map<String, Object> prim : primaries) {
                        String code = (String) prim.get("code");
                        String name = (String) prim.get("name");
                        primaryWeights.add(DynamicAhpResultDTO.DimensionWeight.builder()
                                .code(code)
                                .name(name)
                                .weight(avgPrimaryWeight)
                                .build());
                    }
                }
            }

            // 计算每个一级维度下的二级指标权重
            List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, level);
            List<DynamicAhpCompleteResultDTO.PrimaryWithSecondaries> primariesWithSec = new ArrayList<>();

            for (Map<String, Object> primary : primaries) {
                String primaryCode = (String) primary.get("code");
                String primaryName = (String) primary.get("name");

                // 查找一级维度权重
                BigDecimal primaryWeight = primaryWeights.stream()
                        .filter(w -> primaryCode.equals(w.getCode()))
                        .findFirst()
                        .map(DynamicAhpResultDTO.DimensionWeight::getWeight)
                        .orElse(BigDecimal.ZERO);

                if (primaryWeight.compareTo(BigDecimal.ZERO) == 0) {
                    // 如果没有一级维度权重，使用平均权重
                    primaryWeight = BigDecimal.ONE.divide(new BigDecimal(primaries.size()), 6, BigDecimal.ROUND_HALF_UP);
                    log.warn("一级维度 {} 权重为0，使用平均权重: {}", primaryName, primaryWeight);
                }

                // 获取二级指标
                List<Map<String, Object>> secondaries = getSecondaryDimensions(templateId, level, primaryCode);
                List<DynamicAhpCompleteResultDTO.SecondaryWeight> secWeights = new ArrayList<>();

                if (secondaries.size() > 1) {
                    try {
                        DynamicAhpResultDTO secResult = calculateWeights(
                                templateId, expertId, level, "SECONDARY_BETWEEN", primaryCode);
                        for (DynamicAhpResultDTO.DimensionWeight dw : secResult.getWeights()) {
                            // 综合权重 = 层级权重 × 一级维度权重 × 二级指标权重
                            BigDecimal combinedWeight = levelWeight.multiply(primaryWeight).multiply(dw.getWeight());
                            totalWeight = totalWeight.add(combinedWeight);

                            secWeights.add(DynamicAhpCompleteResultDTO.SecondaryWeight.builder()
                                    .secondaryCode(dw.getCode())
                                    .secondaryName(dw.getName())
                                    .secondaryDescription(dw.getDescription())
                                    .secondaryWeight(dw.getWeight())
                                    .combinedWeight(combinedWeight)
                                    .consistent(secResult.getConsistent())
                                    .cr(secResult.getCr())
                                    .build());
                        }
                        if (!secResult.getConsistent()) {
                            allConsistent = false;
                            result.getInconsistentMatrices().add(level + "-" + primaryName);
                        }
                    } catch (Exception e) {
                        log.warn("计算二级指标权重失败: {}, 使用平均权重", e.getMessage());
                        // 使用平均权重
                        BigDecimal avgSecWeight = BigDecimal.ONE.divide(new BigDecimal(secondaries.size()), 6, BigDecimal.ROUND_HALF_UP);
                        for (Map<String, Object> sec : secondaries) {
                            BigDecimal combinedWeight = levelWeight.multiply(primaryWeight).multiply(avgSecWeight);
                            totalWeight = totalWeight.add(combinedWeight);
                            secWeights.add(DynamicAhpCompleteResultDTO.SecondaryWeight.builder()
                                    .secondaryCode((String) sec.get("code"))
                                    .secondaryName((String) sec.get("name"))
                                    .secondaryDescription((String) sec.get("description"))
                                    .metricType((String) sec.get("metricType"))
                                    .secondaryWeight(avgSecWeight)
                                    .combinedWeight(combinedWeight)
                                    .consistent(true)
                                    .build());
                        }
                    }
                } else if (secondaries.size() == 1) {
                    // 只有一个指标时，权重为1
                    Map<String, Object> sec = secondaries.get(0);
                    // 综合权重 = 层级权重 × 一级维度权重
                    BigDecimal combinedWeight = levelWeight.multiply(primaryWeight);
                    totalWeight = totalWeight.add(combinedWeight);

                    secWeights.add(DynamicAhpCompleteResultDTO.SecondaryWeight.builder()
                            .secondaryCode((String) sec.get("code"))
                            .secondaryName((String) sec.get("name"))
                            .secondaryDescription((String) sec.get("description"))
                            .metricType((String) sec.get("metricType"))
                            .secondaryWeight(BigDecimal.ONE)
                            .combinedWeight(combinedWeight)
                            .consistent(true)
                            .build());
                }

                primariesWithSec.add(DynamicAhpCompleteResultDTO.PrimaryWithSecondaries.builder()
                        .levelName(level)
                        .primaryCode(primaryCode)
                        .primaryName(primaryName)
                        .primaryDescription((String) primary.get("description"))
                        .primaryWeight(primaryWeight)
                        .combinedWeight(levelWeight.multiply(primaryWeight))  // 一级综合权重 = 层级权重 × 一级权重
                        .secondaries(secWeights)
                        .build());
            }

            result.getAllLeaves().addAll(primariesWithSec.stream()
                    .flatMap(p -> p.getSecondaries().stream()
                            .map(s -> DynamicAhpResultDTO.CombinedWeight.builder()
                                    .levelName(level)
                                    .primaryCode(p.getPrimaryCode())
                                    .primaryName(p.getPrimaryName())
                                    .primaryDescription(p.getPrimaryDescription())
                                    .primaryWeight(p.getPrimaryWeight())
                                    .primaryCombinedWeight(p.getCombinedWeight())  // 一级综合权重
                                    .secondaryCode(s.getSecondaryCode())
                                    .secondaryName(s.getSecondaryName())
                                    .secondaryDescription(s.getSecondaryDescription())
                                    .secondaryWeight(s.getSecondaryWeight())
                                    .combinedWeight(s.getCombinedWeight())
                                    .path(level + "/" + p.getPrimaryName() + "/" + s.getSecondaryName())
                                    .build()))
                    .collect(Collectors.toList()));
        }

        // 归一化综合权重（确保总和为1）
        // 同时归一化一级和二级的综合权重
        if (totalWeight.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal normFactor = BigDecimal.ONE.divide(totalWeight, 10, RoundingMode.HALF_UP);

            // 按一级维度分组，用于归一化一级综合权重
            Map<String, List<DynamicAhpResultDTO.CombinedWeight>> byPrimary = new LinkedHashMap<>();
            for (DynamicAhpResultDTO.CombinedWeight leaf : result.getAllLeaves()) {
                String primaryKey = leaf.getLevelName() + "_" + leaf.getPrimaryCode();
                byPrimary.computeIfAbsent(primaryKey, k -> new ArrayList<>()).add(leaf);
            }

            // 计算并归一化每个一级维度的综合权重
            Map<String, BigDecimal> primaryNormalizedWeights = new HashMap<>();
            for (Map.Entry<String, List<DynamicAhpResultDTO.CombinedWeight>> entry : byPrimary.entrySet()) {
                List<DynamicAhpResultDTO.CombinedWeight> leaves = entry.getValue();
                if (!leaves.isEmpty()) {
                    // 计算该一级维度下所有二级综合权重之和
                    BigDecimal primaryTotal = leaves.stream()
                            .map(DynamicAhpResultDTO.CombinedWeight::getCombinedWeight)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    // 归一化
                    BigDecimal normalizedPrimary = primaryTotal.multiply(normFactor).setScale(6, RoundingMode.HALF_UP);
                    primaryNormalizedWeights.put(entry.getKey(), normalizedPrimary);

                    // 归一化每个二级综合权重
                    for (DynamicAhpResultDTO.CombinedWeight leaf : leaves) {
                        leaf.setCombinedWeight(leaf.getCombinedWeight().multiply(normFactor).setScale(6, RoundingMode.HALF_UP));
                    }
                }
            }

            totalWeight = BigDecimal.ONE;
        }

        result.setTotalWeight(totalWeight);
        result.setAllConsistent(allConsistent);

        log.info("计算完成 - 总权重: {}, 指标数量: {}, 一致性: {}",
                totalWeight, result.getAllLeaves().size(), allConsistent);

        return result;
    }

    /**
     * 保存计算结果到数据库
     * 同时保存一级维度和二级指标的综合权重
     */
    @Transactional
    public int saveWeights(Long templateId, Long expertId) {
        // 先删除旧数据
        weightsRepository.deleteByExpertIdAndTemplateId(expertId, templateId);

        // 计算所有权重
        DynamicAhpCompleteResultDTO result = calculateAllWeights(templateId, expertId);

        // 获取模板名称
        DynamicTemplate template = templateRepository.findById(templateId).orElse(null);
        String templateName = template != null ? template.getTemplateName() : "模板" + templateId;

        // 获取专家名称
        String expertName = "专家" + expertId;
        try {
            List<Map<String, Object>> experts = getExperts();
            for (Map<String, Object> e : experts) {
                if (expertId.equals(e.get("expertId"))) {
                    expertName = (String) e.get("expertName");
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("获取专家名称失败: {}", e.getMessage());
        }

        List<DynamicAhpWeights> weightsList = new ArrayList<>();

        // 按一级维度分组，计算一级综合权重
        Map<String, BigDecimal> primaryCombinedWeights = new HashMap<>();
        Map<String, DynamicAhpResultDTO.CombinedWeight> primaryInfoMap = new HashMap<>();

        for (DynamicAhpResultDTO.CombinedWeight leaf : result.getAllLeaves()) {
            String key = leaf.getLevelName() + "_" + leaf.getPrimaryCode();
            // 累加二级综合权重
            primaryCombinedWeights.merge(key, leaf.getCombinedWeight(), BigDecimal::add);
            if (!primaryInfoMap.containsKey(key)) {
                primaryInfoMap.put(key, leaf);
            }
        }

        // 保存一级维度的权重记录
        int sortOrder = 0;
        for (Map.Entry<String, BigDecimal> entry : primaryCombinedWeights.entrySet()) {
            String[] parts = entry.getKey().split("_", 2);
            String levelName = parts[0];
            String primaryCode = parts[1];

            DynamicAhpResultDTO.CombinedWeight info = primaryInfoMap.get(entry.getKey());
            if (info != null) {
                weightsList.add(DynamicAhpWeights.builder()
                        .expertId(expertId)
                        .expertName(expertName)
                        .templateId(templateId)
                        .templateName(templateName)
                        .levelName(levelName)
                        .dimensionType(DynamicAhpWeights.DimensionType.PRIMARY)
                        .dimensionCode(primaryCode)
                        .dimensionName(info.getPrimaryName())
                        .dimensionDescription(info.getPrimaryDescription())
                        .weight(info.getPrimaryWeight())
                        .combinedWeight(entry.getValue())  // 一级综合权重
                        .weightPath(levelName + "/" + info.getPrimaryName())
                        .sortOrder(sortOrder++)
                        .build());
            }
        }

        // 保存二级指标的权重记录
        for (DynamicAhpResultDTO.CombinedWeight leaf : result.getAllLeaves()) {
            weightsList.add(DynamicAhpWeights.builder()
                    .expertId(expertId)
                    .expertName(expertName)
                    .templateId(templateId)
                    .templateName(templateName)
                    .levelName(leaf.getLevelName())
                    .dimensionType(DynamicAhpWeights.DimensionType.SECONDARY)
                    .dimensionCode(leaf.getSecondaryCode())
                    .dimensionName(leaf.getSecondaryName())
                    .dimensionDescription(leaf.getSecondaryDescription())
                    .weight(leaf.getSecondaryWeight())
                    .combinedWeight(leaf.getCombinedWeight())  // 二级综合权重
                    .parentCode(leaf.getPrimaryCode())
                    .weightPath(leaf.getPath())
                    .sortOrder(sortOrder++)
                    .build());
        }

        weightsRepository.saveAll(weightsList);
        log.info("保存权重完成 - 共 {} 条记录（其中一级 {} 条，二级 {} 条）",
                weightsList.size(),
                weightsList.stream().filter(w -> w.getDimensionType() == DynamicAhpWeights.DimensionType.PRIMARY).count(),
                weightsList.stream().filter(w -> w.getDimensionType() == DynamicAhpWeights.DimensionType.SECONDARY).count());

        return weightsList.size();
    }

    /**
     * 获取已保存的权重
     */
    public DynamicAhpCompleteResultDTO getWeights(Long templateId, Long expertId) {
        List<DynamicAhpWeights> weights = weightsRepository
                .findByExpertIdAndTemplateIdOrderByLevelNameAscSortOrderAsc(expertId, templateId);

        DynamicAhpCompleteResultDTO result = DynamicAhpCompleteResultDTO.builder()
                .templateId(templateId)
                .expertId(expertId)
                .allLeaves(new ArrayList<>())
                .build();

        BigDecimal totalWeight = BigDecimal.ZERO;
        for (DynamicAhpWeights w : weights) {
            totalWeight = totalWeight.add(w.getCombinedWeight() != null ? w.getCombinedWeight() : BigDecimal.ZERO);
            result.getAllLeaves().add(DynamicAhpResultDTO.CombinedWeight.builder()
                    .levelName(w.getLevelName())
                    .primaryCode(w.getParentCode())
                    .secondaryCode(w.getDimensionCode())
                    .secondaryName(w.getDimensionName())
                    .secondaryWeight(w.getWeight())
                    .combinedWeight(w.getCombinedWeight())
                    .path(w.getWeightPath())
                    .build());
        }

        result.setTotalWeight(totalWeight);
        return result;
    }

    /**
     * 获取指定层级的权重
     */
    public List<DynamicAhpResultDTO> getWeightsByLevel(Long templateId, Long expertId, String levelName) {
        List<DynamicAhpResultDTO> results = new ArrayList<>();

        // 获取一级维度权重
        try {
            DynamicAhpResultDTO primaryResult = calculateWeights(templateId, expertId, levelName, "PRIMARY_BETWEEN", null);
            results.add(primaryResult);
        } catch (Exception e) {
            log.warn("获取一级维度权重失败: {}", e.getMessage());
        }

        // 获取每个一级维度下的二级指标权重
        List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, levelName);
        for (Map<String, Object> primary : primaries) {
            String primaryCode = (String) primary.get("code");
            try {
                DynamicAhpResultDTO secResult = calculateWeights(
                        templateId, expertId, levelName, "SECONDARY_BETWEEN", primaryCode);
                results.add(secResult);
            } catch (Exception e) {
                log.warn("获取二级指标权重失败: {}", e.getMessage());
            }
        }

        return results;
    }

    /**
     * 模拟AHP打分
     * 包括：
     * 1. 层级间比较矩阵（LEVEL_BETWEEN）
     * 2. 一级维度间比较矩阵（PRIMARY_BETWEEN）
     * 3. 二级指标间比较矩阵（SECONDARY_BETWEEN）
     */
    @Transactional
    public int simulateScores(Long templateId, List<Long> expertIds) {
        int totalCount = 0;

        for (Long expertId : expertIds) {
            log.info("---------- 为专家 {} 生成模拟数据 ----------", expertId);

            // 获取所有层级
            List<String> levels = getLevelNames(templateId);
            if (levels.isEmpty()) {
                log.warn("专家 {}: 没有找到层级数据", expertId);
                continue;
            }
            log.info("专家 {}: 找到 {} 个层级", expertId, levels.size());

            // 模拟层级间比较矩阵（LEVEL_BETWEEN）
            if (levels.size() > 1) {
                List<Map<String, Object>> levelDimensions = new ArrayList<>();
                for (String levelName : levels) {
                    Map<String, Object> levelDim = new LinkedHashMap<>();
                    levelDim.put("code", levelName);
                    levelDim.put("name", levelName);
                    levelDimensions.add(levelDim);
                }
                int levelCount = simulateLevelBetweenMatrix(templateId, expertId, levelDimensions);
                log.info("专家 {}: 生成层级间矩阵 {} 条", expertId, levelCount);
                totalCount += levelCount;
            } else {
                log.info("专家 {}: 只有一个层级，跳过层级间矩阵", expertId);
            }

            for (String level : levels) {
                List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, level);
                if (primaries.isEmpty()) {
                    log.warn("专家 {}: 层级 '{}' 没有一级维度", expertId, level);
                    continue;
                }
                log.info("专家 {}: 层级 '{}' 有 {} 个一级维度", expertId, level, primaries.size());

                // 模拟一级维度间比较
                int primaryCount = simulateMatrix(templateId, expertId, level, null, primaries);
                log.info("专家 {}: 层级 '{}' 一级维度矩阵 {} 条", expertId, level, primaryCount);
                totalCount += primaryCount;

                // 模拟二级指标间比较
                for (Map<String, Object> primary : primaries) {
                    String primaryCode = (String) primary.get("code");
                    List<Map<String, Object>> secs = getSecondaryDimensions(templateId, level, primaryCode);
                    if (secs.size() > 1) {
                        int secCount = simulateMatrix(templateId, expertId, level, primaryCode, secs);
                        log.info("专家 {}: 层级 '{}' 一级维度 '{}' 二级矩阵 {} 条", expertId, level, primary.get("name"), secCount);
                        totalCount += secCount;
                    }
                }
            }
            log.info("专家 {}: 共生成 {} 条记录", expertId, totalCount);
        }

        return totalCount;
    }

    /**
     * 模拟层级间比较矩阵（LEVEL_BETWEEN）
     * 层级间矩阵使用层级名称作为code和name
     */
    private int simulateLevelBetweenMatrix(Long templateId, Long expertId, List<Map<String, Object>> levels) {
        int n = levels.size();
        if (n < 2) {
            log.info("层级数量 < 2，跳过层级间矩阵生成");
            return 0;
        }

        log.info("开始生成层级间矩阵: expertId={}, 层级数量={}", expertId, n);

        // 获取专家名称
        String expertName = "专家" + expertId;
        try {
            List<Map<String, Object>> experts = getExperts();
            for (Map<String, Object> e : experts) {
                if (expertId.equals(e.get("expertId"))) {
                    expertName = (String) e.get("expertName");
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("获取专家名称失败，使用默认名称: {}", e.getMessage());
        }

        // 获取模板名称
        DynamicTemplate template = templateRepository.findById(templateId).orElse(null);
        String templateName = template != null ? template.getTemplateName() : "模板" + templateId;

        List<DynamicAhpMatrix> matrices = new ArrayList<>();

        // 生成随机权重
        Random random = new Random();
        double[] randomWeights = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            randomWeights[i] = random.nextDouble() * 10 + 1;
            sum += randomWeights[i];
        }

        // 归一化
        for (int i = 0; i < n; i++) {
            randomWeights[i] /= sum;
        }

        // 计算标度并构建矩阵记录
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double ratio = randomWeights[i] / randomWeights[j];
                // 限制在 [1/9, 9] 范围内
                if (ratio < 0.111) ratio = 0.111;
                if (ratio > 9) ratio = 9;

                BigDecimal score = BigDecimal.valueOf(ratio).setScale(4, RoundingMode.HALF_UP);
                BigDecimal confidence = BigDecimal.valueOf(0.6 + random.nextDouble() * 0.35).setScale(2, RoundingMode.HALF_UP);

                DynamicAhpMatrix matrix = DynamicAhpMatrix.builder()
                        .expertId(expertId)
                        .expertName(expertName)
                        .templateId(templateId)
                        .templateName(templateName)
                        .levelName("LEVEL_BETWEEN")
                        .comparisonType(DynamicAhpMatrix.ComparisonType.LEVEL_BETWEEN)
                        .rowCode((String) levels.get(i).get("code"))
                        .rowName((String) levels.get(i).get("name"))
                        .colCode((String) levels.get(j).get("code"))
                        .colName((String) levels.get(j).get("name"))
                        .score(score)
                        .confidence(confidence)
                        .dimensionCodes("LEVEL_BETWEEN:" + levels.get(i).get("code") + "," + levels.get(j).get("code"))
                        .dimensionNames(levels.get(i).get("name") + "," + levels.get(j).get("name"))
                        .dimensionCount(2)
                        .build();

                matrices.add(matrix);

                log.info("生成层级比较: {} vs {} = {}", levels.get(i).get("name"), levels.get(j).get("name"), score);
            }
        }

        // 删除旧的层级间矩阵记录
        List<DynamicAhpMatrix> oldMatrices = matrixRepository
                .findByExpertIdAndTemplateIdAndComparisonType(
                        expertId, templateId, DynamicAhpMatrix.ComparisonType.LEVEL_BETWEEN);
        if (!oldMatrices.isEmpty()) {
            matrixRepository.deleteAll(oldMatrices);
            log.info("删除旧层级间矩阵: {} 条", oldMatrices.size());
        }

        // 保存新的矩阵记录
        matrixRepository.saveAll(matrices);
        log.info("层级间矩阵保存完成: expertId={}, 保存了 {} 条记录", expertId, matrices.size());

        return matrices.size();
    }

    public int batchSimulateScores(Long templateId) {
        log.info("========== 开始批量模拟 ==========");
        log.info("模板ID: {}", templateId);

        // 检查模板是否存在
        DynamicTemplate template = templateRepository.findById(templateId).orElse(null);
        if (template == null) {
            log.error("模板不存在: templateId={}", templateId);
            throw new IllegalArgumentException("模板不存在: " + templateId);
        }
        log.info("模板名称: {}", template.getTemplateName());

        // 获取专家列表
        List<Map<String, Object>> experts = getExperts();
        if (experts.isEmpty()) {
            log.warn("没有找到专家，无法批量模拟");
            throw new IllegalStateException("没有找到专家，请先添加专家数据");
        }
        log.info("找到 {} 位专家", experts.size());
        for (Map<String, Object> e : experts) {
            log.info("  - 专家: id={}, name={}", e.get("expertId"), e.get("expertName"));
        }

        // 获取层级列表
        List<String> levels = getLevelNames(templateId);
        if (levels.isEmpty()) {
            log.warn("模板没有层级数据: templateId={}", templateId);
            throw new IllegalStateException("模板没有层级数据，请先导入指标模板");
        }
        log.info("找到 {} 个层级: {}", levels.size(), levels);

        // 获取每个层级的维度信息
        for (String level : levels) {
            List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, level);
            log.info("层级 '{}' 有 {} 个一级维度", level, primaries.size());
            for (Map<String, Object> prim : primaries) {
                String primCode = (String) prim.get("code");
                List<Map<String, Object>> secs = getSecondaryDimensions(templateId, level, primCode);
                log.info("  - 一级维度 '{}' 有 {} 个二级指标", prim.get("name"), secs.size());
            }
        }

        List<Long> expertIds = experts.stream()
                .map(e -> ((Number) e.get("expertId")).longValue())
                .collect(Collectors.toList());

        log.info("开始为 {} 位专家生成模拟数据...", expertIds.size());
        int totalCount = simulateScores(templateId, expertIds);
        log.info("批量模拟完成 - 共生成 {} 条矩阵记录", totalCount);
        log.info("========== 批量模拟结束 ==========");

        return totalCount;
    }

    private int simulateMatrix(Long templateId, Long expertId, String levelName,
                                String parentCode, List<Map<String, Object>> dimensions) {
        int n = dimensions.size();
        if (n < 2) {
            log.info("维度数量 < 2，跳过矩阵生成");
            return 0;
        }

        // 获取专家名称
        String expertName = "专家" + expertId;
        try {
            List<Map<String, Object>> experts = getExperts();
            for (Map<String, Object> e : experts) {
                if (expertId.equals(e.get("expertId"))) {
                    expertName = (String) e.get("expertName");
                    break;
                }
            }
        } catch (Exception e) {
            log.warn("获取专家名称失败，使用默认名称: {}", e.getMessage());
        }

        // 获取模板名称
        DynamicTemplate template = templateRepository.findById(templateId).orElse(null);
        String templateName = template != null ? template.getTemplateName() : "模板" + templateId;

        List<DynamicAhpMatrix> matrices = new ArrayList<>();

        // 确定矩阵类型
        DynamicAhpMatrix.ComparisonType compType;
        if (parentCode == null) {
            compType = DynamicAhpMatrix.ComparisonType.PRIMARY_BETWEEN;
        } else {
            compType = DynamicAhpMatrix.ComparisonType.SECONDARY_BETWEEN;
        }

        // 生成随机权重
        Random random = new Random();
        double[] randomWeights = new double[n];
        double sum = 0;
        for (int i = 0; i < n; i++) {
            randomWeights[i] = random.nextDouble() * 10 + 1;
            sum += randomWeights[i];
        }

        // 归一化
        for (int i = 0; i < n; i++) {
            randomWeights[i] /= sum;
        }

        // 计算标度并构建矩阵记录
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double ratio = randomWeights[i] / randomWeights[j];
                // 限制在 [1/9, 9] 范围内
                if (ratio < 0.111) ratio = 0.111;
                if (ratio > 9) ratio = 9;

                BigDecimal score = BigDecimal.valueOf(ratio).setScale(4, RoundingMode.HALF_UP);
                BigDecimal confidence = BigDecimal.valueOf(0.6 + random.nextDouble() * 0.35).setScale(2, RoundingMode.HALF_UP);

                DynamicAhpMatrix matrix = DynamicAhpMatrix.builder()
                        .expertId(expertId)
                        .expertName(expertName)
                        .templateId(templateId)
                        .templateName(templateName)
                        .levelName(levelName)
                        .comparisonType(compType)
                        .parentCode(parentCode)
                        .rowCode((String) dimensions.get(i).get("code"))
                        .rowName((String) dimensions.get(i).get("name"))
                        .colCode((String) dimensions.get(j).get("code"))
                        .colName((String) dimensions.get(j).get("name"))
                        .score(score)
                        .confidence(confidence)
                        .dimensionCodes(levelName + ":" + dimensions.get(i).get("code") + "," + dimensions.get(j).get("code"))
                        .dimensionNames(dimensions.get(i).get("name") + "," + dimensions.get(j).get("name"))
                        .dimensionCount(2)
                        .build();

                matrices.add(matrix);
            }
        }

        // 删除旧的矩阵记录
        if (parentCode == null) {
            // 一级维度矩阵
            List<DynamicAhpMatrix> oldMatrices = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
                            expertId, templateId, levelName, compType);
            if (!oldMatrices.isEmpty()) {
                matrixRepository.deleteAll(oldMatrices);
                log.info("删除旧一级维度矩阵: {} 条", oldMatrices.size());
            }
        } else {
            // 二级指标矩阵
            List<DynamicAhpMatrix> oldMatrices = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndParentCode(
                            expertId, templateId, levelName, parentCode);
            if (!oldMatrices.isEmpty()) {
                matrixRepository.deleteAll(oldMatrices);
                log.info("删除旧二级指标矩阵: {} 条", oldMatrices.size());
            }
        }

        // 保存新的矩阵记录
        matrixRepository.saveAll(matrices);
        log.info("矩阵保存完成: expertId={}, levelName={}, matrixType={}, 保存了 {} 条记录",
                expertId, levelName, compType, matrices.size());

        return matrices.size();
    }

    /**
     * 清除AHP矩阵
     */
    @Transactional
    public void clearMatrix(Long templateId, Long expertId) {
        matrixRepository.deleteByExpertIdAndTemplateId(expertId, templateId);
        weightsRepository.deleteByExpertIdAndTemplateId(expertId, templateId);
    }

    /**
     * 获取配置状态
     */
    public Map<String, Object> getConfigurationStatus(Long templateId, Long expertId) {
        Map<String, Object> status = new LinkedHashMap<>();

        List<String> levels = getLevelNames(templateId);
        int totalMatrices = 0;
        int completedMatrices = 0;

        for (String level : levels) {
            List<Map<String, Object>> primaries = getPrimaryDimensions(templateId, level);
            totalMatrices++;

            // 检查一级维度矩阵是否完成
            List<DynamicAhpMatrix> primaryMatrix = matrixRepository
                    .findByExpertIdAndTemplateIdAndLevelNameAndComparisonType(
                            expertId, templateId, level, DynamicAhpMatrix.ComparisonType.PRIMARY_BETWEEN);
            if (!primaryMatrix.isEmpty()) {
                completedMatrices++;
            }

            // 检查二级指标矩阵
            for (Map<String, Object> primary : primaries) {
                String primaryCode = (String) primary.get("code");
                List<Map<String, Object>> secondaries = getSecondaryDimensions(templateId, level, primaryCode);
                if (secondaries.size() > 1) {
                    totalMatrices++;
                    List<DynamicAhpMatrix> secMatrix = matrixRepository
                            .findByExpertIdAndTemplateIdAndLevelNameAndParentCode(
                                    expertId, templateId, level, primaryCode);
                    if (!secMatrix.isEmpty()) {
                        completedMatrices++;
                    }
                }
            }
        }

        status.put("totalMatrices", totalMatrices);
        status.put("completedMatrices", completedMatrices);
        status.put("progress", totalMatrices > 0 ? (double) completedMatrices / totalMatrices * 100 : 0);
        status.put("levels", levels);

        return status;
    }

    // ==================== 辅助方法 ====================

    private Map<String, Object> toDimensionMap(DynamicDimension d) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", d.getId());
        map.put("code", d.getCode());
        map.put("name", d.getName());
        map.put("description", d.getDescription());
        map.put("sortOrder", d.getSortOrder());
        map.put("weight", d.getWeight());
        map.put("metricType", d.getMetricType() != null ? d.getMetricType().name() : null);
        map.put("aggregationMethod", d.getAggregationMethod());
        map.put("scoreDirection", d.getScoreDirection() != null ? d.getScoreDirection().name() : null);
        map.put("unit", d.getUnit());
        map.put("baselineValue", d.getBaselineValue());
        map.put("targetValue", d.getTargetValue());
        map.put("averageValue", d.getAverageValue());
        return map;
    }
}
