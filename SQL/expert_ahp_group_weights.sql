-- 专家集结权重快照表
-- 存储每次集结计算的权重结果，以专家ID集合作为唯一标识
-- 专家集合相同则更新，不同则新增

CREATE TABLE IF NOT EXISTS `expert_ahp_group_weights` (
  `id`                               BIGINT        NOT NULL AUTO_INCREMENT,
  `group_id`                          VARCHAR(50)   NOT NULL COMMENT '专家组ID（UUID）',
  `expert_ids`                        VARCHAR(500)  NOT NULL COMMENT '专家ID集合（按ID排序，逗号分隔），作为唯一标识',
  `expert_count`                      INT           NOT NULL COMMENT '参与专家数量',
  `group_name`                        VARCHAR(200)  NULL COMMENT '专家组名称（可选描述）',

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
  `ind_weight_blind_rate`               DECIMAL(10,6) NULL COMMENT '致盲率得分权重',

  -- 专家权重明细JSON
  `expert_weights_json` LONGTEXT NULL COMMENT '专家权重明细（JSON格式）',

  `created_at`          TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at`          TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_expert_ids` (`expert_ids`),
  INDEX `idx_group_id` (`group_id`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='专家集结权重快照';
