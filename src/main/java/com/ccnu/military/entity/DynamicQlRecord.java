package com.ccnu.military.entity;

import lombok.Data;
import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 动态定性评估记录实体
 * 存储：专家对各作战在各定性指标上的评分
 */
@Data
@Entity
@Table(name = "dynamic_ql_record",
       uniqueConstraints = @UniqueConstraint(columnNames = {"batch_id", "expert_id", "operation_id", "secondary_code"}))
public class DynamicQlRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // ==================== 批次与模板 ====================
    @Column(name = "batch_id", nullable = false)
    private String batchId;

    @Column(name = "template_id")
    private Long templateId;

    // ==================== 专家信息 ====================
    @Column(name = "expert_id", nullable = false)
    private Long expertId;

    @Column(name = "expert_name")
    private String expertName;

    @Column(name = "expert_credibility", precision = 5, scale = 2)
    private BigDecimal expertCredibility;

    // ==================== 作战信息 ====================
    @Column(name = "operation_id", nullable = false)
    private String operationId;

    // ==================== 指标信息 ====================
    @Column(name = "secondary_code", nullable = false)
    private String secondaryCode;

    @Column(name = "secondary_name")
    private String secondaryName;

    @Column(name = "level_name")
    private String levelName;

    @Column(name = "primary_dimension")
    private String primaryDimension;

    // ==================== 评分数据（15级评分） ====================
    @Column(name = "score_level", nullable = false, length = 8)
    private String scoreLevel; // 直接存字符串: A+, A, A-, B+, B...

    @Column(name = "score_value", precision = 10, scale = 6)
    private BigDecimal scoreValue;

    // 把握度（0-100）
    @Column(name = "confidence_value", precision = 5, scale = 2)
    private BigDecimal confidenceValue;

    // ==================== 备注与时间 ====================
    @Column(name = "evaluation_remark", columnDefinition = "TEXT")
    private String evaluationRemark;

    @Column(name = "evaluated_at")
    private LocalDateTime evaluatedAt;

    // ==================== 时间戳 ====================
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // ==================== 生命周期回调 ====================

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (evaluatedAt == null) {
            evaluatedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // ==================== 静态工具方法 ====================

    /**
     * 评分等级（15级）
     */
    public static final class ScoreLevel {
        public static final String AP = "A+";
        public static final String A = "A";
        public static final String AM = "A-";
        public static final String BP = "B+";
        public static final String B = "B";
        public static final String BM = "B-";
        public static final String CP = "C+";
        public static final String C = "C";
        public static final String CM = "C-";
        public static final String DP = "D+";
        public static final String D = "D";
        public static final String DM = "D-";
        public static final String EP = "E+";
        public static final String E = "E";
        public static final String EM = "E-";

        // 等级分值映射
        private static final Map<String, Double> VALUE_MAP = new LinkedHashMap<>();
        private static final Map<String, String> NAME_MAP = new LinkedHashMap<>();
        private static final Map<String, String> CATEGORY_MAP = new LinkedHashMap<>();

        static {
            VALUE_MAP.put(AP, 97.5); NAME_MAP.put(AP, "优+"); CATEGORY_MAP.put(AP, "优");
            VALUE_MAP.put(A, 92.5); NAME_MAP.put(A, "优"); CATEGORY_MAP.put(A, "优");
            VALUE_MAP.put(AM, 87.5); NAME_MAP.put(AM, "优-"); CATEGORY_MAP.put(AM, "优");
            VALUE_MAP.put(BP, 82.5); NAME_MAP.put(BP, "良+"); CATEGORY_MAP.put(BP, "良");
            VALUE_MAP.put(B, 77.5); NAME_MAP.put(B, "良"); CATEGORY_MAP.put(B, "良");
            VALUE_MAP.put(BM, 72.5); NAME_MAP.put(BM, "良-"); CATEGORY_MAP.put(BM, "良");
            VALUE_MAP.put(CP, 67.5); NAME_MAP.put(CP, "合格+"); CATEGORY_MAP.put(CP, "合格");
            VALUE_MAP.put(C, 62.5); NAME_MAP.put(C, "合格"); CATEGORY_MAP.put(C, "合格");
            VALUE_MAP.put(CM, 57.5); NAME_MAP.put(CM, "合格-"); CATEGORY_MAP.put(CM, "合格");
            VALUE_MAP.put(DP, 51.0); NAME_MAP.put(DP, "差+"); CATEGORY_MAP.put(DP, "差");
            VALUE_MAP.put(D, 43.0); NAME_MAP.put(D, "差"); CATEGORY_MAP.put(D, "差");
            VALUE_MAP.put(DM, 34.5); NAME_MAP.put(DM, "差-"); CATEGORY_MAP.put(DM, "差");
            VALUE_MAP.put(EP, 25.0); NAME_MAP.put(EP, "极差+"); CATEGORY_MAP.put(EP, "极差");
            VALUE_MAP.put(E, 15.0); NAME_MAP.put(E, "极差"); CATEGORY_MAP.put(E, "极差");
            VALUE_MAP.put(EM, 5.0); NAME_MAP.put(EM, "极差-"); CATEGORY_MAP.put(EM, "极差");
        }

        public static double getValue(String code) {
            return VALUE_MAP.getOrDefault(code, 50.0);
        }

        public static String getName(String code) {
            return NAME_MAP.getOrDefault(code, code);
        }

        public static String getCategory(String code) {
            return CATEGORY_MAP.getOrDefault(code, "未分类");
        }

        public static List<String> getAllCodes() {
            return new ArrayList<>(VALUE_MAP.keySet());
        }
    }
}
