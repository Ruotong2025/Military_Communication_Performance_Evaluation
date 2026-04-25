package com.ccnu.military.service;

import com.ccnu.military.dto.AhpWeightResultDTO;
import com.ccnu.military.dto.ImportResultDTO;
import com.ccnu.military.dto.IndicatorTemplateDTO;
import com.ccnu.military.dto.IndicatorTreeDTO;
import com.ccnu.military.entity.*;
import com.ccnu.military.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DynamicIndicatorService {

    @Value("${python.executable:python}")
    private String pythonExecutable;

    @Value("${python.dynamic-parser-script:python_service/dynamic_parser.py}")
    private String pythonScriptPath;

    @Value("${python.timeout:60}")
    private long pythonTimeout;

    private final IndicatorTemplateRepository templateRepository;
    private final LevelDefinitionRepository levelRepository;
    private final PrimaryDimensionRepository primaryRepository;
    private final SecondaryDimensionRepository secondaryRepository;
    private final AhpJudgmentMatrixRepository matrixRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public DynamicIndicatorService(
            IndicatorTemplateRepository templateRepository,
            LevelDefinitionRepository levelRepository,
            PrimaryDimensionRepository primaryRepository,
            SecondaryDimensionRepository secondaryRepository,
            AhpJudgmentMatrixRepository matrixRepository) {
        this.templateRepository = templateRepository;
        this.levelRepository = levelRepository;
        this.primaryRepository = primaryRepository;
        this.secondaryRepository = secondaryRepository;
        this.matrixRepository = matrixRepository;
    }

    /**
     * 解析Excel文件（不保存到数据库）
     */
    public IndicatorTreeDTO parseExcel(MultipartFile file) {
        try {
            // 保存上传文件到临时目录
            File tempFile = File.createTempFile("upload_", ".xlsx");
            file.transferTo(tempFile);

            try {
                // 调用Python解析器
                Map<String, Object> input = new HashMap<>();
                input.put("action", "parse");
                input.put("filePath", tempFile.getAbsolutePath());

                String inputJson = objectMapper.writeValueAsString(input);
                String output = executePythonScript(inputJson);

                Map<String, Object> result = objectMapper.readValue(output, Map.class);

                if (Boolean.TRUE.equals(result.get("success")) || result.containsKey("levels")) {
                    return convertToIndicatorTree(result);
                } else {
                    String errorMsg = (String) result.get("message");
                    String traceback = result.containsKey("traceback") ? (String) result.get("traceback") : "";
                    log.error("Python解析失败: {}, traceback: {}", errorMsg, traceback);
                    throw new RuntimeException(errorMsg);
                }
            } finally {
                tempFile.delete();
            }
        } catch (Exception e) {
            log.error("解析Excel失败", e);
            throw new RuntimeException("解析Excel失败: " + e.getMessage());
        }
    }

    /**
     * 导入指标到数据库
     */
    @Transactional
    public ImportResultDTO importIndicators(MultipartFile file, String templateName) {
        try {
            // 1. 解析Excel
            IndicatorTreeDTO parsed = parseExcel(file);

            // 2. 创建模板
            IndicatorTemplate template = new IndicatorTemplate();
            template.setTemplateName(templateName != null ? templateName : "默认模板");
            template.setTemplateCode(generateTemplateCode());
            template.setSourceFile(file.getOriginalFilename());
            template.setStatus(IndicatorTemplate.Status.ACTIVE);
            template = templateRepository.save(template);

            // 3. 保存层级和维度
            int primaryCount = 0;
            int secondaryCount = 0;

            for (IndicatorTreeDTO.LevelNode levelNode : parsed.getLevels()) {
                LevelDefinition level = new LevelDefinition();
                level.setTemplate(template);
                level.setLevelName(levelNode.getName());
                level.setLevelCode(generateCode(levelNode.getName()));
                level.setDescription(levelNode.getDescription());
                level.setSortOrder(levelNode.getSortOrder());
                level = levelRepository.save(level);

                if (levelNode.getPrimaryDimensions() != null) {
                    for (IndicatorTreeDTO.PrimaryNode primaryNode : levelNode.getPrimaryDimensions()) {
                        PrimaryDimension primary = new PrimaryDimension();
                        primary.setLevel(level);
                        primary.setDimensionName(primaryNode.getName());
                        primary.setDimensionCode(primaryNode.getCode());
                        primary.setSortOrder(primaryNode.getSortOrder());
                        primary = primaryRepository.save(primary);
                        primaryCount++;

                        if (primaryNode.getSecondaryDimensions() != null) {
                            for (IndicatorTreeDTO.SecondaryNode secNode : primaryNode.getSecondaryDimensions()) {
                                SecondaryDimension secondary = new SecondaryDimension();
                                secondary.setPrimaryDimension(primary);
                                secondary.setDimensionName(secNode.getName());
                                secondary.setDimensionCode(secNode.getCode());
                                secondary.setSortOrder(secNode.getSortOrder());
                                secondary.setMetricType(
                                        "QUALITATIVE".equals(secNode.getMetricType()) ?
                                                SecondaryDimension.MetricType.QUALITATIVE :
                                                SecondaryDimension.MetricType.QUANTITATIVE);
                                secondary.setScoreDirection(
                                        "NEGATIVE".equals(secNode.getScoreDirection()) ?
                                                SecondaryDimension.ScoreDirection.NEGATIVE :
                                                SecondaryDimension.ScoreDirection.POSITIVE);
                                secondaryRepository.save(secondary);
                                secondaryCount++;
                            }
                        }
                    }
                }
            }

            // 4. 更新模板统计
            template.setLevelCount(parsed.getLevels().size());
            template.setPrimaryCount(primaryCount);
            template.setSecondaryCount(secondaryCount);
            templateRepository.save(template);

            // 5. 返回结果（包含模板ID）
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
     * 获取指标模板列表（不包含嵌套的levels数据，避免循环引用）
     */
    public List<IndicatorTemplateDTO> getTemplates() {
        List<IndicatorTemplate> templates = templateRepository.findAllByOrderByCreatedAtDesc();
        return templates.stream().map(t -> {
            IndicatorTemplateDTO dto = new IndicatorTemplateDTO();
            dto.setId(t.getId());
            dto.setTemplateName(t.getTemplateName());
            dto.setTemplateCode(t.getTemplateCode());
            dto.setDescription(t.getDescription());
            dto.setLevelCount(t.getLevelCount());
            dto.setPrimaryCount(t.getPrimaryCount());
            dto.setSecondaryCount(t.getSecondaryCount());
            dto.setStatus(t.getStatus() != null ? t.getStatus().name() : null);
            dto.setSourceFile(t.getSourceFile());
            dto.setCreatedAt(t.getCreatedAt());
            dto.setUpdatedAt(t.getUpdatedAt());
            return dto;
        }).collect(java.util.stream.Collectors.toList());
    }

    /**
     * 获取指标树
     */
    public IndicatorTreeDTO getIndicatorTree(Long templateId) {
        IndicatorTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("模板不存在"));

        List<LevelDefinition> levels = levelRepository.findByTemplateIdOrderBySortOrder(templateId);

        List<IndicatorTreeDTO.LevelNode> levelNodes = new ArrayList<>();
        int totalPrimary = 0;
        int totalSecondary = 0;
        List<String> levelNames = new ArrayList<>();
        Map<String, Integer> primaryCountMap = new HashMap<>();

        for (LevelDefinition level : levels) {
            IndicatorTreeDTO.LevelNode levelNode = new IndicatorTreeDTO.LevelNode();
            levelNode.setId(level.getId());
            levelNode.setName(level.getLevelName());
            levelNode.setDescription(level.getDescription());
            levelNode.setSortOrder(level.getSortOrder());

            List<PrimaryDimension> primaries = primaryRepository.findByLevelIdOrderBySortOrder(level.getId());
            List<IndicatorTreeDTO.PrimaryNode> primaryNodes = new ArrayList<>();

            for (PrimaryDimension primary : primaries) {
                IndicatorTreeDTO.PrimaryNode primaryNode = new IndicatorTreeDTO.PrimaryNode();
                primaryNode.setId(primary.getId());
                primaryNode.setName(primary.getDimensionName());
                primaryNode.setCode(primary.getDimensionCode());
                primaryNode.setSortOrder(primary.getSortOrder());
                primaryNode.setWeight(primary.getWeight() != null ? primary.getWeight().doubleValue() : 0.0);

                List<SecondaryDimension> secondaries = secondaryRepository.findByPrimaryDimensionIdOrderBySortOrder(primary.getId());
                List<IndicatorTreeDTO.SecondaryNode> secondaryNodes = new ArrayList<>();

                for (SecondaryDimension sec : secondaries) {
                    IndicatorTreeDTO.SecondaryNode secNode = new IndicatorTreeDTO.SecondaryNode();
                    secNode.setId(sec.getId());
                    secNode.setName(sec.getDimensionName());
                    secNode.setCode(sec.getDimensionCode());
                    secNode.setSortOrder(sec.getSortOrder());
                    secNode.setMetricType(sec.getMetricType().name());
                    secNode.setAggregationMethod(sec.getAggregationMethod());
                    secNode.setScoreDirection(sec.getScoreDirection().name());
                    secNode.setUnit(sec.getUnit());
                    secNode.setWeight(sec.getWeight() != null ? sec.getWeight().doubleValue() : 0.0);
                    secondaryNodes.add(secNode);
                    totalSecondary++;
                }

                primaryNode.setSecondaryDimensions(secondaryNodes);
                primaryNodes.add(primaryNode);
                totalPrimary++;
            }

            levelNode.setPrimaryDimensions(primaryNodes);
            levelNodes.add(levelNode);
            levelNames.add(level.getLevelName());
            primaryCountMap.put(level.getLevelName(), primaries.size());
        }

        IndicatorTreeDTO.Statistics stats = new IndicatorTreeDTO.Statistics();
        stats.setLevelCount(levels.size());
        stats.setTotalPrimaryDimensions(totalPrimary);
        stats.setTotalSecondaryDimensions(totalSecondary);
        stats.setLevelNames(levelNames);
        stats.setPrimaryDimensionCountPerLevel(primaryCountMap);

        IndicatorTreeDTO result = new IndicatorTreeDTO();
        result.setLevels(levelNodes);
        result.setStatistics(stats);

        return result;
    }

    /**
     * 获取指定层级的一级维度
     */
    public List<PrimaryDimension> getPrimaryDimensions(Long levelId) {
        return primaryRepository.findByLevelIdOrderBySortOrder(levelId);
    }

    /**
     * 获取指定一级维度的二级维度
     */
    public List<SecondaryDimension> getSecondaryDimensions(Long primaryId) {
        return secondaryRepository.findByPrimaryDimensionIdOrderBySortOrder(primaryId);
    }

    /**
     * 获取AHP矩阵数据
     */
    public Map<String, Object> getAhpMatrix(Long levelId) {
        LevelDefinition level = levelRepository.findById(levelId)
                .orElseThrow(() -> new RuntimeException("层级不存在"));

        List<PrimaryDimension> primaries = primaryRepository.findByLevelIdOrderBySortOrder(levelId);

        List<String> dimensions = primaries.stream()
                .map(PrimaryDimension::getDimensionName)
                .collect(Collectors.toList());

        int n = dimensions.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                matrix[i][j] = (i == j) ? 1.0 : 0.0;
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("levelId", levelId);
        result.put("levelName", level.getLevelName());
        result.put("dimensions", dimensions);
        result.put("matrix", matrix);

        return result;
    }

    /**
     * 计算AHP权重
     */
    public AhpWeightResultDTO calculateAhpWeights(Long levelId, double[][] matrix) {
        try {
            LevelDefinition level = levelRepository.findById(levelId)
                    .orElseThrow(() -> new RuntimeException("层级不存在"));

            List<PrimaryDimension> primaries = primaryRepository.findByLevelIdOrderBySortOrder(levelId);

            if (primaries.size() != matrix.length) {
                throw new RuntimeException("矩阵维度与一级维度数量不匹配");
            }

            // 调用Python计算AHP权重
            Map<String, Object> input = new HashMap<>();
            input.put("action", "ahp");

            Map<String, Integer> priorities = new HashMap<>();
            for (int i = 0; i < primaries.size(); i++) {
                priorities.put(primaries.get(i).getDimensionName(), i + 1);
            }
            input.put("priorities", priorities);

            String inputJson = objectMapper.writeValueAsString(input);
            String output = executePythonScript(inputJson);

            Map<String, Object> result = objectMapper.readValue(output, Map.class);

            if (Boolean.TRUE.equals(result.get("success"))) {
                Map<String, Object> weightsMap = (Map<String, Object>) result.get("weights");
                Double cr = ((Number) result.get("consistencyRatio")).doubleValue();

                Map<String, Double> weights = new HashMap<>();
                for (Map.Entry<String, Object> entry : weightsMap.entrySet()) {
                    weights.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                }

                return new AhpWeightResultDTO(levelId, weights, cr);
            } else {
                throw new RuntimeException((String) result.get("message"));
            }

        } catch (Exception e) {
            log.error("AHP计算失败", e);
            throw new RuntimeException("AHP计算失败: " + e.getMessage());
        }
    }

    /**
     * 保存AHP权重
     */
    @Transactional
    public void saveAhpWeights(Long levelId, Map<String, Double> weights) {
        LevelDefinition level = levelRepository.findById(levelId)
                .orElseThrow(() -> new RuntimeException("层级不存在"));

        List<PrimaryDimension> primaries = primaryRepository.findByLevelIdOrderBySortOrder(levelId);

        for (PrimaryDimension primary : primaries) {
            Double weight = weights.get(primary.getDimensionName());
            if (weight != null) {
                primary.setWeight(BigDecimal.valueOf(weight));
                primaryRepository.save(primary);
            }
        }
    }

    /**
     * 更新二级维度配置
     */
    @Transactional
    public void updateSecondaryConfig(Long id, SecondaryDimension config) {
        SecondaryDimension secondary = secondaryRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("二级维度不存在"));

        if (config.getMetricType() != null) {
            secondary.setMetricType(config.getMetricType());
        }
        if (config.getAggregationMethod() != null) {
            secondary.setAggregationMethod(config.getAggregationMethod());
        }
        if (config.getScoreDirection() != null) {
            secondary.setScoreDirection(config.getScoreDirection());
        }
        if (config.getUnit() != null) {
            secondary.setUnit(config.getUnit());
        }
        if (config.getBaselineValue() != null) {
            secondary.setBaselineValue(config.getBaselineValue());
        }
        if (config.getTargetValue() != null) {
            secondary.setTargetValue(config.getTargetValue());
        }

        secondaryRepository.save(secondary);
    }

    /**
     * 删除模板
     */
    @Transactional
    public void deleteTemplate(Long templateId) {
        templateRepository.deleteById(templateId);
    }

    /**
     * 执行Python脚本
     */
    private String executePythonScript(String inputJson) throws Exception {
        ProcessBuilder processBuilder = new ProcessBuilder(
                pythonExecutable,
                "-u",
                pythonScriptPath
        );

        Map<String, String> env = processBuilder.environment();
        env.put("PYTHONIOENCODING", "utf-8");

        Process process = processBuilder.start();

        // 发送输入
        try (OutputStream writer = process.getOutputStream()) {
            writer.write(inputJson.getBytes(StandardCharsets.UTF_8));
            writer.flush();
        }

        // 读取输出
        StringBuilder output = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
        }

        boolean finished = process.waitFor(pythonTimeout, TimeUnit.SECONDS);
        if (!finished) {
            process.destroy();
            throw new RuntimeException("Python脚本执行超时");
        }

        int exitCode = process.exitValue();
        if (exitCode != 0) {
            throw new RuntimeException("Python脚本执行失败: " + output);
        }

        return output.toString().trim();
    }

    /**
     * 将Python返回结果转换为IndicatorTreeDTO
     */
    @SuppressWarnings("unchecked")
    private IndicatorTreeDTO convertToIndicatorTree(Map<String, Object> result) {
        IndicatorTreeDTO dto = new IndicatorTreeDTO();

        List<Map<String, Object>> levelsData = (List<Map<String, Object>>) result.get("levels");
        List<IndicatorTreeDTO.LevelNode> levelNodes = new ArrayList<>();

        int levelOrder = 0;
        for (Map<String, Object> levelData : levelsData) {
            IndicatorTreeDTO.LevelNode levelNode = new IndicatorTreeDTO.LevelNode();
            levelNode.setName((String) levelData.get("name"));
            levelNode.setDescription((String) levelData.get("description"));
            levelNode.setSortOrder(++levelOrder);

            List<Map<String, Object>> primariesData = (List<Map<String, Object>>) levelData.get("primaryDimensions");
            List<IndicatorTreeDTO.PrimaryNode> primaryNodes = new ArrayList<>();

            int primaryOrder = 0;
            for (Map<String, Object> primaryData : primariesData) {
                IndicatorTreeDTO.PrimaryNode primaryNode = new IndicatorTreeDTO.PrimaryNode();
                primaryNode.setName((String) primaryData.get("name"));
                primaryNode.setCode((String) primaryData.get("code"));
                primaryNode.setSortOrder(++primaryOrder);

                List<Map<String, Object>> secondariesData = (List<Map<String, Object>>) primaryData.get("secondaryDimensions");
                List<IndicatorTreeDTO.SecondaryNode> secondaryNodes = new ArrayList<>();

                int secOrder = 0;
                for (Map<String, Object> secData : secondariesData) {
                    IndicatorTreeDTO.SecondaryNode secNode = new IndicatorTreeDTO.SecondaryNode();
                    secNode.setName((String) secData.get("name"));
                    secNode.setCode((String) secData.get("code"));
                    secNode.setSortOrder(++secOrder);
                    secNode.setMetricType("QUANTITATIVE");
                    secNode.setScoreDirection("POSITIVE");
                    secondaryNodes.add(secNode);
                }

                primaryNode.setSecondaryDimensions(secondaryNodes);
                primaryNodes.add(primaryNode);
            }

            levelNode.setPrimaryDimensions(primaryNodes);
            levelNodes.add(levelNode);
        }

        dto.setLevels(levelNodes);

        // 统计信息
        IndicatorTreeDTO.Statistics stats = new IndicatorTreeDTO.Statistics();
        stats.setLevelCount(levelNodes.size());
        stats.setLevelNames(levelNodes.stream().map(IndicatorTreeDTO.LevelNode::getName).collect(Collectors.toList()));

        int totalPrimary = levelNodes.stream().mapToInt(l -> l.getPrimaryDimensions().size()).sum();
        int totalSecondary = levelNodes.stream()
                .flatMap(l -> l.getPrimaryDimensions().stream())
                .mapToInt(p -> p.getSecondaryDimensions().size()).sum();

        stats.setTotalPrimaryDimensions(totalPrimary);
        stats.setTotalSecondaryDimensions(totalSecondary);

        Map<String, Integer> primaryCountMap = new HashMap<>();
        for (IndicatorTreeDTO.LevelNode level : levelNodes) {
            primaryCountMap.put(level.getName(), level.getPrimaryDimensions().size());
        }
        stats.setPrimaryDimensionCountPerLevel(primaryCountMap);

        dto.setStatistics(stats);

        return dto;
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
