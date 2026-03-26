package com.ccnu.military.service;

import com.ccnu.military.dto.GlobalWeightsRequest;
import com.ccnu.military.entity.ExpertBaseInfo;
import com.ccnu.military.entity.ExpertCredibilityScore;
import com.ccnu.military.repository.ExpertBaseInfoRepository;
import com.ccnu.military.repository.ExpertCredibilityScoreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 专家可信度评估服务
 * 按照用户提供的打分规则，实现10个维度的自动评分
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExpertCredibilityService {

    private final ExpertBaseInfoRepository expertBaseInfoRepository;
    private final ExpertCredibilityScoreRepository scoreRepository;
    private final ExpertScoringCalculator scoringCalculator;

    // ============================================================
    // 1. 职称得分计算 (title_ql) - 满分100分
    // ============================================================
    private BigDecimal calculateTitleScore(ExpertBaseInfo expert) {
        if (expert.getTitleLevel() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseScore;
        switch (expert.getTitleLevel()) {
            case 4: // 正高
                baseScore = new BigDecimal("90");
                if (Boolean.TRUE.equals(expert.getIsAcademician())) {
                    baseScore = baseScore.add(new BigDecimal("10")); // 院士+10
                }
                if (Boolean.TRUE.equals(expert.getIsYangtzeScholar())) {
                    baseScore = baseScore.add(new BigDecimal("8")); // 长江学者+8
                }
                if (Boolean.TRUE.equals(expert.getIsExcellentYouth())) {
                    baseScore = baseScore.add(new BigDecimal("5")); // 杰青+5
                }
                break;
            case 3: // 副高
                baseScore = new BigDecimal("75");
                if (Boolean.TRUE.equals(expert.getIsDoctoralSupervisor())) {
                    baseScore = baseScore.add(new BigDecimal("5")); // 博导+5
                }
                if (Boolean.TRUE.equals(expert.getIsMasterSupervisor())) {
                    baseScore = baseScore.add(new BigDecimal("3")); // 硕导+3
                }
                break;
            case 2: // 中级
                baseScore = new BigDecimal("60");
                break;
            case 1: // 初级
                baseScore = new BigDecimal("45");
                break;
            default:
                baseScore = BigDecimal.ZERO;
        }
        return baseScore.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 2. 职务得分计算 (position_ql) - 满分100分
    // ============================================================
    private BigDecimal calculatePositionScore(ExpertBaseInfo expert) {
        if (expert.getPositionLevel() == null) {
            return BigDecimal.ZERO;
        }
        switch (expert.getPositionLevel()) {
            case 3: // 高层
                return new BigDecimal("90").setScale(2, RoundingMode.HALF_UP);
            case 2: // 中层
                return new BigDecimal("75").setScale(2, RoundingMode.HALF_UP);
            case 1: // 一般
                return new BigDecimal("60").setScale(2, RoundingMode.HALF_UP);
            default:
                return new BigDecimal("50").setScale(2, RoundingMode.HALF_UP);
        }
    }

    // ============================================================
    // 3. 学习经历得分 (education_experience_ql) - 满分100分
    // ============================================================
    private BigDecimal calculateEducationScore(ExpertBaseInfo expert) {
        if (expert.getEducationLevel() == null) {
            return BigDecimal.ZERO;
        }
        BigDecimal baseScore;
        switch (expert.getEducationLevel()) {
            case 3: // 博士
                baseScore = new BigDecimal("85");
                if ("985".equals(expert.getSchoolLevel())) {
                    baseScore = baseScore.add(new BigDecimal("10"));
                } else if ("211".equals(expert.getSchoolLevel())) {
                    baseScore = baseScore.add(new BigDecimal("5"));
                }
                break;
            case 2: // 硕士
                baseScore = new BigDecimal("70");
                if ("985".equals(expert.getSchoolLevel())) {
                    baseScore = baseScore.add(new BigDecimal("8"));
                } else if ("211".equals(expert.getSchoolLevel())) {
                    baseScore = baseScore.add(new BigDecimal("4"));
                }
                break;
            case 1: // 本科
                baseScore = new BigDecimal("55");
                if ("985".equals(expert.getSchoolLevel())) {
                    baseScore = baseScore.add(new BigDecimal("6"));
                } else if ("211".equals(expert.getSchoolLevel())) {
                    baseScore = baseScore.add(new BigDecimal("3"));
                }
                break;
            default:
                return BigDecimal.ZERO;
        }
        return baseScore.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 4. 学术成果得分 (academic_achievements_ql) - 满分100分
    // ============================================================
    private BigDecimal calculateAcademicScore(ExpertBaseInfo expert) {
        int total = expert.getAcademicCount() != null ? expert.getAcademicCount() : 0;
        int sciEiCount = expert.getAcademicSciEiCount() != null ? expert.getAcademicSciEiCount() : 0;
        int coreCount = expert.getAcademicCoreCount() != null ? expert.getAcademicCoreCount() : 0;

        BigDecimal baseScore;
        if (total >= 20) {
            baseScore = new BigDecimal("80");
        } else if (total >= 10) {
            baseScore = new BigDecimal("65");
        } else if (total >= 5) {
            baseScore = new BigDecimal("50");
        } else if (total >= 1) {
            baseScore = new BigDecimal("35");
        } else {
            baseScore = new BigDecimal("20");
        }

        // SCI/EI每篇+1分，核心每篇+0.5分
        BigDecimal additional = new BigDecimal(sciEiCount).multiply(new BigDecimal("1"))
                .add(new BigDecimal(coreCount).multiply(new BigDecimal("0.5")));

        return baseScore.add(additional).min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 5. 科研成果得分 (research_achievements_ql) - 满分100分
    // ============================================================
    private BigDecimal calculateResearchScore(ExpertBaseInfo expert) {
        int researchCount = expert.getResearchCount() != null ? expert.getResearchCount() : 0;
        int researchParticipate = expert.getResearchParticipateCount() != null ? expert.getResearchParticipateCount() : 0;
        int patentCount = expert.getPatentCount() != null ? expert.getPatentCount() : 0;
        int softwareCount = expert.getSoftwareCopyrightCount() != null ? expert.getSoftwareCopyrightCount() : 0;
        int monographCount = expert.getMonographCount() != null ? expert.getMonographCount() : 0;

        BigDecimal score = BigDecimal.ZERO;

        // 国家级科研项目(主持) +15分/项，上限50
        score = score.add(new BigDecimal(Math.min(researchCount, 4) * 15));
        // 国家级科研项目(参与) +5分/项，上限25
        score = score.add(new BigDecimal(Math.min(researchParticipate, 5) * 5));
        // 发明专利(授权) +5分/项，上限30
        score = score.add(new BigDecimal(Math.min(patentCount, 6) * 5));
        // 软件著作权 +2分/项，上限10
        score = score.add(new BigDecimal(Math.min(softwareCount, 5) * 2));
        // 专著/教材 +10分/本，上限20
        score = score.add(new BigDecimal(Math.min(monographCount, 2) * 10));

        return score.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 6. 演习训练经历得分 (exercise_experience_ql) - 满分100分
    // ============================================================
    private BigDecimal calculateExerciseScore(ExpertBaseInfo expert) {
        int nationalCount = expert.getNationalExerciseCount() != null ? expert.getNationalExerciseCount() : 0;
        int nationalPartCount = expert.getNationalExerciseParticipateCount() != null ? expert.getNationalExerciseParticipateCount() : 0;
        int regionalCount = expert.getRegionalExerciseCount() != null ? expert.getRegionalExerciseCount() : 0;
        int regionalPartCount = expert.getRegionalExerciseParticipateCount() != null ? expert.getRegionalExerciseParticipateCount() : 0;
        int practiceYears = expert.getMilitaryPracticeYears() != null ? expert.getMilitaryPracticeYears() : 0;

        BigDecimal score = BigDecimal.ZERO;

        // 国家级演习(主持) +20分/次，上限60
        score = score.add(new BigDecimal(Math.min(nationalCount, 3) * 20));
        // 国家级演习(参与) +10分/次，上限40
        score = score.add(new BigDecimal(Math.min(nationalPartCount, 4) * 10));
        // 省级/战区级演习(主持) +12分/次，上限50
        score = score.add(new BigDecimal(Math.min(regionalCount, 4) * 12));
        // 省级/战区级演习(参与) +6分/次，上限30
        score = score.add(new BigDecimal(Math.min(regionalPartCount, 5) * 6));
        // 部队实践/挂职 +15分/年，上限45
        score = score.add(new BigDecimal(Math.min(practiceYears, 3) * 15));

        return score.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 7. 军事训练学知识得分 - 满分100分
    // 8. 系统仿真知识得分 - 满分100分
    // 9. 数理统计学知识得分 - 满分100分
    // 综合得分 = 自评×0.3 + 考核×0.7
    // ============================================================
    private BigDecimal calculateKnowledgeScore(Boolean hasKnowledge, Integer examScore) {
        if (Boolean.TRUE.equals(hasKnowledge)) {
            // 有该知识基础，根据考核成绩计算
            if (examScore != null) {
                // 综合得分 = 自评(默认85) × 0.3 + 考核成绩 × 0.7
                BigDecimal selfScore = new BigDecimal("85"); // 有知识基础自评默认为熟练水平
                return selfScore.multiply(new BigDecimal("0.3"))
                        .add(new BigDecimal(examScore).multiply(new BigDecimal("0.7")))
                        .setScale(2, RoundingMode.HALF_UP);
            } else {
                // 有知识但无考核成绩，按熟练水平给分
                return new BigDecimal("82").setScale(2, RoundingMode.HALF_UP);
            }
        } else {
            // 无该知识基础
            return new BigDecimal("50").setScale(2, RoundingMode.HALF_UP);
        }
    }

    // ============================================================
    // 10. 专业年限得分 (professional_years_qt) - 满分100分
    // ============================================================
    private BigDecimal calculateProfessionalYearsScore(ExpertBaseInfo expert) {
        Integer years = expert.getProfessionalYears();
        if (years == null || years <= 0) {
            return new BigDecimal("30").setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal score;
        if (years >= 30) {
            score = new BigDecimal("95").add(new BigDecimal(years - 30).multiply(new BigDecimal("0.5")));
        } else if (years >= 20) {
            score = new BigDecimal("85").add(new BigDecimal(years - 20).multiply(new BigDecimal("1")));
        } else if (years >= 10) {
            score = new BigDecimal("65").add(new BigDecimal(years - 10).multiply(new BigDecimal("2")));
        } else if (years >= 5) {
            score = new BigDecimal("50").add(new BigDecimal(years - 5).multiply(new BigDecimal("3")));
        } else {
            score = new BigDecimal("30").add(new BigDecimal(years).multiply(new BigDecimal("5")));
        }

        return score.min(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 综合得分计算（加权平均）
    // ============================================================
    private BigDecimal calculateTotalScore(ExpertCredibilityScore score) {
        BigDecimal total = BigDecimal.ZERO;

        total = total.add(score.getTitleQl().multiply(score.getWeightTitle()));
        total = total.add(score.getPositionQl().multiply(score.getWeightPosition()));
        total = total.add(score.getEducationExperienceQl().multiply(score.getWeightEducation()));
        total = total.add(score.getAcademicAchievementsQl().multiply(score.getWeightAcademic()));
        total = total.add(score.getResearchAchievementsQl().multiply(score.getWeightResearch()));
        total = total.add(score.getExerciseExperienceQl().multiply(score.getWeightExercise()));
        total = total.add(score.getMilitaryTrainingKnowledgeQl().multiply(score.getWeightMilitaryTraining()));
        total = total.add(score.getSystemSimulationKnowledgeQl().multiply(score.getWeightSystemSimulation()));
        total = total.add(score.getStatisticsKnowledgeQl().multiply(score.getWeightStatistics()));
        total = total.add(score.getProfessionalYearsQt().multiply(score.getWeightProfessionalYears()));

        return total.setScale(2, RoundingMode.HALF_UP);
    }

    // ============================================================
    // 可信度等级判定
    // ============================================================
    private String determineCredibilityLevel(BigDecimal totalScore) {
        if (totalScore.compareTo(new BigDecimal("90")) >= 0) {
            return "A";
        } else if (totalScore.compareTo(new BigDecimal("75")) >= 0) {
            return "B";
        } else if (totalScore.compareTo(new BigDecimal("60")) >= 0) {
            return "C";
        } else {
            return "D";
        }
    }

    // ============================================================
    // 公开方法
    // ============================================================

    /**
     * 对单个专家进行可信度评估
     */
    @Transactional
    public ExpertCredibilityScore evaluateExpert(Long expertId) {
        ExpertBaseInfo expert = expertBaseInfoRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("专家不存在: " + expertId));

        return evaluateAndSave(expert);
    }

    /**
     * 对单个专家进行可信度评估（根据姓名）
     */
    @Transactional
    public ExpertCredibilityScore evaluateExpertByName(String expertName) {
        ExpertBaseInfo expert = expertBaseInfoRepository.findByExpertName(expertName)
                .orElseThrow(() -> new IllegalArgumentException("专家不存在: " + expertName));

        return evaluateAndSave(expert);
    }

    /**
     * 评估并保存结果
     */
    private ExpertCredibilityScore evaluateAndSave(ExpertBaseInfo expert) {
        ExpertCredibilityScore score = new ExpertCredibilityScore();
        score.setExpertId(expert.getExpertId());
        score.setExpertName(expert.getExpertName());
        score.setEvaluationDate(LocalDate.now());

        // 计算10个维度得分
        score.setTitleQl(calculateTitleScore(expert));
        score.setPositionQl(calculatePositionScore(expert));
        score.setEducationExperienceQl(calculateEducationScore(expert));
        score.setAcademicAchievementsQl(calculateAcademicScore(expert));
        score.setResearchAchievementsQl(calculateResearchScore(expert));
        score.setExerciseExperienceQl(calculateExerciseScore(expert));
        score.setMilitaryTrainingKnowledgeQl(
                calculateKnowledgeScore(expert.getHasMilitaryTraining(), expert.getMilitaryTrainingScore()));
        score.setSystemSimulationKnowledgeQl(
                calculateKnowledgeScore(expert.getHasSystemSimulation(), expert.getSystemSimulationScore()));
        score.setStatisticsKnowledgeQl(
                calculateKnowledgeScore(expert.getHasStatistics(), expert.getStatisticsScore()));
        score.setProfessionalYearsQt(calculateProfessionalYearsScore(expert));

        // 计算综合得分
        BigDecimal totalScore = calculateTotalScore(score);
        score.setTotalScore(totalScore);
        score.setCredibilityLevel(determineCredibilityLevel(totalScore));

        // 删除旧评估记录并保存新记录
        scoreRepository.deleteByExpertId(expert.getExpertId());
        return scoreRepository.save(score);
    }

    /**
     * 批量评估所有专家
     */
    @Transactional
    public List<ExpertCredibilityScore> evaluateAllExperts() {
        List<ExpertBaseInfo> experts = expertBaseInfoRepository.findAllActiveExperts();
        return experts.stream()
                .map(this::evaluateAndSave)
                .collect(Collectors.toList());
    }

    /**
     * 批量评估指定专家
     */
    @Transactional
    public Map<String, Object> evaluateBatch(List<Long> expertIds, boolean overwrite) {
        List<ExpertBaseInfo> experts = expertBaseInfoRepository.findAllById(expertIds);
        List<ExpertCredibilityScore> successScores = new ArrayList<>();
        List<Long> failedIds = new ArrayList<>();
        int skipped = 0;

        for (ExpertBaseInfo expert : experts) {
            try {
                // 检查是否已有评估记录
                Optional<ExpertCredibilityScore> existingScore = scoreRepository.findByExpertId(expert.getExpertId());
                if (existingScore.isPresent() && !overwrite) {
                    skipped++;
                    successScores.add(existingScore.get());
                    continue;
                }
                ExpertCredibilityScore score = evaluateAndSave(expert);
                successScores.add(score);
            } catch (Exception e) {
                log.error("评估专家失败: {}", expert.getExpertName(), e);
                failedIds.add(expert.getExpertId());
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("successCount", successScores.size());
        result.put("skippedCount", skipped);
        result.put("failedCount", failedIds.size());
        result.put("failedIds", failedIds);
        result.put("scores", successScores);
        return result;
    }

    /**
     * 获取所有评估结果（带专家信息）
     */
    public List<ExpertCredibilityScore> getAllScores() {
        return scoreRepository.findAllScoresOrderByTotalScore();
    }

    /**
     * 根据专家ID获取评估结果
     */
    public Optional<ExpertCredibilityScore> getScoreByExpertId(Long expertId) {
        return scoreRepository.findByExpertId(expertId);
    }

    /**
     * 获取所有启用的专家列表
     */
    public List<ExpertBaseInfo> getAllActiveExperts() {
        return expertBaseInfoRepository.findAllActiveExperts();
    }

    /**
     * 保存或更新专家基础信息
     */
    @Transactional
    public ExpertBaseInfo saveExpert(ExpertBaseInfo expert) {
        return expertBaseInfoRepository.save(expert);
    }

    /**
     * 根据ID删除专家（同时删除评估记录）
     */
    @Transactional
    public void deleteExpert(Long expertId) {
        scoreRepository.deleteByExpertId(expertId);
        expertBaseInfoRepository.deleteById(expertId);
    }

    /**
     * 根据可信度等级筛选
     */
    public List<ExpertCredibilityScore> getScoresByLevel(String level) {
        return scoreRepository.findByCredibilityLevelOrderByTotalScoreDesc(level);
    }

    /**
     * 统一更新所有评估记录的十维权重，并按新权重重算综合得分与可信度等级
     */
    @Transactional
    public Map<String, Object> applyGlobalWeights(GlobalWeightsRequest req) {
        if (req.getWeightTitle() == null || req.getWeightPosition() == null
                || req.getWeightEducation() == null || req.getWeightAcademic() == null
                || req.getWeightResearch() == null || req.getWeightExercise() == null
                || req.getWeightMilitaryTraining() == null || req.getWeightSystemSimulation() == null
                || req.getWeightStatistics() == null || req.getWeightProfessionalYears() == null) {
            throw new IllegalArgumentException("请填写全部10项权重");
        }
        BigDecimal sum = req.getWeightTitle().add(req.getWeightPosition())
                .add(req.getWeightEducation()).add(req.getWeightAcademic())
                .add(req.getWeightResearch()).add(req.getWeightExercise())
                .add(req.getWeightMilitaryTraining()).add(req.getWeightSystemSimulation())
                .add(req.getWeightStatistics()).add(req.getWeightProfessionalYears());
        if (sum.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("权重之和须大于0");
        }
        RoundingMode mode = RoundingMode.HALF_UP;
        BigDecimal wTitle = req.getWeightTitle().divide(sum, 6, mode);
        BigDecimal wPos = req.getWeightPosition().divide(sum, 6, mode);
        BigDecimal wEdu = req.getWeightEducation().divide(sum, 6, mode);
        BigDecimal wAca = req.getWeightAcademic().divide(sum, 6, mode);
        BigDecimal wRes = req.getWeightResearch().divide(sum, 6, mode);
        BigDecimal wExe = req.getWeightExercise().divide(sum, 6, mode);
        BigDecimal wMil = req.getWeightMilitaryTraining().divide(sum, 6, mode);
        BigDecimal wSim = req.getWeightSystemSimulation().divide(sum, 6, mode);
        BigDecimal wStat = req.getWeightStatistics().divide(sum, 6, mode);
        BigDecimal wYears = req.getWeightProfessionalYears().divide(sum, 6, mode);

        List<ExpertCredibilityScore> list = scoreRepository.findAll();
        if (list.isEmpty()) {
            Map<String, Object> empty = new HashMap<>();
            empty.put("updated", 0);
            empty.put("message", "暂无评估记录");
            return empty;
        }
        for (ExpertCredibilityScore s : list) {
            s.setWeightTitle(wTitle.setScale(2, mode));
            s.setWeightPosition(wPos.setScale(2, mode));
            s.setWeightEducation(wEdu.setScale(2, mode));
            s.setWeightAcademic(wAca.setScale(2, mode));
            s.setWeightResearch(wRes.setScale(2, mode));
            s.setWeightExercise(wExe.setScale(2, mode));
            s.setWeightMilitaryTraining(wMil.setScale(2, mode));
            s.setWeightSystemSimulation(wSim.setScale(2, mode));
            s.setWeightStatistics(wStat.setScale(2, mode));
            s.setWeightProfessionalYears(wYears.setScale(2, mode));

            BigDecimal total = scoringCalculator.calculateTotalScore(s);
            s.setTotalScore(total);
            s.setCredibilityLevel(scoringCalculator.determineCredibilityLevel(total));
            scoreRepository.save(s);
        }
        Map<String, Object> out = new HashMap<>();
        out.put("updated", list.size());
        return out;
    }

    /**
     * 获取评分详情（包含10个维度的详细得分）
     */
    public Map<String, Object> getEvaluationDetails(Long expertId) {
        ExpertBaseInfo expert = expertBaseInfoRepository.findById(expertId)
                .orElseThrow(() -> new IllegalArgumentException("专家不存在: " + expertId));

        ExpertCredibilityScore score = scoreRepository.findByExpertId(expertId)
                .orElse(null);

        Map<String, Object> details = new HashMap<>();
        details.put("expert", expert);
        details.put("score", score);

        if (score != null) {
            Map<String, BigDecimal> dimensionScores = new LinkedHashMap<>();
            dimensionScores.put("职称得分", score.getTitleQl());
            dimensionScores.put("职务得分", score.getPositionQl());
            dimensionScores.put("学习经历得分", score.getEducationExperienceQl());
            dimensionScores.put("学术成果得分", score.getAcademicAchievementsQl());
            dimensionScores.put("科研成果得分", score.getResearchAchievementsQl());
            dimensionScores.put("演习训练得分", score.getExerciseExperienceQl());
            dimensionScores.put("军事训练学知识得分", score.getMilitaryTrainingKnowledgeQl());
            dimensionScores.put("系统仿真知识得分", score.getSystemSimulationKnowledgeQl());
            dimensionScores.put("数理统计学知识得分", score.getStatisticsKnowledgeQl());
            dimensionScores.put("专业年限得分", score.getProfessionalYearsQt());
            dimensionScores.put("综合可信度得分", score.getTotalScore());

            Map<String, BigDecimal> weights = new LinkedHashMap<>();
            weights.put("职称权重", score.getWeightTitle());
            weights.put("职务权重", score.getWeightPosition());
            weights.put("学习经历权重", score.getWeightEducation());
            weights.put("学术成果权重", score.getWeightAcademic());
            weights.put("科研成果权重", score.getWeightResearch());
            weights.put("演习训练权重", score.getWeightExercise());
            weights.put("军事训练学权重", score.getWeightMilitaryTraining());
            weights.put("系统仿真权重", score.getWeightSystemSimulation());
            weights.put("数理统计权重", score.getWeightStatistics());
            weights.put("专业年限权重", score.getWeightProfessionalYears());

            details.put("dimensionScores", dimensionScores);
            details.put("weights", weights);
            details.put("credibilityLevel", score.getCredibilityLevel());

            // 生成评估依据说明
            Map<String, Object> reasoning = scoringCalculator.generateEvaluationReasoning(expert, score);
            details.put("reasoning", reasoning);
        }

        return details;
    }

    /**
     * 获取专家列表（含评估状态）
     * 返回已评估和未评估的专家列表
     */
    public Map<String, Object> getExpertsWithStatus() {
        List<ExpertBaseInfo> allExperts = expertBaseInfoRepository.findAllActiveExperts();
        List<ExpertCredibilityScore> allScores = scoreRepository.findAll();

        // 创建 expertId -> score 的映射
        Map<Long, ExpertCredibilityScore> scoreMap = allScores.stream()
                .collect(Collectors.toMap(ExpertCredibilityScore::getExpertId, s -> s, (a, b) -> b));

        List<Map<String, Object>> evaluatedList = new ArrayList<>();
        List<Map<String, Object>> unevaluatedList = new ArrayList<>();

        for (ExpertBaseInfo expert : allExperts) {
            ExpertCredibilityScore score = scoreMap.get(expert.getExpertId());
            Map<String, Object> item = new HashMap<>();
            item.put("expertId", expert.getExpertId());
            item.put("expertName", expert.getExpertName());
            item.put("title", expert.getTitle());
            item.put("education", expert.getEducation());
            item.put("workUnit", expert.getWorkUnit());
            item.put("hasScore", score != null);

            if (score != null) {
                item.put("totalScore", score.getTotalScore());
                item.put("credibilityLevel", score.getCredibilityLevel());
                item.put("evaluationDate", score.getEvaluationDate());
                // 十维得分（供列表表格展示，与 ExpertCredibilityScore 字段名一致供前端使用）
                item.put("titleQl", score.getTitleQl());
                item.put("positionQl", score.getPositionQl());
                item.put("educationExperienceQl", score.getEducationExperienceQl());
                item.put("academicAchievementsQl", score.getAcademicAchievementsQl());
                item.put("researchAchievementsQl", score.getResearchAchievementsQl());
                item.put("exerciseExperienceQl", score.getExerciseExperienceQl());
                item.put("militaryTrainingKnowledgeQl", score.getMilitaryTrainingKnowledgeQl());
                item.put("systemSimulationKnowledgeQl", score.getSystemSimulationKnowledgeQl());
                item.put("statisticsKnowledgeQl", score.getStatisticsKnowledgeQl());
                item.put("professionalYearsQt", score.getProfessionalYearsQt());
                evaluatedList.add(item);
            } else {
                unevaluatedList.add(item);
            }
        }

        // 按综合得分降序排列
        evaluatedList.sort((a, b) -> {
            BigDecimal scoreA = (BigDecimal) a.get("totalScore");
            BigDecimal scoreB = (BigDecimal) b.get("totalScore");
            return scoreB.compareTo(scoreA);
        });

        Map<String, Object> result = new HashMap<>();
        result.put("evaluated", evaluatedList);
        result.put("unevaluated", unevaluatedList);
        result.put("evaluatedCount", evaluatedList.size());
        result.put("unevaluatedCount", unevaluatedList.size());
        result.put("totalCount", allExperts.size());

        return result;
    }

    /**
     * 获取评估统计信息
     */
    public Map<String, Object> getStatistics() {
        List<ExpertCredibilityScore> allScores = scoreRepository.findAll();
        List<ExpertBaseInfo> allExperts = expertBaseInfoRepository.findAllActiveExperts();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalExperts", allExperts.size());
        stats.put("evaluatedCount", allScores.size());
        stats.put("unevaluatedCount", allExperts.size() - allScores.size());

        // 各等级统计
        Map<String, Long> levelCount = new HashMap<>();
        levelCount.put("A", 0L);
        levelCount.put("B", 0L);
        levelCount.put("C", 0L);
        levelCount.put("D", 0L);

        BigDecimal totalScoreSum = BigDecimal.ZERO;
        for (ExpertCredibilityScore score : allScores) {
            String level = score.getCredibilityLevel();
            levelCount.put(level, levelCount.getOrDefault(level, 0L) + 1);
            totalScoreSum = totalScoreSum.add(score.getTotalScore());
        }

        stats.put("levelCount", levelCount);

        // 平均分
        if (!allScores.isEmpty()) {
            BigDecimal avgScore = totalScoreSum.divide(new BigDecimal(allScores.size()), 2, RoundingMode.HALF_UP);
            stats.put("averageScore", avgScore);
        } else {
            stats.put("averageScore", BigDecimal.ZERO);
        }

        return stats;
    }
}
