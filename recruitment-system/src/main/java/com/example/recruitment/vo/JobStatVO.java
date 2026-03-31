package com.example.recruitment.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobStatVO {
    private String name;
    private Long count;
    private BigDecimal avgSalary;
}

