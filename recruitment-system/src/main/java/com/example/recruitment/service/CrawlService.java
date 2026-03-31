package com.example.recruitment.service;

import com.example.recruitment.entity.CrawlTask;

public interface CrawlService {

    Long createTask(CrawlTask task);

    void startTask(Long taskId);
}

