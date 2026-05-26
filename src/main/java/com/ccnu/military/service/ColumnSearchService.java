package com.ccnu.military.service;

import com.ccnu.military.dto.ColumnSimilarityResult;
import com.ccnu.military.dto.IndicatorAnalysisResult;
import com.ccnu.military.dto.SourceDataDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 字段相似度搜索服务
 * 根据数据源名称搜索数据库表中的相似字段
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ColumnSearchService {

    private final JdbcTemplate jdbcTemplate;
    private final IndicatorVectorService indicatorVectorService;

    // 记录表与其中文字段名的映射
    private static final Map<String, Map<String, String>> TABLE_COLUMN_LABELS = new HashMap<>();

    static {
        // records_military_operation_info 表字段标签
        Map<String, String> opInfo = new HashMap<>();
        opInfo.put("operation_id", "作战编号");
        opInfo.put("avg_network_setup_time_ms", "网络建立时间");
        opInfo.put("total_node_count", "节点总数");
        opInfo.put("isolated_node_count", "孤立节点数");
        opInfo.put("actual_connections", "实际连接数");
        opInfo.put("command_personnel_count", "指挥人员数");
        opInfo.put("operator_personnel_count", "操作人员数");
        opInfo.put("maintenance_personnel_count", "维护人员数");
        opInfo.put("avg_experience_years", "平均经验年限");
        opInfo.put("annual_maintenance_hours", "年维护工时");
        opInfo.put("avg_training_frequency_per_year", "年培训频次");
        opInfo.put("total_equipment_count", "装备总数");
        opInfo.put("damaged_equipment_count", "战损装备数");
        opInfo.put("new_equipment_ratio", "新装备占比");
        opInfo.put("total_power_consumption_kw", "总功耗");
        opInfo.put("annual_electricity_consumption_kwh", "年用电量");
        opInfo.put("annual_fuel_consumption_liters", "年耗油量");
        opInfo.put("spectrum_reserve_mhz", "频谱预留");
        opInfo.put("spare_parts_satisfaction_rate", "备件满足率");
        opInfo.put("total_transport_distance_km", "运输总里程");
        opInfo.put("avg_altitude_m", "平均海拔");
        opInfo.put("weather_condition", "天气");
        opInfo.put("temperature_celsius", "气温");
        opInfo.put("electromagnetic_interference_level", "电磁干扰等级");
        TABLE_COLUMN_LABELS.put("records_military_operation_info", opInfo);

        // records_military_communication_info 表字段标签
        Map<String, String> commInfo = new HashMap<>();
        commInfo.put("operation_id", "作战编号");
        commInfo.put("src_node_id", "源节点");
        commInfo.put("dst_node_id", "目的节点");
        commInfo.put("comm_type", "通信类型");
        commInfo.put("start_time_ms", "起始时间");
        commInfo.put("end_time_ms", "结束时间");
        commInfo.put("call_req_ms", "呼叫请求时延");
        commInfo.put("call_resp_ms", "呼叫响应时延");
        commInfo.put("call_setup_ms", "呼叫建立时间");
        commInfo.put("call_success", "呼叫是否成功");
        commInfo.put("msg_bytes", "消息字节数");
        commInfo.put("trans_delay_ms", "传输延迟");
        commInfo.put("bandwidth_hz", "带宽");
        commInfo.put("snr_db", "信噪比");
        commInfo.put("throughput_bps", "吞吐量");
        commInfo.put("tx_power_dbm", "发射功率");
        commInfo.put("rx_power_dbm", "接收功率");
        commInfo.put("distance_km", "距离");
        commInfo.put("total_bits", "总比特数");
        commInfo.put("error_bits", "错误比特数");
        commInfo.put("packets_sent", "发送包数");
        commInfo.put("packets_lost", "丢包数");
        commInfo.put("fail_reason", "失败原因");
        commInfo.put("retry_cnt", "重试次数");
        commInfo.put("noise_power_dbm", "噪声功率");
        commInfo.put("jamming_power_dbm", "干扰功率");
        commInfo.put("sinr_db", "SINR");
        commInfo.put("jamming_margin_db", "干扰裕量");
        commInfo.put("detected", "是否被侦获");
        commInfo.put("intercepted", "是否被截获");
        commInfo.put("operator_id", "操作员");
        commInfo.put("operator_reaction_ms", "操作反应时间");
        commInfo.put("op_success", "操作成功");
        commInfo.put("comm_success", "通信成功");
        TABLE_COLUMN_LABELS.put("records_military_communication_info", commInfo);

        // records_link_maintenance_events 表字段标签
        Map<String, String> linkInfo = new HashMap<>();
        linkInfo.put("operation_id", "作战编号");
        linkInfo.put("source_node", "源节点");
        linkInfo.put("target_node", "目的节点");
        linkInfo.put("equipment_id", "设备");
        linkInfo.put("is_critical_link", "是否关键链路");
        linkInfo.put("interruption_start_ms", "中断开始");
        linkInfo.put("interruption_end_ms", "中断结束");
        linkInfo.put("interruption_reason", "中断原因");
        linkInfo.put("interruption_type", "中断类型");
        linkInfo.put("recovery_start_ms", "恢复开始");
        linkInfo.put("recovery_end_ms", "恢复结束");
        linkInfo.put("recovery_duration_ms", "恢复耗时");
        linkInfo.put("recovery_method", "恢复方式");
        linkInfo.put("recovery_success", "恢复是否成功");
        linkInfo.put("maintenance_required", "是否需要维护");
        linkInfo.put("maintenance_start_ms", "维护开始");
        linkInfo.put("maintenance_end_ms", "维护结束");
        linkInfo.put("maintenance_duration_ms", "维护耗时");
        linkInfo.put("maintenance_success", "维护成功");
        linkInfo.put("failure_reason", "故障原因");
        linkInfo.put("repair_method", "修复方式");
        linkInfo.put("operator_id", "操作员");
        linkInfo.put("feedback_content", "反馈内容");
        linkInfo.put("feedback_submitted", "是否已提交反馈");
        TABLE_COLUMN_LABELS.put("records_link_maintenance_events", linkInfo);

        // records_security_events 表字段标签
        Map<String, String> secInfo = new HashMap<>();
        secInfo.put("operation_id", "作战编号");
        secInfo.put("event_type", "事件类型");
        secInfo.put("event_time_ms", "事件时间");
        secInfo.put("node_id", "节点");
        secInfo.put("key_id", "密钥标识");
        secInfo.put("key_age_ms", "密钥龄期");
        secInfo.put("detected_by", "侦测方");
        secInfo.put("intercepted_content", "截获内容");
        secInfo.put("intercept_method", "截获手段");
        secInfo.put("interception_attempt_source", "企图截获来源");
        secInfo.put("impact_level", "影响等级");
        TABLE_COLUMN_LABELS.put("records_security_events", secInfo);

        // records_comm_attack_operation 表字段标签
        Map<String, String> attackInfo = new HashMap<>();
        attackInfo.put("operation_id", "作战编号");
        attackInfo.put("attack_type", "攻击类型");
        attackInfo.put("target_node", "目标节点");
        attackInfo.put("attack_start_time_ms", "攻击开始时间");
        attackInfo.put("attack_end_time_ms", "攻击结束时间");
        attackInfo.put("attack_success", "攻击是否成功");
        attackInfo.put("jamming_frequency_start_mhz", "干扰频率起始");
        attackInfo.put("jamming_frequency_end_mhz", "干扰频率结束");
        attackInfo.put("jamming_power_dbm", "干扰功率");
        TABLE_COLUMN_LABELS.put("records_comm_attack_operation", attackInfo);

        // records_comm_defense_operation 表字段标签
        Map<String, String> defenseInfo = new HashMap<>();
        defenseInfo.put("operation_id", "作战编号");
        defenseInfo.put("defense_type", "防御类型");
        defenseInfo.put("protected_node", "防御节点");
        defenseInfo.put("defense_start_time_ms", "防御开始时间");
        defenseInfo.put("defense_end_time_ms", "防御结束时间");
        defenseInfo.put("defense_success", "防御是否成功");
        defenseInfo.put("frequency_range_start_mhz", "频率范围起始");
        defenseInfo.put("frequency_range_end_mhz", "频率范围结束");
        TABLE_COLUMN_LABELS.put("records_comm_defense_operation", defenseInfo);
    }

    // 表中文名映射
    private static final Map<String, String> TABLE_LABELS = new HashMap<>();
    static {
        TABLE_LABELS.put("records_military_operation_info", "作战基础信息");
        TABLE_LABELS.put("records_military_communication_info", "通信记录");
        TABLE_LABELS.put("records_link_maintenance_events", "链路维护事件");
        TABLE_LABELS.put("records_security_events", "安全事件");
        TABLE_LABELS.put("records_comm_attack_operation", "通信攻击作战");
        TABLE_LABELS.put("records_comm_defense_operation", "通信防御作战");
    }

    // 数值类型字段（用于判断是否匹配指标类型）
    private static final Set<String> NUMERIC_TYPES = Set.of(
            "int", "bigint", "smallint", "tinyint", "decimal", "float", "double"
    );

    /**
     * 搜索相似字段
     * @param dataName 数据源名称（如"成功传输的数据量"）
     * @param limit 返回数量
     * @return 相似字段列表
     */
    public List<ColumnSimilarityResult> searchSimilarColumns(String dataName, int limit) {
        if (dataName == null || dataName.trim().isEmpty()) {
            log.warn("[调试] searchSimilarColumns 收到空查询: dataName={}", dataName);
            return Collections.emptyList();
        }

        String query = dataName.trim();
        log.info("========================================");
        log.info("[调试] searchSimilarColumns 收到查询请求");
        log.info("[调试] - dataName: {}", query);
        log.info("[调试] - limit: {}", limit);
        log.info("========================================");

        try {
            List<ColumnSimilarityResult> results = searchSimilarColumnsByVector(query, limit);
            log.info("[调试] 向量搜索完成，返回 {} 条结果", results.size());
            if (results.isEmpty()) {
                log.warn("[调试] 向量搜索返回空结果，尝试关键词匹配");
                results = searchSimilarColumnsByKeyword(query, limit);
                log.info("[调试] 关键词搜索完成，返回 {} 条结果", results.size());
            }
            return results;
        } catch (Exception e) {
            log.error("[调试] 向量搜索异常: {}", e.getMessage(), e);
            log.info("[调试] 异常，回退到关键词匹配");
            return searchSimilarColumnsByKeyword(query, limit);
        }
    }

    /**
     * 基于向量余弦相似度搜索相似字段
     */
    private List<ColumnSimilarityResult> searchSimilarColumnsByVector(String query, int limit) {
        log.info("[调试] ===== 进入 searchSimilarColumnsByVector =====");
        log.info("[调试] - query: {}", query);
        log.info("[调试] - limit: {}", limit);

        // 1. 编码查询词
        List<Double> queryVector = indicatorVectorService.encodeText(query);
        log.info("[调试] 向量编码完成，向量维度: {}", queryVector.size());
        if (queryVector.isEmpty()) {
            log.warn("[调试] 向量编码返回空结果，使用关键词匹配");
            return searchSimilarColumnsByKeyword(query, limit);
        }

        // 2. 遍历所有表的列，构建候选列表
        List<ColumnInfo> allColumns = new ArrayList<>();
        for (String tableName : TABLE_COLUMN_LABELS.keySet()) {
            for (Map.Entry<String, String> entry : TABLE_COLUMN_LABELS.get(tableName).entrySet()) {
                allColumns.add(new ColumnInfo(entry.getKey(), entry.getValue(), tableName));
            }
        }
        log.info("[调试] 构建候选列完成，总候选数: {}", allColumns.size());

        // 3. 批量编码所有列标签
        List<String> labels = allColumns.stream().map(col -> col.label).collect(Collectors.toList());
        log.info("[调试] 开始批量编码 {} 个标签...", labels.size());
        List<List<Double>> vectors = indicatorVectorService.encodeTexts(labels);
        log.info("[调试] 批量编码完成，返回 {} 个向量", vectors.size());
        if (vectors.isEmpty()) {
            log.warn("[调试] 批量向量编码返回空结果，使用关键词匹配");
            return searchSimilarColumnsByKeyword(query, limit);
        }

        // 4. 逐个计算余弦相似度
        List<ColumnSimilarityResult> results = new ArrayList<>();
        for (int i = 0; i < allColumns.size(); i++) {
            double similarity = indicatorVectorService.computeCosineSimilarity(queryVector, vectors.get(i));
            ColumnInfo col = allColumns.get(i);
            results.add(ColumnSimilarityResult.builder()
                    .columnName(col.name)
                    .columnLabel(col.label)
                    .tableName(col.tableName)
                    .tableLabel(TABLE_LABELS.getOrDefault(col.tableName, col.tableName))
                    .similarity(similarity)
                    .dataType(getColumnDataType(col.tableName, col.name))
                    .build());
        }

        // 5. 按相似度降序排列，取 top N
        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        List<ColumnSimilarityResult> topResults = results.stream().limit(limit).collect(Collectors.toList());
        log.info("[调试] 向量搜索完成: 查询='{}', 返回 {} 条结果，最高相似度={}",
                query, topResults.size(),
                topResults.isEmpty() ? 0 : topResults.get(0).getSimilarity());

        // 打印 top 结果详情
        if (!topResults.isEmpty()) {
            log.info("[调试] Top 结果详情:");
            for (int i = 0; i < Math.min(3, topResults.size()); i++) {
                ColumnSimilarityResult r = topResults.get(i);
                log.info("[调试]   {}. {}/{} 相似度={}", i + 1, r.getTableLabel(), r.getColumnLabel(), r.getSimilarity());
            }
        }

        return topResults;
    }

    private static class ColumnInfo {
        String name;
        String label;
        String tableName;
        ColumnInfo(String name, String label, String tableName) {
            this.name = name;
            this.label = label;
            this.tableName = tableName;
        }
    }

    /**
     * 基于关键词匹配的搜索（回退方案）
     */
    private List<ColumnSimilarityResult> searchSimilarColumnsByKeyword(String query, int limit) {
        log.info("[调试] ===== 进入 searchSimilarColumnsByKeyword =====");
        log.info("[调试] - query: {}", query);
        log.info("[调试] - limit: {}", limit);

        List<ColumnSimilarityResult> results = new ArrayList<>();
        int totalColumns = 0;
        int matchedColumns = 0;

        for (String tableName : TABLE_COLUMN_LABELS.keySet()) {
            Map<String, String> columnLabels = TABLE_COLUMN_LABELS.get(tableName);

            for (Map.Entry<String, String> entry : columnLabels.entrySet()) {
                totalColumns++;
                String columnName = entry.getKey();
                String columnLabel = entry.getValue();
                double similarity = calculateSimilarity(query, columnName, columnLabel);
                String tableLabel = TABLE_LABELS.getOrDefault(tableName, tableName);
                String dataType = getColumnDataType(tableName, columnName);

                results.add(ColumnSimilarityResult.builder()
                        .columnName(columnName)
                        .columnLabel(columnLabel)
                        .tableName(tableName)
                        .tableLabel(tableLabel)
                        .similarity(similarity)
                        .dataType(dataType)
                        .build());

                if (similarity > 0.5) {
                    matchedColumns++;
                    log.info("[调试] 关键词匹配 - 相似度 {}: {}/{} vs '{}'",
                        similarity, tableLabel, columnLabel, query);
                }
            }
        }

        results.sort((a, b) -> Double.compare(b.getSimilarity(), a.getSimilarity()));
        List<ColumnSimilarityResult> limitedResults = results.stream().limit(limit).collect(Collectors.toList());

        log.info("[调试] 关键词匹配完成，总候选列: {}，相似度>0.5的: {}，返回前 {} 条",
            totalColumns, matchedColumns, limitedResults.size());

        // 打印返回结果
        if (!limitedResults.isEmpty()) {
            log.info("[调试] 返回结果详情:");
            for (int i = 0; i < Math.min(3, limitedResults.size()); i++) {
                ColumnSimilarityResult r = limitedResults.get(i);
                log.info("[调试]   {}. {}/{} 相似度={}", i + 1, r.getTableLabel(), r.getColumnLabel(), r.getSimilarity());
            }
        }

        return limitedResults;
    }

    /**
     * 计算相似度
     */
    private double calculateSimilarity(String query, String columnName, String columnLabel) {
        query = query.toLowerCase();
        columnName = columnName.toLowerCase();
        columnLabel = columnLabel.toLowerCase();

        // 1. 精确匹配（中文字段名）
        if (columnLabel.equals(query)) {
            return 1.0;
        }

        // 2. 字段名精确匹配
        if (columnName.equals(query)) {
            return 0.95;
        }

        // 3. 包含匹配
        if (columnLabel.contains(query) || query.contains(columnLabel)) {
            return 0.85;
        }

        if (columnName.contains(query) || query.contains(columnName)) {
            return 0.8;
        }

        // 4. 关键词匹配
        List<String> queryKeywords = extractKeywords(query);
        List<String> labelKeywords = extractKeywords(columnLabel);
        List<String> nameKeywords = extractKeywords(columnName);

        double maxSimilarity = 0;
        for (String qk : queryKeywords) {
            for (String lk : labelKeywords) {
                double sim = keywordSimilarity(qk, lk);
                maxSimilarity = Math.max(maxSimilarity, sim);
            }
            for (String nk : nameKeywords) {
                double sim = keywordSimilarity(qk, nk);
                maxSimilarity = Math.max(maxSimilarity, sim * 0.9); // 字段名权重稍低
            }
        }

        // 5. 语义相似度检测（基于关键词重叠）
        Set<String> querySet = new HashSet<>(queryKeywords);
        Set<String> labelSet = new HashSet<>(labelKeywords);
        Set<String> nameSet = new HashSet<>(nameKeywords);

        Set<String> intersection = new HashSet<>(querySet);
        intersection.retainAll(labelSet);
        double labelOverlap = querySet.isEmpty() ? 0 : (double) intersection.size() / querySet.size();

        intersection = new HashSet<>(querySet);
        intersection.retainAll(nameSet);
        double nameOverlap = querySet.isEmpty() ? 0 : (double) intersection.size() / querySet.size();

        double overlapSimilarity = Math.max(labelOverlap, nameOverlap * 0.9);

        // 增强相似度计算，确保更多相关字段被匹配
        return Math.max(maxSimilarity, overlapSimilarity * 0.95);
    }

    /**
     * 提取关键词
     */
    private List<String> extractKeywords(String text) {
        List<String> keywords = new ArrayList<>();
        // 按常见分隔符分割
        String[] parts = text.split("[\\s_\\-\\.\\(\\)\\uff08\\uff09]+");
        for (String part : parts) {
            if (part.length() >= 2) {
                keywords.add(part);
            }
        }
        return keywords;
    }

    /**
     * 关键词相似度（编辑距离基础）
     */
    private double keywordSimilarity(String s1, String s2) {
        if (s1.equals(s2)) return 1.0;
        if (s1.isEmpty() || s2.isEmpty()) return 0;

        int len1 = s1.length();
        int len2 = s2.length();
        int maxLen = Math.max(len1, len2);

        // 快速检查：包含关系
        if (s1.contains(s2) || s2.contains(s1)) {
            return (double) Math.min(len1, len2) / maxLen;
        }

        // 编辑距离
        int distance = levenshteinDistance(s1, s2);
        return 1.0 - (double) distance / maxLen;
    }

    /**
     * 编辑距离
     */
    private int levenshteinDistance(String s1, String s2) {
        int len1 = s1.length();
        int len2 = s2.length();
        int[][] dp = new int[len1 + 1][len2 + 1];

        for (int i = 0; i <= len1; i++) dp[i][0] = i;
        for (int j = 0; j <= len2; j++) dp[0][j] = j;

        for (int i = 1; i <= len1; i++) {
            for (int j = 1; j <= len2; j++) {
                int cost = s1.charAt(i - 1) == s2.charAt(j - 1) ? 0 : 1;
                dp[i][j] = Math.min(Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1), dp[i - 1][j - 1] + cost);
            }
        }
        return dp[len1][len2];
    }

    /**
     * 获取字段数据类型
     */
    private String getColumnDataType(String tableName, String columnName) {
        try {
            String sql = "SELECT DATA_TYPE FROM information_schema.COLUMNS " +
                    "WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?";
            List<String> types = jdbcTemplate.queryForList(sql, String.class, tableName, columnName);
            if (!types.isEmpty()) {
                return types.get(0);
            }
        } catch (Exception e) {
            log.debug("获取字段类型失败: {}.{}", tableName, columnName);
        }
        return "varchar";
    }

    /**
     * 从 API 分析结果中提取建议的字段
     * @param apiResult API 分析结果
     * @return 建议的字段列表
     */
    public List<ColumnSimilarityResult> extractFromApiResult(IndicatorAnalysisResult apiResult) {
        List<ColumnSimilarityResult> suggestions = new ArrayList<>();

        if (apiResult == null || apiResult.getSourceDataList() == null) {
            return suggestions;
        }

        for (SourceDataDTO sourceData : apiResult.getSourceDataList()) {
            String dataName = sourceData.getSourceDataName();
            String measurementMethod = sourceData.getMeasurementMethod();
            String dataType = sourceData.getDataType();

            // 搜索相似字段
            List<ColumnSimilarityResult> similarColumns = searchSimilarColumns(dataName, 3);

            // 如果找到相似字段，使用相似度最高的
            if (!similarColumns.isEmpty()) {
                ColumnSimilarityResult best = similarColumns.get(0);
                best.setSimilarity(0.95); // API 建议默认高置信度
                suggestions.add(best);
            } else {
                // 没有找到匹配，创建占位建议
                suggestions.add(ColumnSimilarityResult.builder()
                        .columnName(dataName)
                        .columnLabel(dataName)
                        .tableName("待确定")
                        .tableLabel("待确定")
                        .similarity(0.8)
                        .dataType(dataType != null ? dataType : "NUMERIC")
                        .build());
            }
        }

        return suggestions;
    }
}
