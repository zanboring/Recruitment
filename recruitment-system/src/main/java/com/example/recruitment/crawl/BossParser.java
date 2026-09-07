package com.example.recruitment.crawl;

import com.example.recruitment.entity.Job;
import com.example.recruitment.util.HashUtil;
import com.example.recruitment.util.SalaryUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
public class BossParser implements JobParser {

    @Override
    public String getPlatform() {
        return "BOSS直聘";
    }

    @Override
    public List<Job> parseJobList(Document document) {
        List<Job> jobs = new ArrayList<>();
        // Boss 直聘动态渲染后的卡片选择器（已验证）
        Elements jobCards = document.select("li.job-card-box, div.job-card, .position-item");
        for (Element card : jobCards) {
            try {
                Job job = new Job();
                // 职位名称
                Element titleEl = card.selectFirst(".job-name, .job-title, h3");
                String title = titleEl != null ? titleEl.text() : "未知职位";
                job.setTitle(title);
                
                // 薪资
                Element salaryEl = card.selectFirst(".job-salary, .salary");
                String salary = salaryEl != null ? salaryEl.text() : "";
                SalaryUtil.SalaryRange range = SalaryUtil.parse(salary);
                job.setMinSalary(range.getMin());
                job.setMaxSalary(range.getMax());
                
                // 城市
                Element cityEl = card.selectFirst(".job-addr, .city, .location");
                String city = cityEl != null ? cityEl.text() : "";
                job.setCity(city.replace("[", "").replace("]", "").trim());
                
                // 公司名称
                Element companyEl = card.selectFirst(".company-name, .company, .boss-name");
                String company = companyEl != null ? companyEl.text() : "";
                job.setCompanyName(company);
                job.setSourceSite(getPlatform());
                job.setJobKey(HashUtil.sha256(getPlatform() + title + company));
                job.setJobStatus("active");
                job.setLastSeenAt(java.time.LocalDateTime.now());
                
                // 详情页URL：优先取包含 job_detail 的链接，否则取卡片内第一个有效链接
                String detailUrl = "";
                Element detailLink = card.selectFirst("a[href*=job_detail], a[href*=jobdetail], a[href*='/job/']");
                if (detailLink != null) {
                    detailUrl = detailLink.absUrl("href");
                }
                // 如果上面没拿到，尝试从卡片的 data-job-id 或 onclick 属性拼接
                if (detailUrl == null || detailUrl.isBlank()) {
                    String jobId = card.attr("data-job-id");
                    if (jobId != null && !jobId.isBlank()) {
                        detailUrl = "https://www.zhipin.com/job_detail/" + jobId + ".html";
                    }
                }
                job.setUrl(detailUrl);
                
                jobs.add(job);
            } catch (Exception e) {
                log.warn("Boss直聘岗位卡片解析异常: {}", e.getMessage());
            }
        }
        return jobs;
    }

    @Override
    public Job parseJobDetail(Document document, Job job) {
        try {
            // 职位描述
            String desc = firstText(document, ".job-detail-box, .job-desc, .description, .detail-content");
            job.setJobDesc(desc);
            
            // 经验要求
            String exp = firstMatch(document.text(), "(?i)(\\d+[-~至]\\d+年|\\d+年经验?|经验不限|应届生|实习生?)");
            job.setExperience(exp.isEmpty() ? "经验不限" : exp);
            
            // 学历要求
            String edu = firstMatch(document.text(), "(?i)(大专|本科|硕士|博士|不限学历?|学历不限)");
            job.setEducation(edu.isEmpty() ? "不限" : edu);
            
            // 发布时间
            String publish = firstMatch(document.text(), "(?i)(\\d{1,2}月\\d{1,2}日|\\d+天前|今天|昨天)");
            job.setPublishTime(parsePublishTime(publish));
            
            // 技能标签
            String skills = extractSkills(document.text());
            job.setSkills(skills);
            
        } catch (Exception e) {
            log.warn("Boss直聘岗位详情解析异常: {}", e.getMessage());
        }
        return job;
    }

    private String firstText(Element parent, String css) {
        Element e = parent.selectFirst(css);
        return e == null ? "" : e.text().trim();
    }

    private String firstMatch(String text, String regex) {
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractSkills(String text) {
        List<String> skills = new java.util.ArrayList<>();
        String[] skillKeywords = {"Java", "Spring", "Vue", "React", "Python", "MySQL", "Redis"};
        for (String skill : skillKeywords) {
            if (text.contains(skill)) {
                skills.add(skill);
            }
        }
        return String.join(",", skills);
    }

    private java.time.LocalDateTime parsePublishTime(String publishText) {
        if (publishText == null || publishText.isBlank()) {
            return java.time.LocalDateTime.now();
        }
        try {
            if (publishText.contains("今天")) {
                return java.time.LocalDateTime.now();
            }
            if (publishText.contains("天前")) {
                int days = Integer.parseInt(publishText.replaceAll("\\D+", ""));
                return java.time.LocalDateTime.now().minusDays(days);
            }
            return java.time.LocalDate.parse(publishText, 
                java.time.format.DateTimeFormatter.ofPattern("MM-dd"))
                .atStartOfDay();
        } catch (Exception e) {
            return java.time.LocalDateTime.now();
        }
    }
}