package com.example.recruitment.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Job {
    private Long id;
    private Long companyId;
    private String title;
    private String city;
    private String experience;
    private String education;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String salaryUnit;
    private String skills;
    private String jobDesc;
    private LocalDateTime publishTime;
    private LocalDateTime createdAt;
}

