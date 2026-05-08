package com.example.recruitment.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobRecommendVO {
    private Long jobId;
    private String title;
    private String companyName;
    private String city;
    private String salary;
    private String skills;
    private String education;
    private String experience;
    
    private String url;
    private String sourceSite;
    
    private BigDecimal overallScore;
    private BigDecimal skillMatchScore;
    private BigDecimal educationMatchScore;
    private BigDecimal experienceMatchScore;
}