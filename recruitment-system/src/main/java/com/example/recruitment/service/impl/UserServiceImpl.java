package com.example.recruitment.service.impl;

import com.example.recruitment.dto.ChangePasswordDTO;
import com.example.recruitment.dto.UserLoginDTO;
import com.example.recruitment.dto.UserRegisterDTO;
import com.example.recruitment.entity.User;
import com.example.recruitment.exception.BusinessException;
import com.example.recruitment.mapper.UserMapper;
import com.example.recruitment.service.UserService;
import com.example.recruitment.util.JwtUtil;
import com.example.recruitment.util.PasswordUtil;
import com.example.recruitment.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;

    @Override
    public UserVO login(UserLoginDTO dto) {
        log.debug("用户登录: username={}", dto.getUsername());
        
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null) {
            log.warn("登录失败: 用户不存在 - username={}", dto.getUsername());
            throw new BusinessException("用户不存在");
        }
        
        String hash = PasswordUtil.hashPassword(dto.getPassword(), user.getSalt());
        if (!hash.equals(user.getPassword())) {
            log.warn("登录失败: 密码错误 - username={}", dto.getUsername());
            throw new BusinessException("用户名或密码错误");
        }
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        // 生成 JWT Token（替代之前的 UUID）
        vo.setToken(jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole()));
        
        log.info("用户登录成功: userId={}, username={}, role={}", user.getId(), user.getUsername(), user.getRole());
        return vo;
    }

    @Override
    public void register(UserRegisterDTO dto) {
        log.debug("用户注册: username={}, email={}", dto.getUsername(), dto.getEmail());
        
        User exist = userMapper.findByUsername(dto.getUsername());
        if (exist != null) {
            log.warn("注册失败: 用户名已存在 - username={}", dto.getUsername());
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
        
        log.info("用户注册成功: userId={}, username={}, email={}", user.getId(), user.getUsername(), user.getEmail());
    }

    @Override
    public void changePassword(Long userId, ChangePasswordDTO dto) {
        log.debug("修改密码: userId={}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("修改密码失败: 用户不存在 - userId={}", userId);
            throw new BusinessException("用户不存在");
        }
        
        String oldHash = PasswordUtil.hashPassword(dto.getOldPassword(), user.getSalt());
        if (!oldHash.equals(user.getPassword())) {
            log.warn("修改密码失败: 旧密码错误 - userId={}", userId);
            throw new BusinessException("旧密码错误");
        }
        
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            log.warn("修改密码失败: 两次密码不一致 - userId={}", userId);
            throw new BusinessException("两次输入的密码不一致");
        }
        
        String newSalt = PasswordUtil.generateSalt();
        String newHash = PasswordUtil.hashPassword(dto.getNewPassword(), newSalt);
        
        user.setSalt(newSalt);
        user.setPassword(newHash);
        userMapper.update(user);
        
        log.info("密码修改成功: userId={}, username={}", user.getId(), user.getUsername());
    }

    @Override
    public UserVO getUserInfo(Long userId) {
        log.debug("查询用户信息: userId={}", userId);
        
        User user = userMapper.selectById(userId);
        if (user == null) {
            log.warn("查询用户信息失败: 用户不存在 - userId={}", userId);
            throw new BusinessException("用户不存在");
        }
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
        
        log.debug("查询用户信息成功: userId={}, username={}", user.getId(), user.getUsername());
        return vo;
    }
}

