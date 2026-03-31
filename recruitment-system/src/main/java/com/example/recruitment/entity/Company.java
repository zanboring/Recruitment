package com.example.recruitment.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class Company {
    private Long id;
    private String name;
    private String industry;
    private String city;
    private String address;
    private String size;
    private String website;
    private LocalDateTime createdAt;
}

