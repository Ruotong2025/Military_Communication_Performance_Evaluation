package com.ccnu.military.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;

/**
 * 模拟数据生成请求
 */
@Data
public class GenerateExpertsRequest {

    /**
     * 生成数量
     */
    private Integer count = 10;

    /**
     * 姓名列表（可选，不填则自动生成）
     */
    private List<String> names;

    /**
     * 生成模式: append(新增) / overwrite(覆盖)
     * - append: 保留已有数据，在末尾追加新数据
     * - overwrite: 清空所有现有数据，重新生成
     * 默认: append
     */
    private String generateMode = "append";

    /**
     * 分布模式: 控制生成的专家等级分布
     * - excellent: 优秀偏多 (A级40%, B级35%, C级20%, D级5%)
     * - balanced: 均衡分布 (A级20%, B级30%, C级35%, D级15%)
     * - ordinary: 普通偏多 (A级5%, B级20%, C级40%, D级35%)
     * 默认: balanced
     */
    private String distributionMode = "balanced";

    /**
     * 职称分布
     * key: 职称等级 (1-初级, 2-中级, 3-副高, 4-正高)
     * value: 百分比 (0-100)
     * 例如: {"4":30, "3":40, "2":25, "1":5}
     */
    private Map<String, Integer> titleDistribution;

    /**
     * 学历分布
     * key: 学历等级 (1-本科, 2-硕士, 3-博士)
     * value: 百分比 (0-100)
     * 例如: {"3":45, "2":40, "1":15}
     */
    private Map<String, Integer> educationDistribution;

    /**
     * 职务分布
     * key: 职务等级 (1-一般, 2-中层, 3-高层)
     * value: 百分比 (0-100)
     */
    private Map<String, Integer> positionDistribution;

    /**
     * 学校层次分布
     * key: 学校层次 (985, 211, 普通)
     * value: 百分比 (0-100)
     */
    private Map<String, Integer> schoolLevelDistribution;

    /**
     * 生成后是否自动评估
     * 默认: true
     */
    private Boolean autoEvaluate = true;

    /**
     * 离散程度 0~1：越大则论文数、项目数、考核分等随机区间越宽，专家间差异更明显；越小则更集中
     */
    private Double dispersion = 0.65;
}
