package com.example.recruitment.dto;

import com.alibaba.excel.annotation.ExcelProperty;

import java.math.BigDecimal;

public class JobExcelRow {

    @ExcelProperty(index = 0, value = "公司ID")
    private Long companyId;

    @ExcelProperty(index = 1, value = "岗位名称")
    private String title;

    @ExcelProperty(index = 2, value = "工作城市")
    private String city;

    @ExcelProperty(index = 3, value = "经验要求")
    private String experience;

    @ExcelProperty(index = 4, value = "学历要求")
    private String education;

    @ExcelProperty(index = 5, value = "最低薪资")
    private BigDecimal minSalary;

    @ExcelProperty(index = 6, value = "最高薪资")
    private BigDecimal maxSalary;

    @ExcelProperty(index = 7, value = "薪资单位")
    private String salaryUnit;

    @ExcelProperty(index = 8, value = "技能标签")
    private String skills;

    @ExcelProperty(index = 9, value = "岗位描述")
    private String jobDesc;

    @ExcelProperty(index = 10, value = "发布时间（可选）")
    private String publishTime;

    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public BigDecimal getMinSalary() { return minSalary; }
    public void setMinSalary(BigDecimal minSalary) { this.minSalary = minSalary; }
    public BigDecimal getMaxSalary() { return maxSalary; }
    public void setMaxSalary(BigDecimal maxSalary) { this.maxSalary = maxSalary; }
    public String getSalaryUnit() { return salaryUnit; }
    public void setSalaryUnit(String salaryUnit) { this.salaryUnit = salaryUnit; }
    public String getSkills() { return skills; }
    public void setSkills(String skills) { this.skills = skills; }
    public String getJobDesc() { return jobDesc; }
    public void setJobDesc(String jobDesc) { this.jobDesc = jobDesc; }
    public String getPublishTime() { return publishTime; }
    public void setPublishTime(String publishTime) { this.publishTime = publishTime; }
}