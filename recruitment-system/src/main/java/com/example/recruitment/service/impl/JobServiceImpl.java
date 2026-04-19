package com.example.recruitment.service.impl;

import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.JobMapper;
import com.example.recruitment.service.JobService;
import com.example.recruitment.vo.AIFeedbackVO;
import com.example.recruitment.vo.JobTrendVO;
import com.example.recruitment.vo.JobStatVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;

    // 热门技能权重配置
    private static final Map<String, Integer> SKILL_WEIGHTS = new HashMap<>();
    static {
        SKILL_WEIGHTS.put("Java", 10);
        SKILL_WEIGHTS.put("Python", 10);
        SKILL_WEIGHTS.put("Go", 9);
        SKILL_WEIGHTS.put("大数据", 9);
        SKILL_WEIGHTS.put("算法", 9);
        SKILL_WEIGHTS.put("前端", 8);
        SKILL_WEIGHTS.put("Vue", 8);
        SKILL_WEIGHTS.put("React", 8);
        SKILL_WEIGHTS.put("Spring", 8);
        SKILL_WEIGHTS.put("MySQL", 7);
        SKILL_WEIGHTS.put("Linux", 7);
        SKILL_WEIGHTS.put("测试", 6);
        SKILL_WEIGHTS.put("运维", 6);
        SKILL_WEIGHTS.put("C++", 7);
        SKILL_WEIGHTS.put("PHP", 5);
    }

    @Override
    public void addJob(Job job) {
        job.setCreatedAt(LocalDateTime.now());
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
    public Job getJob(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public PageInfo<Job> listJobs(JobQueryDTO dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<Job> list = jobMapper.selectByCondition(dto);
        return new PageInfo<>(list);
    }

    @Override
    public List<JobStatVO> statByCity() {
        return jobMapper.statByCity();
    }

    @Override
    public List<JobStatVO> statByCompany() {
        return jobMapper.statByCompany();
    }

    @Override
    public List<JobStatVO> statBySkill() {
        return jobMapper.statBySkill();
    }

    @Override
    public List<JobStatVO> statBySalaryRange() {
        return jobMapper.statBySalaryRange();
    }

    @Override
    public List<JobStatVO> statByEducation() {
        return jobMapper.statByEducation();
    }

    @Override
    public List<JobStatVO> statByExperience() {
        return jobMapper.statByExperience();
    }

    @Override
    public List<JobStatVO> statByStatus() {
        return jobMapper.statByStatus();
    }

    @Override
    public List<JobStatVO> statTopTitles() {
        return jobMapper.statTopTitles();
    }

    @Override
    public BigDecimal predictSalary(String city, String experience, String education, String skills) {
        BigDecimal avg = jobMapper.predictSalary(city, experience, education, skills);
        if (avg == null) {
            return BigDecimal.ZERO;
        }
        return avg;
    }

    @Override
    public List<Job> recommendJobs(String skills, String city) {
        JobQueryDTO dto = new JobQueryDTO();
        dto.setCity(city);
        dto.setKeyword(null);
        dto.setPageNum(1);
        dto.setPageSize(100);
        List<Job> list = jobMapper.selectByCondition(dto);

        if (skills == null || skills.isEmpty()) {
            return list.stream().limit(10).collect(Collectors.toList());
        }
        String[] wanted = skills.toLowerCase(Locale.ROOT).split(",");
        return list.stream()
                .sorted(Comparator.comparingInt(j -> -matchScore((Job) j, wanted)))
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public String buildAnalysisSummary() {
        List<JobStatVO> cityStats = statByCity();
        List<JobStatVO> skillStats = statBySkill();
        List<JobStatVO> salaryStats = statBySalaryRange();
        List<JobStatVO> titleStats = statTopTitles();

        JobStatVO topCity = cityStats.isEmpty() ? null : cityStats.get(0);
        JobStatVO topSkill = skillStats.isEmpty() ? null : skillStats.get(0);
        JobStatVO topTitle = titleStats.isEmpty() ? null : titleStats.get(0);
        BigDecimal avg = predictSalary(null, null, null, null);
        JobStatVO topBand = salaryStats.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingLong(v -> v.getCount() == null ? 0L : v.getCount()))
                .orElse(null);

        JobTrendVO trend = jobMapper.jobTrendLast7Days();
        long last7 = trend == null || trend.getLast7Days() == null ? 0 : trend.getLast7Days();
        long prev7 = trend == null || trend.getPrev7Days() == null ? 0 : trend.getPrev7Days();
        String trendText;
        if (prev7 == 0 && last7 == 0) {
            trendText = "暂无可用趋势数据";
        } else if (prev7 == 0) {
            trendText = "近期需求明显增加";
        } else {
            double ratio = (last7 * 1.0d) / prev7;
            if (ratio >= 1.15d) {
                trendText = "近期需求呈上升趋势";
            } else if (ratio <= 0.85d) {
                trendText = "近期需求呈回落趋势";
            } else {
                trendText = "近期需求整体较为稳定";
            }
        }

        return String.format(
                "当前岗位需求较集中的城市为%s；热门岗位以%s为代表；热门技能以%s为代表；平均薪资约为%s元。薪资分布以%s区间为主，且%s。",
                topCity == null ? "暂无数据" : topCity.getName(),
                topTitle == null ? "暂无数据" : topTitle.getName(),
                topSkill == null ? "暂无数据" : topSkill.getName(),
                avg == null ? "0.00" : avg.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                topBand == null ? "暂无数据" : topBand.getName(),
                trendText
        );
    }

    private int matchScore(Job job, String[] wanted) {
        if (job.getSkills() == null) return 0;
        String s = job.getSkills().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String w : wanted) {
            if (s.contains(w.trim())) {
                score++;
            }
        }
        return score;
    }

    @Override
    public AIFeedbackVO analyzeWithAI(JobQueryDTO dto) {
        AIFeedbackVO result = new AIFeedbackVO();

        // 获取筛选后的数据（不加分页，获取所有符合条件的数据）
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

        List<Job> jobs = jobMapper.selectByCondition(queryDto);

        if (jobs == null || jobs.isEmpty()) {
            result.setSummary("暂无符合筛选条件的岗位数据，请尝试调整筛选条件或先进行数据爬取。");
            result.setSuggestions(Collections.singletonList("建议：执行爬虫任务获取更多岗位数据后再进行分析"));
            return result;
        }

        // 1. 优质岗位推荐
        List<AIFeedbackVO.QualityJob> qualityJobs = new ArrayList<>();
        List<Job> topJobs = jobs.stream()
                .sorted(Comparator.comparing(this::calculateJobScore).reversed())
                .limit(10)
                .collect(Collectors.toList());

        for (Job job : topJobs) {
            AIFeedbackVO.QualityJob qj = new AIFeedbackVO.QualityJob();
            qj.setId(job.getId());
            qj.setTitle(job.getTitle());
            qj.setCompanyName(job.getCompanyName());
            qj.setCity(job.getCity());
            qj.setSalary(formatSalary(job.getMinSalary(), job.getMaxSalary()));
            qj.setSkills(job.getSkills());
            qj.setRecommendReason(generateRecommendReason(job));
            qualityJobs.add(qj);
        }
        result.setQualityJobs(qualityJobs);

        // 2. 需求趋势分析
        AIFeedbackVO.TrendAnalysis trend = new AIFeedbackVO.TrendAnalysis();
        Map<String, Long> cityCount = jobs.stream()
                .filter(j -> j.getCity() != null && !j.getCity().isBlank() && !j.getCity().equals("未知"))
                .collect(Collectors.groupingBy(Job::getCity, Collectors.counting()));
        String hotCity = cityCount.isEmpty() ? "暂无数据" :
                cityCount.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
        trend.setHotCity(hotCity);

        Map<String, Long> skillCount = new HashMap<>();
        for (Job job : jobs) {
            if (job.getSkills() != null) {
                for (String skill : job.getSkills().split(",")) {
                    skillCount.merge(skill.trim(), 1L, Long::sum);
                }
            }
        }
        String hotSkill = skillCount.isEmpty() ? "暂无数据" :
                skillCount.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
        trend.setHotSkill(hotSkill);

        Map<String, Long> titleCount = jobs.stream()
                .filter(j -> j.getTitle() != null && !j.getTitle().isBlank())
                .collect(Collectors.groupingBy(Job::getTitle, Collectors.counting()));
        String hotTitle = titleCount.isEmpty() ? "暂无数据" :
                titleCount.entrySet().stream().max(Map.Entry.comparingByValue()).get().getKey();
        trend.setHotTitle(hotTitle);

        // 生成趋势文本
        String trendText = String.format("当前热门城市为%s，热门技能为%s，热门岗位为%s。",
                hotCity, hotSkill, hotTitle);
        trend.setTrendText(trendText);
        result.setTrendAnalysis(trend);

        // 3. 技能需求排名
        List<AIFeedbackVO.SkillDemand> skillDemands = skillCount.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(10)
                .map(e -> {
                    AIFeedbackVO.SkillDemand sd = new AIFeedbackVO.SkillDemand();
                    sd.setSkill(e.getKey());
                    sd.setCount(e.getValue().intValue());
                    // 根据需求量判断热门程度
                    long max = skillCount.values().stream().max(Long::compare).orElse(1L);
                    if (e.getValue() >= max * 0.8) {
                        sd.setLevel("非常热门");
                    } else if (e.getValue() >= max * 0.5) {
                        sd.setLevel("热门");
                    } else {
                        sd.setLevel("一般");
                    }
                    return sd;
                })
                .collect(Collectors.toList());
        result.setSkillDemands(skillDemands);

        // 4. 薪资分析
        AIFeedbackVO.SalaryAnalysis salaryAnalysis = new AIFeedbackVO.SalaryAnalysis();
        BigDecimal totalSalary = BigDecimal.ZERO;
        BigDecimal maxSalary = BigDecimal.ZERO;
        int count = 0;
        for (Job job : jobs) {
            if (job.getMaxSalary() != null) {
                BigDecimal avg = job.getMaxSalary();
                totalSalary = totalSalary.add(avg);
                if (avg.compareTo(maxSalary) > 0) {
                    maxSalary = avg;
                }
                count++;
            }
        }
        if (count > 0) {
            BigDecimal avgSalary = totalSalary.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
            salaryAnalysis.setAvgSalary(avgSalary.toPlainString() + "元/月");
            salaryAnalysis.setTopSalary(maxSalary.setScale(0, RoundingMode.HALF_UP).toPlainString() + "元/月");

            // 计算薪资范围
            List<BigDecimal> salaries = jobs.stream()
                    .filter(j -> j.getMaxSalary() != null)
                    .map(j -> j.getMaxSalary())
                    .sorted()
                    .collect(Collectors.toList());
            if (!salaries.isEmpty()) {
                int lowIdx = salaries.size() / 4;
                int highIdx = salaries.size() * 3 / 4;
                salaryAnalysis.setSalaryRange(salaries.get(lowIdx).setScale(0, RoundingMode.HALF_UP).toPlainString() +
                        "-" + salaries.get(highIdx).setScale(0, RoundingMode.HALF_UP).toPlainString() + "元/月");
            }
        } else {
            salaryAnalysis.setAvgSalary("暂无数据");
            salaryAnalysis.setTopSalary("暂无数据");
            salaryAnalysis.setSalaryRange("暂无数据");
        }
        result.setSalaryAnalysis(salaryAnalysis);

        // 5. 求职建议
        List<String> suggestions = generateSuggestions(jobs, hotCity, hotSkill, skillDemands);
        result.setSuggestions(suggestions);

        // 6. 生成总结
        String summary = String.format("根据筛选结果，共找到%d个符合条件岗位。热门城市：%s，热门技能：%s，平均薪资：%s。建议重点关注%s相关岗位，提升%s技能储备。",
                jobs.size(), hotCity, hotSkill,
                salaryAnalysis.getAvgSalary(),
                hotTitle,
                hotSkill);
        result.setSummary(summary);

        return result;
    }

    private int calculateJobScore(Job job) {
        int score = 0;
        // 薪资得分
        if (job.getMaxSalary() != null) {
            score += job.getMaxSalary().divide(BigDecimal.valueOf(1000), 2, RoundingMode.HALF_UP).intValue();
        }
        // 技能匹配得分
        if (job.getSkills() != null) {
            for (String skill : job.getSkills().split(",")) {
                score += SKILL_WEIGHTS.getOrDefault(skill.trim(), 1);
            }
        }
        // 热门城市加成
        Set<String> hotCities = new HashSet<>(Arrays.asList("北京", "上海", "广州", "深圳", "杭州"));
        if (job.getCity() != null && hotCities.contains(job.getCity())) {
            score += 20;
        }
        return score;
    }

    private String formatSalary(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) return "面议";
        if (min != null && max != null) {
            return min.setScale(0, RoundingMode.HALF_UP) + "-" + max.setScale(0, RoundingMode.HALF_UP) + "元/月";
        }
        if (max != null) {
            return max.setScale(0, RoundingMode.HALF_UP) + "元/月以下";
        }
        return min.setScale(0, RoundingMode.HALF_UP) + "元/月以上";
    }

    private String generateRecommendReason(Job job) {
        StringBuilder sb = new StringBuilder();
        // 薪资优势
        if (job.getMaxSalary() != null && job.getMaxSalary().compareTo(BigDecimal.valueOf(20000)) > 0) {
            sb.append("薪资竞争力强；");
        }
        // 技能匹配
        if (job.getSkills() != null) {
            List<String> hotSkills = Arrays.asList("Java", "Python", "Go", "大数据", "算法", "Vue", "React");
            for (String skill : job.getSkills().split(",")) {
                if (hotSkills.contains(skill.trim())) {
                    sb.append("热门技术栈").append(skill.trim()).append("；");
                    break;
                }
            }
        }
        // 城市优势
        Set<String> tier1Cities = new HashSet<>(Arrays.asList("北京", "上海", "广州", "深圳", "杭州"));
        if (job.getCity() != null && tier1Cities.contains(job.getCity())) {
            sb.append("一线城市发展机会多；");
        }
        if (sb.length() == 0) {
            sb.append("综合条件良好");
        }
        return sb.toString();
    }

    private List<String> generateSuggestions(List<Job> jobs, String hotCity, String hotSkill,
                                            List<AIFeedbackVO.SkillDemand> skillDemands) {
        List<String> suggestions = new ArrayList<>();

        // 城市建议
        suggestions.add(String.format("建议优先考虑%s等一线城市，岗位机会多、薪资水平高。", hotCity));

        // 技能建议
        if (skillDemands.size() >= 3) {
            String top3Skills = skillDemands.stream()
                    .limit(3)
                    .map(AIFeedbackVO.SkillDemand::getSkill)
                    .collect(Collectors.joining("、"));
            suggestions.add(String.format("重点学习：%s，这些技能在当前市场需求最高。", top3Skills));
        }

        // 学历建议
        Map<String, Long> eduCount = jobs.stream()
                .filter(j -> j.getEducation() != null)
                .collect(Collectors.groupingBy(Job::getEducation, Collectors.counting()));
        if (!eduCount.isEmpty()) {
            String topEdu = eduCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
            suggestions.add(String.format("当前%s学历岗位占比最高，建议根据自身情况合理定位。", topEdu));
        }

        // 经验建议
        Map<String, Long> expCount = jobs.stream()
                .filter(j -> j.getExperience() != null)
                .collect(Collectors.groupingBy(Job::getExperience, Collectors.counting()));
        if (!expCount.isEmpty()) {
            String topExp = expCount.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
            suggestions.add(String.format("市场对%s经验的需求最大，建议积累相应项目经验。", topExp));
        }

        // 通用建议
        suggestions.add("建议持续关注BOSS直聘、智联招聘、前程无忧、猎聘四大平台，岗位更新及时。");
        suggestions.add("面试时突出项目经验和实际问题解决能力，展示个人技术成长空间。");

        return suggestions;
    }
}

