package com.example.recruitment.service;

import com.example.recruitment.entity.CrawlTask;

import java.util.List;

public interface CrawlService {

    Long createTask(CrawlTask task);

    void startTask(Long taskId);

    List<CrawlTask> listTasks();

    void deleteTask(Long taskId);
}

