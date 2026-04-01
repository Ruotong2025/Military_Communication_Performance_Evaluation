-- ============================================================
-- 惩罚模型计算结果表（存储每个作战任务的惩罚计算结果）
-- 数据库: military_operational_effectiveness_evaluation
-- 依赖: expert_weighted_evaluation_result / score_military_comm_effect
-- 说明: 惩罚模型参数在前端代码中默认写死，不存储到数据库
-- ============================================================

-- ----------------------------
-- 表：penalty_model_result（惩罚模型计算结果）
-- 存储每个作战任务经过惩罚计算后的最终得分
-- ----------------------------
DROP TABLE IF EXISTS `penalty_model_result`;
CREATE TABLE `penalty_model_result` (
  `id`                    bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id`         varchar(50)      NOT NULL                COMMENT '评估批次ID',
  `operation_id`          varchar(50)      NOT NULL                COMMENT '作战任务ID',

  -- 原始集结综合得分（惩罚前）
  `original_score`       decimal(10, 6)   NOT NULL                COMMENT '集结综合得分（原始，Sstage）',

  -- 综合惩罚因子
  `overall_penalty`       decimal(8, 6)   NOT NULL                COMMENT '综合惩罚因子 P = min(F1..Fn)',

  -- 最终效能得分（惩罚后）
  `final_score`           decimal(10, 6)   NOT NULL                COMMENT '最终效能得分 Sfinal = Sstage × P',

  -- 惩罚幅度
  `penalty_amplitude`     decimal(8, 6)   NOT NULL                COMMENT '惩罚幅度 (Sstage - Sfinal) / Sstage',

  -- 各指标惩罚详情（JSON 数组），元素示例：
  --   [{ "indicator_key": "reliability_crash_rate_qt",
  --      "indicator_score": 45.2,          -- 该作战该指标得分（百分制）
  --      "threshold": 70,
  --      "m": 0.8,
  --      "fi": 0.5171 },
  --    { "indicator_key": "effect_mission_completion_rate_qt",
  --      "indicator_score": 72.0,
  --      "threshold": 70,
  --      "m": 0.8,
  --      "fi": 1.0000 }]
  `penalty_details`       json             NOT NULL                COMMENT '各指标惩罚因子详情（JSON）',

  -- 批次统计口径（便于前端回显"配置区"，不参与核心计算）
  `batch_avg_score_map`   json             DEFAULT NULL           COMMENT '各指标批次算术平均分 { indicator_key: avg_score（0~1） }',
  `batch_min_score_map`   json             DEFAULT NULL           COMMENT '各指标批次最低分   { indicator_key: min_score（0~1） }',
  `batch_avg_fi_map`      json             DEFAULT NULL           COMMENT '各指标 Fi（均值口径）{ indicator_key: fi }',
  `batch_min_fi_map`      json             DEFAULT NULL           COMMENT '各指标 Fi（批次最低分口径）{ indicator_key: fi }',

  `created_at`            timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`            timestamp        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_evaluation_operation` (`evaluation_id` ASC, `operation_id` ASC),
  INDEX `idx_evaluation_id`  (`evaluation_id` ASC),
  INDEX `idx_operation_id`    (`operation_id` ASC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '惩罚模型计算结果表（每作战一条记录）' ROW_FORMAT = DYNAMIC;
