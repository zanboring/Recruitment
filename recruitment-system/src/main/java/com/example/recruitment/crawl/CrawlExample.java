package com.example.recruitment.crawl;

import com.example.recruitment.entity.Job;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CrawlExample {

    private final ParserFactory parserFactory;

    public void demonstrateCrawlProcess(String siteCode, String url) {
        try {
            JobParser parser = parserFactory.getParserByCode(siteCode);
            if (parser == null) {
                log.warn("未找到对应的解析器: {}", siteCode);
                return;
            }

            log.info("开始爬取平台: {}", parser.getPlatform());

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(30000)
                    .get();

            List<Job> jobs = parser.parseJobList(doc);

            log.info("成功解析 {} 个岗位", jobs.size());

            for (Job job : jobs) {
                log.info("岗位: {}, 公司: {}, 薪资: {}-{}",
                        job.getTitle(),
                        job.getCompanyName(),
                        job.getMinSalary(),
                        job.getMaxSalary());
            }

        } catch (Exception e) {
            log.error("爬取失败: {}", e.getMessage());
        }
    }
}
