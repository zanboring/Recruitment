package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.service.DataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/data")
@RequiredArgsConstructor
public class DataController {

    private final DataService dataService;

    @PostMapping("/import")
    public Result<Void> importJobs(@RequestPart("file") MultipartFile file) {
        dataService.importJobs(file);
        return Result.success();
    }

    @GetMapping("/export")
    public void exportJobs(HttpServletResponse response,
                             @RequestParam(required = false, defaultValue = "2000") Integer limit) {
        dataService.exportJobs(response, limit);
    }
}

