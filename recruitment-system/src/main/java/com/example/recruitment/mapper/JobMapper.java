package com.example.recruitment.mapper;

import com.example.recruitment.dto.JobQueryDTO;
import com.example.recruitment.entity.Job;
import com.example.recruitment.vo.JobStatVO;
import com.example.recruitment.vo.JobTrendVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface JobMapper {

    int insert(Job job);

    int update(Job job);

    int deleteById(Long id);

    Job selectById(Long id);

    List<Job> selectByCondition(JobQueryDTO dto);

    List<JobStatVO> statByCity();

    List<JobStatVO> statByCompany();

    List<JobStatVO> statBySkill();

    List<JobStatVO> statBySalaryRange();

    List<JobStatVO> statByEducation();

    List<JobStatVO> statByExperience();

    List<JobStatVO> statByStatus();

    List<JobStatVO> statTopTitles();

    JobTrendVO jobTrendLast7Days();

    BigDecimal predictSalary(@Param("city") String city,
                             @Param("experience") String experience,
                             @Param("education") String education,
                             @Param("skills") String skills);

    List<Job> selectForExport(@Param("limit") Integer limit);

    Job selectByJobKey(@Param("jobKey") String jobKey);

    int markOfflineByAbsentKeys(@Param("sourceSite") String sourceSite,
                                @Param("keys") List<String> keys);

    int markOfflineWhenNoKeys(@Param("sourceSite") String sourceSite);
}

