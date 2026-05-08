package com.example.recruitment.service;

import com.example.recruitment.dto.ChangePasswordDTO;
import com.example.recruitment.dto.UserLoginDTO;
import com.example.recruitment.dto.UserRegisterDTO;
import com.example.recruitment.entity.User;
import com.example.recruitment.vo.UserVO;

import java.util.List;

public interface UserService {

    UserVO login(UserLoginDTO dto);

    void register(UserRegisterDTO dto);

    void changePassword(Long userId, ChangePasswordDTO dto);

    UserVO getUserInfo(Long userId);
    
    // 管理员功能
    List<UserVO> listUsers(String username, Integer pageNum, Integer pageSize);
    
    UserVO getUserById(Long id);
    
    void updateUser(Long id, User user);
    
    void deleteUser(Long id);
    
    void toggleUserStatus(Long id, boolean enabled);
    
    long countUsers();
}

