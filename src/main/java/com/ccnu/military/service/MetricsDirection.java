package com.ccnu.military.service;

import java.util.*;

/**
 * 军事通信效能指标方向配置
 * true = 正向指标（越大越好），直接归一化
 * false = 负向指标（越小越好），翻转后归一化
 */
public class MetricsDirection {

    /**
     * 原始指标字段名（metrics_military_comm_effect 表中的字段，18个）
     */
    private static final List<String> METRICS_FIELDS;

    /**
     * score 表字段名（score_military_comm_effect 表中的指标列，18个）
     * 与 metrics 表原始字段一一对应
     */
    private static final List<String> SCORE_FIELDS;

    /**
     * 指标方向映射（key = 原始字段名）
     */
    private static final Map<String, Boolean> DIRECTION_MAP;

    /**
     * 原始字段 → score 表字段的映射
     */
    private static final Map<String, String> FIELD_TO_SCORE_COLUMN;

    static {
        DIRECTION_MAP = new LinkedHashMap<>();

        // 安全性（3项）
        DIRECTION_MAP.put("security_key_leakage_count", false);
        DIRECTION_MAP.put("security_detected_probability", false);
        DIRECTION_MAP.put("security_interception_resistance_probability", true);

        // 可靠性（3项）
        DIRECTION_MAP.put("reliability_crash_count", false);
        DIRECTION_MAP.put("reliability_recovery_time", false);
        DIRECTION_MAP.put("reliability_communication_availability", true);

        // 传输（6项）
        DIRECTION_MAP.put("transmission_bandwidth", true);
        DIRECTION_MAP.put("transmission_call_setup_time", false);
        DIRECTION_MAP.put("transmission_delay", false);
        DIRECTION_MAP.put("transmission_bit_error_rate", false);
        DIRECTION_MAP.put("transmission_throughput", true);
        DIRECTION_MAP.put("transmission_spectral_efficiency", true);

        // 抗干扰（3项）
        DIRECTION_MAP.put("anti_jamming_sinr", true);
        DIRECTION_MAP.put("anti_jamming_margin", true);
        DIRECTION_MAP.put("anti_jamming_communication_distance", true);

        // 效能（3项）
        DIRECTION_MAP.put("effect_damage_rate", false);
        DIRECTION_MAP.put("effect_mission_completion_rate", true);
        DIRECTION_MAP.put("effect_blind_rate", false);

        // 按 score 表实际列顺序构建字段列表
        METRICS_FIELDS = new ArrayList<>(DIRECTION_MAP.keySet());

        // score 表实际字段名（来自 DESCRIBE score_military_comm_effect）
        SCORE_FIELDS = new ArrayList<>();
        for (String field : METRICS_FIELDS) {
            SCORE_FIELDS.add(toScoreColumn(field));
        }

        // 构建原始字段 → score 列名的映射
        FIELD_TO_SCORE_COLUMN = new LinkedHashMap<>();
        for (int i = 0; i < METRICS_FIELDS.size(); i++) {
            FIELD_TO_SCORE_COLUMN.put(METRICS_FIELDS.get(i), SCORE_FIELDS.get(i));
        }
    }

    /**
     * 将原始指标字段名转为 score 表列名
     */
    private static String toScoreColumn(String field) {
        switch (field) {
            case "security_key_leakage_count":                  return "security_key_leakage_qt";
            case "security_detected_probability":                return "security_detected_probability_qt";
            case "security_interception_resistance_probability":  return "security_interception_resistance_ql";
            case "reliability_crash_count":                      return "reliability_crash_rate_qt";
            case "reliability_recovery_time":                    return "reliability_recovery_capability_qt";
            case "reliability_communication_availability":       return "reliability_communication_availability_qt";
            case "transmission_bandwidth":                      return "transmission_bandwidth_qt";
            case "transmission_call_setup_time":                 return "transmission_call_setup_time_qt";
            case "transmission_delay":                           return "transmission_transmission_delay_qt";
            case "transmission_bit_error_rate":                  return "transmission_bit_error_rate_qt";
            case "transmission_throughput":                      return "transmission_throughput_qt";
            case "transmission_spectral_efficiency":             return "transmission_spectral_efficiency_qt";
            case "anti_jamming_sinr":                            return "anti_jamming_sinr_qt";
            case "anti_jamming_margin":                          return "anti_jamming_anti_jamming_margin_qt";
            case "anti_jamming_communication_distance":          return "anti_jamming_communication_distance_qt";
            case "effect_damage_rate":                           return "effect_damage_rate_qt";
            case "effect_mission_completion_rate":               return "effect_mission_completion_rate_qt";
            case "effect_blind_rate":                            return "effect_blind_rate_qt";
            default:                                             return field;
        }
    }

