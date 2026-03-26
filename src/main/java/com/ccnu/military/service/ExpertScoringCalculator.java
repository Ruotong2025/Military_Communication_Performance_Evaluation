package com.ccnu.military.service;

import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.entity.ExpertCredibilityScore;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 专家可信度评分计算器（无状态，可共享）
 * 消除 ExpertMockDataService 与 ExpertCredibilityService 之间的循环依赖
 */
@Component
public class ExpertScoringCalculator {

    // ============================================================
    // 1. 职称得分计算 (title_ql) - 满分100分
    // ============================================================
    public BigDecimal calculateTitleScore(ExpertBaseInfo expert) {
        if (expert.getTitleLevel() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseScore;
        switch (expert.getTitleLevel()) {
            case 4:
                baseScore = new BigDecimal("90");
                if (Boolean.TRUE.equals(expert.getIsAcademician())) baseScore = baseScore.add(new BigDecimal("10"));
                if (Boolean.TRUE.equals(expert.getIsYangtzeScholar())) baseScore = baseScore.add(new BigDecimal("8"));
                if (Boolean.TRUE.equals(expert.getIsExcellentYouth())) baseScore = baseScore.add(new BigDecimal("5"));
                break;
            case 3:
                baseScore = new BigDecimal("75");
                if (Boolean.TRUE.equals(expert.getIsDoctoralSupervisor())) baseScore = baseScore.add(new BigDecimal("5"));
                if (Boolean.TRUE.equals(expert.getIsMasterSupervisor())) baseScore = baseScore.add(new BigDecimal("3"));
                break;
            case 2:
                baseScore = new BigDecimal("60");
                break;
            case 1:
                baseScore = new BigDecimal("45");
                break;
            default:
                return BigDecimal.ZERO;
        }
        return baseScore.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 2. 职务得分计算 (position_ql)
    // ============================================================
    public BigDecimal calculatePositionScore(ExpertBaseInfo expert) {
        if (expert.getPositionLevel() == null) return BigDecimal.ZERO;
        switch (expert.getPositionLevel()) {
            case 3: return new BigDecimal("90").setScale(2, RoundingMode.HALF_UP);
            case 2: return new BigDecimal("75").setScale(2, RoundingMode.HALF_UP);
            case 1: return new BigDecimal("60").setScale(2, RoundingMode.HALF_UP);
            default: return new BigDecimal("50").setScale(2, RoundingMode.HALF_UP);
        }
    }

    // ============================================================
    // 3. 学习经历得分 (education_experience_ql)
    // ============================================================
    public BigDecimal calculateEducationScore(ExpertBaseInfo expert) {
        if (expert.getEducationLevel() == null) return BigDecimal.ZERO;
        BigDecimal baseScore;
        switch (expert.getEducationLevel()) {
            case 3:
                baseScore = new BigDecimal("85");
                if ("985".equals(expert.getSchoolLevel())) baseScore = baseScore.add(new BigDecimal("10"));
                else if ("211".equals(expert.getSchoolLevel())) baseScore = baseScore.add(new BigDecimal("5"));
                break;
            case 2:
                baseScore = new BigDecimal("70");
                if ("985".equals(expert.getSchoolLevel())) baseScore = baseScore.add(new BigDecimal("8"));
                else if ("211".equals(expert.getSchoolLevel())) baseScore = baseScore.add(new BigDecimal("4"));
                break;
            case 1:
                baseScore = new BigDecimal("55");
                if ("985".equals(expert.getSchoolLevel())) baseScore = baseScore.add(new BigDecimal("6"));
                else if ("211".equals(expert.getSchoolLevel())) baseScore = baseScore.add(new BigDecimal("3"));
                break;
            default:
                return BigDecimal.ZERO;
        }
        return baseScore.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 4. 学术成果得分 (academic_achievements_ql)
    // ============================================================
    public BigDecimal calculateAcademicScore(ExpertBaseInfo expert) {
        int total = expert.getAcademicCount() != null ? expert.getAcademicCount() : 0;
        int sciEi = expert.getAcademicSciEiCount() != null ? expert.getAcademicSciEiCount() : 0;
        int core = expert.getAcademicCoreCount() != null ? expert.getAcademicCoreCount() : 0;
        BigDecimal base = total >= 20 ? new BigDecimal("80") : total >= 10 ? new BigDecimal("65")
                : total >= 5 ? new BigDecimal("50") : total >= 1 ? new BigDecimal("35") : new BigDecimal("20");
        return base.add(new BigDecimal(sciEi).multiply(new BigDecimal("1")))
                .add(new BigDecimal(core).multiply(new BigDecimal("0.5")))
                .min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 5. 科研成果得分 (research_achievements_ql)
    // ============================================================
    public BigDecimal calculateResearchScore(ExpertBaseInfo expert) {
        int rc = expert.getResearchCount() != null ? expert.getResearchCount() : 0;
        int rp = expert.getResearchParticipateCount() != null ? expert.getResearchParticipateCount() : 0;
        int pc = expert.getPatentCount() != null ? expert.getPatentCount() : 0;
        int sc = expert.getSoftwareCopyrightCount() != null ? expert.getSoftwareCopyrightCount() : 0;
        int mc = expert.getMonographCount() != null ? expert.getMonographCount() : 0;
        BigDecimal score = new BigDecimal(Math.min(rc, 4) * 15 + Math.min(rp, 5) * 5
                + Math.min(pc, 6) * 5 + Math.min(sc, 5) * 2 + Math.min(mc, 2) * 10);
        return score.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 6. 演习训练经历得分 (exercise_experience_ql)
    // ============================================================
    public BigDecimal calculateExerciseScore(ExpertBaseInfo expert) {
        int nec = expert.getNationalExerciseCount() != null ? expert.getNationalExerciseCount() : 0;
        int nep = expert.getNationalExerciseParticipateCount() != null ? expert.getNationalExerciseParticipateCount() : 0;
        int rec = expert.getRegionalExerciseCount() != null ? expert.getRegionalExerciseCount() : 0;
        int rep = expert.getRegionalExerciseParticipateCount() != null ? expert.getRegionalExerciseParticipateCount() : 0;
        int mpy = expert.getMilitaryPracticeYears() != null ? expert.getMilitaryPracticeYears() : 0;
        BigDecimal score = new BigDecimal(Math.min(nec, 3) * 20 + Math.min(nep, 4) * 10
                + Math.min(rec, 4) * 12 + Math.min(rep, 5) * 6 + Math.min(mpy, 3) * 15);
        return score.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 7/8/9. 专业知识得分（军训/仿真/统计）
    // ============================================================
    public BigDecimal calculateKnowledgeScore(Boolean hasKnowledge, Integer examScore) {
        if (Boolean.TRUE.equals(hasKnowledge)) {
            if (examScore != null) {
                return new BigDecimal("85").multiply(new BigDecimal("0.3"))
                        .add(new BigDecimal(examScore).multiply(new BigDecimal("0.7")))
                        .setScale(2, RoundingMode.HALF_UP);
            }
            return new BigDecimal("82").setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal("50").setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 10. 专业年限得分 (professional_years_qt)
    // ============================================================
    public BigDecimal calculateProfessionalYearsScore(ExpertBaseInfo expert) {
        int years = expert.getProfessionalYears() != null ? expert.getProfessionalYears() : 0;
        if (years <= 0) return new BigDecimal("30").setScale(2, RoundingMode.HALF_UP);
        BigDecimal score;
        if (years >= 30) score = new BigDecimal("95").add(new BigDecimal(years - 30).multiply(new BigDecimal("0.5")));
        else if (years >= 20) score = new BigDecimal("85").add(new BigDecimal(years - 20).multiply(new BigDecimal("1")));
        else if (years >= 10) score = new BigDecimal("65").add(new BigDecimal(years - 10).multiply(new BigDecimal("2")));
        else if (years >= 5) score = new BigDecimal("50").add(new BigDecimal(years - 5).multiply(new BigDecimal("3")));
        else score = new BigDecimal("30").add(new BigDecimal(years).multiply(new BigDecimal("5")));
        return score.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 综合得分计算（加权平均）
    // ============================================================
    public BigDecimal calculateTotalScore(ExpertCredibilityScore s) {
        BigDecimal total = BigDecimal.ZERO;
        total = total.add(s.getTitleQl().multiply(s.getWeightTitle()));
        total = total.add(s.getPositionQl().multiply(s.getWeightPosition()));
        total = total.add(s.getEducationExperienceQl().multiply(s.getWeightEducation()));
        total = total.add(s.getAcademicAchievementsQl().multiply(s.getWeightAcademic()));
        total = total.add(s.getResearchAchievementsQl().multiply(s.getWeightResearch()));
        total = total.add(s.getExerciseExperienceQl().multiply(s.getWeightExercise()));
        total = total.add(s.getMilitaryTrainingKnowledgeQl().multiply(s.getWeightMilitaryTraining()));
        total = total.add(s.getSystemSimulationKnowledgeQl().multiply(s.getWeightSystemSimulation()));
        total = total.add(s.getStatisticsKnowledgeQl().multiply(s.getWeightStatistics()));
        total = total.add(s.getProfessionalYearsQt().multiply(s.getWeightProfessionalYears()));
        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 可信度等级判定
    // ============================================================
    public String determineCredibilityLevel(BigDecimal totalScore) {
        if (totalScore.compareTo(new BigDecimal("90")) >= 0) return "A";
        if (totalScore.compareTo(new BigDecimal("75")) >= 0) return "B";
        if (totalScore.compareTo(new BigDecimal("60")) >= 0) return "C";
        return "D";
    }

    // ============================================================
    // 完整评估一个专家
    // ============================================================
    public ExpertCredibilityScore evaluateExpert(ExpertBaseInfo expert) {
        ExpertCredibilityScore score = new ExpertCredibilityScore();
        score.setExpertId(expert.getExpertId());
        score.setExpertName(expert.getExpertName());
        score.setEvaluationDate(LocalDate.now());

        score.setTitleQl(calculateTitleScore(expert));
        score.setPositionQl(calculatePositionScore(expert));
        score.setEducationExperienceQl(calculateEducationScore(expert));
        score.setAcademicAchievementsQl(calculateAcademicScore(expert));
        score.setResearchAchievementsQl(calculateResearchScore(expert));
        score.setExerciseExperienceQl(calculateExerciseScore(expert));
        score.setMilitaryTrainingKnowledgeQl(calculateKnowledgeScore(expert.getHasMilitaryTraining(), expert.getMilitaryTrainingScore()));
        score.setSystemSimulationKnowledgeQl(calculateKnowledgeScore(expert.getHasSystemSimulation(), expert.getSystemSimulationScore()));
        score.setStatisticsKnowledgeQl(calculateKnowledgeScore(expert.getHasStatistics(), expert.getStatisticsScore()));
        score.setProfessionalYearsQt(calculateProfessionalYearsScore(expert));

        BigDecimal totalScore = calculateTotalScore(score);
        score.setTotalScore(totalScore);
        score.setCredibilityLevel(determineCredibilityLevel(totalScore));

        return score;
    }

    // ============================================================
    // 生成评估依据说明
    // ============================================================

    /**
     * 生成评估依据说明（用于前端展示）
     */
    public Map<String, Object> generateEvaluationReasoning(ExpertBaseInfo expert, ExpertCredibilityScore score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();

        // 职称评估依据
        reasoning.put("titleReasoning", generateTitleReasoning(expert, score.getTitleQl()));

        // 职务评估依据
        reasoning.put("positionReasoning", generatePositionReasoning(expert, score.getPositionQl()));

        // 学历评估依据
        reasoning.put("educationReasoning", generateEducationReasoning(expert, score.getEducationExperienceQl()));

        // 学术成果评估依据
        reasoning.put("academicReasoning", generateAcademicReasoning(expert, score.getAcademicAchievementsQl()));

        // 科研成果评估依据
        reasoning.put("researchReasoning", generateResearchReasoning(expert, score.getResearchAchievementsQl()));

        // 演习训练评估依据
        reasoning.put("exerciseReasoning", generateExerciseReasoning(expert, score.getExerciseExperienceQl()));

        // 军事训练学评估依据
        reasoning.put("militaryTrainingReasoning", generateKnowledgeReasoning(
                "军事训练学", expert.getHasMilitaryTraining(), expert.getMilitaryTrainingScore(),
                score.getMilitaryTrainingKnowledgeQl()));

        // 系统仿真评估依据
        reasoning.put("systemSimulationReasoning", generateKnowledgeReasoning(
                "系统仿真", expert.getHasSystemSimulation(), expert.getSystemSimulationScore(),
                score.getSystemSimulationKnowledgeQl()));

        // 数理统计评估依据
        reasoning.put("statisticsReasoning", generateKnowledgeReasoning(
                "数理统计学", expert.getHasStatistics(), expert.getStatisticsScore(),
                score.getStatisticsKnowledgeQl()));

        // 专业年限评估依据
        reasoning.put("professionalYearsReasoning", generateProfessionalYearsReasoning(
                expert.getProfessionalYears(), score.getProfessionalYearsQt()));

        return reasoning;
    }

    private Map<String, Object> generateTitleReasoning(ExpertBaseInfo expert, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);
        reasoning.put("description", "职称评估基于职称等级和荣誉加成");

        List<String> factors = new ArrayList<>();
        String baseTitle = "";

        switch (expert.getTitleLevel() != null ? expert.getTitleLevel() : 0) {
            case 4: baseTitle = "正高职称"; factors.add("正高职称基础分 90"); break;
            case 3: baseTitle = "副高职称"; factors.add("副高职称基础分 75"); break;
            case 2: baseTitle = "中级职称"; factors.add("中级职称基础分 60"); break;
            case 1: baseTitle = "初级职称"; factors.add("初级职称基础分 45"); break;
            default: factors.add("无职称信息");
        }

        if (Boolean.TRUE.equals(expert.getIsAcademician())) {
            factors.add("院士身份 +10");
        }
        if (Boolean.TRUE.equals(expert.getIsYangtzeScholar())) {
            factors.add("长江学者 +8");
        }
        if (Boolean.TRUE.equals(expert.getIsExcellentYouth())) {
            factors.add("国家杰青 +5");
        }
        if (Boolean.TRUE.equals(expert.getIsDoctoralSupervisor())) {
            factors.add("博士生导师 +5");
        }
        if (Boolean.TRUE.equals(expert.getIsMasterSupervisor())) {
            factors.add("硕士生导师 +3");
        }

        reasoning.put("baseTitle", baseTitle);
        reasoning.put("factors", factors);
        reasoning.put("summary", String.join(", ", factors));
        return reasoning;
    }

    private Map<String, Object> generatePositionReasoning(ExpertBaseInfo expert, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);

        String positionLevel = "";
        String baseScore = "";
        switch (expert.getPositionLevel() != null ? expert.getPositionLevel() : 0) {
            case 3: positionLevel = "高层职务"; baseScore = "90"; break;
            case 2: positionLevel = "中层职务"; baseScore = "75"; break;
            case 1: positionLevel = "一般职务"; baseScore = "60"; break;
            default: positionLevel = "其他职务"; baseScore = "50";
        }

        reasoning.put("positionLevel", positionLevel);
        reasoning.put("position", expert.getPosition());
        reasoning.put("baseScore", baseScore);
        reasoning.put("summary", positionLevel + " (" + expert.getPosition() + ")");
        return reasoning;
    }

    private Map<String, Object> generateEducationReasoning(ExpertBaseInfo expert, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);

        List<String> factors = new ArrayList<>();
        String education = "";

        switch (expert.getEducationLevel() != null ? expert.getEducationLevel() : 0) {
            case 3: education = "博士"; factors.add("博士学历 +85"); break;
            case 2: education = "硕士"; factors.add("硕士学历 +70"); break;
            case 1: education = "本科"; factors.add("本科学历 +55"); break;
            default: education = "其他";
        }

        if ("985".equals(expert.getSchoolLevel())) {
            factors.add("985院校 +10");
        } else if ("211".equals(expert.getSchoolLevel())) {
            factors.add("211院校 +5");
        }

        reasoning.put("education", education);
        reasoning.put("schoolLevel", expert.getSchoolLevel());
        reasoning.put("graduatedSchool", expert.getGraduatedSchool());
        reasoning.put("factors", factors);
        reasoning.put("summary", education + "学历，毕业于" + expert.getGraduatedSchool());
        return reasoning;
    }

    private Map<String, Object> generateAcademicReasoning(ExpertBaseInfo expert, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);

        int total = expert.getAcademicCount() != null ? expert.getAcademicCount() : 0;
        int sciEi = expert.getAcademicSciEiCount() != null ? expert.getAcademicSciEiCount() : 0;
        int core = expert.getAcademicCoreCount() != null ? expert.getAcademicCoreCount() : 0;

        List<String> factors = new ArrayList<>();
        String baseDesc = "";

        if (total >= 20) {
            baseDesc = "论文数量丰富(≥20篇)";
            factors.add("论文数量 80分");
        } else if (total >= 10) {
            baseDesc = "论文数量较多(≥10篇)";
            factors.add("论文数量 65分");
        } else if (total >= 5) {
            baseDesc = "论文数量一般(≥5篇)";
            factors.add("论文数量 50分");
        } else if (total >= 1) {
            baseDesc = "有少量论文";
            factors.add("论文数量 35分");
        } else {
            baseDesc = "无论文发表";
            factors.add("论文数量 20分");
        }

        if (sciEi > 0) {
            factors.add("SCI/EI " + sciEi + "篇 ×1分 = +" + sciEi);
        }
        if (core > 0) {
            factors.add("核心期刊 " + core + "篇 ×0.5分 = +" + String.format("%.1f", core * 0.5));
        }

        reasoning.put("totalPapers", total);
        reasoning.put("sciEiPapers", sciEi);
        reasoning.put("corePapers", core);
        reasoning.put("baseDesc", baseDesc);
        reasoning.put("factors", factors);
        reasoning.put("summary", "论文" + total + "篇 (SCI/EI:" + sciEi + ", 核心:" + core + ")");
        return reasoning;
    }

    private Map<String, Object> generateResearchReasoning(ExpertBaseInfo expert, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);

        int rc = expert.getResearchCount() != null ? expert.getResearchCount() : 0;
        int rp = expert.getResearchParticipateCount() != null ? expert.getResearchParticipateCount() : 0;
        int pc = expert.getPatentCount() != null ? expert.getPatentCount() : 0;
        int sc = expert.getSoftwareCopyrightCount() != null ? expert.getSoftwareCopyrightCount() : 0;
        int mc = expert.getMonographCount() != null ? expert.getMonographCount() : 0;

        List<String> factors = new ArrayList<>();

        if (rc > 0) {
            int contributed = Math.min(rc, 4);
            factors.add("主持项目 " + rc + "项 ×15分 = +" + (contributed * 15));
        }
        if (rp > 0) {
            int participated = Math.min(rp, 5);
            factors.add("参与项目 " + rp + "项 ×5分 = +" + (participated * 5));
        }
        if (pc > 0) {
            int patents = Math.min(pc, 6);
            factors.add("专利 " + pc + "项 ×5分 = +" + (patents * 5));
        }
        if (sc > 0) {
            int copyrights = Math.min(sc, 5);
            factors.add("软件著作权 " + sc + "项 ×2分 = +" + (copyrights * 2));
        }
        if (mc > 0) {
            int monographs = Math.min(mc, 2);
            factors.add("专著 " + mc + "部 ×10分 = +" + (monographs * 10));
        }

        reasoning.put("researchCount", rc);
        reasoning.put("patentCount", pc);
        reasoning.put("softwareCopyrightCount", sc);
        reasoning.put("monographCount", mc);
        reasoning.put("factors", factors);
        reasoning.put("summary", "主持" + rc + "项, 参与" + rp + "项, 专利" + pc + "项");
        return reasoning;
    }

    private Map<String, Object> generateExerciseReasoning(ExpertBaseInfo expert, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);

        int nec = expert.getNationalExerciseCount() != null ? expert.getNationalExerciseCount() : 0;
        int nep = expert.getNationalExerciseParticipateCount() != null ? expert.getNationalExerciseParticipateCount() : 0;
        int rec = expert.getRegionalExerciseCount() != null ? expert.getRegionalExerciseCount() : 0;
        int rep = expert.getRegionalExerciseParticipateCount() != null ? expert.getRegionalExerciseParticipateCount() : 0;
        int mpy = expert.getMilitaryPracticeYears() != null ? expert.getMilitaryPracticeYears() : 0;

        List<String> factors = new ArrayList<>();

        if (nec > 0) {
            int count = Math.min(nec, 3);
            factors.add("国家级演习主持 " + nec + "次 ×20分 = +" + (count * 20));
        }
        if (nep > 0) {
            int count = Math.min(nep, 4);
            factors.add("国家级演习参与 " + nep + "次 ×10分 = +" + (count * 10));
        }
        if (rec > 0) {
            int count = Math.min(rec, 4);
            factors.add("省级演习主持 " + rec + "次 ×12分 = +" + (count * 12));
        }
        if (rep > 0) {
            int count = Math.min(rep, 5);
            factors.add("省级演习参与 " + rep + "次 ×6分 = +" + (count * 6));
        }
        if (mpy > 0) {
            int years = Math.min(mpy, 3);
            factors.add("部队实践 " + mpy + "年 ×15分 = +" + (years * 15));
        }

        reasoning.put("nationalCount", nec);
        reasoning.put("regionalCount", rec);
        reasoning.put("practiceYears", mpy);
        reasoning.put("factors", factors);
        reasoning.put("summary", "国家级" + nec + "次, 省级" + (rec + rep) + "次, 实践" + mpy + "年");
        return reasoning;
    }

    private Map<String, Object> generateKnowledgeReasoning(String knowledgeName, Boolean hasKnowledge,
                                                           Integer examScore, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);
        reasoning.put("knowledgeName", knowledgeName);
        reasoning.put("hasKnowledge", hasKnowledge);
        reasoning.put("examScore", examScore);

        List<String> factors = new ArrayList<>();

        if (Boolean.TRUE.equals(hasKnowledge)) {
            if (examScore != null) {
                factors.add("有" + knowledgeName + "知识");
                factors.add("自评(默认85) × 0.3 = 25.5");
                factors.add("考核成绩(" + examScore + ") × 0.7 = " + String.format("%.1f", examScore * 0.7));
                reasoning.put("summary", "有" + knowledgeName + "知识，考核成绩" + examScore + "分");
            } else {
                factors.add("有" + knowledgeName + "知识(无考核成绩)");
                factors.add("按熟练水平计分 82分");
                reasoning.put("summary", "有" + knowledgeName + "知识，按熟练水平计分");
            }
        } else {
            factors.add("无" + knowledgeName + "知识");
            factors.add("基础分 50分");
            reasoning.put("summary", "无" + knowledgeName + "知识");
        }

        reasoning.put("factors", factors);
        return reasoning;
    }

    private Map<String, Object> generateProfessionalYearsReasoning(Integer years, BigDecimal score) {
        Map<String, Object> reasoning = new LinkedHashMap<>();
        reasoning.put("score", score);
        reasoning.put("years", years != null ? years : 0);

        List<String> factors = new ArrayList<>();
        String level = "";

        if (years == null || years <= 0) {
            level = "无专业年限";
            factors.add("无专业年限记录");
            factors.add("基础分 30分");
        } else if (years >= 30) {
            level = "资深专家(30年+)";
            factors.add("30年以上，每增加1年 +0.5分");
            factors.add("基础分 95 + " + (years - 30) + " × 0.5 = " + (95 + (years - 30) * 0.5));
        } else if (years >= 20) {
            level = "专家(20-30年)";
            factors.add("20-30年，每增加1年 +1分");
            factors.add("基础分 85 + " + (years - 20) + " × 1 = " + (85 + years - 20));
        } else if (years >= 10) {
            level = "熟手(10-20年)";
            factors.add("10-20年，每增加1年 +2分");
            factors.add("基础分 65 + " + (years - 10) + " × 2 = " + (65 + (years - 10) * 2));
        } else if (years >= 5) {
            level = "成长(5-10年)";
            factors.add("5-10年，每增加1年 +3分");
            factors.add("基础分 50 + " + (years - 5) + " × 3 = " + (50 + (years - 5) * 3));
        } else {
            level = "新手(5年以下)";
            factors.add("5年以下，每增加1年 +5分");
            factors.add("基础分 30 + " + years + " × 5 = " + (30 + years * 5));
        }

        reasoning.put("level", level);
        reasoning.put("factors", factors);
        reasoning.put("summary", level + "，专业年限" + (years != null ? years : 0) + "年");
        return reasoning;
    }
}
