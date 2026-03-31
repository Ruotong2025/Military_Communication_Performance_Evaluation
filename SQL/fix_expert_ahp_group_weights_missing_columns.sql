-- 与实体 ExpertAhpGroupWeights / SQL/expert_ahp_group_weights.sql 对齐。
-- 若某列已存在，跳过对应语句（MySQL 会报 Duplicate column，可忽略）。

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `cr_anti_jamming` DECIMAL(8,6) NULL COMMENT '抗干扰能力指标层CR' AFTER `cr_transmission`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `cr_effect` DECIMAL(8,6) NULL COMMENT '效能影响指标层CR' AFTER `cr_anti_jamming`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `dim_weight_anti_jamming` DECIMAL(10,6) NULL COMMENT '抗干扰能力维度权重' AFTER `dim_weight_transmission`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `dim_weight_effect` DECIMAL(10,6) NULL COMMENT '效能影响维度权重' AFTER `dim_weight_anti_jamming`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `ind_weight_sinr` DECIMAL(10,6) NULL COMMENT '信干噪比得分权重' AFTER `ind_weight_spectral_efficiency`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `ind_weight_anti_jamming_margin` DECIMAL(10,6) NULL COMMENT '抗干扰余量得分权重' AFTER `ind_weight_sinr`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `ind_weight_communication_distance` DECIMAL(10,6) NULL COMMENT '通信距离得分权重' AFTER `ind_weight_anti_jamming_margin`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `ind_weight_damage_rate` DECIMAL(10,6) NULL COMMENT '战损率得分权重' AFTER `ind_weight_communication_distance`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `ind_weight_mission_completion_rate` DECIMAL(10,6) NULL COMMENT '任务完成率得分权重' AFTER `ind_weight_damage_rate`;

ALTER TABLE `expert_ahp_group_weights`
  ADD COLUMN `ind_weight_blind_rate` DECIMAL(10,6) NULL COMMENT '致盲率得分权重' AFTER `ind_weight_mission_completion_rate`;
