package com.example.recruitment.service;

import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.vo.JobStatVO;
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

    BigDecimal predictSalary(String city, String experience, String education, String skills);

    List<Job> recommendJobs(String skills, String city);
}

