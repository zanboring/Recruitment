package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.service.DataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
@Tag(name = "数据管理", description = "数据导入导出接口")
public class DataController {

    private final DataService dataService;

    @PostMapping("/import")
    @Operation(summary = "导入数据", description = "从Excel文件导入招聘数据")
    public Result<Void> importJobs(@RequestPart("file") MultipartFile file) {
        log.info("导入数据请求: fileName={}, size={}", file.getOriginalFilename(), file.getSize());
        dataService.importJobs(file);
        log.info("数据导入成功");
        return Result.success();
    }

    @GetMapping("/export")
    @Operation(summary = "导出数据", description = "导出招聘数据到Excel文件")
    public void exportJobs(HttpServletResponse response,
                             @RequestParam(required = false, defaultValue = "2000") Integer limit) {
        log.info("导出数据请求: limit={}", limit);
        dataService.exportJobs(response, limit);
        log.info("数据导出成功");
    }
}

