package com.example.recruitment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SysLog {
    private Long id;
    private String username;
    private String action;
    private String method;
    private String uri;
    private String ip;
    private String params;
    private Boolean success;
    private String errorMsg;
    private LocalDateTime createdAt;
}

