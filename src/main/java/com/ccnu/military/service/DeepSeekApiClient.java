package com.ccnu.military.service;

import com.ccnu.military.dto.IndicatorAnalysisResult;
import com.ccnu.military.dto.SourceDataDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;

/**
 * DeepSeek API 客户端
 */
@Slf4j
@Component
public class DeepSeekApiClient {

    @Value("${deepseek.api.url:https://api.deepseek.com}")
    private String apiUrl;

    @Value("${deepseek.api.key}")
    private String apiKey;

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public DeepSeekApiClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .build();
    }

    /**
     * 批量分析多个指标
     */
    public Map<String, IndicatorAnalysisResult> batchAnalyzeIndicators(
            List<String> indicators,
            String domain,
            String category) throws Exception {

        long startTime = System.currentTimeMillis();
        String prompt = buildBatchPrompt(indicators, domain, category);

        log.info("开始批量分析 {} 个指标", indicators.size());

        String response = chat(prompt);
        Map<String, IndicatorAnalysisResult> results = parseBatchResponse(response, indicators);

        long duration = System.currentTimeMillis() - startTime;
        log.info("批量分析完成，{} 个指标，耗时 {} ms", indicators.size(), duration);

        return results;
    }

    /**
     * 单个指标分析
     */
    public IndicatorAnalysisResult analyzeSingleIndicator(
            String indicatorName,
            String domain) throws Exception {

        String prompt = buildSinglePrompt(indicatorName, domain);
        String response = chat(prompt);

        // 直接解析单个指标的响应
        try {
            String jsonStr = extractJsonFromResponse(response);
            Map<String, Object> responseMap = objectMapper.readValue(jsonStr, Map.class);

            // 检查是否是单个对象格式（没有results包装）
            if (responseMap.containsKey("indicatorName")) {
                return parseSingleResult(responseMap);
            }

            // 检查是否是results数组格式
            Object resultListRaw = responseMap.get("results");
            if (resultListRaw instanceof List && !((List<?>) resultListRaw).isEmpty()) {
                Object firstItem = ((List<?>) resultListRaw).get(0);
                if (firstItem instanceof Map) {
                    return parseSingleResult((Map<String, Object>) firstItem);
                }
            }

            throw new RuntimeException("AI响应格式错误：未找到指标数据");
        } catch (Exception e) {
            log.error("解析单个指标响应失败: {}", response, e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }
    }

    // ============================================
    // Prompt 构建
    // ============================================

    private String buildBatchPrompt(List<String> indicators, String domain, String category) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个军事通信指标分析专家。请分析以下指标并返回JSON格式结果。\n\n");

        if (domain != null && !domain.isEmpty()) {
            sb.append("【领域】").append(domain).append("\n");
        }
        if (category != null && !category.isEmpty()) {
            sb.append("【指标大类】").append(category).append("\n");
        }

        sb.append("\n【待分析指标】（共").append(indicators.size()).append("个）：\n");
        for (int i = 0; i < indicators.size(); i++) {
            sb.append(i + 1).append(". ").append(indicators.get(i)).append("\n");
        }

        sb.append("\n【分析要求】：\n");
        sb.append("对每个指标，请判断其类型并提供详细信息。\n\n");
        sb.append("判断规则：\n");
        sb.append("- 定量指标(QUANTITATIVE)：可以直接通过仪器测量或数值计算得到精确数值的指标\n");
        sb.append("- 定性指标(QUALITATIVE)：需要专家主观判断或只能定性描述的指标\n\n");
        sb.append("【返回格式】（必须严格遵循JSON格式）：\n");
        sb.append("{\n");
        sb.append("  \"results\": [\n");
        sb.append("    {\n");
        sb.append("      \"indicatorName\": \"指标名称\",\n");
        sb.append("      \"indicatorType\": \"QUANTITATIVE或QUALITATIVE\",\n");
        sb.append("      \"indicatorTypeDesc\": \"定量指标或定性指标\",\n");
        sb.append("      \"confidence\": 0.95,\n");
        sb.append("      \"description\": \"指标的简要说明（50字以内）\",\n");
        sb.append("      \"unit\": \"单位（定量指标必填）\",\n\n");
        sb.append("      \"formula\": \"计算公式（使用标准符号，如：SINR = Ps / (Pn + Pi)）\",\n");
        sb.append("      \"formulaDescription\": \"公式文字说明（如：信干噪比 = 信号功率 / (噪声功率 + 干扰功率)）\",\n");
        sb.append("      \"calculationMethod\": \"计算方法简述（50字以内）\",\n\n");
        sb.append("      \"sourceDataList\": [\n");
        sb.append("        {\n");
        sb.append("          \"dataName\": \"基础数据名称（必须与公式中的变量对应）\",\n");
        sb.append("          \"formulaSymbol\": \"公式中的符号（如：Ps, Pn）\",\n");
        sb.append("          \"measurementMethod\": \"数据来源表名（如：通信记录、作战基础信息、链路维护事件、安全事件、攻击作战、防御作战）\",\n");
        sb.append("          \"dataType\": \"NUMERIC或PERCENTAGE\",\n");
        sb.append("          \"unit\": \"单位\",\n");
        sb.append("          \"isFormulaRelated\": true,\n");
        sb.append("          \"isEssential\": true,\n");
        sb.append("          \"priority\": 1,\n");
        sb.append("          \"confidence\": 0.9\n");
        sb.append("        }\n");
        sb.append("      ]\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("【数据来源表名说明】：\n");
        sb.append("请根据基础数据判断其最可能来自哪个数据表：\n");
        sb.append("- 通信记录：通信过程中的性能数据，如信号功率、噪声功率、传输延迟、吞吐量、信噪比等\n");
        sb.append("- 作战基础信息：作战相关的统计数据，如人员数量、装备数量、功耗等\n");
        sb.append("- 链路维护事件：链路中断、恢复、维护相关的事件数据\n");
        sb.append("- 安全事件：密钥、安全相关的攻击检测、加密认证数据\n");
        sb.append("- 攻击作战：通信对抗中的攻击作战数据\n");
        sb.append("- 防御作战：通信对抗中的防御作战数据\n");
        sb.append("【公式编写规范】：\n");
        sb.append("1. 使用标准工程符号（如：Ps=信号功率，Pn=噪声功率，Pi=干扰功率）\n");
        sb.append("2. 公式应简洁明了，能清晰表达计算关系\n");
        sb.append("3. 只列出公式计算必需的基础数据，不要列出其他相关但非必需的数据\n\n");
        sb.append("【示例】：\n\n");
        sb.append("输入：信干噪比\n");
        sb.append("正确输出：\n");
        sb.append("{\n");
        sb.append("  \"indicatorName\": \"信干噪比\",\n");
        sb.append("  \"indicatorType\": \"QUANTITATIVE\",\n");
        sb.append("  \"indicatorTypeDesc\": \"定量指标\",\n");
        sb.append("  \"formula\": \"SINR = Ps / (Pn + Pi)\",\n");
        sb.append("  \"formulaDescription\": \"信干噪比 = 信号功率 / (噪声功率 + 干扰功率)\",\n");
        sb.append("  \"calculationMethod\": \"信号功率除以噪声与干扰功率之和\",\n");
        sb.append("  \"sourceDataList\": [\n");
        sb.append("    {\"dataName\": \"信号功率\", \"formulaSymbol\": \"Ps\", \"isFormulaRelated\": true, \"isEssential\": true},\n");
        sb.append("    {\"dataName\": \"噪声功率\", \"formulaSymbol\": \"Pn\", \"isFormulaRelated\": true, \"isEssential\": true},\n");
        sb.append("    {\"dataName\": \"干扰功率\", \"formulaSymbol\": \"Pi\", \"isFormulaRelated\": true, \"isEssential\": true}\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("注意：\n");
        sb.append("1. 必须为每个指标都返回分析结果\n");
        sb.append("2. sourceDataList 只包含公式计算必需的数据\n");
        sb.append("3. 定性指标的 formula 相关字段可省略，sourceDataList 设为 []\n");

        return sb.toString();
    }

    private String buildSinglePrompt(String indicatorName, String domain) {
        StringBuilder sb = new StringBuilder();
        sb.append("你是一个军事通信指标分析专家。请分析以下指标并返回JSON格式结果。\n\n");
        sb.append("指标名称：").append(indicatorName).append("\n");
        if (domain != null) {
            sb.append(domain);
        }
        sb.append("\n\n请返回JSON格式：\n");
        sb.append("{\n");
        sb.append("  \"indicatorName\": \"").append(indicatorName).append("\",\n");
        sb.append("  \"indicatorType\": \"QUANTITATIVE或QUALITATIVE\",\n");
        sb.append("  \"indicatorTypeDesc\": \"定量指标或定性指标\",\n");
        sb.append("  \"confidence\": 0.95,\n");
        sb.append("  \"description\": \"简要说明\",\n");
        sb.append("  \"unit\": \"单位\",\n");
        sb.append("  \"formula\": \"计算公式（如：SINR = Ps / (Pn + Pi)）\",\n");
        sb.append("  \"formulaDescription\": \"公式说明（如：信干噪比 = 信号功率 / (噪声功率 + 干扰功率)）\",\n");
        sb.append("  \"calculationMethod\": \"计算方法\",\n");
        sb.append("  \"sourceDataList\": [\n");
        sb.append("    {\n");
        sb.append("      \"dataName\": \"基础数据名称\",\n");
        sb.append("      \"formulaSymbol\": \"公式符号（如Ps, Pn）\",\n");
        sb.append("      \"measurementMethod\": \"数据来源表名（如：通信记录、作战基础信息、链路维护事件、安全事件、攻击作战、防御作战）\",\n");
        sb.append("      \"dataType\": \"NUMERIC或PERCENTAGE\",\n");
        sb.append("      \"unit\": \"单位（如dBm, %, s）\",\n");
        sb.append("      \"isFormulaRelated\": true,\n");
        sb.append("      \"isEssential\": true,\n");
        sb.append("      \"priority\": 1,\n");
        sb.append("      \"confidence\": 0.9\n");
        sb.append("    }\n");
        sb.append("  ]\n");
        sb.append("}\n\n");
        sb.append("【数据来源表名说明】：\n");
        sb.append("- 通信记录：信号功率、噪声功率、传输延迟、吞吐量、信噪比等\n");
        sb.append("- 作战基础信息：人员数量、装备数量、功耗等\n");
        sb.append("- 链路维护事件：链路中断、恢复、维护事件\n");
        sb.append("- 安全事件：密钥、安全攻击检测数据\n");
        sb.append("- 攻击作战：通信对抗攻击数据\n");
        sb.append("- 防御作战：通信对抗防御数据\n\n");
        sb.append("【注意】：\n");
        sb.append("1. sourceDataList 必须包含公式中所有必需的基础数据\n");
        sb.append("2. dataName 和 formulaSymbol 必须对应\n");
        sb.append("3. dataType 必须为 NUMERIC 或 PERCENTAGE\n");
        sb.append("4. unit 必须填写对应的单位（如dBm, %, s）\n");
        sb.append("5. 如果是定性指标，formula 可省略，sourceDataList 设为 []\n");
        return sb.toString();
    }

    // ============================================
    // 响应解析
    // ============================================

    private Map<String, IndicatorAnalysisResult> parseBatchResponse(
            String response,
            List<String> indicators) throws Exception {

        Map<String, IndicatorAnalysisResult> results = new LinkedHashMap<>();

        try {
            String jsonStr = extractJsonFromResponse(response);
            Map<String, Object> responseMap = objectMapper.readValue(jsonStr, Map.class);

            Object resultListRaw = responseMap.get("results");
            List<Map<String, Object>> resultList = new ArrayList<>();

            if (resultListRaw instanceof List) {
                for (Object item : (List<?>) resultListRaw) {
                    if (item instanceof Map) {
                        resultList.add((Map<String, Object>) item);
                    }
                }
            }

            for (Map<String, Object> item : resultList) {
                String name = (String) item.get("indicatorName");
                IndicatorAnalysisResult analysis = parseSingleResult(item);
                results.put(name, analysis);
            }

            if (results.size() < indicators.size()) {
                log.warn("AI返回结果数量不足，尝试按顺序匹配...");
                for (int i = 0; i < indicators.size(); i++) {
                    String name = indicators.get(i);
                    if (!results.containsKey(name) && i < resultList.size()) {
                        results.put(name, parseSingleResult(resultList.get(i)));
                    }
                }
            }

        } catch (Exception e) {
            log.error("解析AI响应失败: {}", response, e);
            throw new RuntimeException("解析AI响应失败: " + e.getMessage(), e);
        }

        return results;
    }

    private IndicatorAnalysisResult parseSingleResult(Map<String, Object> item) {
        IndicatorAnalysisResult result = new IndicatorAnalysisResult();
        result.setIndicatorName((String) item.get("indicatorName"));
        result.setIndicatorType((String) item.get("indicatorType"));
        result.setIndicatorTypeDesc((String) item.get("indicatorTypeDesc"));
        result.setConfidence(getDoubleValue(item.get("confidence")));
        result.setDescription((String) item.get("description"));
        result.setUnit((String) item.get("unit"));
        result.setFormula((String) item.get("formula"));
        result.setFormulaDescription((String) item.get("formulaDescription"));
        result.setCalculationMethod((String) item.get("calculationMethod"));
        result.setSourceDataHint((String) item.get("sourceDataHint"));

        log.info("[调试] parseSingleResult 解析指标: indicatorName={}", item.get("indicatorName"));

        Object sourceDataRaw = item.get("sourceDataList");
        log.info("[调试] parseSingleResult - sourceDataRaw 类型: {}, 值: {}",
            sourceDataRaw != null ? sourceDataRaw.getClass().getSimpleName() : "null",
            sourceDataRaw);

        if (sourceDataRaw != null) {
            List<SourceDataDTO> sourceDataList = new ArrayList<>();
            try {
                // 如果是字符串，尝试解析为 JSON 数组
                if (sourceDataRaw instanceof String) {
                    String str = (String) sourceDataRaw;
                    log.info("[调试] sourceDataRaw 是字符串，长度: {}", str.length());
                    if (!str.isEmpty() && str.startsWith("[")) {
                        List<Map<String, Object>> parsed = objectMapper.readValue(str, List.class);
                        log.info("[调试] 字符串解析为 List，大小: {}", parsed.size());
                        for (Map<String, Object> sd : parsed) {
                            sourceDataList.add(parseSourceData(sd));
                        }
                    } else {
                        log.warn("[调试] sourceDataRaw 字符串不是以 [ 开头，跳过解析");
                    }
                } else if (sourceDataRaw instanceof List) {
                    // 已经是 List，直接解析
                    List<?> rawList = (List<?>) sourceDataRaw;
                    log.info("[调试] sourceDataRaw 已是 List，大小: {}", rawList.size());
                    for (Object sd : rawList) {
                        if (sd instanceof Map) {
                            sourceDataList.add(parseSourceData((Map<String, Object>) sd));
                        } else {
                            log.warn("[调试] sourceDataList 中的元素不是 Map: {}", sd.getClass().getSimpleName());
                        }
                    }
                } else {
                    log.warn("[调试] sourceDataRaw 类型不支持: {}", sourceDataRaw.getClass().getSimpleName());
                }
            } catch (Exception e) {
                log.error("[调试] 解析 sourceDataList 失败: {}", e.getMessage(), e);
            }
            log.info("[调试] parseSingleResult - 最终 sourceDataList 大小: {}", sourceDataList.size());
            result.setSourceDataList(sourceDataList);
        } else {
            log.info("[调试] parseSingleResult - sourceDataRaw 为 null，跳过解析");
        }

        return result;
    }

    private SourceDataDTO parseSourceData(Map<String, Object> sd) {
        String dataName = (String) sd.get("dataName");
        String formulaSymbol = (String) sd.get("formulaSymbol");
        String measurementMethod = (String) sd.get("measurementMethod");
        String dataType = (String) sd.get("dataType");
        String unit = (String) sd.get("unit");

        log.info("[调试] parseSourceData 解析到: dataName={}, formulaSymbol={}, measurementMethod={}, dataType={}, unit={}",
            dataName, formulaSymbol, measurementMethod, dataType, unit);

        return SourceDataDTO.builder()
                .sourceDataName(dataName)
                .formulaSymbol(formulaSymbol)
                .measurementMethod(measurementMethod)
                .dataType(dataType)
                .unit(unit)
                .priority(getIntValue(sd.get("priority")))
                .confidence(getDoubleValue(sd.get("confidence")))
                .isFormulaRelated(getBooleanValue(sd.get("isFormulaRelated")))
                .isEssential(getBooleanValue(sd.get("isEssential")))
                .build();
    }

    private Double getDoubleValue(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Integer) return ((Integer) value).doubleValue();
        if (value instanceof Number) return ((Number) value).doubleValue();
        return 0.0;
    }

    private Integer getIntValue(Object value) {
        if (value == null) return 1;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Number) return ((Number) value).intValue();
        return 1;
    }

    private Boolean getBooleanValue(Object value) {
        if (value == null) return false;
        if (value instanceof Boolean) return (Boolean) value;
        return false;
    }

    // ============================================
    // HTTP 调用
    // ============================================

    private String chat(String prompt) throws Exception {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", "deepseek-v4-flash");

        Map<String, String> systemMessage = new HashMap<>();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个军事通信指标分析专家，擅长分析指标特性并返回JSON格式的分析结果。请严格按照要求的JSON格式输出，不要包含其他文字。");

        Map<String, String> userMessage = new HashMap<>();
        userMessage.put("role", "user");
        userMessage.put("content", prompt);

        requestBody.put("messages", Arrays.asList(systemMessage, userMessage));
        requestBody.put("temperature", 0.3);
        requestBody.put("max_tokens", 4000);

        String jsonBody = objectMapper.writeValueAsString(requestBody);

        log.debug("DeepSeek API Request: {}", jsonBody);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl + "/v1/chat/completions"))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .timeout(Duration.ofSeconds(120))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        log.debug("DeepSeek API Response: {}", response.body());

        if (response.statusCode() != 200) {
            throw new RuntimeException("DeepSeek API 调用失败: " + response.statusCode() + " - " + response.body());
        }

        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> choices = (List<Map<String, Object>>) responseMap.get("choices");
        if (choices != null && !choices.isEmpty()) {
            Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
            return (String) message.get("content");
        }

        throw new RuntimeException("DeepSeek API 响应格式错误");
    }

    private String extractJsonFromResponse(String response) {
        int jsonStart = response.indexOf("```json");
        if (jsonStart >= 0) {
            int start = response.indexOf("{", jsonStart);
            int end = response.lastIndexOf("```", response.length() - 1);
            if (start >= 0 && end > start) {
                return response.substring(start, end);
            }
        }

        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }

        return response;
    }
}
