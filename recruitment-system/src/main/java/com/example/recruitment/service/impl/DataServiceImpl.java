package com.example.recruitment.service.impl;

import com.alibaba.excel.EasyExcel;
import com.example.recruitment.dto.JobExcelRow;
import com.example.recruitment.entity.Job;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.CrawlTaskMapper;
import com.example.recruitment.mapper.JobMapper;
import com.example.recruitment.service.DataService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DataServiceImpl implements DataService {

    private final JobMapper jobMapper;
    private final CrawlTaskMapper crawlTaskMapper;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int cleanupAllData() {
        int deletedJobs = jobMapper.deleteAll();
        int deletedTasks = crawlTaskMapper.deleteAll();
        log.warn("[数据清洗操作] 删除岗位{}条, 删除任务{}条", deletedJobs, deletedTasks);
        return deletedJobs;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DataCleanupResult cleanAndStandardizeData() {
        log.info("开始数据清洗标准化任务");
        
        int totalCount = jobMapper.countAll();
        int successCount = 0;
        int deletedCount = 0;
        int failedCount = 0;
        
        List<Job> allJobs = jobMapper.selectAll();
        
        for (Job job : allJobs) {
            try {
                // 空值数据过滤
                if (isEmptyJob(job)) {
                    jobMapper.deleteById(job.getId());
                    deletedCount++;
                    continue;
                }
                
                // 薪资数据标准化
                standardizeSalary(job);
                
                // 学历要求标准化
                standardizeEducation(job);
                
                // 工作经验标准化
                standardizeExperience(job);
                
                jobMapper.updateById(job);
                successCount++;
            } catch (Exception e) {
                log.warn("清洗岗位数据失败: jobId={}, error={}", job.getId(), e.getMessage());
                failedCount++;
            }
        }
        
        log.info("数据清洗完成: 总数={}, 成功={}, 删除={}, 失败={}", totalCount, successCount, deletedCount, failedCount);
        
        return new DataCleanupResult(totalCount, successCount, deletedCount, failedCount);
    }

    private boolean isEmptyJob(Job job) {
        return job.getTitle() == null || job.getTitle().trim().isEmpty()
                || job.getCompanyId() == null;
    }

    private void standardizeSalary(Job job) {
        if (job.getMinSalary() != null && job.getMaxSalary() != null) {
            return;
        }
        
        String salary = job.getSalary();
        if (salary == null || salary.trim().isEmpty() || salary.contains("面议")) {
            return;
        }
        
        try {
            String s = salary.trim();
            // 处理 "15-20K" 格式
            if (s.matches("\\d+[-~到]\\d+[Kk]")) {
                String[] parts = s.replaceAll("[Kk]", "").split("[-~到]");
                job.setMinSalary(BigDecimal.valueOf(Long.parseLong(parts[0].trim()) * 1000));
                job.setMaxSalary(BigDecimal.valueOf(Long.parseLong(parts[1].trim()) * 1000));
            }
            // 处理 "15-20千/月" 格式
            else if (s.matches("\\d+[-~到]\\d+千")) {
                String[] parts = s.replace("千", "").split("[-~到]");
                job.setMinSalary(BigDecimal.valueOf(Long.parseLong(parts[0].trim()) * 1000));
                job.setMaxSalary(BigDecimal.valueOf(Long.parseLong(parts[1].trim()) * 1000));
            }
            // 处理 "2-3万/月" 格式
            else if (s.matches("\\d+[-~到]\\d+万")) {
                String[] parts = s.replace("万", "").split("[-~到]");
                job.setMinSalary(BigDecimal.valueOf(Long.parseLong(parts[0].trim()) * 10000));
                job.setMaxSalary(BigDecimal.valueOf(Long.parseLong(parts[1].trim()) * 10000));
            }
            job.setSalaryUnit("元/月");
        } catch (Exception e) {
            log.debug("薪资解析失败: {}", salary);
        }
    }

    private void standardizeEducation(Job job) {
        String education = job.getEducation();
        if (education == null) {
            return;
        }
        
        String standardized = education.trim();
        
        // 标准化学历值
        if (standardized.contains("博士") || standardized.contains("PhD")) {
            job.setEducation("博士");
        } else if (standardized.contains("硕士") || standardized.contains("研究生")) {
            job.setEducation("硕士");
        } else if (standardized.contains("本科") || standardized.contains("学士")) {
            job.setEducation("本科");
        } else if (standardized.contains("大专") || standardized.contains("专科")) {
            job.setEducation("大专");
        } else if (standardized.contains("高中") || standardized.contains("中专")) {
            job.setEducation("高中");
        } else if (standardized.contains("不限") || standardized.contains("无要求")) {
            job.setEducation("不限");
        }
    }

    private void standardizeExperience(Job job) {
        String experience = job.getExperience();
        if (experience == null) {
            return;
        }
        
        String standardized = experience.trim();
        
        // 标准化经验值
        if (standardized.contains("不限") || standardized.contains("经验不限")) {
            job.setExperience("经验不限");
        } else if (standardized.contains("应届") || standardized.contains("毕业生")) {
            job.setExperience("应届");
        } else if (standardized.contains("1年") || standardized.contains("一") && standardized.contains("年")) {
            if (standardized.contains("3")) {
                job.setExperience("1-3年");
            } else {
                job.setExperience("1年以下");
            }
        } else if (standardized.contains("3年")) {
            if (standardized.contains("5")) {
                job.setExperience("3-5年");
            } else {
                job.setExperience("1-3年");
            }
        } else if (standardized.contains("5年")) {
            if (standardized.contains("10")) {
                job.setExperience("5-10年");
            } else {
                job.setExperience("3-5年");
            }
        } else if (standardized.contains("10年") || standardized.contains("十年")) {
            job.setExperience("10年以上");
        }
    }

    public static class DataCleanupResult {
        private final int total;
        private final int success;
        private final int deleted;
        private final int failed;

        public DataCleanupResult(int total, int success, int deleted, int failed) {
            this.total = total;
            this.success = success;
            this.deleted = deleted;
            this.failed = failed;
        }

        public int getTotal() { return total; }
        public int getSuccess() { return success; }
        public int getDeleted() { return deleted; }
        public int getFailed() { return failed; }
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

