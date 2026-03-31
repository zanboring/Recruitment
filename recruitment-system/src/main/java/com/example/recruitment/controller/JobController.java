package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.service.JobService;
import com.example.recruitment.vo.JobStatVO;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobService jobService;

    @PostMapping
    public Result<Void> add(@RequestBody Job job) {
        jobService.addJob(job);
        return Result.success();
    }

    @PutMapping
    public Result<Void> update(@RequestBody Job job) {
        jobService.updateJob(job);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        jobService.deleteJob(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    public Result<Job> detail(@PathVariable Long id) {
        return Result.success(jobService.getJob(id));
    }

    @PostMapping("/page")
    public Result<PageInfo<Job>> page(@RequestBody JobQueryDTO dto) {
        return Result.success(jobService.listJobs(dto));
    }

    @GetMapping("/stat/city")
    public Result<List<JobStatVO>> statByCity() {
        return Result.success(jobService.statByCity());
    }

    @GetMapping("/stat/company")
    public Result<List<JobStatVO>> statByCompany() {
        return Result.success(jobService.statByCompany());
    }

    @GetMapping("/stat/skill")
    public Result<List<JobStatVO>> statBySkill() {
        return Result.success(jobService.statBySkill());
    }

    @GetMapping("/stat/salary-range")
    public Result<List<JobStatVO>> statBySalaryRange() {
        return Result.success(jobService.statBySalaryRange());
    }

    @GetMapping("/stat/education")
    public Result<List<JobStatVO>> statByEducation() {
        return Result.success(jobService.statByEducation());
    }

    @GetMapping("/stat/experience")
    public Result<List<JobStatVO>> statByExperience() {
        return Result.success(jobService.statByExperience());
    }

    @GetMapping("/stat/status")
    public Result<List<JobStatVO>> statByStatus() {
        return Result.success(jobService.statByStatus());
    }

    @GetMapping("/predict-salary")
    public Result<BigDecimal> predictSalary(@RequestParam(required = false) String city,
                                            @RequestParam(required = false) String experience,
                                            @RequestParam(required = false) String education,
                                            @RequestParam(required = false) String skills) {
        return Result.success(jobService.predictSalary(city, experience, education, skills));
    }

    @GetMapping("/recommend")
    public Result<List<Job>> recommend(@RequestParam(required = false) String skills,
                                       @RequestParam(required = false) String city) {
        return Result.success(jobService.recommendJobs(skills, city));
    }

    @GetMapping("/analysis/summary")
    public Result<String> analysisSummary() {
        return Result.success(jobService.buildAnalysisSummary());
    }

    @GetMapping("/analysis/top-titles")
    public Result<List<JobStatVO>> topTitles() {
        return Result.success(jobService.statTopTitles());
    }
}

