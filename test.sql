/*
 Navicat Premium Dump SQL

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 90600 (9.6.0)
 Source Host           : localhost:3306
 Source Schema         : military_operational_effectiveness_evaluation

 Target Server Type    : MySQL
 Target Server Version : 90600 (9.6.0)
 File Encoding         : 65001

 Date: 31/03/2026 19:49:45
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for expert_base_info
-- ----------------------------
DROP TABLE IF EXISTS `expert_base_info`;
CREATE TABLE `expert_base_info`  (
                                     `expert_id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                                     `expert_name` varchar(100) CHARACTER SET utf8mb4  NOT NULL COMMENT '专家姓名',
                                     `gender` varchar(10) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '性别',
                                     `birth_date` date NULL DEFAULT NULL COMMENT '出生日期',
                                     `phone` varchar(20) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '联系电话',
                                     `email` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '电子邮箱',
                                     `work_unit` varchar(200) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '工作单位',
                                     `department` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '所在部门',
                                     `title` varchar(50) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '职称(教授/研究员/高工等)',
                                     `title_level` int NULL DEFAULT NULL COMMENT '职称等级(1-初级 2-中级 3-副高 4-正高)',
                                     `is_academician` tinyint(1) NULL DEFAULT 0 COMMENT '是否为院士',
                                     `is_yangtze_scholar` tinyint(1) NULL DEFAULT 0 COMMENT '是否为长江学者',
                                     `is_excellent_youth` tinyint(1) NULL DEFAULT 0 COMMENT '是否为杰青',
                                     `is_doctoral_supervisor` tinyint(1) NULL DEFAULT 0 COMMENT '是否为博导',
                                     `is_master_supervisor` tinyint(1) NULL DEFAULT 0 COMMENT '是否为硕导',
                                     `position` varchar(50) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '职务(主任/院长/所长等)',
                                     `position_level` int NULL DEFAULT NULL COMMENT '职务等级(1-一般 2-中层 3-高层)',
                                     `education` varchar(50) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '最高学历(本科/硕士/博士)',
                                     `education_level` int NULL DEFAULT NULL COMMENT '学历等级(1-本科 2-硕士 3-博士)',
                                     `graduated_school` varchar(200) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '毕业院校',
                                     `school_level` varchar(20) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '学校层次(985/211/普通)',
                                     `major` varchar(100) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '所学专业',
                                     `work_years` int NULL DEFAULT NULL COMMENT '工作年限',
                                     `professional_years` int NULL DEFAULT NULL COMMENT '专业工作年限',
                                     `exercise_experience` text CHARACTER SET utf8mb4  NULL COMMENT '演习训练经历(JSON格式)',
                                     `academic_count` int NULL DEFAULT 0 COMMENT '学术论文数量',
                                     `academic_sci_ei_count` int NULL DEFAULT 0 COMMENT 'SCI/EI论文数量',
                                     `academic_core_count` int NULL DEFAULT 0 COMMENT '核心期刊论文数量',
                                     `research_count` int NULL DEFAULT 0 COMMENT '科研项目数量(主持)',
                                     `research_participate_count` int NULL DEFAULT 0 COMMENT '科研项目数量(参与)',
                                     `patent_count` int NULL DEFAULT 0 COMMENT '专利数量(授权)',
                                     `software_copyright_count` int NULL DEFAULT 0 COMMENT '软件著作权数量',
                                     `monograph_count` int NULL DEFAULT 0 COMMENT '专著/教材数量',
                                     `award_count` int NULL DEFAULT 0 COMMENT '获奖数量',
                                     `expertise_area` varchar(500) CHARACTER SET utf8mb4  NULL DEFAULT NULL COMMENT '专业领域(JSON数组)',
                                     `has_military_training` tinyint(1) NULL DEFAULT 0 COMMENT '是否有军事训练学知识',
                                     `has_system_simulation` tinyint(1) NULL DEFAULT 0 COMMENT '是否有系统仿真知识',
                                     `has_statistics` tinyint(1) NULL DEFAULT 0 COMMENT '是否有数理统计学知识',
                                     `military_training_score` int NULL DEFAULT NULL COMMENT '军事训练学考核成绩(0-100)',
                                     `system_simulation_score` int NULL DEFAULT NULL COMMENT '系统仿真考核成绩(0-100)',
                                     `statistics_score` int NULL DEFAULT NULL COMMENT '数理统计考核成绩(0-100)',
                                     `national_exercise_count` int NULL DEFAULT 0 COMMENT '国家级演习次数(主持)',
                                     `national_exercise_participate_count` int NULL DEFAULT 0 COMMENT '国家级演习次数(参与)',
                                     `regional_exercise_count` int NULL DEFAULT 0 COMMENT '省级/战区级演习次数(主持)',
                                     `regional_exercise_participate_count` int NULL DEFAULT 0 COMMENT '省级/战区级演习次数(参与)',
                                     `military_practice_years` int NULL DEFAULT 0 COMMENT '部队实践/挂职年限',
                                     `status` tinyint(1) NULL DEFAULT 1 COMMENT '状态(1-启用 0-禁用)',
                                     `remarks` text CHARACTER SET utf8mb4  NULL COMMENT '备注',
                                     `created_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP,
                                     `updated_at` timestamp NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                     PRIMARY KEY (`expert_id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 102 CHARACTER SET = utf8mb4  COMMENT = '专家基础信息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of expert_base_info
-- ----------------------------
INSERT INTO `expert_base_info` VALUES (92, '黄志明', '女', '1989-12-07', '13210814617', '39845595@mil.cn', '南部战区联合参谋部', '作战实验中心', '副研究员', 3, 0, 0, 0, 0, 1, '院长', 3, '博士', 3, '海军工程大学', '211', '人工智能', 18, 14, NULL, 18, 12, 5, 3, 4, 3, 2, 1, 1, '数据链技术,抗干扰通信,军事通信系统', 1, 1, 1, 81, 85, 84, 1, 3, 3, 3, 2, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (93, '高勇', '女', '1991-01-13', '16949283325', '1250031@mil.cn', '海军装备研究院', '通信指挥室', '研究员', 4, 0, 0, 0, 1, 1, '主任', 3, '博士', 3, '北京航空航天大学', '985', '数据科学与大数据技术', 28, 22, NULL, 35, 26, 7, 4, 5, 5, 4, 2, 2, '电磁频谱管理,数据链技术,效能评估,抗干扰通信', 1, 1, 1, 98, 93, 90, 2, 4, 4, 4, 3, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (94, '王超', '男', '1977-12-10', '16386388030', '953178@edu.cn', '国防大学', '数据研发中心', '副研究员', 3, 0, 0, 0, 0, 1, '所长', 3, '博士', 3, '哈尔滨工程大学', '211', '电子信息工程', 18, 14, NULL, 18, 12, 5, 3, 4, 3, 2, 1, 1, '抗干扰通信,卫星通信,军事通信系统,网络攻防', 1, 1, 1, 77, 85, 78, 1, 3, 3, 3, 2, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (95, '唐文', '男', '1979-06-05', '17832609411', '701047@mil.cn', '战略支援部队某基地', '效能评估室', '高级工程师', 4, 0, 0, 0, 1, 1, '主任', 3, '博士', 3, '电子科技大学', '985', '军事运筹学', 28, 22, NULL, 35, 26, 7, 4, 5, 5, 4, 2, 2, '军事通信系统,抗干扰通信', 1, 1, 1, 89, 97, 96, 2, 4, 4, 4, 3, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (96, '吴艳', '女', '1972-12-24', '15248615400', '701631@mil.cn', '第二炮兵工程学院', '训练考核中心', '教授', 4, 0, 0, 0, 1, 1, '部长', 3, '博士', 3, '国防科技大学', '985', '系统工程', 28, 22, NULL, 35, 26, 7, 4, 5, 5, 4, 2, 2, '数据链技术,人工智能军事应用,军事通信系统', 1, 1, 1, 93, 95, 93, 2, 4, 4, 4, 3, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (97, '杨桂英', '女', '1989-07-01', '19494873231', '26300503@mil.cn', '东部战区空军某部', '效能评估室', '高级工程师', 3, 0, 0, 0, 0, 1, '所长', 3, '博士', 3, '南京航空航天大学', '211', '通信工程', 18, 14, NULL, 18, 12, 5, 3, 4, 3, 2, 1, 1, '抗干扰通信,电磁频谱管理', 1, 1, 1, 79, 82, 85, 1, 3, 3, 3, 2, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (98, '马洋', '男', '1980-03-14', '19071355810', '1253407@mil.cn', '某集团军通信部', '训练考核中心', '工程师', 2, 0, 0, 0, 0, 0, '科长', 2, '硕士', 2, '西安电子科技大学', '211', '系统工程', 12, 8, NULL, 8, 4, 3, 1, 2, 1, 1, 0, 0, '网络攻防,密码学,军事通信系统,人工智能军事应用', 1, 1, 1, 64, 68, 68, 0, 1, 2, 2, 1, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (99, '林军', '女', '1985-07-23', '15778069408', '842980@mil.cn', '军事科学院', '效能评估室', '研究员', 4, 0, 0, 0, 1, 1, '主任', 3, '博士', 3, '浙江大学', '985', '软件工程', 28, 22, NULL, 35, 26, 7, 4, 5, 5, 4, 2, 2, '指挥控制系统,网络攻防', 1, 1, 1, 89, 88, 96, 2, 4, 4, 4, 3, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (100, '唐洋', '男', '1977-02-28', '19375786586', '702971@edu.cn', '中部战区陆军某旅', '数据研发中心', '工程师', 2, 0, 0, 0, 0, 1, '科长', 2, '硕士', 2, '空军工程大学', '211', '系统工程', 12, 8, NULL, 8, 4, 3, 1, 2, 1, 1, 0, 0, '卫星通信,军事仿真,数据链技术,密码学', 1, 1, 1, 63, 68, 69, 0, 1, 2, 2, 1, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');
INSERT INTO `expert_base_info` VALUES (101, '刘志强', '男', '1988-03-04', '13764294790', '20981339@edu.cn', '东部战区空军某部', '数据研发中心', '高级工程师', 3, 0, 0, 0, 0, 1, '部长', 3, '博士', 3, '南京航空航天大学', '211', '计算机科学与技术', 18, 14, NULL, 18, 12, 5, 3, 4, 3, 2, 1, 1, '数据融合,人工智能军事应用,指挥控制系统', 1, 1, 1, 80, 80, 84, 1, 3, 3, 3, 2, 1, NULL, '2026-03-27 10:47:02', '2026-03-27 10:47:02');

SET FOREIGN_KEY_CHECKS = 1;
