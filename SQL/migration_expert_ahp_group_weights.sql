-- ============================================================
-- 迁移脚本：expert_ahp_group_weights 表重构
-- 从硬编码列模式迁移到 JSON 存储模式
-- 参照 expert_ahp_individual_weights 表的设计
-- ============================================================
-- 执行前请备份数据库！

-- 1. 备份旧表（以防万一）
CREATE TABLE IF NOT EXISTS expert_ahp_group_weights_backup AS SELECT * FROM expert_ahp_group_weights;

-- 2. 删除旧表
DROP TABLE IF EXISTS expert_ahp_group_weights;

-- 3. 创建新表（JSON 存储模式）
CREATE TABLE `expert_ahp_group_weights` (
  `id` bigint NOT NULL AUTO_INCREMENT,

  -- 专家组基本信息
  `group_id` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家组ID（UUID）',
  `expert_ids` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '专家ID集合（排序后逗号分隔），唯一标识',
  `expert_count` int DEFAULT NULL COMMENT '专家数量',
  `group_name` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '专家组名称',
  `updated_at` datetime DEFAULT NULL,

  -- 域间一级（效能 vs 装备）
  `cross_domain_score` decimal(10,6) DEFAULT NULL COMMENT '域间 Saaty 标度 a（效能相对装备）',
  `cross_domain_confidence` decimal(3,2) DEFAULT NULL COMMENT '域间把握度',
  `eff_domain_weight` decimal(10,6) DEFAULT NULL COMMENT '域间：效能体系全局权重 w_eff=a/(1+a)',
  `eq_domain_weight` decimal(10,6) DEFAULT NULL COMMENT '域间：装备体系全局权重 w_eq=1/(1+a)',

  -- 效能维度层
  `eff_dim_weights_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '效能维度层权重 JSON {dimName: weight}',
  `eff_dim_count` int DEFAULT NULL COMMENT '效能维度数量',

  -- 效能叶子指标
  `eff_leaf_weights_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '效能叶子指标全局权重 JSON Array of {dim,indicator,globalWeight}',
  `eff_leaf_count` int DEFAULT NULL COMMENT '效能叶子指标数量',
  `eff_cr` decimal(8,6) DEFAULT NULL COMMENT '效能维度层 CR',

  -- 装备维度层
  `eq_dim_weights_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '装备维度层权重 JSON {dimName: weight}',
  `eq_dim_count` int DEFAULT NULL COMMENT '装备维度数量',

  -- 装备叶子指标
  `eq_leaf_weights_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '装备叶子指标全局权重 JSON Array of {dim,indicator,globalWeight}',
  `eq_leaf_count` int DEFAULT NULL COMMENT '装备叶子指标数量',
  `eq_cr_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '装备各层 CR JSON',

  -- 专家权重明细
  `expert_weights_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '专家权重明细 JSON（可信度信息）',

  -- 完整快照
  `aggregated_unified_json` longtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL COMMENT '集结后完整统一快照 JSON（参照 AhpIndividualResult）',

  `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expert_ids` (`expert_ids`)
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '专家集结 AHP 统一层次权重快照（效能+装备完整展开）' ROW_FORMAT = Dynamic;

-- 4. 验证
-- SELECT COUNT(*) FROM expert_ahp_group_weights;
-- SELECT COUNT(*) FROM expert_ahp_group_weights_backup;