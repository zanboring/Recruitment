package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.service.JobService;
import com.example.recruitment.vo.AIFeedbackVO;
import com.example.recruitment.vo.JobStatVO;
import com.github.pagehelper.PageInfo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

@Slf4j
@RestController
@RequestMapping("/api/jobs")
@RequiredArgsConstructor
@Tag(name = "岗位管理", description = "岗位信息查询、统计、分析等接口")
public class JobController {

    private final JobService jobService;

    @PostMapping
    @Operation(summary = "新增岗位", description = "添加新的岗位信息")
    public Result<Void> add(@RequestBody Job job) {
        log.info("新增岗位: title={}, company={}", job.getTitle(), job.getCompanyName());
        jobService.addJob(job);
        return Result.success();
    }

    @PutMapping
    @Operation(summary = "更新岗位", description = "更新岗位信息")
    public Result<Void> update(@RequestBody Job job) {
        log.info("更新岗位: id={}, title={}", job.getId(), job.getTitle());
        jobService.updateJob(job);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除岗位", description = "根据ID删除岗位")
    public Result<Void> delete(@PathVariable Long id) {
        log.info("删除岗位: id={}", id);
        jobService.deleteJob(id);
        return Result.success();
    }

    @GetMapping("/{id}")
    @Operation(summary = "岗位详情", description = "根据ID查询岗位详情")
    public Result<Job> detail(@PathVariable Long id) {
        log.info("查询岗位详情: id={}", id);
        return Result.success(jobService.getJob(id));
    }

    @PostMapping("/page")
    @Operation(summary = "岗位列表", description = "分页查询岗位列表，支持多条件筛选")
    public Result<PageInfo<Job>> page(@RequestBody JobQueryDTO dto) {
        log.info("查询岗位列表: page={}, size={}, keyword={}", dto.getPageNum(), dto.getPageSize(), dto.getKeyword());
        return Result.success(jobService.listJobs(dto));
    }

    @GetMapping("/stat/city")
    @Operation(summary = "城市统计", description = "按城市统计岗位数量和平均薪资")
    public Result<List<JobStatVO>> statByCity() {
        return Result.success(jobService.statByCity());
    }

    @GetMapping("/stat/company")
    @Operation(summary = "企业统计", description = "按企业统计岗位数量和平均薪资")
    public Result<List<JobStatVO>> statByCompany() {
        return Result.success(jobService.statByCompany());
    }

    @GetMapping("/stat/skill")
    @Operation(summary = "技能统计", description = "按技能统计岗位数量和平均薪资")
    public Result<List<JobStatVO>> statBySkill() {
        return Result.success(jobService.statBySkill());
    }

    @GetMapping("/stat/salary-range")
    @Operation(summary = "薪资区间统计", description = "按薪资区间统计岗位数量")
    public Result<List<JobStatVO>> statBySalaryRange() {
        return Result.success(jobService.statBySalaryRange());
    }

    @GetMapping("/stat/education")
    @Operation(summary = "学历统计", description = "按学历要求统计岗位数量")
    public Result<List<JobStatVO>> statByEducation() {
        return Result.success(jobService.statByEducation());
    }

    @GetMapping("/stat/experience")
    @Operation(summary = "经验统计", description = "按经验要求统计岗位数量")
    public Result<List<JobStatVO>> statByExperience() {
        return Result.success(jobService.statByExperience());
    }

    @GetMapping("/stat/status")
    @Operation(summary = "状态统计", description = "按岗位状态统计数量")
    public Result<List<JobStatVO>> statByStatus() {
        return Result.success(jobService.statByStatus());
    }

    @GetMapping("/predict-salary")
    @Operation(summary = "薪资预测", description = "根据条件预测平均薪资")
    public Result<BigDecimal> predictSalary(@RequestParam(required = false) String city,
                                            @RequestParam(required = false) String experience,
                                            @RequestParam(required = false) String education,
                                            @RequestParam(required = false) String skills) {
        log.info("薪资预测: city={}, experience={}, education={}", city, experience, education);
        return Result.success(jobService.predictSalary(city, experience, education, skills));
    }

    @GetMapping("/recommend")
    @Operation(summary = "岗位推荐", description = "根据技能和城市推荐岗位")
    public Result<List<Job>> recommend(@RequestParam(required = false) String skills,
                                       @RequestParam(required = false) String city) {
        log.info("岗位推荐: skills={}, city={}", skills, city);
        return Result.success(jobService.recommendJobs(skills, city));
    }

    @GetMapping("/analysis/summary")
    @Operation(summary = "分析摘要", description = "生成数据分析摘要")
    public Result<String> analysisSummary() {
        return Result.success(jobService.buildAnalysisSummary());
    }

    @GetMapping("/analysis/top-titles")
    @Operation(summary = "热门岗位", description = "查询热门岗位TOP10")
    public Result<List<JobStatVO>> topTitles() {
        return Result.success(jobService.statTopTitles());
    }

    @PostMapping("/ai-analysis")
    @Operation(summary = "AI智能分析", description = "基于筛选条件进行AI智能分析")
    public Result<AIFeedbackVO> aiAnalysis(@RequestBody(required = false) JobQueryDTO dto) {
        log.info("AI智能分析请求");
        return Result.success(jobService.analyzeWithAI(dto));
    }
}

