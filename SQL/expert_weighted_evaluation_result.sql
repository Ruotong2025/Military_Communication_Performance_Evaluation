-- 专家集结加权评估结果表
-- 用于存储集结后的权重与评估得分
-- 每次执行集结计算会覆盖同 evaluation_id 的旧数据

CREATE TABLE IF NOT EXISTS `expert_weighted_evaluation_result` (
  `id`                               BIGINT        NOT NULL AUTO_INCREMENT,
  `evaluation_id`                    VARCHAR(50)   NOT NULL COMMENT '评估批次ID',
  `operation_id`                     VARCHAR(50)   NOT NULL COMMENT '作战任务ID',
  `expert_count`                     INT           NOT NULL COMMENT '参与专家数量',
  `expert_ids`                       VARCHAR(500)  NULL COMMENT '参与专家ID，逗号分隔',

  -- 一致性（6个CR）
  `cr_dim`                           DECIMAL(8,6)  NULL COMMENT '维度层CR',
  `cr_security`                      DECIMAL(8,6)  NULL COMMENT '安全性指标层CR',
  `cr_reliability`                   DECIMAL(8,6)  NULL COMMENT '可靠性指标层CR',
  `cr_transmission`                  DECIMAL(8,6)  NULL COMMENT '传输能力指标层CR',
  `cr_anti_jamming`                 DECIMAL(8,6)  NULL COMMENT '抗干扰能力指标层CR',
  `cr_effect`                       DECIMAL(8,6)  NULL COMMENT '效能影响指标层CR',

  -- 5个维度集结权重
  `dim_weight_security`              DECIMAL(10,6) NULL COMMENT '安全性维度权重',
  `dim_weight_reliability`          DECIMAL(10,6) NULL COMMENT '可靠性维度权重',
  `dim_weight_transmission`         DECIMAL(10,6) NULL COMMENT '传输能力维度权重',
  `dim_weight_anti_jamming`         DECIMAL(10,6) NULL COMMENT '抗干扰能力维度权重',
  `dim_weight_effect`               DECIMAL(10,6) NULL COMMENT '效能影响维度权重',

  -- 18个二级指标综合权重
  `ind_weight_key_leakage`               DECIMAL(10,6) NULL COMMENT '密钥泄露得分权重',
  `ind_weight_detected_probability`      DECIMAL(10,6) NULL COMMENT '被侦察得分权重',
  `ind_weight_interception_resistance`   DECIMAL(10,6) NULL COMMENT '抗拦截得分权重',
  `ind_weight_crash_rate`                DECIMAL(10,6) NULL COMMENT '崩溃比例得分权重',
  `ind_weight_recovery_capability`       DECIMAL(10,6) NULL COMMENT '恢复能力得分权重',
  `ind_weight_communication_availability` DECIMAL(10,6) NULL COMMENT '通信可用得分权重',
  `ind_weight_bandwidth`                 DECIMAL(10,6) NULL COMMENT '带宽得分权重',
  `ind_weight_call_setup_time`           DECIMAL(10,6) NULL COMMENT '呼叫建立得分权重',
  `ind_weight_transmission_delay`        DECIMAL(10,6) NULL COMMENT '传输时延得分权重',
  `ind_weight_bit_error_rate`            DECIMAL(10,6) NULL COMMENT '误码率得分权重',
  `ind_weight_throughput`                DECIMAL(10,6) NULL COMMENT '吞吐量得分权重',
  `ind_weight_spectral_efficiency`       DECIMAL(10,6) NULL COMMENT '频谱效率得分权重',
  `ind_weight_sinr`                      DECIMAL(10,6) NULL COMMENT '信干噪比得分权重',
  `ind_weight_anti_jamming_margin`       DECIMAL(10,6) NULL COMMENT '抗干扰余量得分权重',
  `ind_weight_communication_distance`    DECIMAL(10,6) NULL COMMENT '通信距离得分权重',
  `ind_weight_damage_rate`               DECIMAL(10,6) NULL COMMENT '战损率得分权重',
  `ind_weight_mission_completion_rate`   DECIMAL(10,6) NULL COMMENT '任务完成率得分权重',
  `ind_weight_blind_rate`        DECIMAL(10,6) NULL COMMENT '致盲率得分权重',

  -- 18个指标加权得分
  `score_key_leakage`               DECIMAL(8,6) NULL COMMENT '密钥泄露加权得分',
  `score_detected_probability`      DECIMAL(8,6) NULL COMMENT '被侦察加权得分',
  `score_interception_resistance`   DECIMAL(8,6) NULL COMMENT '抗拦截加权得分',
  `score_crash_rate`                DECIMAL(8,6) NULL COMMENT '崩溃比例加权得分',
  `score_recovery_capability`       DECIMAL(8,6) NULL COMMENT '恢复能力加权得分',
  `score_communication_availability` DECIMAL(8,6) NULL COMMENT '通信可用加权得分',
  `score_bandwidth`                 DECIMAL(8,6) NULL COMMENT '带宽加权得分',
  `score_call_setup_time`           DECIMAL(8,6) NULL COMMENT '呼叫建立加权得分',
  `score_transmission_delay`        DECIMAL(8,6) NULL COMMENT '传输时延加权得分',
  `score_bit_error_rate`            DECIMAL(8,6) NULL COMMENT '误码率加权得分',
  `score_throughput`                DECIMAL(8,6) NULL COMMENT '吞吐量加权得分',
  `score_spectral_efficiency`       DECIMAL(8,6) NULL COMMENT '频谱效率加权得分',
  `score_sinr`                      DECIMAL(8,6) NULL COMMENT '信干噪比加权得分',
  `score_anti_jamming_margin`       DECIMAL(8,6) NULL COMMENT '抗干扰余量加权得分',
  `score_communication_distance`    DECIMAL(8,6) NULL COMMENT '通信距离加权得分',
  `score_damage_rate`               DECIMAL(8,6) NULL COMMENT '战损率加权得分',
  `score_mission_completion_rate`   DECIMAL(8,6) NULL COMMENT '任务完成率加权得分',
  `score_blind_rate`        DECIMAL(8,6) NULL COMMENT '致盲率加权得分',

  -- 5个维度加权得分
  `score_security`      DECIMAL(8,6) NULL COMMENT '安全性维度加权得分',
  `score_reliability`   DECIMAL(8,6) NULL COMMENT '可靠性维度加权得分',
  `score_transmission`  DECIMAL(8,6) NULL COMMENT '传输能力维度加权得分',
  `score_anti_jamming` DECIMAL(8,6) NULL COMMENT '抗干扰能力维度加权得分',
  `score_effect`        DECIMAL(8,6) NULL COMMENT '效能影响维度加权得分',

  -- 综合得分
  `total_score`         DECIMAL(10,6) NULL COMMENT '综合加权得分',

  -- 专家权重明细JSON
  `expert_weights_json` LONGTEXT NULL COMMENT '专家权重明细（JSON格式）',

  `created_at`          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_eval_operation` (`evaluation_id`, `operation_id`),
  INDEX `idx_evaluation_id` (`evaluation_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专家集结加权评估结果';
