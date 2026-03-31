package com.example.recruitment.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.recruitment.dto.JobExcelRow;
import com.example.recruitment.entity.Job;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.JobMapper;
import com.example.recruitment.service.DataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
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

@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private final JobMapper jobMapper;

    @Override
    @Transactional
    public void importJobs(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择要导入的 Excel 文件");
        }
        List<JobExcelRow> rows;
        try {
            rows = EasyExcel.read(file.getInputStream())
                    .head(JobExcelRow.class)
                    .sheet()
                    .doReadSync();
        } catch (Exception e) {
            throw new BusinessException("Excel 解析失败：" + e.getMessage());
        }

        if (rows == null || rows.isEmpty()) {
            return;
        }

        for (JobExcelRow r : rows) {
            if (r == null || r.getTitle() == null || r.getTitle().trim().isEmpty()) {
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
        }
    }

    @Override
    public void exportJobs(HttpServletResponse response, Integer limit) {
        int safeLimit = (limit == null || limit <= 0) ? 2000 : Math.min(limit, 10000);

        List<Job> jobs = jobMapper.selectForExport(safeLimit);
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
            String fileName = URLEncoder.encode("招聘数据.xlsx", StandardCharsets.UTF_8).replaceAll("\\+", "%20");
            response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            response.setHeader("Content-Disposition", "attachment;filename*=utf-8''" + fileName);
            EasyExcel.write(response.getOutputStream())
                    .head(JobExcelRow.class)
                    .sheet("sheet1")
                    .doWrite(rows);
        } catch (Exception e) {
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
            // fallback: only date
            try {
                return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            } catch (Exception e) {
                return null;
            }
        }
    }
}

