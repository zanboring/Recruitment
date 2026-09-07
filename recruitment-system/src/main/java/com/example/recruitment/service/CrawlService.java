package com.example.recruitment.service;

import com.example.recruitment.entity.CrawlTask;
import com.example.recruitment.entity.Job;

import java.util.List;

public interface CrawlService {

    Long createTask(CrawlTask task);

    void startTask(Long taskId);

    List<CrawlTask> listTasks();

    void deleteTask(Long taskId);

    List<Job> crawlJobs(String site, String keyword, String city);
}