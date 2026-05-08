package com.example.recruitment.service.impl;

import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.dto.JobRecommendDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.JobMapper;
import com.example.recruitment.service.AIService;
import com.example.recruitment.service.JobService;
import com.example.recruitment.vo.AIFeedbackVO;
import com.example.recruitment.vo.JobRecommendVO;
import com.example.recruitment.vo.JobTrendVO;
import com.example.recruitment.vo.JobStatVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 岗位服务实现
 * 优化要点：
 * 1. 抽取公共配置常量到静态内部类
 * 2. 重构薪资预测逻辑，提取独立方法
 * 3. 优化岗位推荐算法，提高匹配精度
 * 4. 简化AI分析报告生成流程
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;
    private final AIService aiService;

    /**
     * 薪资因子配置（城市、经验、学历）
     */
    private static final class SalaryFactors {
        // 城市薪资系数（以二线城市为基准1.0）
        static final Map<String, Double> CITY = Map.ofEntries(
            Map.entry("北京", 1.8), Map.entry("上海", 1.75), Map.entry("深圳", 1.65), Map.entry("杭州", 1.45),
            Map.entry("广州", 1.35), Map.entry("南京", 1.25), Map.entry("成都", 1.15), Map.entry("武汉", 1.12),
            Map.entry("苏州", 1.30), Map.entry("重庆", 1.10), Map.entry("西安", 1.08), Map.entry("天津", 1.18),
            Map.entry("长沙", 1.05), Map.entry("郑州", 1.02), Map.entry("东莞", 1.28)
        );

        // 经验薪资系数
        static final Map<String, Double> EXPERIENCE = Map.of(
            "应届", 0.70, "经验不限", 0.85, "1-3年", 0.90,
            "3-5年", 1.20, "5-10年", 1.50, "10年以上", 1.80
        );

        // 学历薪资系数
        static final Map<String, Double> EDUCATION = Map.of(
            "大专", 0.80, "本科", 1.00, "硕士", 1.35,
            "博士", 1.70, "不限", 0.95
        );

        // 高薪技能列表
        static final Set<String> PREMIUM_SKILLS = Set.of(
            "算法", "大数据", "人工智能", "机器学习", "深度学习", "Go", "架构师"
        );

        // 权重配置
        static final double CITY_WEIGHT = 0.4;
        static final double EXP_WEIGHT = 0.35;
        static final double EDU_WEIGHT = 0.25;
        static final double PREMIUM_SKILL_BONUS = 0.05;
        static final double MAX_PREMIUM_BONUS = 0.15;
    }

    /**
     * 技能权重配置
     */
    private static final Map<String, Integer> SKILL_WEIGHTS = Map.ofEntries(
        Map.entry("人工智能", 12), Map.entry("AI", 12), Map.entry("算法", 11),
        Map.entry("机器学习", 11), Map.entry("深度学习", 11), Map.entry("LLM", 11),
        Map.entry("大模型", 11), Map.entry("Java", 10), Map.entry("Python", 10),
        Map.entry("Go", 9), Map.entry("Golang", 9), Map.entry("大数据", 9),
        Map.entry("SpringBoot", 9), Map.entry("SpringCloud", 9), Map.entry("Kubernetes", 9),
        Map.entry("K8s", 9), Map.entry("DevOps", 9), Map.entry("前端", 8),
        Map.entry("Vue3", 8), Map.entry("Vue", 8), Map.entry("React", 8),
        Map.entry("TypeScript", 8), Map.entry("Node.js", 8), Map.entry("Spring", 8),
        Map.entry("微服务", 8), Map.entry("MySQL", 7), Map.entry("Redis", 7),
        Map.entry("MongoDB", 7), Map.entry("Elasticsearch", 7), Map.entry("Kafka", 7),
        Map.entry("Spark", 7), Map.entry("Flink", 7), Map.entry("Hadoop", 7),
        Map.entry("Linux", 7), Map.entry("Docker", 7), Map.entry("CI/CD", 7),
        Map.entry("测试", 6), Map.entry("自动化测试", 6), Map.entry("运维", 6),
        Map.entry("C++", 7), Map.entry("PHP", 5), Map.entry(".NET", 5),
        Map.entry("Rust", 8), Map.entry("TensorFlow", 10), Map.entry("PyTorch", 10),
        Map.entry("数据分析", 9)
    );

    /**
     * 学历等级映射
     */
    private static final Map<String, Integer> EDUCATION_LEVEL = Map.of(
        "初中及以下", 1, "高中", 2, "中专", 3, "大专", 4,
        "本科", 5, "硕士", 6, "博士", 7, "不限", 0
    );

    /**
     * 经验年限映射
     */
    private static final Map<String, Integer> EXPERIENCE_YEARS = Map.of(
        "应届", 0, "经验不限", 0, "1年以下", 0, "1-3年", 2,
        "3-5年", 4, "5-10年", 7, "10年以上", 10, "不限", 0
    );

    /**
     * 推荐算法权重配置
     */
    private static final double SKILL_WEIGHT = 0.7;
    private static final double EDUCATION_WEIGHT = 0.2;
    private static final double EXPERIENCE_WEIGHT = 0.1;

    @Override
    public void addJob(Job job) {
        job.setCreatedAt(java.time.LocalDateTime.now());
        jobMapper.insert(job);
    }

    @Override
    public void updateJob(Job job) {
        if (job.getId() == null) {
            throw new BusinessException("ID 不能为空");
        }
        jobMapper.update(job);
    }

    @Override
    public void deleteJob(Long id) {
        jobMapper.deleteById(id);
    }

    @Override
    public int batchDeleteJobs(List<Long> ids) {
        return (ids == null || ids.isEmpty()) ? 0 : jobMapper.batchDelete(ids);
    }

    @Override
    public Job getJob(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public PageInfo<Job> listJobs(JobQueryDTO dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        return new PageInfo<>(jobMapper.selectByCondition(dto));
    }

    @Override
    public List<JobStatVO> statByCity() { return jobMapper.statByCity(); }
    @Override
    public List<JobStatVO> statByCompany() { return jobMapper.statByCompany(); }
    @Override
    public List<JobStatVO> statBySkill() { return jobMapper.statBySkill(); }
    @Override
    public List<JobStatVO> statBySalaryRange() { return jobMapper.statBySalaryRange(); }
    @Override
    public List<JobStatVO> statByEducation() { return jobMapper.statByEducation(); }
    @Override
    public List<JobStatVO> statByExperience() { return jobMapper.statByExperience(); }
    @Override
    public List<JobStatVO> statByStatus() { return jobMapper.statByStatus(); }
    @Override
    public List<JobStatVO> statTopTitles() { return jobMapper.statTopTitles(); }
    @Override
    public JobTrendVO jobTrendLast7Days() { return jobMapper.jobTrendLast7Days(); }

    /**
     * 薪资预测（优化版）
     */
    @Override
    public BigDecimal predictSalary(String city, String experience, String education, String skills) {
        // 获取基础平均薪资
        BigDecimal baseAvg = jobMapper.predictSalary(city, experience, education, skills);
        if (baseAvg == null || baseAvg.compareTo(BigDecimal.ZERO) == 0) {
            baseAvg = new BigDecimal("12000");
        }

        double factor = calculateSalaryFactor(city, experience, education, skills);
        BigDecimal predicted = baseAvg.multiply(BigDecimal.valueOf(factor));

        // 确保预测值在合理范围内
        return predicted.max(new BigDecimal("5000"))
                       .min(new BigDecimal("100000"))
                       .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * 计算薪资因子（独立抽取）
     */
    private double calculateSalaryFactor(String city, String experience, String education, String skills) {
        double factor = 1.0;
        int weightCount = 0;

        // 城市加权因子
        if (city != null && !city.isBlank() && SalaryFactors.CITY.containsKey(city)) {
            factor += (SalaryFactors.CITY.get(city) - 1.0) * SalaryFactors.CITY_WEIGHT;
            weightCount++;
        }

        // 经验加权因子
        if (experience != null && !experience.isBlank() && SalaryFactors.EXPERIENCE.containsKey(experience)) {
            factor += (SalaryFactors.EXPERIENCE.get(experience) - 1.0) * SalaryFactors.EXP_WEIGHT;
            weightCount++;
        }

        // 学历加权因子
        if (education != null && !education.isBlank() && SalaryFactors.EDUCATION.containsKey(education)) {
            factor += (SalaryFactors.EDUCATION.get(education) - 1.0) * SalaryFactors.EDU_WEIGHT;
            weightCount++;
        }

        // 技能溢价加成
        if (skills != null && !skills.isBlank()) {
            long premiumCount = Arrays.stream(skills.split(","))
                    .map(String::trim)
                    .filter(SalaryFactors.PREMIUM_SKILLS::contains)
                    .count();
            if (premiumCount > 0) {
                factor += Math.min(premiumCount * SalaryFactors.PREMIUM_SKILL_BONUS, SalaryFactors.MAX_PREMIUM_BONUS);
                weightCount++;
            }
        }

        return weightCount == 0 ? 1.0 : factor;
    }

    @Override
    public List<Job> recommendJobs(String skills, String education, Integer experience, String city) {
        JobQueryDTO dto = new JobQueryDTO();
        dto.setCity(city);
        dto.setPageNum(1);
        dto.setPageSize(100);
        List<Job> list = jobMapper.selectByCondition(dto);

        if (skills == null || skills.isEmpty()) {
            return list.stream().limit(10).collect(Collectors.toList());
        }

        Set<String> userSkills = parseSkills(skills);

        Comparator<Job> comparator = (education != null && !education.isEmpty() && experience != null)
            ? Comparator.comparingDouble(job -> calculateCompositeScore(job, userSkills, education, experience))
            : Comparator.comparingDouble(job -> calculateJaccardSimilarity(userSkills, job.getSkills()) * 100);

        return list.stream()
                .sorted(comparator.reversed())
                .limit(10)
                .collect(Collectors.toList());
    }

    /**
     * 解析技能字符串为集合
     */
    private Set<String> parseSkills(String skills) {
        return Arrays.stream(skills.toLowerCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
    }

    /**
     * 计算综合匹配分数
     */
    private double calculateCompositeScore(Job job, Set<String> userSkills, String education, Integer experience) {
        double skillScore = calculateJaccardSimilarity(userSkills, job.getSkills()) * 100;
        double eduScore = calculateEducationMatchScore(education, job.getEducation()).doubleValue();
        double expScore = calculateExperienceMatchScore(experience, job.getExperience()).doubleValue();
        return skillScore * SKILL_WEIGHT + eduScore * EDUCATION_WEIGHT + expScore * EXPERIENCE_WEIGHT;
    }

    @Override
    public String buildAnalysisSummary() {
        List<JobStatVO> cityStats = statByCity();
        List<JobStatVO> skillStats = statBySkill();
        List<JobStatVO> salaryStats = statBySalaryRange();
        List<JobStatVO> titleStats = statTopTitles();

        String topCity = cityStats.isEmpty() ? "暂无数据" : cityStats.get(0).getName();
        String topSkill = skillStats.isEmpty() ? "暂无数据" : skillStats.get(0).getName();
        String topTitle = titleStats.isEmpty() ? "暂无数据" : titleStats.get(0).getName();
        BigDecimal avg = predictSalary(null, null, null, null);

        JobStatVO topBand = salaryStats.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingLong(v -> v.getCount() == null ? 0L : v.getCount()))
                .orElse(null);

        String trendText = analyzeTrend(jobTrendLast7Days());

        return String.format(
            "当前岗位需求较集中的城市为%s；热门岗位以%s为代表；热门技能以%s为代表；平均薪资约为%s元。薪资分布以%s区间为主，且%s。",
            topCity, topTitle, topSkill,
            avg == null ? "0.00" : avg.setScale(2, RoundingMode.HALF_UP).toPlainString(),
            topBand == null ? "暂无数据" : topBand.getName(),
            trendText
        );
    }

    /**
     * 分析趋势文本
     */
    private String analyzeTrend(JobTrendVO trend) {
        if (trend == null) return "暂无可用趋势数据";

        long last7 = trend.getLast7Days() == null ? 0 : trend.getLast7Days();
        long prev7 = trend.getPrev7Days() == null ? 0 : trend.getPrev7Days();

        if (prev7 == 0 && last7 == 0) return "暂无可用趋势数据";
        if (prev7 == 0) return "近期需求明显增加";

        double ratio = (double) last7 / prev7;
        if (ratio >= 1.15) return "近期需求呈上升趋势";
        if (ratio <= 0.85) return "近期需求呈回落趋势";
        return "近期需求整体较为稳定";
    }

    /**
     * 计算Jaccard相似度
     */
    private double calculateJaccardSimilarity(Set<String> userSkills, String jobSkillsStr) {
        if (jobSkillsStr == null || jobSkillsStr.isEmpty()) return 0.0;

        Set<String> jobSkills = Arrays.stream(jobSkillsStr.toLowerCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (userSkills.isEmpty() && jobSkills.isEmpty()) return 0.0;

        long intersection = userSkills.stream().filter(jobSkills::contains).count();
        Set<String> union = new HashSet<>(userSkills);
        union.addAll(jobSkills);

        return union.isEmpty() ? 0.0 : (double) intersection / union.size();
    }

    /**
     * 计算学历匹配分数
     */
    private BigDecimal calculateEducationMatchScore(String userEducation, String jobEducation) {
        if (userEducation == null || userEducation.isEmpty() || jobEducation == null || jobEducation.isEmpty()) {
            return BigDecimal.valueOf(50);
        }

        Integer userLevel = EDUCATION_LEVEL.getOrDefault(userEducation, 0);
        Integer jobLevel = EDUCATION_LEVEL.getOrDefault(jobEducation, 0);

        if (jobLevel == 0) return BigDecimal.valueOf(80);
        if (userLevel == jobLevel) return BigDecimal.valueOf(100);
        if (userLevel >= jobLevel + 1) return BigDecimal.valueOf(80);
        return BigDecimal.valueOf(20);
    }

    /**
     * 计算经验匹配分数
     */
    private BigDecimal calculateExperienceMatchScore(Integer userYears, String jobExperience) {
        if (userYears == null || userYears < 0 || jobExperience == null || jobExperience.isEmpty()) {
            return BigDecimal.valueOf(50);
        }

        Integer jobYears = EXPERIENCE_YEARS.getOrDefault(jobExperience, 0);
        if (jobYears == 0) return BigDecimal.valueOf(80);

        int diff = Math.abs(userYears - jobYears);
        if (diff == 0) return BigDecimal.valueOf(100);
        if (diff <= 1) return BigDecimal.valueOf(85);
        if (diff <= 2) return BigDecimal.valueOf(70);
        if (diff <= 3) return BigDecimal.valueOf(50);
        return BigDecimal.valueOf(30);
    }

    @Override
    public AIFeedbackVO analyzeWithAI(JobQueryDTO dto) {
        AIFeedbackVO result = new AIFeedbackVO();
        List<Job> jobs = getJobsForAnalysis(dto);

        if (jobs.isEmpty()) {
            result.setSummary("暂无符合筛选条件的岗位数据");
            result.setSuggestions(Collections.singletonList("建议：执行爬虫任务获取更多岗位数据"));
            return result;
        }

        // 规则引擎数据聚合
        result.setQualityJobs(extractQualityJobs(jobs));
        result.setTrendAnalysis(analyzeTrend(jobs));
        result.setSkillDemands(analyzeSkillDemands(jobs));
        result.setSalaryAnalysis(analyzeSalary(jobs));
        result.setSuggestions(generateSuggestions(jobs));

        // AI增强分析报告
        result.setSummary(generateAIAnalysisReport(jobs, result));

        return result;
    }

    /**
     * 获取用于分析的岗位数据
     */
    private List<Job> getJobsForAnalysis(JobQueryDTO dto) {
        JobQueryDTO queryDto = new JobQueryDTO();
        if (dto != null) {
            queryDto.setKeyword(dto.getKeyword());
            queryDto.setCity(dto.getCity());
            queryDto.setExperience(dto.getExperience());
            queryDto.setEducation(dto.getEducation());
            queryDto.setSourceSite(dto.getSourceSite());
            queryDto.setStatus(dto.getStatus() != null && !dto.getStatus().isEmpty() ? dto.getStatus() : "ACTIVE");
            queryDto.setMinSalary(dto.getMinSalary());
            queryDto.setMaxSalary(dto.getMaxSalary());
        } else {
            queryDto.setStatus("ACTIVE");
        }
        queryDto.setPageNum(1);
        queryDto.setPageSize(200);
        return jobMapper.selectByCondition(queryDto);
    }

    /**
     * 提取优质岗位
     */
    private List<AIFeedbackVO.QualityJob> extractQualityJobs(List<Job> jobs) {
        return jobs.stream()
                .sorted(Comparator.comparing(this::calculateJobScore).reversed())
                .limit(10)
                .map(this::convertToQualityJob)
                .collect(Collectors.toList());
    }

    /**
     * 转换为QualityJob对象
     */
    private AIFeedbackVO.QualityJob convertToQualityJob(Job job) {
        AIFeedbackVO.QualityJob qj = new AIFeedbackVO.QualityJob();
        qj.setId(job.getId());
        qj.setTitle(job.getTitle());
        qj.setCompanyName(job.getCompanyName());
        qj.setCity(job.getCity());
        qj.setSalary(formatSalary(job.getMinSalary(), job.getMaxSalary()));
        qj.setSkills(job.getSkills());
        qj.setRecommendReason(generateRecommendReason(job));
        return qj;
    }

    /**
     * 分析趋势
     */
    private AIFeedbackVO.TrendAnalysis analyzeTrend(List<Job> jobs) {
        AIFeedbackVO.TrendAnalysis trend = new AIFeedbackVO.TrendAnalysis();

        String hotCity = jobs.stream()
                .filter(j -> j.getCity() != null && !j.getCity().isBlank() && !j.getCity().equals("未知"))
                .collect(Collectors.groupingBy(Job::getCity, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无数据");
        trend.setHotCity(hotCity);

        String hotSkill = analyzeHotSkill(jobs);
        trend.setHotSkill(hotSkill);

        String hotTitle = jobs.stream()
                .filter(j -> j.getTitle() != null && !j.getTitle().isBlank())
                .collect(Collectors.groupingBy(Job::getTitle, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无数据");
        trend.setHotTitle(hotTitle);

        trend.setTrendText(String.format("当前热门城市为%s，热门技能为%s，热门岗位为%s。", hotCity, hotSkill, hotTitle));
        return trend;
    }

    /**
     * 分析热门技能
     */
    private String analyzeHotSkill(List<Job> jobs) {
        Map<String, Long> skillCount = new HashMap<>();
        for (Job job : jobs) {
            if (job.getSkills() != null) {
                for (String skill : job.getSkills().split(",")) {
                    skillCount.merge(skill.trim(), 1L, Long::sum);
                }
            }
        }
        return skillCount.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("暂无数据");
    }

    /**
     * 分析技能需求
     */
    private List<AIFeedbackVO.SkillDemand> analyzeSkillDemands(List<Job> jobs) {
        Map<String, Long> skillCount = new HashMap<>();
        for (Job job : jobs) {
            if (job.getSkills() != null) {
                for (String skill : job.getSkills().split(",")) {
                    skillCount.merge(skill.trim(), 1L, Long::sum);
                }
            }
        }

        long maxCount = skillCount.values().stream().max(Long::compare).orElse(1L);

        return skillCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    AIFeedbackVO.SkillDemand sd = new AIFeedbackVO.SkillDemand();
                    sd.setSkill(e.getKey());
                    sd.setCount(e.getValue().intValue());
                    sd.setLevel(e.getValue() >= maxCount * 0.8 ? "非常热门" :
                               e.getValue() >= maxCount * 0.5 ? "热门" : "一般");
                    return sd;
                })
                .collect(Collectors.toList());
    }

    /**
     * 分析薪资
     */
    private AIFeedbackVO.SalaryAnalysis analyzeSalary(List<Job> jobs) {
        AIFeedbackVO.SalaryAnalysis analysis = new AIFeedbackVO.SalaryAnalysis();

        List<BigDecimal> salaries = jobs.stream()
                .filter(j -> j.getMaxSalary() != null)
                .map(Job::getMaxSalary)
                .sorted()
                .collect(Collectors.toList());

        if (salaries.isEmpty()) {
            analysis.setAvgSalary("暂无数据");
            analysis.setTopSalary("暂无数据");
            analysis.setSalaryRange("暂无数据");
            return analysis;
        }

        BigDecimal total = salaries.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avg = total.divide(BigDecimal.valueOf(salaries.size()), 2, RoundingMode.HALF_UP);
        BigDecimal max = salaries.get(salaries.size() - 1);

        int lowIdx = salaries.size() / 4;
        int highIdx = salaries.size() * 3 / 4;

        analysis.setAvgSalary(avg.toPlainString() + "元/月");
        analysis.setTopSalary(max.setScale(0, RoundingMode.HALF_UP).toPlainString() + "元/月");
        analysis.setSalaryRange(salaries.get(lowIdx).setScale(0, RoundingMode.HALF_UP).toPlainString() +
                "-" + salaries.get(highIdx).setScale(0, RoundingMode.HALF_UP).toPlainString() + "元/月");

        return analysis;
    }

    /**
     * 生成建议
     */
    private List<String> generateSuggestions(List<Job> jobs) {
        List<String> suggestions = new ArrayList<>();

        String hotCity = jobs.stream()
                .filter(j -> j.getCity() != null && !j.getCity().isBlank())
                .collect(Collectors.groupingBy(Job::getCity, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("一线");
        suggestions.add(String.format("建议优先考虑%s等一线城市，岗位机会多、薪资水平高。", hotCity));

        String topEdu = jobs.stream()
                .filter(j -> j.getEducation() != null)
                .collect(Collectors.groupingBy(Job::getEducation, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("本科");
        suggestions.add(String.format("当前%s学历岗位占比最高，建议根据自身情况合理定位。", topEdu));

        String topExp = jobs.stream()
                .filter(j -> j.getExperience() != null)
                .collect(Collectors.groupingBy(Job::getExperience, Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("1-3年");
        suggestions.add(String.format("市场对%s经验的需求最大，建议积累相应项目经验。", topExp));

        suggestions.add("建议持续关注BOSS直聘、智联招聘、前程无忧、猎聘四大平台，岗位更新及时。");
        suggestions.add("面试时突出项目经验和实际问题解决能力，展示个人技术成长空间。");

        return suggestions;
    }

    /**
     * 生成AI分析报告
     */
    private String generateAIAnalysisReport(List<Job> jobs, AIFeedbackVO result) {
        if (!aiService.isApiKeyConfigured()) {
            return buildRuleBasedSummary(jobs.size(), result);
        }

        try {
            String aiResponse = aiService.chatSync(buildSystemPrompt(), buildDataContext(jobs, result));
            return aiResponse != null && !aiResponse.isBlank() ? aiResponse : buildRuleBasedSummary(jobs.size(), result);
        } catch (Exception e) {
            log.error("AI分析报告生成失败: {}", e.getMessage());
            return buildRuleBasedSummary(jobs.size(), result);
        }
    }

    /**
     * 构建系统提示词
     */
    private String buildSystemPrompt() {
        return """
            你是一位资深招聘市场数据分析师，擅长从招聘数据中发现趋势、洞察机会。
            你需要基于用户提供的数据进行客观、专业的分析，给出有价值的求职建议。
            回答使用中文，采用Markdown格式，适当使用emoji增强可读性。
            输出使用##作为主标题，###作为子标题，使用**粗体**强调关键数据。
            """;
    }

    /**
     * 构建数据上下文
     */
    private String buildDataContext(List<Job> jobs, AIFeedbackVO result) {
        StringBuilder context = new StringBuilder();
        context.append("请根据以下招聘数据进行专业分析，生成一份结构化的分析报告。\n\n");
        context.append("【基础数据概况】\n");
        context.append("- 样本总量：").append(jobs.size()).append("个岗位\n");

        AIFeedbackVO.TrendAnalysis trend = result.getTrendAnalysis();
        if (trend != null) {
            context.append("- 热门城市：").append(trend.getHotCity()).append("\n");
            context.append("- 热门技能：").append(trend.getHotSkill()).append("\n");
            context.append("- 热门岗位：").append(trend.getHotTitle()).append("\n");
        }

        AIFeedbackVO.SalaryAnalysis salary = result.getSalaryAnalysis();
        if (salary != null) {
            context.append("- 平均薪资：").append(salary.getAvgSalary()).append("\n");
            context.append("- 最高薪资：").append(salary.getTopSalary()).append("\n");
            context.append("- 薪资区间：").append(salary.getSalaryRange()).append("\n");
        }

        context.append("\n请按以下格式输出分析报告（使用Markdown格式）：\n");
        context.append("## 📊 数据总览\n（简要总结整体情况）\n");
        context.append("## 🏙️ 城市与薪资分析\n（分析各城市就业机会和薪资水平）\n");
        context.append("## 💻 技能与岗位洞察\n（分析热门技能的市场需求和前景）\n");
        context.append("## 💡 求职建议\n（给出3-5条具体可执行的建议）\n");
        context.append("请确保分析有据可依，基于上述真实数据。");

        return context.toString();
    }

    /**
     * 规则引擎兜底摘要
     */
    private String buildRuleBasedSummary(int jobCount, AIFeedbackVO result) {
        AIFeedbackVO.TrendAnalysis trend = result.getTrendAnalysis();
        AIFeedbackVO.SalaryAnalysis salary = result.getSalaryAnalysis();

        return String.format(
            "## 📊 数据总览\n\n当前共找到 **%d** 个符合条件的活跃岗位。\n\n" +
            "## 🏙️ 城市与薪资分析\n\n" +
            "- **最热城市**：%s\n- **平均薪资**：%s\n- **薪资区间**：%s\n- **最高薪资**：%s\n\n" +
            "## 💻 技能与岗位洞察\n\n" +
            "- **热门技能**：%s\n- **热门岗位**：%s\n\n" +
            "## 💡 求职建议\n\n" +
            "1. 优先关注 **%s** 等一线城市，机会多薪资高\n" +
            "2. 重点提升技术能力，关注市场需求\n" +
            "3. 根据自身经验水平合理定位\n" +
            "4. 多平台投递，提高面试成功率\n\n" +
            "> ✅ 以上分析基于系统中已爬取的真实招聘数据生成。",
            jobCount,
            trend != null ? trend.getHotCity() : "暂无数据",
            salary != null ? salary.getAvgSalary() : "暂无数据",
            salary != null ? salary.getSalaryRange() : "暂无数据",
            salary != null ? salary.getTopSalary() : "暂无数据",
            trend != null ? trend.getHotSkill() : "暂无数据",
            trend != null ? trend.getHotTitle() : "暂无数据",
            trend != null ? trend.getHotCity() : "一线"
        );
    }

    /**
     * 计算岗位分数
     */
    private int calculateJobScore(Job job) {
        int score = 0;
        if (job.getMaxSalary() != null) {
            score += job.getMaxSalary().divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP).intValue();
        }
        if (job.getSkills() != null) {
            for (String skill : job.getSkills().split(",")) {
                score += SKILL_WEIGHTS.getOrDefault(skill.trim(), 1);
            }
        }
        Set<String> hotCities = Set.of("北京", "上海", "广州", "深圳", "杭州");
        if (job.getCity() != null && hotCities.contains(job.getCity())) {
            score += 20;
        }
        return score;
    }

    /**
     * 格式化薪资
     */
    private String formatSalary(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return "面议";
        if (min != null && max != null) {
            return min.setScale(0, RoundingMode.HALF_UP) + "-" + max.setScale(0, RoundingMode.HALF_UP) + "元/月";
        }
        if (max != null) return max.setScale(0, RoundingMode.HALF_UP) + "元/月以下";
        if (min != null) return min.setScale(0, RoundingMode.HALF_UP) + "元/月以上";
        return "薪资面议";
    }

    /**
     * 生成推荐理由
     */
    private String generateRecommendReason(Job job) {
        StringBuilder sb = new StringBuilder();
        if (job.getMaxSalary() != null && job.getMaxSalary().compareTo(BigDecimal.valueOf(20000)) > 0) {
            sb.append("薪资竞争力强；");
        }
        if (job.getSkills() != null) {
            Set<String> hotSkills = Set.of("Java", "Python", "Go", "大数据", "算法", "Vue", "React");
            for (String skill : job.getSkills().split(",")) {
                if (hotSkills.contains(skill.trim())) {
                    sb.append("热门技术栈").append(skill.trim()).append("；");
                    break;
                }
            }
        }
        Set<String> tier1Cities = Set.of("北京", "上海", "广州", "深圳", "杭州");
        if (job.getCity() != null && tier1Cities.contains(job.getCity())) {
            sb.append("一线城市发展机会多；");
        }
        return sb.length() == 0 ? "综合条件良好" : sb.toString();
    }

    @Override
    public List<JobRecommendVO> intelligentRecommend(JobRecommendDTO dto) {
        JobQueryDTO queryDto = new JobQueryDTO();
        queryDto.setCity(dto.getCity());
        queryDto.setPageNum(1);
        queryDto.setPageSize(200);

        List<Job> jobs = jobMapper.selectByCondition(queryDto);
        if (jobs.isEmpty()) return Collections.emptyList();

        return jobs.stream()
                .map(job -> calculateMatchScore(job, dto.getSkills(), dto.getEducation(), dto.getExperienceYears()))
                .filter(vo -> vo.getOverallScore().compareTo(BigDecimal.ZERO) > 0)
                .sorted(Comparator.comparing(JobRecommendVO::getOverallScore).reversed())
                .limit(dto.getLimit() != null ? dto.getLimit() : 10)
                .collect(Collectors.toList());
    }

    /**
     * 计算匹配分数（智能推荐）
     */
    private JobRecommendVO calculateMatchScore(Job job, String userSkills, String userEducation, Integer userExperienceYears) {
        JobRecommendVO vo = new JobRecommendVO();
        vo.setJobId(job.getId());
        vo.setTitle(job.getTitle());
        vo.setCompanyName(job.getCompanyName());
        vo.setCity(job.getCity());
        vo.setSalary(formatSalary(job.getMinSalary(), job.getMaxSalary()));
        vo.setSkills(job.getSkills());
        vo.setEducation(job.getEducation());
        vo.setExperience(job.getExperience());
        vo.setUrl(job.getUrl());
        vo.setSourceSite(job.getSourceSite());

        BigDecimal skillScore = calculateSkillMatchScore(userSkills, job.getSkills());
        BigDecimal educationScore = calculateEducationMatchScore(userEducation, job.getEducation());
        BigDecimal experienceScore = calculateExperienceMatchScore(userExperienceYears, job.getExperience());

        BigDecimal overallScore = skillScore.multiply(BigDecimal.valueOf(SKILL_WEIGHT))
                .add(educationScore.multiply(BigDecimal.valueOf(EDUCATION_WEIGHT)))
                .add(experienceScore.multiply(BigDecimal.valueOf(EXPERIENCE_WEIGHT)));

        vo.setSkillMatchScore(skillScore);
        vo.setEducationMatchScore(educationScore);
        vo.setExperienceMatchScore(experienceScore);
        vo.setOverallScore(overallScore);

        return vo;
    }

    /**
     * 计算技能匹配分数
     */
    private BigDecimal calculateSkillMatchScore(String userSkills, String jobSkills) {
        if (userSkills == null || userSkills.isEmpty() || jobSkills == null || jobSkills.isEmpty()) {
            return BigDecimal.ZERO;
        }

        Set<String> userSkillSet = Arrays.stream(userSkills.toLowerCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        Set<String> jobSkillSet = Arrays.stream(jobSkills.toLowerCase().split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());

        if (userSkillSet.isEmpty() || jobSkillSet.isEmpty()) {
            return BigDecimal.ZERO;
        }

        long intersection = userSkillSet.stream().filter(jobSkillSet::contains).count();
        Set<String> union = new HashSet<>(userSkillSet);
        union.addAll(jobSkillSet);

        return union.isEmpty() ? BigDecimal.ZERO :
               BigDecimal.valueOf((double) intersection / union.size() * 100).setScale(2, RoundingMode.HALF_UP);
    }
}