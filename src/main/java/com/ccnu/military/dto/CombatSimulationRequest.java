package com.ccnu.military.dto;

import lombok.Data;

import java.util.Map;

/**
 * 作战模拟数据生成请求（与前端表单一致）
 */
@Data
public class CombatSimulationRequest {

    /** 作战条数 1~100 */
    private Integer count = 10;

    /** 优秀程度：high / medium / low */
    private String excellentLevel = "medium";

    /** 离散程度：high / medium / low */
    private String dispersionLevel = "medium";

    /** 随机种子，可空 */
    private String seed;

    /** 写入模式：overwrite（先清空再写入）/ append（追加），默认 overwrite */
    private String mode = "overwrite";

    /**
     * 字段范围覆盖
     * 格式：字段名或 表名.列名 -> {"min": number, "max": number}
     * 例如：{"records_military_operation_info.isolated_node_count": {"min": 1, "max": 4}}
     */
    private Map<String, OverrideRange> overrides;

    /**
     * 枚举列固定值：表名.列名 或 列名 -> 枚举字符串（与 Python 脚本选项一致）
     */
    private Map<String, String> enumOverrides;
}
