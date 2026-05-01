package com.example.recruitment.entity;

import java.time.LocalDateTime;

/**
 * 知识库实体类
 * 存储常见招聘问答对，供本地模型使用
 */
public class KnowledgeBase {

    /** 主键ID */
    private Long id;

    /** 问题 */
    private String question;

    /** 回答 */
    private String answer;

    /** 标签，多个用逗号分隔 */
    private String tags;

    /** 来源：manual=手动添加, zhipu=智谱学习, ollama=本地生成 */
    private String source;

    /** 使用次数 */
    private Integer usageCount;

    /** 状态：1=启用, 0=禁用 */
    private Integer status;

    /** 质量评分：0=未评分, 1=低, 2=中, 3=高 */
    private Integer qualityScore;

    /** 创建时间 */
    private LocalDateTime createdAt;

    /** 更新时间 */
    private LocalDateTime updatedAt;

    public KnowledgeBase() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQuestion() {
        return question;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }

    public Integer getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(Integer qualityScore) {
        this.qualityScore = qualityScore;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "KnowledgeBase{" +
                "id=" + id +
                ", question='" + question + '\'' +
                ", tags='" + tags + '\'' +
                ", source='" + source + '\'' +
                ", usageCount=" + usageCount +
                ", status=" + status +
                ", qualityScore=" + qualityScore +
                '}';
    }
}
