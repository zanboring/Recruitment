package com.example.recruitment.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Job {
    private Long id;
    private Long companyId;
    private String title;
    private String companyName;
    private String sourceSite;
    private String jobKey;
    private String jobStatus;
    private String city;
    private String experience;
    private String education;
    private BigDecimal minSalary;
    private BigDecimal maxSalary;
    private String salaryUnit;
    private String skills;
    private String jobDesc;
    private String url;
    private LocalDateTime publishTime;
    private LocalDateTime lastSeenAt;
    private LocalDateTime createdAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCompanyId() { return companyId; }
    public void setCompanyId(Long companyId) { this.companyId = companyId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompanyName() { return companyName; }
    public void setCompanyName(String companyName) { this.companyName = companyName; }
    public String getSourceSite() { return sourceSite; }
    public void setSourceSite(String sourceSite) { this.sourceSite = sourceSite; }
    public String getJobKey() { return jobKey; }
    public void setJobKey(String jobKey) { this.jobKey = jobKey; }
    public String getJobStatus() { return jobStatus; }
    public void setJobStatus(String jobStatus) { this.jobStatus = jobStatus; }
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
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getPublishTime() { return publishTime; }
    public void setPublishTime(LocalDateTime publishTime) { this.publishTime = publishTime; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(LocalDateTime lastSeenAt) { this.lastSeenAt = lastSeenAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    /**
     * 获取薪资范围字符串（如 "15-25K"）
     */
    public String getSalary() {
        if (minSalary != null && maxSalary != null) {
            String unit = salaryUnit != null ? salaryUnit : "K";
            return minSalary.intValue() + "-" + maxSalary.intValue() + unit;
        } else if (minSalary != null) {
            String unit = salaryUnit != null ? salaryUnit : "K";
            return minSalary.intValue() + unit;
        } else if (maxSalary != null) {
            String unit = salaryUnit != null ? salaryUnit : "K";
            return maxSalary.intValue() + unit;
        }
        return null;
    }
}