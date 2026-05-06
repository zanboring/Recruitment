package com.example.recruitment.dto;

import lombok.Data;

@Data
public class JobRecommendDTO {
    private String skills;
    private String education;
    private Integer experienceYears;
    private String city;
    private Integer limit = 10;
}