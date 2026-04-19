package com.example.recruitment.vo;

import java.util.List;

public class AIFeedbackVO {

    private String summary;
    private List<QualityJob> qualityJobs;
    private TrendAnalysis trendAnalysis;
    private List<String> suggestions;
    private List<SkillDemand> skillDemands;
    private SalaryAnalysis salaryAnalysis;

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public List<QualityJob> getQualityJobs() { return qualityJobs; }
    public void setQualityJobs(List<QualityJob> qualityJobs) { this.qualityJobs = qualityJobs; }
    public TrendAnalysis getTrendAnalysis() { return trendAnalysis; }
    public void setTrendAnalysis(TrendAnalysis trendAnalysis) { this.trendAnalysis = trendAnalysis; }
    public List<String> getSuggestions() { return suggestions; }
    public void setSuggestions(List<String> suggestions) { this.suggestions = suggestions; }
    public List<SkillDemand> getSkillDemands() { return skillDemands; }
    public void setSkillDemands(List<SkillDemand> skillDemands) { this.skillDemands = skillDemands; }
    public SalaryAnalysis getSalaryAnalysis() { return salaryAnalysis; }
    public void setSalaryAnalysis(SalaryAnalysis salaryAnalysis) { this.salaryAnalysis = salaryAnalysis; }

    public static class QualityJob {
        private Long id;
        private String title;
        private String companyName;
        private String city;
        private String salary;
        private String skills;
        private String recommendReason;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCompanyName() { return companyName; }
        public void setCompanyName(String companyName) { this.companyName = companyName; }
        public String getCity() { return city; }
        public void setCity(String city) { this.city = city; }
        public String getSalary() { return salary; }
        public void setSalary(String salary) { this.salary = salary; }
        public String getSkills() { return skills; }
        public void setSkills(String skills) { this.skills = skills; }
        public String getRecommendReason() { return recommendReason; }
        public void setRecommendReason(String recommendReason) { this.recommendReason = recommendReason; }
    }

    public static class TrendAnalysis {
        private String trendText;
        private String hotCity;
        private String hotSkill;
        private String hotTitle;

        public String getTrendText() { return trendText; }
        public void setTrendText(String trendText) { this.trendText = trendText; }
        public String getHotCity() { return hotCity; }
        public void setHotCity(String hotCity) { this.hotCity = hotCity; }
        public String getHotSkill() { return hotSkill; }
        public void setHotSkill(String hotSkill) { this.hotSkill = hotSkill; }
        public String getHotTitle() { return hotTitle; }
        public void setHotTitle(String hotTitle) { this.hotTitle = hotTitle; }
    }

    public static class SkillDemand {
        private String skill;
        private Integer count;
        private String level;

        public String getSkill() { return skill; }
        public void setSkill(String skill) { this.skill = skill; }
        public Integer getCount() { return count; }
        public void setCount(Integer count) { this.count = count; }
        public String getLevel() { return level; }
        public void setLevel(String level) { this.level = level; }
    }

    public static class SalaryAnalysis {
        private String avgSalary;
        private String topSalary;
        private String salaryRange;

        public String getAvgSalary() { return avgSalary; }
        public void setAvgSalary(String avgSalary) { this.avgSalary = avgSalary; }
        public String getTopSalary() { return topSalary; }
        public void setTopSalary(String topSalary) { this.topSalary = topSalary; }
        public String getSalaryRange() { return salaryRange; }
        public void setSalaryRange(String salaryRange) { this.salaryRange = salaryRange; }
    }
}