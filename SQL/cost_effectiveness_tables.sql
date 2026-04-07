-- ============================================================
-- 效费分析成本评估通用表设计
-- 数据库: military_operational_effectiveness_evaluation
-- 采用"指标配置 + 评估记录"分离设计，支持动态扩展指标
-- ============================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- 表1：cost_indicator_config（成本指标配置表）
-- 定义成本指标的类型、计算方式、归一化参数等元数据
-- ----------------------------
DROP TABLE IF EXISTS `cost_indicator_config`;
CREATE TABLE `cost_indicator_config` (
  `id`                bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `indicator_key`     varchar(80)    NOT NULL                COMMENT '指标唯一标识（如 cost_personnel_total）',
  `indicator_name`    varchar(200)   NOT NULL                COMMENT '指标中文名称',
  `indicator_name_en` varchar(200)   DEFAULT NULL            COMMENT '指标英文名称（可选）',
  `category`          varchar(50)    NOT NULL                COMMENT '指标大类（人力/装备/能源/备件物流/训练/基础设施）',
  `sub_category`      varchar(50)   DEFAULT NULL            COMMENT '指标子类（可选）',

  -- 指标类型与来源配置
  `indicator_type`    enum('cost','benefit') NOT NULL         COMMENT '指标类型：cost=成本型(越大成本越高), benefit=效益型(越大成本越低)',
  `source_table`      varchar(100)   DEFAULT NULL            COMMENT '数据来源表名（NULL表示手动输入）',
  `source_field`      varchar(100)   DEFAULT NULL            COMMENT '数据来源字段名',
  `source_expression` varchar(500)   DEFAULT NULL            COMMENT '数据来源表达式（复杂计算时使用，如多个字段求和）',

  -- 归一化参数
  `normalization_min` decimal(15, 4) DEFAULT NULL            COMMENT '归一化下限ck,min',
  `normalization_max` decimal(15, 4) DEFAULT NULL            COMMENT '归一化上限ck,max',
  `use_actual_range`  tinyint(1)     DEFAULT 1               COMMENT '是否使用实际数据范围作为归一化边界（1=是，0=使用固定值）',

  -- 权重参数
  `weight`            decimal(8, 6)  DEFAULT NULL            COMMENT '该指标在成本指数中的权重（0~1）',
  `is_active`         tinyint(1)     DEFAULT 1               COMMENT '是否启用（1=启用，0=禁用）',
  `sort_order`        int            DEFAULT 0               COMMENT '排序序号',

  -- 计量单位
  `unit`              varchar(30)    DEFAULT NULL            COMMENT '计量单位',
  `unit_abbrev`       varchar(10)    DEFAULT NULL            COMMENT '单位简称',

  -- 描述与备注
  `description`       text           DEFAULT NULL            COMMENT '指标说明',
  `remarks`           text           DEFAULT NULL            COMMENT '备注',

  `created_at`        timestamp      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_indicator_key` (`indicator_key` ASC),
  INDEX `idx_category` (`category` ASC),
  INDEX `idx_is_active` (`is_active` ASC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 
  COMMENT = '成本指标配置表（定义指标元数据，支持动态扩展）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 表2：cost_evaluation_record（成本评估记录表）
-- 存储每次评估的成本数据，按指标键值对存储
-- ----------------------------
DROP TABLE IF EXISTS `cost_evaluation_record`;
CREATE TABLE `cost_evaluation_record` (
  `id`                bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id`     varchar(50)    NOT NULL                COMMENT '评估批次ID',
  `operation_id`      varchar(50)    NOT NULL                COMMENT '作战任务ID',

  -- 成本指数计算结果
  `cost_index`        decimal(8, 6)  DEFAULT NULL            COMMENT '综合成本指数 C（归一化加权求和后，0~1）',
  `cost_index_raw`    decimal(15, 4) DEFAULT NULL            COMMENT '原始成本指数（加权求和前，未归一化）',

  -- 效费比计算结果
  `effectiveness_score` decimal(10, 6) DEFAULT NULL          COMMENT '效能得分 E（来自惩罚模型最终得分）',
  `cost_effectiveness_ratio` decimal(12, 6) DEFAULT NULL     COMMENT '效费比 R = E/C',

  -- 成本分类汇总（JSON格式存储各类成本小计）
  `cost_by_category`  json           DEFAULT NULL            COMMENT '各类成本汇总 { "人力": 0.15, "装备": 0.20, ... }',

  -- 原始指标数据（JSON格式存储所有指标原始值）
  `raw_indicators`    json           DEFAULT NULL            COMMENT '原始指标数据 { "cost_personnel_total": 12000, ... }',
  `normalized_indicators` json       DEFAULT NULL            COMMENT '归一化后指标数据 { "cost_personnel_total": 0.45, ... }',
  `effective_normalized` json       DEFAULT NULL            COMMENT '方向调整后的成本分量（效益型已反向，0~1）',

  -- 权重快照（存储计算时的权重配置）
  `weights_snapshot`  json           DEFAULT NULL            COMMENT '权重快照 { "cost_personnel_total": 0.15, ... }',

  -- 归一化边界快照
  `normalization_bounds` json         DEFAULT NULL            COMMENT '归一化边界快照 { min: {...}, max: {...} }',

  -- 备注
  `remarks`           text           DEFAULT NULL            COMMENT '备注',

  `created_at`        timestamp      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_eval_operation` (`evaluation_id` ASC, `operation_id` ASC),
  INDEX `idx_evaluation_id` (`evaluation_id` ASC),
  INDEX `idx_operation_id` (`operation_id` ASC),
  INDEX `idx_cost_index` (`cost_index` ASC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 
  COMMENT = '成本评估记录表（存储每次评估的成本数据）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 表3：cost_evaluation_batch（成本评估批次配置表）
-- 存储每次评估的全局配置
-- ----------------------------
DROP TABLE IF EXISTS `cost_evaluation_batch`;
CREATE TABLE `cost_evaluation_batch` (
  `id`                bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `evaluation_id`     varchar(50)    NOT NULL                COMMENT '评估批次ID',
  `batch_name`        varchar(200)   DEFAULT NULL            COMMENT '批次名称（可选）',
  `evaluation_time`   datetime       NOT NULL                COMMENT '评估时间',

  -- 全局配置
  `weight_method`     enum('equal','ahp','manual') DEFAULT 'equal' COMMENT '权重确定方法：等权/AHP/手动',
  `normalization_method` enum('minmax','zscore','manual') DEFAULT 'minmax' COMMENT '归一化方法',

  -- 使用的指标配置快照
  `active_indicators` json           DEFAULT NULL            COMMENT '启用的指标列表（JSON数组）',

  -- 参评作战ID列表快照（用于追溯每次评估的输入数据）
  `operation_ids`      json           DEFAULT NULL            COMMENT '本次评估参评的作战ID列表（JSON数组）',

  -- 全局归一化边界（用于跨批次比较）
  `global_min_values` json           DEFAULT NULL            COMMENT '全局最小值快照 { indicator_key: min_value }',
  `global_max_values` json           DEFAULT NULL            COMMENT '全局最大值快照 { indicator_key: max_value }',

  -- 状态与统计
  `status`            enum('pending','computing','completed','failed') DEFAULT 'pending' COMMENT '批次状态',
  `operation_count`   int            DEFAULT 0               COMMENT '该批次作战任务数量',
  `completed_count`   int            DEFAULT 0               COMMENT '已完成评估数量',

  -- ========== 汇总计算结果（写入批次表，便于追溯每次评估的最终输出）==========
  `avg_cost_index`     decimal(8, 6)  DEFAULT NULL            COMMENT '批次平均成本指数 C（所有作战均值，归一化到 0~1）',
  `avg_effectiveness_score` decimal(10, 6) DEFAULT NULL       COMMENT '批次平均效能得分 E',
  `avg_cost_effectiveness_ratio` decimal(12, 6) DEFAULT NULL  COMMENT '批次平均效费比 R = E/C',
  `min_cost_index`    decimal(8, 6)  DEFAULT NULL            COMMENT '成本指数最小值',
  `max_cost_index`    decimal(8, 6)  DEFAULT NULL            COMMENT '成本指数最大值',
  `min_cost_effectiveness_ratio` decimal(12, 6) DEFAULT NULL  COMMENT '效费比最小值',
  `max_cost_effectiveness_ratio` decimal(12, 6) DEFAULT NULL  COMMENT '效费比最大值',
  `category_summary`  json           DEFAULT NULL            COMMENT '各类成本汇总（批次整体各维度加权成本占比）',
  `weights_config`    json           DEFAULT NULL            COMMENT '本次评估使用的权重配置快照',

  -- 备注
  `remarks`           text           DEFAULT NULL            COMMENT '备注',

  `created_at`        timestamp      DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at`        timestamp      DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',

  PRIMARY KEY (`id`),
  UNIQUE INDEX `uk_evaluation_id` (`evaluation_id` ASC),
  INDEX `idx_evaluation_time` (`evaluation_time` ASC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 
  COMMENT = '成本评估批次配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- 初始化成本指标配置数据
-- 从 records_military_operation_info 表映射
-- ----------------------------
INSERT IGNORE INTO `cost_indicator_config`
  (`indicator_key`, `indicator_name`, `indicator_name_en`, `category`, `sub_category`,
   `indicator_type`, `source_table`, `source_field`, `source_expression`,
   `normalization_min`, `normalization_max`, `use_actual_range`,
   `weight`, `is_active`, `sort_order`,
   `unit`, `unit_abbrev`, `description`)
VALUES
-- 人力成本指标
('cost_personnel_command', '指挥人员数量', NULL, '人力成本', NULL, 'cost', 'records_military_operation_info', 'command_personnel_count', NULL, NULL, NULL, 1, 0.05, 1, 1, '人', NULL, '各级指挥员及参谋人员编制数量'),
('cost_personnel_operator', '操作人员数量', NULL, '人力成本', NULL, 'cost', 'records_military_operation_info', 'operator_personnel_count', NULL, NULL, NULL, 1, 0.05, 1, 2, '人', NULL, '直接操作主战装备的人员编制数量'),
('cost_personnel_maintenance', '维修人员数量', NULL, '人力成本', NULL, 'cost', 'records_military_operation_info', 'maintenance_personnel_count', NULL, NULL, NULL, 1, 0.04, 1, 3, '人', NULL, '从事装备维修保障的人员编制数量'),
('cost_personnel_experience', '操作人员平均工作经验', NULL, '人力成本', NULL, 'cost', 'records_military_operation_info', 'avg_experience_years', NULL, NULL, NULL, 1, 0.03, 1, 4, '年', NULL, '操作人员掌握主战装备的平均年限'),
('cost_personnel_maintenance_hours', '年度维修工时', NULL, '人力成本', NULL, 'cost', 'records_military_operation_info', 'annual_maintenance_hours', NULL, NULL, NULL, 1, 0.03, 1, 5, '小时', NULL, '战役级装备维修机构投入的年总维修工时'),
('cost_personnel_training', '人均培训频次', NULL, '人力成本', NULL, 'cost', 'records_military_operation_info', 'avg_training_frequency_per_year', NULL, NULL, NULL, 1, 0.03, 1, 6, '次/年', NULL, '人均参与重大演训活动的频次'),

-- 装备成本指标
('cost_equipment_total', '总装备数量', NULL, '装备成本', NULL, 'cost', 'records_military_operation_info', 'total_equipment_count', NULL, NULL, NULL, 1, 0.06, 1, 10, '辆/台/架', NULL, '各类主战装备实有总数'),
('cost_equipment_damaged', '受损装备数量', NULL, '装备成本', NULL, 'cost', 'records_military_operation_info', 'damaged_equipment_count', NULL, NULL, NULL, 1, 0.04, 1, 11, '辆/台/架', NULL, '未恢复的受损装备数量'),
('cost_equipment_new_ratio', '新装备占比', NULL, '装备成本', NULL, 'cost', 'records_military_operation_info', 'new_equipment_ratio', NULL, NULL, NULL, 1, 0.03, 1, 12, '%', NULL, '服役不足5年的装备占比'),

-- 能源成本指标
('cost_energy_power', '整体功耗', NULL, '能源成本', NULL, 'cost', 'records_military_operation_info', 'total_power_consumption_kw', NULL, NULL, NULL, 1, 0.04, 1, 20, 'kW', NULL, '通信装备整体功率消耗'),
('cost_energy_electricity', '年度耗电量', NULL, '能源成本', NULL, 'cost', 'records_military_operation_info', 'annual_electricity_consumption_kwh', NULL, NULL, NULL, 1, 0.04, 1, 21, 'kWh', NULL, '年度总耗电量'),
('cost_energy_fuel', '年度耗油量', NULL, '能源成本', NULL, 'cost', 'records_military_operation_info', 'annual_fuel_consumption_liters', NULL, NULL, NULL, 1, 0.04, 1, 22, '升', NULL, '年度总耗油量'),
('cost_energy_spectrum', '频谱储备', NULL, '能源成本', NULL, 'cost', 'records_military_operation_info', 'spectrum_reserve_mhz', NULL, NULL, NULL, 1, 0.02, 1, 23, 'MHz', NULL, '预留可用频段总带宽'),

-- 备件物流成本指标
('cost_spare_satisfaction', '备件满足率', NULL, '备件物流', NULL, 'cost', 'records_military_operation_info', 'spare_parts_satisfaction_rate', NULL, NULL, NULL, 1, 0.03, 1, 30, '%', NULL, '备件即时满足率'),
('cost_spare_transport', '日均投送吨公里', NULL, '备件物流', NULL, 'cost', 'records_military_operation_info', 'total_transport_distance_km', NULL, NULL, NULL, 1, 0.02, 1, 31, '吨·公里', NULL, '每日备件运输总工作量');

-- ============================================================
-- 增量迁移脚本（已有 cost_evaluation_record 表的环境执行）
-- 如果 cost_evaluation_record 已存在，执行以下 ALTER TABLE 追加新字段
-- ============================================================
-- ALTER TABLE `cost_evaluation_record`
--   ADD COLUMN `effective_normalized` json DEFAULT NULL COMMENT '方向调整后的成本分量（效益型已反向，0~1）' AFTER `normalized_indicators`;

-- ALTER TABLE `cost_evaluation_batch`
--   ADD COLUMN `operation_ids`      json           DEFAULT NULL COMMENT '本次评估参评的作战ID列表（JSON数组）' AFTER `active_indicators`,
--   ADD COLUMN `avg_cost_index`     decimal(8, 6)  DEFAULT NULL COMMENT '批次平均成本指数 C' AFTER `completed_count`,
--   ADD COLUMN `avg_effectiveness_score` decimal(10, 6) DEFAULT NULL COMMENT '批次平均效能得分 E' AFTER `avg_cost_index`,
--   ADD COLUMN `avg_cost_effectiveness_ratio` decimal(12, 6) DEFAULT NULL COMMENT '批次平均效费比 R' AFTER `avg_effectiveness_score`,
--   ADD COLUMN `min_cost_index`     decimal(8, 6)  DEFAULT NULL COMMENT '成本指数最小值' AFTER `avg_cost_effectiveness_ratio`,
--   ADD COLUMN `max_cost_index`     decimal(8, 6)  DEFAULT NULL COMMENT '成本指数最大值' AFTER `min_cost_index`,
--   ADD COLUMN `min_cost_effectiveness_ratio` decimal(12, 6) DEFAULT NULL COMMENT '效费比最小值' AFTER `max_cost_index`,
--   ADD COLUMN `max_cost_effectiveness_ratio` decimal(12, 6) DEFAULT NULL COMMENT '效费比最大值' AFTER `min_cost_effectiveness_ratio`,
--   ADD COLUMN `category_summary`   json           DEFAULT NULL COMMENT '各类成本汇总（批次整体各维度加权成本占比）' AFTER `max_cost_effectiveness_ratio`,
--   ADD COLUMN `weights_config`     json           DEFAULT NULL COMMENT '本次评估使用的权重配置快照' AFTER `category_summary`;

SET FOREIGN_KEY_CHECKS = 1;

-- ============================================================
-- 批量更新已有指标为成本型（direction: 统一为 cost 类型）
-- 所有指标方向统一：数值越大，成本越高
-- ============================================================
UPDATE `cost_indicator_config` SET `indicator_type` = 'cost'
WHERE `indicator_key` IN (
  'cost_personnel_experience',
  'cost_personnel_training',
  'cost_equipment_new_ratio',
  'cost_energy_spectrum',
  'cost_spare_satisfaction'
);
