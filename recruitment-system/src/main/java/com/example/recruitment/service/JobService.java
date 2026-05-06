package com.example.recruitment.service;

import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.dto.JobRecommendDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.vo.AIFeedbackVO;
import com.example.recruitment.vo.JobRecommendVO;
import com.example.recruitment.vo.JobStatVO;
import com.example.recruitment.vo.JobTrendVO;
import com.github.pagehelper.PageInfo;

import java.math.BigDecimal;
import java.util.List;

public interface JobService {

    void addJob(Job job);

    void updateJob(Job job);

    void deleteJob(Long id);

    Job getJob(Long id);

    PageInfo<Job> listJobs(JobQueryDTO dto);

    List<JobStatVO> statByCity();

    List<JobStatVO> statByCompany();

    List<JobStatVO> statBySkill();

    List<JobStatVO> statBySalaryRange();

    List<JobStatVO> statByEducation();

    List<JobStatVO> statByExperience();

    List<JobStatVO> statByStatus();

    List<JobStatVO> statTopTitles();

    JobTrendVO jobTrendLast7Days();

    BigDecimal predictSalary(String city, String experience, String education, String skills);

    List<Job> recommendJobs(String skills, String city);

    String buildAnalysisSummary();

    /**
     * AI智能分析 - 基于筛选条件分析优质岗位、推荐理由、需求趋势、求职建议
     * @param dto 筛选条件
     * @return AI分析结果
     */
    AIFeedbackVO analyzeWithAI(JobQueryDTO dto);

    /**
     * 智能岗位推荐 - 基于多级权重合成的技能契合算法
     * 综合考量：技能契合度(70%)、教育背景(20%)、工作经验(10%)
     * @param dto 用户信息（技能、学历、工作经验年限、城市）
     * @return 推荐岗位列表（按综合契合度排序）
     */
    List<JobRecommendVO> intelligentRecommend(JobRecommendDTO dto);
}

