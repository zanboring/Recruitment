package com.example.recruitment.service;

import com.example.recruitment.dto.UserLoginDTO;
import com.example.recruitment.dto.UserRegisterDTO;
import com.example.recruitment.vo.UserVO;

public interface UserService {

    UserVO login(UserLoginDTO dto);

    void register(UserRegisterDTO dto);
}

