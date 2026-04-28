package com.example.recruitment.service.impl;

import com.example.recruitment.entity.Job;
import com.example.recruitment.mapper.JobMapper;
import com.example.recruitment.service.LocalModelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 本地小模型服务实现
 * 基于规则引擎实现轻量级AI响应，支持招聘数据分析
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class LocalModelServiceImpl implements LocalModelService {

    private static final String MODEL_VERSION = "2.0.0";
    private static final Map<String, List<String>> FAQ_DATABASE = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> SALARY_QA = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> SKILL_QA = new ConcurrentHashMap<>();
    private static final Map<String, List<String>> EXPERIENCE_QA = new ConcurrentHashMap<>();

    // 业务常量定义
    private static final int HIGH_SALARY_THRESHOLD = 20000;
    private static final int ENTRY_SALARY_THRESHOLD = 10000;
    private static final int SKILL_RANK_LIMIT = 10;
    private static final int SEMANTIC_SIMILARITY_THRESHOLD = 2;
    private static final int MAX_HOT_JOBS = 5;
    private static final int MAX_CITY_DISTRIBUTION = 5;
    private static final int MAX_CITY_HOT_JOBS = 3;

    private final JobMapper jobMapper;

    static {
        initFAQDatabase();
        initSalaryQA();
        initSkillQA();
        initExperienceQA();
    }

    private static void initFAQDatabase() {
        FAQ_DATABASE.put("介绍项目", Arrays.asList(
            "本项目是一个招聘数据可视化分析系统，主要功能包括招聘数据爬取、清洗、分析和可视化展示。",
            "系统采用Spring Boot 3.2 + Vue 3技术栈，支持AI智能分析和数据可视化展示。",
            "该系统能够帮助求职者了解就业市场趋势，为职业规划提供数据支持。"
        ));

        FAQ_DATABASE.put("技术选型", Arrays.asList(
            "后端采用Spring Boot 3.2框架，前端使用Vue 3 + Element Plus组件库，数据库使用MySQL 8.0。",
            "数据爬取使用WebMagic框架，数据可视化使用ECharts图表库，API文档使用Swagger。",
            "安全方面采用JWT Token认证，密码使用SHA-256加盐加密存储。"
        ));

        FAQ_DATABASE.put("核心功能", Arrays.asList(
            "系统核心功能包括：招聘数据采集、数据清洗与存储、AI智能分析、可视化图表展示、用户权限管理。",
            "支持多平台招聘数据爬取，包括BOSS直聘、智联招聘、前程无忧等主流招聘平台。",
            "提供数据导出功能，支持将招聘数据导出为Excel文件进行离线分析。"
        ));

        FAQ_DATABASE.put("安全机制", Arrays.asList(
            "系统采用JWT Token认证机制，支持无状态会话管理，避免Session劫持风险。",
            "用户密码使用SHA-256算法加盐加密存储，确保密码安全。",
            "API接口配置了CORS跨域保护，仅允许指定域名访问，防止CSRF攻击。"
        ));

        FAQ_DATABASE.put("数据来源", Arrays.asList(
            "数据主要来自BOSS直聘、智联招聘、前程无忧等主流招聘平台，通过网络爬虫自动采集。",
            "数据每日定时更新，确保招聘信息的时效性和准确性。",
            "爬取的数据经过严格清洗和去重处理，保证数据质量。"
        ));

        FAQ_DATABASE.put("未来规划", Arrays.asList(
            "未来计划增加更多数据源支持，扩展到更多招聘平台和行业领域。",
            "优化AI分析算法，提供更精准的岗位推荐和薪资预测功能。",
            "计划开发移动端APP，方便用户随时随地查看招聘数据和分析报告。"
        ));

        FAQ_DATABASE.put("性能优化", Arrays.asList(
            "系统采用HikariCP连接池优化数据库访问性能，支持连接复用和自动管理。",
            "使用Redis缓存热点数据，减少数据库查询压力，提升响应速度。",
            "支持异步爬取任务，采用线程池管理，避免长时间阻塞主线程。"
        ));

        FAQ_DATABASE.put("使用帮助", Arrays.asList(
            "登录系统后，可以在首页查看招聘数据概览和热门岗位推荐。",
            "进入AI分析页面，可以与AI助手对话获取招聘市场分析报告。",
            "使用筛选功能可以按城市、薪资、经验等条件筛选岗位信息。"
        ));

        FAQ_DATABASE.put("默认回答", Arrays.asList(
            "感谢您的提问！我们的招聘数据分析系统可以帮助您了解当前就业市场的趋势和机会。",
            "系统包含丰富的招聘数据和专业的AI分析功能，欢迎继续探索。",
            "如果您有具体的问题，请提供更多细节，我会尽力为您解答。"
        ));
    }

    private static void initSalaryQA() {
        SALARY_QA.put("薪资水平", Arrays.asList(
            "根据最新数据，Java后端开发岗位的平均薪资约为15-25K/月，高级工程师可达30K以上。",
            "长沙地区Java岗位薪资分布：8K以下占15%，8-15K占45%，15-25K占30%，25K以上占10%。",
            "薪资水平与工作经验密切相关：1-3年经验平均12K，3-5年经验平均18K，5年以上平均25K。"
        ));

        SALARY_QA.put("薪资对比", Arrays.asList(
            "一线城市薪资普遍高于二三线城市，同等岗位薪资差距可达30%-50%。",
            "大厂薪资通常比中小企业高20%-30%，但工作强度和要求也更高。",
            "技术栈熟练度对薪资影响显著，掌握热门技术（如Docker、K8s、微服务）可提升薪资竞争力。"
        ));

        SALARY_QA.put("薪资谈判", Arrays.asList(
            "薪资谈判前建议了解市场行情，参考同岗位薪资水平确定合理期望值。",
            "突出自身优势和项目经验，量化工作成果，有助于争取更高薪资。",
            "考虑综合福利：五险一金、年终奖、股票期权等也是重要考量因素。"
        ));
    }

    private static void initSkillQA() {
        SKILL_QA.put("技能要求", Arrays.asList(
            "Java后端开发岗位核心技能：Java基础、Spring Boot、MySQL、Redis、分布式架构。",
            "热门技能趋势：微服务架构、Docker容器化、Kubernetes编排、云原生技术。",
            "软技能要求：良好的沟通能力、团队协作精神、问题分析和解决能力。"
        ));

        SKILL_QA.put("技能学习", Arrays.asList(
            "建议从基础开始：Java SE → Spring Boot → 数据库 → 分布式系统逐步学习。",
            "实践项目经验很重要，建议参与开源项目或做个人项目积累经验。",
            "关注技术社区和博客，保持学习热情，跟上技术发展趋势。"
        ));

        SKILL_QA.put("技能提升", Arrays.asList(
            "深入理解底层原理，不仅仅停留在API使用层面。",
            "学习优秀开源项目的代码结构和设计模式。",
            "通过技术分享和博客写作巩固知识，提升影响力。"
        ));
    }

    private static void initExperienceQA() {
        EXPERIENCE_QA.put("经验要求", Arrays.asList(
            "初级开发：0-2年经验，掌握基础技术栈，能独立完成简单任务。",
            "中级开发：2-5年经验，能独立负责模块开发，具备系统设计能力。",
            "高级开发：5年以上经验，能主导大型项目架构设计，带领团队完成目标。"
        ));

        EXPERIENCE_QA.put("职业发展", Arrays.asList(
            "技术路线：初级→中级→高级→技术专家→架构师。",
            "管理路线：技术主管→技术经理→技术总监→CTO。",
            "建议在3-5年内专注深耕一个领域，成为该领域专家。"
        ));

        EXPERIENCE_QA.put("跳槽建议", Arrays.asList(
            "建议至少在一家公司工作2年以上，积累足够经验再考虑跳槽。",
            "跳槽前明确职业目标，选择能提升自己的机会。",
            "保持良好的职业口碑，与前同事和领导保持良好关系。"
        ));
    }

    @Override
    public boolean isAvailable() {
        return getHealthStatus() == HealthStatus.HEALTHY;
    }

    @Override
    public HealthStatus getHealthStatus() {
        try {
            generateResponse("", "测试请求");
            return HealthStatus.HEALTHY;
        } catch (Exception e) {
            log.error("小模型健康检查失败: {}", e.getMessage());
            return HealthStatus.UNHEALTHY;
        }
    }

    @Override
    public String generateResponse(String systemPrompt, String userMessage) {
        log.info("本地模型处理请求: {}", userMessage);

        try {
            String lowerQuestion = userMessage.toLowerCase().trim();
            
            // 优先尝试数据分析
            String analysisResult = tryDataAnalysis(lowerQuestion);
            if (analysisResult != null) {
                return analysisResult;
            }

            // 尝试多关键词匹配
            String matchedResult = multiKeywordMatch(lowerQuestion);
            if (matchedResult != null) {
                return matchedResult;
            }

            // 尝试语义相似度匹配
            String semanticResult = semanticMatch(lowerQuestion);
            if (semanticResult != null) {
                return semanticResult;
            }

            // 返回默认回答
            return getRandomResponse(FAQ_DATABASE.get("默认回答"));

        } catch (Exception e) {
            log.error("本地模型生成响应失败: {}", e.getMessage());
            return "服务暂时不可用，请稍后重试。";
        }
    }

    /**
     * 尝试基于数据库数据进行分析
     */
    private String tryDataAnalysis(String question) {
        try {
            // 薪资分析
            if (question.contains("薪资") || question.contains("工资") || question.contains("待遇")) {
                return analyzeSalaryData(question);
            }
            
            // 岗位分析
            if (question.contains("岗位") || question.contains("职位") || question.contains("需求")) {
                return analyzeJobData(question);
            }
            
            // 技能分析
            if (question.contains("技能") || question.contains("技术") || question.contains("要求")) {
                return analyzeSkillData(question);
            }
            
            // 城市分析
            if (question.contains("长沙") || question.contains("城市")) {
                return analyzeCityData(question);
            }
            
            return null;
        } catch (Exception e) {
            log.warn("数据分析失败，回退到FAQ: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 薪资数据分析
     */
    private String analyzeSalaryData(String question) {
        try {
            List<Job> jobs = jobMapper.selectAll();
            if (jobs.isEmpty()) {
                return getRandomResponse(SALARY_QA.get("薪资水平"));
            }

            double avgSalary = jobs.stream()
                .filter(j -> j.getSalary() != null)
                .mapToDouble(j -> parseSalary(j.getSalary()))
                .average()
                .orElse(0);

            long highCount = jobs.stream()
                .filter(j -> j.getSalary() != null && parseSalary(j.getSalary()) >= HIGH_SALARY_THRESHOLD)
                .count();

            long lowCount = jobs.stream()
                .filter(j -> j.getSalary() != null && parseSalary(j.getSalary()) < ENTRY_SALARY_THRESHOLD)
                .count();

            StringBuilder result = new StringBuilder();
            result.append(String.format("根据数据库中%d个岗位数据分析：\n", jobs.size()));
            result.append(String.format("• 平均薪资：%.0f元/月\n", avgSalary));
            result.append(String.format("• 高薪岗位（20K以上）：%d个（占比%.1f%%）\n", 
                highCount, jobs.size() > 0 ? (highCount * 100.0 / jobs.size()) : 0));
            result.append(String.format("• 入门岗位（10K以下）：%d个（占比%.1f%%）\n", 
                lowCount, jobs.size() > 0 ? (lowCount * 100.0 / jobs.size()) : 0));
            result.append("\n建议：关注技能提升，积累项目经验，有助于获得更高薪资。");

            return result.toString();
        } catch (Exception e) {
            log.warn("薪资分析失败: {}", e.getMessage());
            return getRandomResponse(SALARY_QA.get("薪资水平"));
        }
    }

    /**
     * 岗位数据分析
     */
    private String analyzeJobData(String question) {
        try {
            List<Job> jobs = jobMapper.selectAll();
            if (jobs.isEmpty()) {
                return "当前数据库中暂无岗位数据，请先运行爬虫采集数据。";
            }

            Map<String, Long> titleCount = jobs.stream()
                .filter(j -> j.getTitle() != null)
                .collect(Collectors.groupingBy(Job::getTitle, Collectors.counting()));

            Map<String, Long> cityCount = jobs.stream()
                .filter(j -> j.getCity() != null)
                .collect(Collectors.groupingBy(Job::getCity, Collectors.counting()));

            StringBuilder result = new StringBuilder();
            result.append(String.format("当前数据库共有%d个招聘岗位\n", jobs.size()));
            
            result.append("\n📊 热门岗位：");
            titleCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(MAX_HOT_JOBS)
                .forEach(e -> result.append(String.format("\n• %s：%d个", e.getKey(), e.getValue())));

            result.append("\n\n🏙️ 城市分布：");
            cityCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(MAX_CITY_DISTRIBUTION)
                .forEach(e -> result.append(String.format("\n• %s：%d个", e.getKey(), e.getValue())));

            return result.toString();
        } catch (Exception e) {
            log.warn("岗位分析失败: {}", e.getMessage());
            return "当前数据库中暂无岗位数据，请先运行爬虫采集数据。";
        }
    }

    /**
     * 技能需求分析
     */
    private String analyzeSkillData(String question) {
        try {
            List<Job> jobs = jobMapper.selectAll();
            if (jobs.isEmpty()) {
                return getRandomResponse(SKILL_QA.get("技能要求"));
            }

            Map<String, Long> skillCount = new HashMap<>();
            for (Job job : jobs) {
                if (job.getSkills() != null) {
                    String[] skills = job.getSkills().split(",");
                    for (String skill : skills) {
                        skill = skill.trim().toLowerCase();
                        if (!skill.isEmpty()) {
                            skillCount.merge(skill, 1L, Long::sum);
                        }
                    }
                }
            }

            if (skillCount.isEmpty()) {
                return getRandomResponse(SKILL_QA.get("技能要求"));
            }

            StringBuilder result = new StringBuilder();
            result.append("🔥 技能需求排名：\n");
            
            skillCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(SKILL_RANK_LIMIT)
                .forEach(e -> result.append(String.format("• %s：%d个岗位需求\n", e.getKey(), e.getValue())));

            result.append("\n💡 建议：优先掌握排名靠前的技能，提升市场竞争力。");

            return result.toString();
        } catch (Exception e) {
            log.warn("技能分析失败: {}", e.getMessage());
            return getRandomResponse(SKILL_QA.get("技能要求"));
        }
    }

    /**
     * 城市数据分析
     */
    private String analyzeCityData(String question) {
        try {
            List<Job> jobs = jobMapper.selectAll();
            if (jobs.isEmpty()) {
                return "当前数据库中暂无岗位数据，请先运行爬虫采集数据。";
            }

            List<Job> changshaJobs = jobs.stream()
                .filter(j -> "长沙".equals(j.getCity()))
                .collect(Collectors.toList());

            if (changshaJobs.isEmpty()) {
                return "长沙地区暂无岗位数据，建议运行爬虫采集数据。";
            }

            double avgSalary = changshaJobs.stream()
                .filter(j -> j.getSalary() != null)
                .mapToDouble(j -> parseSalary(j.getSalary()))
                .average()
                .orElse(0);

            Map<String, Long> titleCount = changshaJobs.stream()
                .filter(j -> j.getTitle() != null)
                .collect(Collectors.groupingBy(Job::getTitle, Collectors.counting()));

            StringBuilder result = new StringBuilder();
            result.append(String.format("🏙️ 长沙地区招聘数据分析：\n"));
            result.append(String.format("• 岗位总数：%d个\n", changshaJobs.size()));
            result.append(String.format("• 平均薪资：%.0f元/月\n", avgSalary));
            
            result.append("\n📋 热门岗位：");
            titleCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(MAX_CITY_HOT_JOBS)
                .forEach(e -> result.append(String.format("\n• %s：%d个", e.getKey(), e.getValue())));

            result.append("\n\n💬 长沙作为新一线城市，IT行业发展迅速，就业机会较多。");

            return result.toString();
        } catch (Exception e) {
            log.warn("城市分析失败: {}", e.getMessage());
            return "长沙地区暂无岗位数据，建议运行爬虫采集数据。";
        }
    }

    /**
     * 多关键词匹配
     */
    private String multiKeywordMatch(String question) {
        List<String> matchedCategories = new ArrayList<>();
        
        Map<String, String> keywordMapping = new LinkedHashMap<>();
        keywordMapping.put("介绍", "介绍项目");
        keywordMapping.put("项目", "介绍项目");
        keywordMapping.put("技术", "技术选型");
        keywordMapping.put("架构", "技术选型");
        keywordMapping.put("功能", "核心功能");
        keywordMapping.put("安全", "安全机制");
        keywordMapping.put("数据来源", "数据来源");
        keywordMapping.put("未来", "未来规划");
        keywordMapping.put("优化", "性能优化");
        keywordMapping.put("性能", "性能优化");
        keywordMapping.put("帮助", "使用帮助");
        keywordMapping.put("薪资", "薪资水平");
        keywordMapping.put("工资", "薪资水平");
        keywordMapping.put("技能", "技能要求");
        keywordMapping.put("经验", "经验要求");

        for (Map.Entry<String, String> entry : keywordMapping.entrySet()) {
            if (question.contains(entry.getKey())) {
                matchedCategories.add(entry.getValue());
            }
        }

        if (!matchedCategories.isEmpty()) {
            String category = matchedCategories.get(0);
            
            if (FAQ_DATABASE.containsKey(category)) {
                return getRandomResponse(FAQ_DATABASE.get(category));
            }
            if (SALARY_QA.containsKey(category)) {
                return getRandomResponse(SALARY_QA.get(category));
            }
            if (SKILL_QA.containsKey(category)) {
                return getRandomResponse(SKILL_QA.get(category));
            }
            if (EXPERIENCE_QA.containsKey(category)) {
                return getRandomResponse(EXPERIENCE_QA.get(category));
            }
        }

        return null;
    }

    /**
     * 简单语义相似度匹配
     */
    private String semanticMatch(String question) {
        List<String> allCategories = new ArrayList<>();
        allCategories.addAll(FAQ_DATABASE.keySet());
        allCategories.addAll(SALARY_QA.keySet());
        allCategories.addAll(SKILL_QA.keySet());
        allCategories.addAll(EXPERIENCE_QA.keySet());

        String bestMatch = null;
        int maxScore = 0;

        for (String category : allCategories) {
            int score = calculateSimilarity(question, category);
            if (score > maxScore && score >= SEMANTIC_SIMILARITY_THRESHOLD) {
                maxScore = score;
                bestMatch = category;
            }
        }

        if (bestMatch != null) {
            if (FAQ_DATABASE.containsKey(bestMatch)) {
                return getRandomResponse(FAQ_DATABASE.get(bestMatch));
            }
            if (SALARY_QA.containsKey(bestMatch)) {
                return getRandomResponse(SALARY_QA.get(bestMatch));
            }
            if (SKILL_QA.containsKey(bestMatch)) {
                return getRandomResponse(SKILL_QA.get(bestMatch));
            }
            if (EXPERIENCE_QA.containsKey(bestMatch)) {
                return getRandomResponse(EXPERIENCE_QA.get(bestMatch));
            }
        }

        return null;
    }

    /**
     * 计算字符串相似度（基于字符匹配的简单算法）
     * 
     * @param str1 待比较字符串1（用户输入的问题）
     * @param str2 待比较字符串2（问题类别名称）
     * @return 相似度分数，范围为[0, str2.length()]
     *         分数越高表示相似度越高，≥ SEMANTIC_SIMILARITY_THRESHOLD 时认为匹配成功
     * @note 时间复杂度：O(n*m)，n为str1长度，m为str2长度
     */
    private int calculateSimilarity(String str1, String str2) {
        int score = 0;
        for (char c : str2.toCharArray()) {
            if (str1.indexOf(c) >= 0) {
                score++;
            }
        }
        return score;
    }

    /**
     * 从列表中随机获取一个响应
     */
    private String getRandomResponse(List<String> responses) {
        if (responses == null || responses.isEmpty()) {
            return "抱歉，我暂时无法回答这个问题。";
        }
        return responses.get(new Random().nextInt(responses.size()));
    }

    /**
     * 解析薪资字符串为数值（单位：元）
     * 支持的格式：
     * - "15-25K" 或 "15K-25K" - K格式薪资范围
     * - "20K" - 单值K格式
     * - "10000-18000monthly" 或 "10000-18000" - 完整数字格式薪资范围
     * - "10000" - 单值完整数字格式
     */
    private double parseSalary(String salary) {
        if (salary == null || salary.trim().isEmpty()) {
            return 0;
        }
        
        try {
            String cleanSalary = salary.trim();
            
            // 移除常见的非数字字符
            cleanSalary = cleanSalary.replace("monthly", "").replace("k", "").replace("K", "").replace(" ", "");
            
            // 处理范围格式 "low-high"
            if (cleanSalary.contains("-")) {
                String[] parts = cleanSalary.split("-");
                if (parts.length == 2) {
                    double low = Double.parseDouble(parts[0]);
                    double high = Double.parseDouble(parts[1]);
                    
                    // 判断是否为K格式（数值小于1000的通常是K格式）
                    if (low < 1000 && high < 1000) {
                        return (low + high) / 2 * 1000;
                    } else {
                        return (low + high) / 2;
                    }
                }
            } else {
                // 单值格式
                double value = Double.parseDouble(cleanSalary);
                
                // 判断是否为K格式（数值小于1000的通常是K格式）
                if (value < 1000) {
                    return value * 1000;
                } else {
                    return value;
                }
            }
        } catch (Exception e) {
            log.debug("薪资解析失败: {}", salary);
        }
        return 0;
    }

    @Override
    public String getVersion() {
        return MODEL_VERSION;
    }
}