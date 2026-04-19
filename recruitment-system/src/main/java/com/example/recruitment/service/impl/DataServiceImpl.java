package com.example.recruitment.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.recruitment.dto.JobExcelRow;
import com.example.recruitment.entity.Job;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.JobMapper;
import com.example.recruitment.service.DataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private final JobMapper jobMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void importJobs(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Excel 文件");
        }
        
        log.info("开始导入Excel数据: fileName={}, size={} bytes", file.getOriginalFilename(), file.getSize());
        
        List<JobExcelRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(JobExcelRow.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            log.error("Excel解析失败: {}", e.getMessage(), e);
            throw new BusinessException("Excel 解析失败：" + e.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            log.warn("Excel文件为空或无有效数据");
            return;
        }

        log.info("Excel解析成功，共{}行数据", rows.size());
        
        int successCount = 0;
        int skipCount = 0;
        
        for (JobExcelRow r : rows) {
            if (r == null || r.getTitle() == null || r.getTitle().trim().isEmpty()) {
                skipCount++;
                continue;
            }
            
            Job job = new Job();
            job.setCompanyId(r.getCompanyId());
            job.setTitle(r.getTitle());
            job.setCity(r.getCity());
            job.setExperience(r.getExperience());
            job.setEducation(r.getEducation());
            job.setMinSalary(r.getMinSalary());
            job.setMaxSalary(r.getMaxSalary());
            job.setSalaryUnit(r.getSalaryUnit());
            job.setSkills(r.getSkills());
            job.setJobDesc(r.getJobDesc());
            job.setCreatedAt(LocalDateTime.now());
            job.setPublishTime(parsePublishTime(r.getPublishTime()));
            
            jobMapper.insert(job);
            successCount++;
        }
        
        log.info("数据导入完成: 成功{}条，跳过{}条", successCount, skipCount);
    }

    @Override
    public void exportJobs(HttpServletResponse response, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 2000 : Math.min(limit, 10000);
        
        log.info("开始导出Excel数据: limit={}", safeLimit);

        List<Job> jobs = jobMapper.selectForExport(safeLimit);
        
        log.info("查询到{}条数据待导出", jobs.size());
        
        List<JobExcelRow> rows = new ArrayList<>();
        if (jobs != null) {
            for (Job j : jobs) {
                JobExcelRow r = new JobExcelRow();
                r.setCompanyId(j.getCompanyId());
                r.setTitle(j.getTitle());
                r.setCity(j.getCity());
                r.setExperience(j.getExperience());
                r.setEducation(j.getEducation());
                r.setMinSalary(j.getMinSalary());
                r.setMaxSalary(j.getMaxSalary());
                r.setSalaryUnit(j.getSalaryUnit());
                r.setSkills(j.getSkills());
                r.setJobDesc(j.getJobDesc());
                r.setPublishTime(j.getPublishTime() == null ? null : j.getPublishTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
                rows.add(r);
            }
        }

        try {
            String fileName = URLEncoder.encode("招聘数据_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName);
            
            EasyExcel.write(response.getOutputStream())
                    .head(JobExcelRow.class)
                    .sheet("招聘数据")
                    .doWrite(rows);
            
            log.info("数据导出成功: {}条", rows.size());
        } catch (Exception e) {
            log.error("Excel导出失败: {}", e.getMessage(), e);
            throw new BusinessException("Excel 导出失败：" + e.getMessage());
        }
    }

    private LocalDateTime parsePublishTime(String publishTime) {
        if (publishTime == null || publishTime.trim().isEmpty()) {
            return null;
        }
        String s = publishTime.trim();
        try {
            return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        } catch (Exception ignore) {
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                log.debug("无法解析发布时间: {}", s);
                return null;
            }
        }
    }
}

