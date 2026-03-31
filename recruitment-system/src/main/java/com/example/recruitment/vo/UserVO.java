package com.example.recruitment.vo;

import lombok.Data;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String role;
    private String email;
    private String token;
}

