package com.example.recruitment.crawl;

import com.example.recruitment.entity.Job;
import org.jsoup.nodes.Document;

import java.util.List;

public interface JobParser {

    String getPlatform();

    List<Job> parseJobList(Document document);

    Job parseJobDetail(Document document, Job job);
}