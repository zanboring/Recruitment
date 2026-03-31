package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.entity.CrawlTask;
import com.example.recruitment.service.CrawlService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
public class CrawlController {

    private final CrawlService crawlService;

    @PostMapping("/task")
    public Result<Long> createTask(@Valid @RequestBody CrawlTask task) {
        return Result.success(crawlService.createTask(task));
    }

    @PostMapping("/task/{id}/start")
    public Result<Void> start(@PathVariable("id") Long id) {
        crawlService.startTask(id);
        return Result.success();
    }
}

