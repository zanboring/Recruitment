package com.example.recruitment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class User {
    private Long id;
    private String username;
    private String password;
    private String salt;
    private String role;
    private String email;
    private LocalDateTime createdAt;
}

