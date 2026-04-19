package com.example.recruitment.vo;

import java.math.BigDecimal;

public class JobStatVO {
    private String name;
    private Long count;
    private BigDecimal avgSalary;

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
    public BigDecimal getAvgSalary() { return avgSalary; }
    public void setAvgSalary(BigDecimal avgSalary) { this.avgSalary = avgSalary; }
}