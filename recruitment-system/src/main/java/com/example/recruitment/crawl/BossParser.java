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
        Elements jobCards = document.select(".job-card-box");
        for (Element card : jobCards) {
            try {
                Job job = new Job();
                String title = card.select(".job-title").first().text();
                job.setTitle(title);
                
                String salary = card.select(".salary").first().text();
                SalaryUtil.SalaryRange range = SalaryUtil.parse(salary);
                job.setMinSalary(range.getMin());
                job.setMaxSalary(range.getMax());
                
                String city = card.select(".city").first().text();
                job.setCity(city.replace("[", "").replace("]", ""));
                
                String company = card.select(".company-name").first().text();
                job.setCompanyName(company);
                job.setJobKey(HashUtil.sha256(getPlatform() + title + company));
                
                jobs.add(job);
            } catch (Exception e) {
                log.warn("岗位卡片解析异常: {}", e.getMessage());
            }
        }
        return jobs;
    }

    @Override
    public Job parseJobDetail(Document document, Job job) {
        try {
            String desc = firstText(document, ".job-desc, .job-detail, .description");
            job.setJobDesc(desc);
            
            String exp = firstMatch(document.text(), "(\\d+[-至]\\d+年|\\d+年|经验不限)");
            job.setExperience(exp.isEmpty() ? "经验不限" : exp);
            
            String edu = firstMatch(document.text(), "(大专|本科|硕士|博士|不限)");
            job.setEducation(edu.isEmpty() ? "不限" : edu);
            
            String publish = firstMatch(document.text(), "(\\d{1,2}-\\d{1,2}|\\d+天前|今天)");
            job.setPublishTime(parsePublishTime(publish));
        } catch (Exception e) {
            log.warn("岗位详情解析异常: {}", e.getMessage());
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