    /**
     * 获取原始指标字段列表（18个）
     */
    public static List<String> getAllFields() {
        return new ArrayList<>(METRICS_FIELDS);
    }

    /**
     * 获取 score 表字段列表（18个，与 score 表实际列对应）
     */
    public static List<String> getScoreFields() {
        return new ArrayList<>(SCORE_FIELDS);
    }

    /**
     * 获取原始字段 → score 列名的映射
     */
    public static Map<String, String> getFieldToScoreColumnMap() {
        return new LinkedHashMap<>(FIELD_TO_SCORE_COLUMN);
    }

    /**
     * 判断是否为正向指标（true=越大越好，false=越小越好）
     */
    public static boolean isPositive(String field) {
        Boolean result = DIRECTION_MAP.get(field);
        return result != null && result;
    }

    /**
     * 判断是否为负向指标
     */
    public static boolean isNegative(String field) {
        Boolean result = DIRECTION_MAP.get(field);
        return result == null || !result;
    }

    /**
     * 将原始值归一化为 score（0~1，越大越好）
     */
    public static Double normalize(Double value, Double min, Double max, String field) {
        if (value == null) {
            return null;
        }
        if (min == null || max == null || max.equals(min)) {
            return 1.0;
        }
        if (isNegative(field)) {
            return (max - value) / (max - min);
        } else {
            return (value - min) / (max - min);
        }
    }

    /**
     * 获取指标中文名称映射
     */
    public static Map<String, String> getFieldNameMap() {
        Map<String, String> names = new LinkedHashMap<>();
        names.put("security_key_leakage_count", "密钥泄露次数");
        names.put("security_detected_probability", "被侦察概率(%)");
        names.put("security_interception_resistance_probability", "抗拦截能力(%)");
        names.put("reliability_crash_count", "网络崩溃次数");
        names.put("reliability_recovery_time", "恢复时间(ms)");
        names.put("reliability_communication_availability", "通信可用性(%)");
        names.put("transmission_bandwidth", "带宽(Mbps)");
        names.put("transmission_call_setup_time", "呼叫建立(ms)");
        names.put("transmission_delay", "传输时延(ms)");
        names.put("transmission_bit_error_rate", "误码率(%)");
        names.put("transmission_throughput", "吞吐量(Mbps)");
        names.put("transmission_spectral_efficiency", "频谱效率");
        names.put("anti_jamming_sinr", "信干噪比(dB)");
        names.put("anti_jamming_margin", "抗干扰余量(dB)");
        names.put("anti_jamming_communication_distance", "通信距离(km)");
        names.put("effect_damage_rate", "毁伤率(%)");
        names.put("effect_mission_completion_rate", "任务完成率(%)");
        names.put("effect_blind_rate", "盲区率(%)");
        return names;
    }

    /**
     * 获取指标一级分类映射
     */
    public static Map<String, String> getFieldCategoryMap() {
        Map<String, String> categories = new LinkedHashMap<>();
        categories.put("security_key_leakage_count", "安全性");
        categories.put("security_detected_probability", "安全性");
        categories.put("security_interception_resistance_probability", "安全性");
        categories.put("reliability_crash_count", "可靠性");
        categories.put("reliability_recovery_time", "可靠性");
        categories.put("reliability_communication_availability", "可靠性");
        categories.put("transmission_bandwidth", "传输");
        categories.put("transmission_call_setup_time", "传输");
        categories.put("transmission_delay", "传输");
        categories.put("transmission_bit_error_rate", "传输");
        categories.put("transmission_throughput", "传输");
        categories.put("transmission_spectral_efficiency", "传输");
        categories.put("anti_jamming_sinr", "抗干扰");
        categories.put("anti_jamming_margin", "抗干扰");
        categories.put("anti_jamming_communication_distance", "抗干扰");
        categories.put("effect_damage_rate", "效能");
        categories.put("effect_mission_completion_rate", "效能");
        categories.put("effect_blind_rate", "效能");
        return categories;
    }
}
