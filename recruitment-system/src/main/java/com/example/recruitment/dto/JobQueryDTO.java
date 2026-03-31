package com.example.recruitment.dto;

import lombok.Data;

@Data
public class JobQueryDTO {
    private String keyword;
    private String city;
    private String companyName;
    private String experience;
    private String education;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}

