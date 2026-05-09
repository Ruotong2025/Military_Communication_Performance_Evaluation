package com.ccnu.military.service;

import com.ccnu.military.dto.DynamicIndicatorTreeDTO;
import com.ccnu.military.dto.ImportResultDTO;
import com.ccnu.military.entity.DynamicDimension;
import com.ccnu.military.entity.DynamicDimension.DimensionLevel;
import com.ccnu.military.entity.DynamicDimension.MetricType;
import com.ccnu.military.entity.DynamicDimension.ScoreDirection;
import com.ccnu.military.entity.DynamicTemplate;
import com.ccnu.military.repository.DynamicDimensionRepository;
import com.ccnu.military.repository.DynamicTemplateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DynamicIndicatorService {

    private final DynamicTemplateRepository templateRepository;
    private final DynamicDimensionRepository dimensionRepository;
    private final PythonEvaluationService pythonService;
    private final JdbcTemplate jdbcTemplate;

    /**
     * 仅解析Excel（不保存到数据库）
     */
    public Map<String, Object> parseExcelOnly(MultipartFile file) {
        try {
            // 1. 保存上传的文件
            Path uploadDir = Path.of(System.getProperty("java.io.tmpdir"), "uploads");
            Files.createDirectories(uploadDir);
            String originalFilename = file.getOriginalFilename();
            Path filePath = uploadDir.resolve(originalFilename != null ? originalFilename : "upload.xlsx");
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 2. 调用Python解析Excel
            Map<String, Object> parseResult = pythonService.parseExcel(filePath.toString());

            // 3. 检查解析是否成功
            if (parseResult == null) {
                throw new RuntimeException("Python解析返回空结果");
            }
            if (parseResult.containsKey("success") && !Boolean.TRUE.equals(parseResult.get("success"))) {
                throw new RuntimeException("Python解析失败: " + parseResult.get("message"));
            }

            return parseResult;

        } catch (Exception e) {
            log.error("解析Excel失败", e);
            throw new RuntimeException("解析Excel失败: " + e.getMessage());
        }
    }

    /**
     * 导入指标（通过Python解析Excel）
     */
    @Transactional
    public ImportResultDTO importIndicators(MultipartFile file, String templateName) {
        try {
            // 1. 保存上传的文件
            Path uploadDir = Path.of(System.getProperty("java.io.tmpdir"), "uploads");
            Files.createDirectories(uploadDir);
            String originalFilename = file.getOriginalFilename();
            Path filePath = uploadDir.resolve(originalFilename != null ? originalFilename : "upload.xlsx");
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

            // 2. 调用Python解析Excel
            Map<String, Object> parseResult = pythonService.parseExcel(filePath.toString());

            // 检查解析是否成功
            if (parseResult == null || !parseResult.containsKey("levels")) {
                throw new RuntimeException("Python解析失败: " + parseResult);
            }

            // 3. 创建模板（优先使用传入的名称，否则使用Python返回的名称）
            String finalTemplateName = templateName;
            if (finalTemplateName == null || finalTemplateName.trim().isEmpty()) {
                finalTemplateName = (String) parseResult.getOrDefault("template_name", "未命名模板");
            }
            DynamicTemplate template = new DynamicTemplate();
            template.setTemplateName(finalTemplateName);
            template.setTemplateCode(generateTemplateCode());
            template.setSourceFile(originalFilename);
            template.setStatus(DynamicTemplate.Status.ACTIVE);
            template = templateRepository.save(template);

            // 4. 解析并保存维度数据
            int levelCount = 0;
            int primaryCount = 0;
            int secondaryCount = 0;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> levelsData = (List<Map<String, Object>>) parseResult.get("levels");

            if (levelsData != null) {
                for (Map<String, Object> levelData : levelsData) {
                    levelCount++;
                    String levelName = (String) levelData.get("name");
                    String levelDesc = (String) levelData.get("description");

                    // 保存层级维度（使用层级唯一code）
                    String levelCode = "L_" + levelCount + "_" + generateCode(levelName);
                    if (dimensionRepository.existsByTemplateIdAndDimensionLevelAndCode(template.getId(), DimensionLevel.LEVEL, levelCode)) {
                        log.warn("层级已存在，跳过: {}", levelName);
                        continue;
                    }
                    DynamicDimension levelDim = new DynamicDimension();
                    levelDim.setTemplateId(template.getId());
                    levelDim.setDimensionLevel(DimensionLevel.LEVEL);
                    levelDim.setName(levelName);
                    levelDim.setCode(levelCode);
                    levelDim.setDescription(levelDesc);
                    levelDim.setSortOrder(levelCount);
                    levelDim = dimensionRepository.save(levelDim);

                    // 保存一级维度
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> primariesData = (List<Map<String, Object>>) levelData.get("primaryDimensions");
                    int primaryOrder = 0;

                    if (primariesData != null) {
                        for (Map<String, Object> primaryData : primariesData) {
                            primaryCount++;
                            primaryOrder++;
                            String primaryName = (String) primaryData.get("name");
                            String primaryCode = (String) primaryData.get("code");

                            // 一级维度code加层级前缀确保唯一
                            String fullPrimaryCode = levelCode + "_P_" + primaryCode;
                            if (dimensionRepository.existsByTemplateIdAndDimensionLevelAndCode(template.getId(), DimensionLevel.PRIMARY, fullPrimaryCode)) {
                                log.warn("一级维度已存在，跳过: {}", primaryName);
                                continue;
                            }
                            DynamicDimension primaryDim = new DynamicDimension();
                            primaryDim.setTemplateId(template.getId());
                            primaryDim.setDimensionLevel(DimensionLevel.PRIMARY);
                            primaryDim.setParentId(levelDim.getId());
                            primaryDim.setName(primaryName);
                            primaryDim.setCode(fullPrimaryCode);
                            primaryDim.setSortOrder(primaryOrder);
                            primaryDim = dimensionRepository.save(primaryDim);

                            // 保存二级维度
                            @SuppressWarnings("unchecked")
                            List<Map<String, Object>> secondariesData = (List<Map<String, Object>>) primaryData.get("secondaryDimensions");
                            int secOrder = 0;

                            if (secondariesData != null) {
                                for (Map<String, Object> secData : secondariesData) {
                                    secondaryCount++;
                                    secOrder++;
                                    String secName = (String) secData.get("name");
                                    String secCode = (String) secData.get("code");
                                    String metricTypeStr = (String) secData.get("metricType");
                                    Object averageValueObj = secData.get("averageValue");

                                    // 二级维度code加一级维度前缀确保唯一
                                    String fullSecCode = fullPrimaryCode + "_S_" + secCode;
                                    if (dimensionRepository.existsByTemplateIdAndDimensionLevelAndCode(template.getId(), DimensionLevel.SECONDARY, fullSecCode)) {
                                        log.warn("二级维度已存在，跳过: {}", secName);
                                        continue;
                                    }

                                    DynamicDimension secDim = new DynamicDimension();
                                    secDim.setTemplateId(template.getId());
                                    secDim.setDimensionLevel(DimensionLevel.SECONDARY);
                                    secDim.setParentId(primaryDim.getId());
                                    secDim.setName(secName);
                                    secDim.setCode(fullSecCode);
                                    secDim.setSortOrder(secOrder);

                                    // 设置指标类型
                                    if ("QUALITATIVE".equals(metricTypeStr)) {
                                        secDim.setMetricType(MetricType.QUALITATIVE);
                                    } else {
                                        secDim.setMetricType(MetricType.QUANTITATIVE);
                                    }

                                    // 设置平均数（仅定量指标）
                                    if (averageValueObj != null && "QUANTITATIVE".equals(metricTypeStr)) {
                                        if (averageValueObj instanceof Number) {
                                            secDim.setAverageValue(BigDecimal.valueOf(((Number) averageValueObj).doubleValue()));
                                        }
                                    }

                                    secDim.setScoreDirection(ScoreDirection.POSITIVE);
                                    secDim.setAggregationMethod("avg");

                                    dimensionRepository.save(secDim);
                                }
                            }
                        }
                    }
                }
            }

            // 5. 更新模板统计
            template.setLevelCount(levelCount);
            template.setPrimaryCount(primaryCount);
            template.setSecondaryCount(secondaryCount);
            templateRepository.save(template);

            // 6. 返回结果
            ImportResultDTO result = new ImportResultDTO();
            result.setTemplateId(template.getId());
            result.setTemplateName(template.getTemplateName());
            result.setTemplateCode(template.getTemplateCode());
            result.setTreeData(getIndicatorTree(template.getId()));
            return result;

        } catch (Exception e) {
            log.error("导入指标失败", e);
            throw new RuntimeException("导入指标失败: " + e.getMessage());
        }
    }

    /**
     * 获取指标树
     */
    public DynamicIndicatorTreeDTO getIndicatorTree(Long templateId) {
        DynamicTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在: " + templateId));

        List<DynamicDimension> allDimensions = dimensionRepository.findByTemplateIdOrderBySortOrder(templateId);
        
        // 按层级分组
        Map<String, List<DynamicDimension>> byLevel = allDimensions.stream()
                .collect(Collectors.groupingBy(d -> d.getDimensionLevel().name()));

        List<DynamicDimension> levels = byLevel.getOrDefault("LEVEL", Collections.emptyList());
        List<DynamicDimension> primaries = byLevel.getOrDefault("PRIMARY", Collections.emptyList());
        List<DynamicDimension> secondaries = byLevel.getOrDefault("SECONDARY", Collections.emptyList());

        // 构建树结构
        Map<Long, List<DynamicDimension>> childrenMap = primaries.stream()
                .collect(Collectors.groupingBy(DynamicDimension::getParentId));
        
        Map<Long, List<DynamicDimension>> primaryChildrenMap = secondaries.stream()
                .collect(Collectors.groupingBy(DynamicDimension::getParentId));

        List<DynamicIndicatorTreeDTO.LevelNode> levelNodes = new ArrayList<>();
        List<String> levelNames = new ArrayList<>();
        Map<String, Integer> primaryCountMap = new HashMap<>();
        
        int totalPrimary = 0;
        int totalSecondary = 0;
        int quantitativeCount = 0;
        int qualitativeCount = 0;

        for (DynamicDimension level : levels) {
            DynamicIndicatorTreeDTO.LevelNode levelNode = new DynamicIndicatorTreeDTO.LevelNode();
            levelNode.setId(level.getId());
            levelNode.setName(level.getName());
            levelNode.setDescription(level.getDescription());
            levelNode.setSortOrder(level.getSortOrder());

            List<DynamicIndicatorTreeDTO.PrimaryNode> primaryNodes = new ArrayList<>();
            List<DynamicDimension> levelPrimaries = childrenMap.getOrDefault(level.getId(), Collections.emptyList());
            
            for (DynamicDimension primary : levelPrimaries) {
                DynamicIndicatorTreeDTO.PrimaryNode primaryNode = new DynamicIndicatorTreeDTO.PrimaryNode();
                primaryNode.setId(primary.getId());
                primaryNode.setName(primary.getName());
                primaryNode.setCode(primary.getCode());
                primaryNode.setSortOrder(primary.getSortOrder());
                primaryNode.setWeight(primary.getWeight() != null ? primary.getWeight().doubleValue() : 0.0);

                List<DynamicIndicatorTreeDTO.SecondaryNode> secondaryNodes = new ArrayList<>();
                List<DynamicDimension> primarySecondaries = primaryChildrenMap.getOrDefault(primary.getId(), Collections.emptyList());
                
                for (DynamicDimension sec : primarySecondaries) {
                    DynamicIndicatorTreeDTO.SecondaryNode secNode = new DynamicIndicatorTreeDTO.SecondaryNode();
                    secNode.setId(sec.getId());
                    secNode.setName(sec.getName());
                    secNode.setCode(sec.getCode());
                    secNode.setSortOrder(sec.getSortOrder());
                    secNode.setMetricType(sec.getMetricType() != null ? sec.getMetricType().name() : "QUANTITATIVE");
                    secNode.setAggregationMethod(sec.getAggregationMethod());
                    secNode.setScoreDirection(sec.getScoreDirection() != null ? sec.getScoreDirection().name() : "POSITIVE");
                    secNode.setUnit(sec.getUnit());
                    secNode.setWeight(sec.getWeight() != null ? sec.getWeight().doubleValue() : 0.0);
                    secNode.setBaselineValue(sec.getBaselineValue() != null ? sec.getBaselineValue().doubleValue() : null);
                    secNode.setTargetValue(sec.getTargetValue() != null ? sec.getTargetValue().doubleValue() : null);
                    secNode.setAverageValue(sec.getAverageValue() != null ? sec.getAverageValue().doubleValue() : null);
                    
                    secondaryNodes.add(secNode);
                    totalSecondary++;
                    
                    if (MetricType.QUANTITATIVE.equals(sec.getMetricType())) {
                        quantitativeCount++;
                    } else {
                        qualitativeCount++;
                    }
                }

                primaryNode.setSecondaryDimensions(secondaryNodes);
                primaryNodes.add(primaryNode);
                totalPrimary++;
            }

            levelNode.setPrimaryDimensions(primaryNodes);
            levelNodes.add(levelNode);
            levelNames.add(level.getName());
            primaryCountMap.put(level.getName(), primaries.size());
        }

        // 统计信息
        DynamicIndicatorTreeDTO.Statistics stats = new DynamicIndicatorTreeDTO.Statistics();
        stats.setLevelCount(levels.size());
        stats.setTotalPrimaryDimensions(totalPrimary);
        stats.setTotalSecondaryDimensions(totalSecondary);
        stats.setQuantitativeCount(quantitativeCount);
        stats.setQualitativeCount(qualitativeCount);
        stats.setLevelNames(levelNames);
        stats.setPrimaryDimensionCountPerLevel(primaryCountMap);

        DynamicIndicatorTreeDTO result = new DynamicIndicatorTreeDTO();
        result.setId(template.getId());
        result.setTemplateName(template.getTemplateName());
        result.setTemplateCode(template.getTemplateCode());
        result.setStatus(template.getStatus().name());
        result.setLevels(levelNodes);
        result.setStatistics(stats);

        return result;
    }

    /**
     * 获取模板列表
     */
    public List<DynamicTemplate> getTemplateList() {
        return templateRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 获取指定模板的定量指标（含平均数）
     */
    public List<DynamicDimension> getQuantitativeIndicators(Long templateId) {
        return dimensionRepository.findQuantitativeIndicators(templateId);
    }

    /**
     * 删除模板（级联删除所有维度）
     * 使用原生SQL按正确顺序删除，避免Hibernate乐观锁问题
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        // 使用JdbcTemplate直接执行SQL，按正确顺序删除
        // 1. 先删除模板（外键约束会自动清理子表）
        // 由于外键约束，可能需要先删除子表数据

        // 按层级顺序删除维度数据（从子到父）
        jdbcTemplate.update("DELETE FROM dynamic_dimension WHERE template_id = ? AND dimension_level = 'SECONDARY'", templateId);
        jdbcTemplate.update("DELETE FROM dynamic_dimension WHERE template_id = ? AND dimension_level = 'PRIMARY'", templateId);
        jdbcTemplate.update("DELETE FROM dynamic_dimension WHERE template_id = ? AND dimension_level = 'LEVEL'", templateId);

        // 最后删除模板
        templateRepository.deleteById(templateId);
    }

    /**
     * 更新指标平均数
     */
    @Transactional
    public DynamicDimension updateAverageValue(Long dimensionId, Double averageValue) {
        DynamicDimension dim = dimensionRepository.findById(dimensionId)
                .orElseThrow(() -> new RuntimeException("维度不存在: " + dimensionId));
        dim.setAverageValue(BigDecimal.valueOf(averageValue));
        return dimensionRepository.save(dim);
    }

    private String generateTemplateCode() {
        return "TPL_" + System.currentTimeMillis();
    }

    private String generateCode(String name) {
        if (name == null || name.isEmpty()) {
            return "unnamed";
        }
        return name.replaceAll("[^\\w\\u4e00-\\u9fff]", "_")
                .toLowerCase()
                .replaceAll("_+", "_")
                .trim();
    }
}
