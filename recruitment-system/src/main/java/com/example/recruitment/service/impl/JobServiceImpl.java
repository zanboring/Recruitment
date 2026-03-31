package com.example.recruitment.service.impl;

import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.JobMapper;
import com.example.recruitment.service.JobService;
import com.example.recruitment.vo.JobStatVO;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobMapper jobMapper;

    @Override
    public void addJob(Job job) {
        job.setCreatedAt(LocalDateTime.now());
        jobMapper.insert(job);
    }

    @Override
    public void updateJob(Job job) {
        if (job.getId() == null) {
            throw new BusinessException("ID 不能为空");
        }
        jobMapper.update(job);
    }

    @Override
    public void deleteJob(Long id) {
        jobMapper.deleteById(id);
    }

    @Override
    public Job getJob(Long id) {
        return jobMapper.selectById(id);
    }

    @Override
    public PageInfo<Job> listJobs(JobQueryDTO dto) {
        PageHelper.startPage(dto.getPageNum(), dto.getPageSize());
        List<Job> list = jobMapper.selectByCondition(dto);
        return new PageInfo<>(list);
    }

    @Override
    public List<JobStatVO> statByCity() {
        return jobMapper.statByCity();
    }

    @Override
    public List<JobStatVO> statByCompany() {
        return jobMapper.statByCompany();
    }

    @Override
    public List<JobStatVO> statBySkill() {
        return jobMapper.statBySkill();
    }

    @Override
    public List<JobStatVO> statBySalaryRange() {
        return jobMapper.statBySalaryRange();
    }

    @Override
    public List<JobStatVO> statByEducation() {
        return jobMapper.statByEducation();
    }

    @Override
    public List<JobStatVO> statByExperience() {
        return jobMapper.statByExperience();
    }

    @Override
    public List<JobStatVO> statByStatus() {
        return jobMapper.statByStatus();
    }

    @Override
    public BigDecimal predictSalary(String city, String experience, String education, String skills) {
        BigDecimal avg = jobMapper.predictSalary(city, experience, education, skills);
        if (avg == null) {
            return BigDecimal.ZERO;
        }
        return avg;
    }

    @Override
    public List<Job> recommendJobs(String skills, String city) {
        JobQueryDTO dto = new JobQueryDTO();
        dto.setCity(city);
        dto.setKeyword(null);
        dto.setPageNum(1);
        dto.setPageSize(100);
        List<Job> list = jobMapper.selectByCondition(dto);

        if (skills == null || skills.isEmpty()) {
            return list.stream().limit(10).collect(Collectors.toList());
        }
        String[] wanted = skills.toLowerCase(Locale.ROOT).split(",");
        return list.stream()
                .sorted(Comparator.comparingInt(j -> -matchScore((Job) j, wanted)))
                .limit(10)
                .collect(Collectors.toList());
    }

    @Override
    public String buildAnalysisSummary() {
        List<JobStatVO> cityStats = statByCity();
        List<JobStatVO> skillStats = statBySkill();
        List<JobStatVO> salaryStats = statBySalaryRange();

        JobStatVO topCity = cityStats.isEmpty() ? null : cityStats.get(0);
        JobStatVO topSkill = skillStats.isEmpty() ? null : skillStats.get(0);
        BigDecimal avg = predictSalary(null, null, null, null);
        JobStatVO topBand = salaryStats.stream()
                .filter(Objects::nonNull)
                .max(Comparator.comparingLong(v -> v.getCount() == null ? 0L : v.getCount()))
                .orElse(null);

        return String.format(
                "当前岗位需求较集中的城市为%s；热门技能以%s为代表；平均薪资约为%s元。薪资分布以%s区间为主，整体呈现技术岗位需求持续稳定的趋势。",
                topCity == null ? "暂无数据" : topCity.getName(),
                topSkill == null ? "暂无数据" : topSkill.getName(),
                avg == null ? "0.00" : avg.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString(),
                topBand == null ? "暂无数据" : topBand.getName()
        );
    }

    private int matchScore(Job job, String[] wanted) {
        if (job.getSkills() == null) return 0;
        String s = job.getSkills().toLowerCase(Locale.ROOT);
        int score = 0;
        for (String w : wanted) {
            if (s.contains(w.trim())) {
                score++;
            }
        }
        return score;
    }
}

