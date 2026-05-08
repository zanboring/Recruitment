package com.example.recruitment.crawl;

import com.example.recruitment.entity.Job;
import com.example.recruitment.util.HashUtil;
import com.example.recruitment.util.SalaryUtil;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class ZhaopinParser implements JobParser {

    @Override
    public String getPlatform() {
        return "智联招聘";
    }

    @Override
    public List<Job> parseJobList(Document document) {
        List<Job> jobs = new ArrayList<>();
        Elements jobCards = document.select(".joblist-box");
        for (int i = 0; i < jobCards.size(); i++) {
            try {
                var card = jobCards.get(i);
                Job job = new Job();
                
                String title = card.select(".job-title").first().text();
                job.setTitle(title);
                
                String salary = card.select(".salary").first().text();
                SalaryUtil.SalaryRange range = SalaryUtil.parse(salary);
                job.setMinSalary(range.getMin());
                job.setMaxSalary(range.getMax());
                
                String company = card.select(".company-name").first().text();
                job.setCompanyName(company);
                
                String infoText = card.text();
                String city = extractCity(infoText);
                job.setCity(city);
                
                job.setJobKey(HashUtil.sha256(getPlatform() + title + company));
                jobs.add(job);
            } catch (Exception e) {
                log.warn("智联招聘岗位卡片解析异常: {}", e.getMessage());
            }
        }
        return jobs;
    }

    @Override
    public Job parseJobDetail(Document document, Job job) {
        return job;
    }

    private String extractCity(String text) {
        Pattern pattern = Pattern.compile("(北京|上海|广州|深圳|杭州|南京|成都|武汉|长沙)");
        Matcher matcher = pattern.matcher(text);
        return matcher.find() ? matcher.group(1) : "未知";
    }
}