package com.ccnu.military.service;

import com.ccnu.military.dto.GenerateExpertsRequest;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.entity.ExpertCredibilityScore;
import com.ccnu.military.repository.ExpertBaseInfoRepository;
import com.ccnu.military.repository.ExpertCredibilityScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 模拟专家数据生成服务
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertMockDataService {

    private final ExpertBaseInfoRepository expertBaseInfoRepository;
    private final ExpertCredibilityScoreRepository scoreRepository;
    private final ExpertScoringCalculator scoringCalculator;

    private final Random random = new Random();

    /**
     * 与前端「预估分布结果」一致：各等级目标人数占总数比例（A、B、C、D）
     */
    private static final int[] GRADE_PCT_EXCELLENT = {40, 35, 20, 5};
    private static final int[] GRADE_PCT_BALANCED = {20, 30, 35, 15};
    private static final int[] GRADE_PCT_ORDINARY = {5, 20, 40, 35};

    // ========== 基础数据池 ==========

    private static final String[] SURNAMES = {
            "王", "李", "张", "刘", "陈", "杨", "黄", "赵", "周", "吴",
            "徐", "孙", "马", "朱", "胡", "郭", "何", "林", "罗", "高",
            "梁", "宋", "郑", "谢", "韩", "唐", "冯", "于", "董", "萧"
    };

    private static final String[] GIVEN_NAMES = {
            "伟", "芳", "娜", "秀英", "敏", "静", "丽", "强", "磊", "军",
            "洋", "勇", "艳", "杰", "娟", "涛", "明", "超", "秀兰", "霞",
            "平", "刚", "桂英", "建华", "建国", "建军", "志强", "志明", "志刚", "文"
    };

    private static final String[] UNITS = {
            "某军区司令部", "某集团军通信部", "海军装备研究院", "空军预警学院",
            "第二炮兵工程学院", "国防大学", "军事科学院", "某通信训练基地",
            "联合参谋部信息通信局", "战略支援部队某基地", "南部战区联合参谋部",
            "中部战区陆军某旅", "北部战区海军某舰艇部队", "东部战区空军某部"
    };

    private static final String[] DEPARTMENTS = {
            "通信指挥室", "网络信息中心", "数据研发中心", "作战实验中心",
            "效能评估室", "系统仿真室", "训练考核中心", "装备论证所"
    };

    private static final String[] MAJORS = {
            "通信工程", "电子信息工程", "计算机科学与技术", "网络工程",
            "信息工程", "软件工程", "数据科学与大数据技术", "人工智能",
            "系统工程", "军事运筹学"
    };

    private static final String[] SCHOOLS_985 = {
            "清华大学", "北京大学", "浙江大学", "复旦大学", "上海交通大学",
            "南京大学", "中国科学技术大学", "哈尔滨工业大学", "西安交通大学",
            "北京航空航天大学", "西北工业大学", "电子科技大学", "国防科技大学"
    };

    private static final String[] SCHOOLS_211 = {
            "北京邮电大学", "南京航空航天大学", "西安电子科技大学", "解放军理工大学",
            "解放军信息工程大学", "海军工程大学", "空军工程大学", "哈尔滨工程大学"
    };

    private static final String[] SCHOOLS_NORMAL = {
            "重庆邮电大学", "南京邮电大学", "杭州电子科技大学", "桂林电子科技大学",
            "西安邮电大学", "成都信息工程大学", "华北水利水电大学"
    };

    private static final String[] EXPERTISE_AREAS = {
            "军事通信系统", "抗干扰通信", "卫星通信", "数据链技术",
            "网络攻防", "电磁频谱管理", "指挥控制系统", "军事仿真",
            "效能评估", "数据融合", "人工智能军事应用", "密码学"
    };

    private static final String[] EDUCATIONS = {"博士", "硕士", "本科"};
    private static final String[] GENDERS = {"男", "女"};
    private static final String[] SCHOOL_LEVELS = {"985", "211", "普通"};

    // ========== 分布预设 ==========

    /**
     * 优秀偏多模式：高级职称、高学历、高职务、顶尖学校占比高
     */
    private static final int[] TITLE_DIST_EXCELLENT = {5, 15, 35, 45};
    private static final int[] EDUCATION_DIST_EXCELLENT = {10, 35, 55};
    private static final int[] POSITION_DIST_EXCELLENT = {20, 35, 45};
    private static final int[] SCHOOL_LEVEL_DIST_EXCELLENT = {40, 30, 30};
    private static final int ACADEMICIAN_PROB_EXCELLENT = 15;
    private static final int YANGTZE_PROB_EXCELLENT = 25;
    private static final int EXCELLENT_YOUTH_PROB_EXCELLENT = 30;
    private static final int[] SCIEI_RANGE_EXCELLENT = {15, 30};
    private static final int[] RESEARCH_RANGE_EXCELLENT = {3, 8};
    private static final int[] EXERCISE_RANGE_EXCELLENT = {2, 6};

    /**
     * 均衡分布模式
     */
    private static final int[] TITLE_DIST_BALANCED = {15, 30, 35, 20};
    private static final int[] EDUCATION_DIST_BALANCED = {30, 45, 25};
    private static final int[] POSITION_DIST_BALANCED = {35, 40, 25};
    private static final int[] SCHOOL_LEVEL_DIST_BALANCED = {30, 30, 40};
    private static final int ACADEMICIAN_PROB_BALANCED = 5;
    private static final int YANGTZE_PROB_BALANCED = 10;
    private static final int EXCELLENT_YOUTH_PROB_BALANCED = 15;
    private static final int[] SCIEI_RANGE_BALANCED = {5, 15};
    private static final int[] RESEARCH_RANGE_BALANCED = {1, 5};
    private static final int[] EXERCISE_RANGE_BALANCED = {0, 3};

    /**
     * 普通偏多模式
     */
    private static final int[] TITLE_DIST_ORDINARY = {25, 40, 25, 10};
    private static final int[] EDUCATION_DIST_ORDINARY = {50, 35, 15};
    private static final int[] POSITION_DIST_ORDINARY = {50, 35, 15};
    private static final int[] SCHOOL_LEVEL_DIST_ORDINARY = {15, 20, 65};
    private static final int ACADEMICIAN_PROB_ORDINARY = 2;
    private static final int YANGTZE_PROB_ORDINARY = 5;
    private static final int EXCELLENT_YOUTH_PROB_ORDINARY = 8;
    private static final int[] SCIEI_RANGE_ORDINARY = {0, 8};
    private static final int[] RESEARCH_RANGE_ORDINARY = {0, 3};
    private static final int[] EXERCISE_RANGE_ORDINARY = {0, 2};

    // ========== 辅助方法 ==========

    private String randomFrom(String[] arr) {
        return arr[random.nextInt(arr.length)];
    }

    private int randomInt(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max + 1);
    }

    /** 带离散度的整数：dispersion 越小越靠近区间中点，越大越接近均匀分布在 [min,max] */
    private int randomInt(int min, int max, double dispersion) {
        if (min >= max) {
            return min;
        }
        double d = Math.max(0.0, Math.min(1.0, dispersion));
        int mid = (min + max) / 2;
        int span = max - min;
        double frac = 0.10 + 0.90 * d;
        int half = (int) Math.round((span / 2.0) * frac);
        int lo = Math.max(min, mid - half);
        int hi = Math.min(max, mid + half);
        if (lo >= hi) {
            return mid;
        }
        return ThreadLocalRandom.current().nextInt(lo, hi + 1);
    }

    private static int pctFromMap(Map<String, Integer> map, String key, int defaultVal) {
        if (map == null) {
            return defaultVal;
        }
        Integer v = map.get(key);
        return v != null ? v : defaultVal;
    }

    private static String normalizeDistributionMode(String raw) {
        if (raw == null) {
            return "balanced";
        }
        String t = raw.trim().toLowerCase();
        if ("excellent".equals(t)) {
            return "excellent";
        }
        if ("ordinary".equals(t)) {
            return "ordinary";
        }
        if ("balanced".equals(t)) {
            return "balanced";
        }
        return "balanced";
    }

    private LocalDate randomBirthDate(double dispersion) {
        int age = randomInt(30, 65, dispersion);
        int year = LocalDate.now().getYear() - age;
        int month = randomInt(1, 12);
        int day = randomInt(1, 28);
        return LocalDate.of(year, month, day);
    }

    private String generateName() {
        return randomFrom(SURNAMES) + randomFrom(GIVEN_NAMES);
    }

    private String generateUniqueName() {
        String name;
        int attempts = 0;
        do {
            name = generateName();
            attempts++;
            if (attempts > 100) {
                name = name + attempts;
                break;
            }
        } while (expertBaseInfoRepository.existsByExpertName(name));
        return name;
    }

    private String generatePhone() {
        return "1" + randomInt(3, 9) + String.format("%09d", random.nextInt(1000000000));
    }

    private String generateEmail(String name, String unit) {
        String domain = unit.contains("军") ? "mil.cn" : "edu.cn";
        return Math.abs(name.hashCode()) + "@" + domain;
    }

    /**
     * 根据百分比分布随机选择索引
     */
    private int weightedSelectByPercent(int[] percents) {
        int total = Arrays.stream(percents).sum();
        int r = random.nextInt(total);
        int cumulative = 0;
        for (int i = 0; i < percents.length; i++) {
            cumulative += percents[i];
            if (r < cumulative) {
                return i;
            }
        }
        return percents.length - 1;
    }

    /**
     * 根据分布模式获取职称分布数组
     */
    private int[] getTitleDist(String mode) {
        switch (mode) {
            case "excellent": return TITLE_DIST_EXCELLENT;
            case "ordinary": return TITLE_DIST_ORDINARY;
            default: return TITLE_DIST_BALANCED;
        }
    }

    /**
     * 根据分布模式获取学历分布数组
     */
    private int[] getEducationDist(String mode) {
        switch (mode) {
            case "excellent": return EDUCATION_DIST_EXCELLENT;
            case "ordinary": return EDUCATION_DIST_ORDINARY;
            default: return EDUCATION_DIST_BALANCED;
        }
    }

    /**
     * 根据分布模式获取职务分布数组
     */
    private int[] getPositionDist(String mode) {
        switch (mode) {
            case "excellent": return POSITION_DIST_EXCELLENT;
            case "ordinary": return POSITION_DIST_ORDINARY;
            default: return POSITION_DIST_BALANCED;
        }
    }

    /**
     * 根据分布模式获取学校层次分布数组
     */
    private int[] getSchoolLevelDist(String mode) {
        switch (mode) {
            case "excellent": return SCHOOL_LEVEL_DIST_EXCELLENT;
            case "ordinary": return SCHOOL_LEVEL_DIST_ORDINARY;
            default: return SCHOOL_LEVEL_DIST_BALANCED;
        }
    }

    /**
     * 获取院士概率
     */
    private int getAcademicianProb(String mode) {
        switch (mode) {
            case "excellent": return ACADEMICIAN_PROB_EXCELLENT;
            case "ordinary": return ACADEMICIAN_PROB_ORDINARY;
            default: return ACADEMICIAN_PROB_BALANCED;
        }
    }

    /**
     * 获取长江学者概率
     */
    private int getYangtzeProb(String mode) {
        switch (mode) {
            case "excellent": return YANGTZE_PROB_EXCELLENT;
            case "ordinary": return YANGTZE_PROB_ORDINARY;
            default: return YANGTZE_PROB_BALANCED;
        }
    }

    /**
     * 获取杰青概率
     */
    private int getExcellentYouthProb(String mode) {
        switch (mode) {
            case "excellent": return EXCELLENT_YOUTH_PROB_EXCELLENT;
            case "ordinary": return EXCELLENT_YOUTH_PROB_ORDINARY;
            default: return EXCELLENT_YOUTH_PROB_BALANCED;
        }
    }

    /**
     * 获取SCI/EI论文数量范围
     */
    private int[] getSciEiRange(String mode) {
        switch (mode) {
            case "excellent": return SCIEI_RANGE_EXCELLENT;
            case "ordinary": return SCIEI_RANGE_ORDINARY;
            default: return SCIEI_RANGE_BALANCED;
        }
    }

    /**
     * 获取主持项目数量范围
     */
    private int[] getResearchRange(String mode) {
        switch (mode) {
            case "excellent": return RESEARCH_RANGE_EXCELLENT;
            case "ordinary": return RESEARCH_RANGE_ORDINARY;
            default: return RESEARCH_RANGE_BALANCED;
        }
    }

    /**
     * 获取演习次数范围
     */
    private int[] getExerciseRange(String mode) {
        switch (mode) {
            case "excellent": return EXERCISE_RANGE_EXCELLENT;
            case "ordinary": return EXERCISE_RANGE_ORDINARY;
            default: return EXERCISE_RANGE_BALANCED;
        }
    }

    // ========== 生成单个专家 ==========

    private ExpertBaseInfo generateExpert(String fixedName, String distributionMode,
                                          Map<String, Integer> customTitleDist,
                                          Map<String, Integer> customEduDist,
                                          Map<String, Integer> customPosDist,
                                          Map<String, Integer> customSchoolDist,
                                          double dispersion) {
        ExpertBaseInfo expert = new ExpertBaseInfo();

        expert.setExpertName(fixedName != null ? fixedName : generateUniqueName());
        expert.setGender(randomFrom(GENDERS));
        expert.setBirthDate(randomBirthDate(dispersion));
        expert.setPhone(generatePhone());
        expert.setEmail(generateEmail(expert.getExpertName(), randomFrom(UNITS)));
        expert.setWorkUnit(randomFrom(UNITS));
        expert.setDepartment(randomFrom(DEPARTMENTS));

        // 职称等级 - 根据分布模式
        int titleLevel;
        if (customTitleDist != null && !customTitleDist.isEmpty()) {
            int[] dist = new int[4];
            dist[0] = pctFromMap(customTitleDist, "1", 15);
            dist[1] = pctFromMap(customTitleDist, "2", 30);
            dist[2] = pctFromMap(customTitleDist, "3", 35);
            dist[3] = pctFromMap(customTitleDist, "4", 20);
            titleLevel = weightedSelectByPercent(dist) + 1;
        } else {
            int[] defaultTitleDist = getTitleDist(distributionMode);
            titleLevel = weightedSelectByPercent(defaultTitleDist) + 1;
        }
        expert.setTitleLevel(titleLevel);
        expert.setTitle(getTitleByLevel(titleLevel));

        // 特殊荣誉（院士、长江学者、杰青）
        int academicianProb = getAcademicianProb(distributionMode);
        int yangtzeProb = getYangtzeProb(distributionMode);
        int excellentYouthProb = getExcellentYouthProb(distributionMode);

        if (titleLevel >= 3 && random.nextDouble() * 100 < academicianProb) {
            expert.setIsAcademician(true);
        } else if (titleLevel >= 3 && random.nextDouble() * 100 < yangtzeProb) {
            expert.setIsYangtzeScholar(true);
        } else if (titleLevel >= 3 && random.nextDouble() * 100 < excellentYouthProb) {
            expert.setIsExcellentYouth(true);
        }
        if (titleLevel >= 3 && random.nextDouble() < 0.50) {
            expert.setIsDoctoralSupervisor(true);
        } else if (titleLevel >= 2 && random.nextDouble() < 0.60) {
            expert.setIsMasterSupervisor(true);
        }

        // 职务等级
        int positionLevel;
        if (customPosDist != null && !customPosDist.isEmpty()) {
            int[] dist = new int[3];
            dist[0] = pctFromMap(customPosDist, "1", 35);
            dist[1] = pctFromMap(customPosDist, "2", 40);
            dist[2] = pctFromMap(customPosDist, "3", 25);
            positionLevel = weightedSelectByPercent(dist) + 1;
        } else {
            int[] defaultPosDist = getPositionDist(distributionMode);
            positionLevel = weightedSelectByPercent(defaultPosDist) + 1;
        }
        expert.setPositionLevel(positionLevel);
        expert.setPosition(getPositionByLevel(positionLevel));

        // 学历等级
        int educationLevel;
        if (customEduDist != null && !customEduDist.isEmpty()) {
            int[] dist = new int[3];
            dist[0] = pctFromMap(customEduDist, "1", 30);
            dist[1] = pctFromMap(customEduDist, "2", 45);
            dist[2] = pctFromMap(customEduDist, "3", 25);
            educationLevel = weightedSelectByPercent(dist) + 1;
        } else {
            int[] defaultEduDist = getEducationDist(distributionMode);
            educationLevel = weightedSelectByPercent(defaultEduDist) + 1;
        }
        expert.setEducationLevel(educationLevel);
        expert.setEducation(EDUCATIONS[educationLevel - 1]);

        // 学校层次
        int[] defaultSchoolDist = getSchoolLevelDist(distributionMode);
        int schoolLevelIdx;
        if (customSchoolDist != null && !customSchoolDist.isEmpty()) {
            int[] dist = new int[3];
            dist[0] = pctFromMap(customSchoolDist, "985", 30);
            dist[1] = pctFromMap(customSchoolDist, "211", 30);
            dist[2] = pctFromMap(customSchoolDist, "0", pctFromMap(customSchoolDist, "普通", 40));
            schoolLevelIdx = weightedSelectByPercent(dist);
        } else {
            schoolLevelIdx = weightedSelectByPercent(defaultSchoolDist);
        }
        String schoolLevel = SCHOOL_LEVELS[schoolLevelIdx];
        expert.setSchoolLevel(schoolLevel);
        if ("985".equals(schoolLevel)) {
            expert.setGraduatedSchool(randomFrom(SCHOOLS_985));
        } else if ("211".equals(schoolLevel)) {
            expert.setGraduatedSchool(randomFrom(SCHOOLS_211));
        } else {
            expert.setGraduatedSchool(randomFrom(SCHOOLS_NORMAL));
        }
        expert.setMajor(randomFrom(MAJORS));

        expert.setWorkYears(randomInt(5, 40, dispersion));
        expert.setProfessionalYears(randomInt(3, 35, dispersion));

        // 学术成果 - 根据分布模式调整数量范围
        int[] sciEiRange = getSciEiRange(distributionMode);
        int[] researchRange = getResearchRange(distributionMode);
        int[] exerciseRange = getExerciseRange(distributionMode);

        expert.setAcademicCount(randomInt(0, 50, dispersion));
        int sciEiCount = randomInt(sciEiRange[0], sciEiRange[1], dispersion);
        expert.setAcademicSciEiCount(Math.min(expert.getAcademicCount(), sciEiCount));
        expert.setAcademicCoreCount(Math.min(expert.getAcademicCount() - expert.getAcademicSciEiCount(), randomInt(0, 20, dispersion)));

        expert.setResearchCount(randomInt(researchRange[0], researchRange[1], dispersion));
        expert.setResearchParticipateCount(randomInt(0, 15, dispersion));
        expert.setPatentCount(randomInt(0, 15, dispersion));
        expert.setSoftwareCopyrightCount(randomInt(0, 10, dispersion));
        expert.setMonographCount(randomInt(0, 5, dispersion));
        expert.setAwardCount(randomInt(0, 8, dispersion));

        // 专业领域
        List<String> areas = new ArrayList<>();
        int areaCount = randomInt(2, 5, dispersion);
        List<String> shuffled = new ArrayList<>(Arrays.asList(EXPERTISE_AREAS));
        Collections.shuffle(shuffled);
        for (int i = 0; i < areaCount && i < shuffled.size(); i++) {
            areas.add(shuffled.get(i));
        }
        expert.setExpertiseArea(String.join(",", areas));

        // 专业知识
        expert.setHasMilitaryTraining(random.nextDouble() < 0.70);
        expert.setHasSystemSimulation(random.nextDouble() < 0.65);
        expert.setHasStatistics(random.nextDouble() < 0.75);

        if (Boolean.TRUE.equals(expert.getHasMilitaryTraining())) {
            expert.setMilitaryTrainingScore(randomInt(60, 98, dispersion));
        }
        if (Boolean.TRUE.equals(expert.getHasSystemSimulation())) {
            expert.setSystemSimulationScore(randomInt(60, 98, dispersion));
        }
        if (Boolean.TRUE.equals(expert.getHasStatistics())) {
            expert.setStatisticsScore(randomInt(60, 98, dispersion));
        }

        // 演习训练
        int nationalCount = random.nextDouble() < 0.3 ? randomInt(0, 1) : 0;
        expert.setNationalExerciseCount(nationalCount);
        expert.setNationalExerciseParticipateCount(randomInt(exerciseRange[0], exerciseRange[1], dispersion));
        expert.setRegionalExerciseCount(randomInt(0, 8, dispersion));
        expert.setRegionalExerciseParticipateCount(randomInt(0, 10, dispersion));
        expert.setMilitaryPracticeYears(randomInt(0, 5, dispersion));

        expert.setStatus(1);

        return expert;
    }

    private static boolean isEmptyMap(Map<String, Integer> m) {
        return m == null || m.isEmpty();
    }

    private static int[] gradePercentagesForMode(String distributionMode) {
        switch (distributionMode) {
            case "excellent":
                return GRADE_PCT_EXCELLENT;
            case "ordinary":
                return GRADE_PCT_ORDINARY;
            default:
                return GRADE_PCT_BALANCED;
        }
    }

    /**
     * 最大余额法：与前端预估卡片一致，保证 nA+nB+nC+nD = total
     */
    private static int[] allocateGradeCounts(int total, int[] pct) {
        double[] exact = new double[4];
        for (int i = 0; i < 4; i++) {
            exact[i] = total * (double) pct[i] / 100.0;
        }
        int[] base = new int[4];
        for (int i = 0; i < 4; i++) {
            base[i] = (int) Math.floor(exact[i]);
        }
        int rem = total - base[0] - base[1] - base[2] - base[3];
        Integer[] order = {0, 1, 2, 3};
        Arrays.sort(order, (i, j) -> Double.compare(exact[j] - base[j], exact[i] - base[i]));
        for (int k = 0; k < rem; k++) {
            base[order[k]]++;
        }
        return base;
    }

    private List<String> buildShuffledGradeTargets(String distributionMode, int count) {
        int[] pct = gradePercentagesForMode(distributionMode);
        int[] n = allocateGradeCounts(count, pct);
        List<String> list = new ArrayList<>(count);
        for (int i = 0; i < n[0]; i++) {
            list.add("A");
        }
        for (int i = 0; i < n[1]; i++) {
            list.add("B");
        }
        for (int i = 0; i < n[2]; i++) {
            list.add("C");
        }
        for (int i = 0; i < n[3]; i++) {
            list.add("D");
        }
        Collections.shuffle(list, random);
        return list;
    }

    private static int gradeRank(String g) {
        if (g == null) {
            return 0;
        }
        switch (g) {
            case "A":
                return 4;
            case "B":
                return 3;
            case "C":
                return 2;
            case "D":
                return 1;
            default:
                return 0;
        }
    }

    /** 与 ExpertScoringCalculator 中 educationLevel 含义一致：1 本科 2 硕士 3 博士 */
    private void setEducationByLevel(ExpertBaseInfo e, int level) {
        e.setEducationLevel(level);
        switch (level) {
            case 1:
                e.setEducation("本科");
                break;
            case 2:
                e.setEducation("硕士");
                break;
            case 3:
                e.setEducation("博士");
                break;
            default:
                e.setEducation("本科");
                break;
        }
    }

    private void clearHonorsIfLowTitle(ExpertBaseInfo e) {
        if (e.getTitleLevel() != null && e.getTitleLevel() < 3) {
            e.setIsAcademician(false);
            e.setIsYangtzeScholar(false);
            e.setIsExcellentYouth(false);
            e.setIsDoctoralSupervisor(false);
            e.setIsMasterSupervisor(false);
        }
    }

    /**
     * 将档案字段调整为能使综合分落在目标等级区间的典型配置（与计算器规则一致）
     */
    private void applyHardTemplateForGrade(ExpertBaseInfo e, String grade) {
        switch (grade) {
            case "D":
                e.setTitleLevel(1);
                e.setTitle(getTitleByLevel(1));
                clearHonorsIfLowTitle(e);
                e.setPositionLevel(1);
                e.setPosition(getPositionByLevel(1));
                setEducationByLevel(e, 1);
                e.setSchoolLevel("普通");
                e.setGraduatedSchool(randomFrom(SCHOOLS_NORMAL));
                e.setAcademicCount(0);
                e.setAcademicSciEiCount(0);
                e.setAcademicCoreCount(0);
                e.setResearchCount(0);
                e.setResearchParticipateCount(0);
                e.setPatentCount(0);
                e.setSoftwareCopyrightCount(0);
                e.setMonographCount(0);
                e.setAwardCount(0);
                e.setNationalExerciseCount(0);
                e.setNationalExerciseParticipateCount(0);
                e.setRegionalExerciseCount(0);
                e.setRegionalExerciseParticipateCount(0);
                e.setMilitaryPracticeYears(0);
                e.setHasMilitaryTraining(false);
                e.setHasSystemSimulation(false);
                e.setHasStatistics(false);
                e.setMilitaryTrainingScore(null);
                e.setSystemSimulationScore(null);
                e.setStatisticsScore(null);
                e.setWorkYears(5);
                e.setProfessionalYears(0);
                break;
            case "C":
                e.setTitleLevel(2);
                e.setTitle(getTitleByLevel(2));
                e.setIsAcademician(false);
                e.setIsYangtzeScholar(false);
                e.setIsExcellentYouth(false);
                e.setIsMasterSupervisor(random.nextBoolean());
                e.setIsDoctoralSupervisor(false);
                e.setPositionLevel(2);
                e.setPosition(getPositionByLevel(2));
                setEducationByLevel(e, 2);
                e.setSchoolLevel("211");
                e.setGraduatedSchool(randomFrom(SCHOOLS_211));
                e.setAcademicCount(8);
                e.setAcademicSciEiCount(4);
                e.setAcademicCoreCount(3);
                e.setResearchCount(1);
                e.setResearchParticipateCount(2);
                e.setPatentCount(1);
                e.setSoftwareCopyrightCount(1);
                e.setMonographCount(0);
                e.setAwardCount(0);
                e.setNationalExerciseCount(0);
                e.setNationalExerciseParticipateCount(1);
                e.setRegionalExerciseCount(2);
                e.setRegionalExerciseParticipateCount(2);
                e.setMilitaryPracticeYears(1);
                e.setHasMilitaryTraining(true);
                e.setHasSystemSimulation(true);
                e.setHasStatistics(true);
                e.setMilitaryTrainingScore(68);
                e.setSystemSimulationScore(66);
                e.setStatisticsScore(67);
                e.setWorkYears(12);
                e.setProfessionalYears(8);
                break;
            case "B":
                e.setTitleLevel(3);
                e.setTitle(getTitleByLevel(3));
                e.setIsAcademician(false);
                e.setIsYangtzeScholar(false);
                e.setIsExcellentYouth(false);
                e.setIsDoctoralSupervisor(random.nextDouble() < 0.5);
                e.setIsMasterSupervisor(true);
                e.setPositionLevel(3);
                e.setPosition(getPositionByLevel(3));
                setEducationByLevel(e, 3);
                e.setSchoolLevel("211");
                e.setGraduatedSchool(randomFrom(SCHOOLS_211));
                e.setAcademicCount(18);
                e.setAcademicSciEiCount(12);
                e.setAcademicCoreCount(5);
                e.setResearchCount(3);
                e.setResearchParticipateCount(4);
                e.setPatentCount(3);
                e.setSoftwareCopyrightCount(2);
                e.setMonographCount(1);
                e.setAwardCount(1);
                e.setNationalExerciseCount(1);
                e.setNationalExerciseParticipateCount(3);
                e.setRegionalExerciseCount(3);
                e.setRegionalExerciseParticipateCount(3);
                e.setMilitaryPracticeYears(2);
                e.setHasMilitaryTraining(true);
                e.setHasSystemSimulation(true);
                e.setHasStatistics(true);
                e.setMilitaryTrainingScore(82);
                e.setSystemSimulationScore(80);
                e.setStatisticsScore(81);
                e.setWorkYears(18);
                e.setProfessionalYears(14);
                break;
            case "A":
            default:
                e.setTitleLevel(4);
                e.setTitle(getTitleByLevel(4));
                e.setIsAcademician(false);
                e.setIsYangtzeScholar(false);
                e.setIsExcellentYouth(false);
                e.setIsDoctoralSupervisor(true);
                e.setIsMasterSupervisor(true);
                e.setPositionLevel(3);
                e.setPosition(getPositionByLevel(3));
                setEducationByLevel(e, 3);
                e.setSchoolLevel("985");
                e.setGraduatedSchool(randomFrom(SCHOOLS_985));
                e.setAcademicCount(35);
                e.setAcademicSciEiCount(26);
                e.setAcademicCoreCount(7);
                e.setResearchCount(4);
                e.setResearchParticipateCount(5);
                e.setPatentCount(5);
                e.setSoftwareCopyrightCount(4);
                e.setMonographCount(2);
                e.setAwardCount(2);
                e.setNationalExerciseCount(2);
                e.setNationalExerciseParticipateCount(4);
                e.setRegionalExerciseCount(4);
                e.setRegionalExerciseParticipateCount(4);
                e.setMilitaryPracticeYears(3);
                e.setHasMilitaryTraining(true);
                e.setHasSystemSimulation(true);
                e.setHasStatistics(true);
                e.setMilitaryTrainingScore(94);
                e.setSystemSimulationScore(93);
                e.setStatisticsScore(95);
                e.setWorkYears(28);
                e.setProfessionalYears(22);
                break;
        }
    }

    private void applyGradeDispersionJitter(ExpertBaseInfo e, String grade, double dispersion) {
        double d = Math.max(0.0, Math.min(1.0, dispersion));
        int span = (int) Math.round(8 * d);
        ThreadLocalRandom r = ThreadLocalRandom.current();
        switch (grade) {
            case "A":
                if (e.getMilitaryTrainingScore() != null) {
                    e.setMilitaryTrainingScore(clamp(e.getMilitaryTrainingScore() + r.nextInt(-span, span + 1), 88, 98));
                }
                if (e.getSystemSimulationScore() != null) {
                    e.setSystemSimulationScore(clamp(e.getSystemSimulationScore() + r.nextInt(-span, span + 1), 88, 98));
                }
                if (e.getStatisticsScore() != null) {
                    e.setStatisticsScore(clamp(e.getStatisticsScore() + r.nextInt(-span, span + 1), 88, 98));
                }
                break;
            case "B":
                if (e.getMilitaryTrainingScore() != null) {
                    e.setMilitaryTrainingScore(clamp(e.getMilitaryTrainingScore() + r.nextInt(-span, span + 1), 76, 90));
                }
                if (e.getSystemSimulationScore() != null) {
                    e.setSystemSimulationScore(clamp(e.getSystemSimulationScore() + r.nextInt(-span, span + 1), 76, 90));
                }
                if (e.getStatisticsScore() != null) {
                    e.setStatisticsScore(clamp(e.getStatisticsScore() + r.nextInt(-span, span + 1), 76, 90));
                }
                break;
            case "C":
                if (e.getMilitaryTrainingScore() != null) {
                    e.setMilitaryTrainingScore(clamp(e.getMilitaryTrainingScore() + r.nextInt(-span, span + 1), 60, 76));
                }
                if (e.getSystemSimulationScore() != null) {
                    e.setSystemSimulationScore(clamp(e.getSystemSimulationScore() + r.nextInt(-span, span + 1), 60, 76));
                }
                if (e.getStatisticsScore() != null) {
                    e.setStatisticsScore(clamp(e.getStatisticsScore() + r.nextInt(-span, span + 1), 60, 76));
                }
                break;
            default:
                break;
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }

    private void boostExpertProfile(ExpertBaseInfo e) {
        if (e.getTitleLevel() == null || e.getTitleLevel() < 4) {
            int nl = Math.min(4, (e.getTitleLevel() != null ? e.getTitleLevel() : 1) + 1);
            e.setTitleLevel(nl);
            e.setTitle(getTitleByLevel(nl));
        }
        if (e.getPositionLevel() == null || e.getPositionLevel() < 3) {
            int pl = Math.min(3, (e.getPositionLevel() != null ? e.getPositionLevel() : 1) + 1);
            e.setPositionLevel(pl);
            e.setPosition(getPositionByLevel(pl));
        }
        if (e.getEducationLevel() == null || e.getEducationLevel() < 3) {
            int el = Math.min(3, (e.getEducationLevel() != null ? e.getEducationLevel() : 1) + 1);
            setEducationByLevel(e, el);
            if ("985".equals(e.getSchoolLevel()) || "211".equals(e.getSchoolLevel())) {
                // keep
            } else if (el >= 2) {
                e.setSchoolLevel("211");
                e.setGraduatedSchool(randomFrom(SCHOOLS_211));
            }
        }
        e.setAcademicCount(Math.min(50, (e.getAcademicCount() != null ? e.getAcademicCount() : 0) + 4));
        e.setAcademicSciEiCount(Math.min(e.getAcademicCount(), (e.getAcademicSciEiCount() != null ? e.getAcademicSciEiCount() : 0) + 3));
        e.setResearchCount(Math.min(8, (e.getResearchCount() != null ? e.getResearchCount() : 0) + 1));
        e.setHasMilitaryTraining(true);
        e.setHasSystemSimulation(true);
        e.setHasStatistics(true);
        int bump = 5;
        e.setMilitaryTrainingScore(clamp((e.getMilitaryTrainingScore() != null ? e.getMilitaryTrainingScore() : 70) + bump, 60, 98));
        e.setSystemSimulationScore(clamp((e.getSystemSimulationScore() != null ? e.getSystemSimulationScore() : 70) + bump, 60, 98));
        e.setStatisticsScore(clamp((e.getStatisticsScore() != null ? e.getStatisticsScore() : 70) + bump, 60, 98));
        e.setProfessionalYears(Math.min(35, (e.getProfessionalYears() != null ? e.getProfessionalYears() : 0) + 2));
    }

    private void reduceExpertProfile(ExpertBaseInfo e) {
        if (e.getTitleLevel() != null && e.getTitleLevel() > 1) {
            int nl = e.getTitleLevel() - 1;
            e.setTitleLevel(nl);
            e.setTitle(getTitleByLevel(nl));
            clearHonorsIfLowTitle(e);
        }
        if (e.getPositionLevel() != null && e.getPositionLevel() > 1) {
            int pl = e.getPositionLevel() - 1;
            e.setPositionLevel(pl);
            e.setPosition(getPositionByLevel(pl));
        }
        if (e.getEducationLevel() != null && e.getEducationLevel() > 1) {
            int el = e.getEducationLevel() - 1;
            setEducationByLevel(e, el);
        }
        e.setAcademicCount(Math.max(0, (e.getAcademicCount() != null ? e.getAcademicCount() : 0) - 4));
        e.setAcademicSciEiCount(Math.min(e.getAcademicCount() != null ? e.getAcademicCount() : 0,
                Math.max(0, (e.getAcademicSciEiCount() != null ? e.getAcademicSciEiCount() : 0) - 2)));
        e.setResearchCount(Math.max(0, (e.getResearchCount() != null ? e.getResearchCount() : 0) - 1));
        int drop = 6;
        if (Boolean.TRUE.equals(e.getHasMilitaryTraining()) && e.getMilitaryTrainingScore() != null) {
            e.setMilitaryTrainingScore(Math.max(60, e.getMilitaryTrainingScore() - drop));
        }
        if (Boolean.TRUE.equals(e.getHasSystemSimulation()) && e.getSystemSimulationScore() != null) {
            e.setSystemSimulationScore(Math.max(60, e.getSystemSimulationScore() - drop));
        } else if (Boolean.TRUE.equals(e.getHasSystemSimulation())) {
            e.setHasSystemSimulation(false);
            e.setSystemSimulationScore(null);
        } else if (Boolean.TRUE.equals(e.getHasStatistics())) {
            e.setHasStatistics(false);
            e.setStatisticsScore(null);
        } else if (Boolean.TRUE.equals(e.getHasMilitaryTraining())) {
            e.setHasMilitaryTraining(false);
            e.setMilitaryTrainingScore(null);
        }
        e.setProfessionalYears(Math.max(0, (e.getProfessionalYears() != null ? e.getProfessionalYears() : 0) - 2));
    }

    /**
     * 迭代微调直至综合分对应等级与目标一致（实体表默认权重与计算器一致）
     */
    private void conformExpertToTargetGrade(ExpertBaseInfo e, String targetGrade, double dispersion) {
        applyHardTemplateForGrade(e, targetGrade);
        applyGradeDispersionJitter(e, targetGrade, dispersion);
        for (int round = 0; round < 2; round++) {
            for (int i = 0; i < 45; i++) {
                BigDecimal total = scoringCalculator.evaluateExpert(e).getTotalScore();
                String lvl = scoringCalculator.determineCredibilityLevel(total);
                if (lvl.equals(targetGrade)) {
                    return;
                }
                if (gradeRank(lvl) < gradeRank(targetGrade)) {
                    boostExpertProfile(e);
                } else {
                    reduceExpertProfile(e);
                }
            }
            applyHardTemplateForGrade(e, targetGrade);
            applyGradeDispersionJitter(e, targetGrade, dispersion);
        }
        BigDecimal total = scoringCalculator.evaluateExpert(e).getTotalScore();
        String lvl = scoringCalculator.determineCredibilityLevel(total);
        if (!lvl.equals(targetGrade)) {
            log.warn("模拟专家 {} 未能精确落入目标等级 {}，当前为 {} (综合分 {})",
                    e.getExpertName(), targetGrade, lvl, total);
        }
    }

    private String getTitleByLevel(int level) {
        switch (level) {
            case 4: return randomFrom(new String[]{"教授", "研究员", "高级工程师"});
            case 3: return randomFrom(new String[]{"副教授", "副研究员", "高级工程师"});
            case 2: return randomFrom(new String[]{"讲师", "工程师"});
            default: return randomFrom(new String[]{"助教", "助理工程师"});
        }
    }

    private String getPositionByLevel(int level) {
        switch (level) {
            case 3: return randomFrom(new String[]{"所长", "主任", "院长", "部长"});
            case 2: return randomFrom(new String[]{"处长", "科长", "室主任"});
            default: return randomFrom(new String[]{"研究员", "专家", "工程师"});
        }
    }

    // ========== 公开方法 ==========

    /**
     * 批量生成专家模拟数据
     */
    @Transactional
    public List<ExpertBaseInfo> generateMockExperts(GenerateExpertsRequest request) {
        int count = request.getCount() != null ? request.getCount() : 10;
        List<String> names = request.getNames();
        String generateMode = request.getGenerateMode() != null ? request.getGenerateMode() : "append";
        String distributionMode = normalizeDistributionMode(request.getDistributionMode());
        double dispersion = request.getDispersion() != null
                ? Math.max(0.0, Math.min(1.0, request.getDispersion()))
                : 0.65;

        // 如果是覆盖模式，先清空现有数据
        if ("overwrite".equals(generateMode)) {
            log.info("覆盖模式：清空现有 {} 条专家数据和 {} 条评估记录",
                    expertBaseInfoRepository.count(), scoreRepository.count());
            scoreRepository.deleteAll();
            expertBaseInfoRepository.deleteAll();
        }

        boolean useGradeTargets = isEmptyMap(request.getTitleDistribution())
                && isEmptyMap(request.getEducationDistribution())
                && isEmptyMap(request.getPositionDistribution())
                && isEmptyMap(request.getSchoolLevelDistribution());
        List<String> gradeTargets = useGradeTargets ? buildShuffledGradeTargets(distributionMode, count) : null;
        int gradeIdx = 0;

        List<ExpertBaseInfo> experts = new ArrayList<>();

        if (names != null && !names.isEmpty()) {
            for (String name : names) {
                if (experts.size() >= count) break;
                if (!"overwrite".equals(generateMode) && expertBaseInfoRepository.existsByExpertName(name)) {
                    log.info("专家已存在，跳过: {}", name);
                    continue;
                }
                String tg = gradeTargets != null ? gradeTargets.get(gradeIdx) : null;
                ExpertBaseInfo expert = generateExpert(name, distributionMode,
                        request.getTitleDistribution(), request.getEducationDistribution(),
                        request.getPositionDistribution(), request.getSchoolLevelDistribution(), dispersion);
                if (tg != null) {
                    conformExpertToTargetGrade(expert, tg, dispersion);
                }
                expert = expertBaseInfoRepository.save(expert);
                experts.add(expert);
                if (gradeTargets != null) {
                    gradeIdx++;
                }
            }
        }

        while (experts.size() < count) {
            String tg = gradeTargets != null ? gradeTargets.get(gradeIdx) : null;
            ExpertBaseInfo expert = generateExpert(null, distributionMode,
                    request.getTitleDistribution(), request.getEducationDistribution(),
                    request.getPositionDistribution(), request.getSchoolLevelDistribution(), dispersion);
            if (tg != null) {
                conformExpertToTargetGrade(expert, tg, dispersion);
            }
            expert = expertBaseInfoRepository.save(expert);
            experts.add(expert);
            if (gradeTargets != null) {
                gradeIdx++;
            }
        }

        log.info("成功生成 {} 位专家模拟数据 (模式: {}, 分布: {}, 等级配额: {})",
                experts.size(), generateMode, distributionMode, useGradeTargets ? "按预估比例" : "未启用");
        return experts;
    }

    /**
     * 生成模拟数据并自动评估
     */
    @Transactional
    public Map<String, Object> generateAndEvaluate(GenerateExpertsRequest request) {
        // 先判断是否覆盖模式，如果是，先清空评估记录
        String generateMode = request.getGenerateMode() != null ? request.getGenerateMode() : "append";
        String distributionMode = normalizeDistributionMode(request.getDistributionMode());

        if ("overwrite".equals(generateMode)) {
            scoreRepository.deleteAll();
        }

        List<ExpertBaseInfo> experts = generateMockExperts(request);
        List<ExpertCredibilityScore> scores = new ArrayList<>();

        // 根据autoEvaluate决定是否自动评估
        boolean autoEvaluate = request.getAutoEvaluate() == null || request.getAutoEvaluate();

        if (autoEvaluate) {
        for (ExpertBaseInfo expert : experts) {
            try {
                    // 检查是否已有评估记录（新增模式下跳过已有）
                    if (!"overwrite".equals(generateMode) && scoreRepository.findByExpertId(expert.getExpertId()).isPresent()) {
                        log.debug("专家已有评估记录，跳过: {}", expert.getExpertName());
                        continue;
                    }
                ExpertCredibilityScore score = scoringCalculator.evaluateExpert(expert);
                scores.add(scoreRepository.save(score));
            } catch (Exception e) {
                log.error("评估专家失败: {}", expert.getExpertName(), e);
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("experts", experts);
        result.put("scores", scores);
        result.put("totalGenerated", experts.size());
        result.put("totalEvaluated", scores.size());
        result.put("generateMode", generateMode);
        result.put("distributionMode", distributionMode);

        return result;
    }
}
