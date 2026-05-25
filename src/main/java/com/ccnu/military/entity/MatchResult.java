package com.ccnu.military.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 指标匹配结果
 * 用于封装三级匹配流程的结果
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResult {

    /**
     * 匹配类型
     */
    private MatchType type;

    /**
     * 精确匹配的指标（第一级命中时）
     */
    private IndicatorDefinition exactMatch;

    /**
     * 语义相似的候选列表（第二级命中时）
     */
    private List<SimilarityCandidate> candidates;

    /**
     * 匹配类型枚举
     */
    public enum MatchType {
        /**
         * 精确匹配命中
         */
        EXACT,

        /**
         * 语义相似匹配
         */
        SIMILAR,

        /**
         * 无匹配结果
         */
        NO_MATCH
    }

    /**
     * 相似候选结果
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SimilarityCandidate {
        /**
         * 指标ID
         */
        private Long id;

        /**
         * 指标名称
         */
        private String name;

        /**
         * 相似度分数
         */
        private Double similarity;
    }

    /**
     * 创建精确匹配结果
     */
    public static MatchResult exact(IndicatorDefinition indicator) {
        return MatchResult.builder()
                .type(MatchType.EXACT)
                .exactMatch(indicator)
                .build();
    }

    /**
     * 创建语义相似匹配结果
     */
    public static MatchResult similar(List<SimilarityCandidate> candidates) {
        return MatchResult.builder()
                .type(MatchType.SIMILAR)
                .candidates(candidates)
                .build();
    }

    /**
     * 创建无匹配结果
     */
    public static MatchResult noMatch() {
        return MatchResult.builder()
                .type(MatchType.NO_MATCH)
                .build();
    }

    /**
     * 判断是否有精确匹配
     */
    public boolean isExactMatch() {
        return type == MatchType.EXACT && exactMatch != null;
    }

    /**
     * 判断是否有相似候选
     */
    public boolean hasSimilarCandidates() {
        return type == MatchType.SIMILAR && candidates != null && !candidates.isEmpty();
    }

    /**
     * 判断是否无匹配
     */
    public boolean isNoMatch() {
        return type == MatchType.NO_MATCH;
    }
}
