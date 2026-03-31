package com.example.recruitment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CrawlTask {
    private Long id;
    private String sourceSite;
    private String keyword;
    private String city;
    private String status;
    private Integer jobCount;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime finishedAt;
}

