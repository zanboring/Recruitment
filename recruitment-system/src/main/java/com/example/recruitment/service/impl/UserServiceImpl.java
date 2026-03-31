package com.example.recruitment.service.impl;

import com.example.recruitment.dto.UserLoginDTO;
import com.example.recruitment.dto.UserRegisterDTO;
import com.example.recruitment.entity.User;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.UserMapper;
import com.example.recruitment.service.UserService;
import com.example.recruitment.util.PasswordUtil;
import com.example.recruitment.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    @Override
    public UserVO login(UserLoginDTO dto) {
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        String hash = PasswordUtil.hashPassword(dto.getPassword(), user.getSalt());
        if (!hash.equals(user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        vo.setToken(UUID.randomUUID().toString());
        return vo;
    }

    @Override
    public void register(UserRegisterDTO dto) {
        User exist = userMapper.findByUsername(dto.getUsername());
        if (exist != null) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(dto.getUsername());
        String salt = PasswordUtil.generateSalt();
        user.setSalt(salt);
        user.setPassword(PasswordUtil.hashPassword(dto.getPassword(), salt));
        user.setRole("USER");
        user.setEmail(dto.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        userMapper.insert(user);
    }
}

