-- 与后端 ExpertAhpIndividualWeights 实体对齐的列清单。
-- 若当前库表缺少某列，请取消注释对应行执行（列已存在则跳过该行，避免报错）。
-- 执行前请备份。

/*
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `ahp_result_json` LONGTEXT NULL COMMENT 'MatrixCalculationResult JSON';

ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `security_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `reliability_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `transmission_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `anti_jamming_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `effect_weight` DECIMAL(10,6) NULL;

ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `security_key_leakage_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `security_detected_probability_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `security_interception_resistance_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `reliability_crash_rate_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `reliability_recovery_capability_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `reliability_communication_availability_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `transmission_bandwidth_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `transmission_call_setup_time_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `transmission_transmission_delay_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `transmission_bit_error_rate_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `transmission_throughput_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `transmission_spectral_efficiency_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `anti_jamming_sinr_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `anti_jamming_anti_jamming_margin_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `anti_jamming_communication_distance_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `effect_damage_rate_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `effect_mission_completion_rate_weight` DECIMAL(10,6) NULL;
ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `effect_blind_rate_weight` DECIMAL(10,6) NULL;

ALTER TABLE `expert_ahp_individual_weights` ADD COLUMN `updated_at` DATETIME NULL;
*/
