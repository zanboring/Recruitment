package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.entity.CrawlTask;
import com.example.recruitment.service.CrawlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;

@Slf4j
@RestController
@RequestMapping("/api/crawl")
@RequiredArgsConstructor
@Tag(name = "爬虫管理", description = "爬虫任务创建、启动、查询接口")
public class CrawlController {

    private final CrawlService crawlService;

    @PostMapping("/task")
    @Operation(summary = "创建爬虫任务", description = "创建新的数据爬取任务")
    @PreAuthorize("hasAuthority('crawl:manage') or hasRole('ADMIN')")
    public Result<Long> createTask(@Valid @RequestBody CrawlTask task) {
        log.info("创建爬虫任务: sourceSite={}, keyword={}, city={}", 
            task.getSourceSite(), task.getKeyword(), task.getCity());
        Long taskId = crawlService.createTask(task);
        log.info("爬虫任务创建成功: taskId={}", taskId);
        return Result.success(taskId);
    }

    @PostMapping("/task/{id}/start")
    @Operation(summary = "启动爬虫任务", description = "启动指定的爬虫任务")
    @PreAuthorize("hasAuthority('crawl:manage') or hasRole('ADMIN')")
    public Result<Void> start(@PathVariable("id") Long id) {
        log.info("启动爬虫任务: taskId={}", id);
        crawlService.startTask(id);
        log.info("爬虫任务已启动: taskId={}", id);
        return Result.success();
    }

    @GetMapping("/tasks")
    @Operation(summary = "查询爬虫任务", description = "查询所有爬虫任务列表")
    public Result<List<CrawlTask>> listTasks() {
        log.info("查询爬虫任务列表");
        List<CrawlTask> tasks = crawlService.listTasks();
        log.info("查询到{}个爬虫任务", tasks.size());
        return Result.success(tasks);
    }

    @PostMapping("/task/{id}/delete")
    @Operation(summary = "删除爬虫任务", description = "删除指定的爬虫任务")
    @PreAuthorize("hasAuthority('crawl:manage') or hasRole('ADMIN')")
    public Result<Void> deleteTask(@PathVariable("id") Long id) {
        log.info("删除爬虫任务: taskId={}", id);
        crawlService.deleteTask(id);
        log.info("爬虫任务已删除: taskId={}", id);
        return Result.success();
    }
}

