-- 专家 AHP 原始两两比较打分（与 expert_base_info.expert_id 对应）
-- 在库 military_operational_effectiveness_evaluation 中执行

CREATE TABLE IF NOT EXISTS `expert_ahp_comparison_score` (
  `id` int NOT NULL AUTO_INCREMENT,
  `expert_id` bigint NOT NULL COMMENT '专家id（关联 expert_base_info）',
  `expert_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '专家名称',
  `comparison_key` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '比较对标识，如:安全性_可靠性',
  `score` decimal(5, 2) NOT NULL COMMENT '打分值(1-9标度)',
  `confidence` decimal(3, 2) NULL DEFAULT 0.80 COMMENT '把握度(0-1)',
  `remark` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_expert_id` (`expert_id` ASC) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '专家AHP原始打分表';
