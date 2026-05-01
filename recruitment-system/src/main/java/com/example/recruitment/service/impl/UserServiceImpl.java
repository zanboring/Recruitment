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
    
    private static final int MAX_LOGIN_FAIL_COUNT = 5;
    private static final int LOCK_DURATION_MINUTES = 30;

    @Override
    public UserVO login(UserLoginDTO dto) {
        log.debug("用户登录: username={}", dto.getUsername());
        
        User user = userMapper.findByUsername(dto.getUsername());
        if (user == null) {
            log.warn("登录失败: 用户不存在 - username={}", dto.getUsername());
            throw new BusinessException("用户不存在");
        }
        
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("登录失败: 账户已锁定 - username={}, lockedUntil={}", dto.getUsername(), user.getLockedUntil());
            throw new BusinessException("账户已锁定，请稍后再试");
        }
        
        if (!PasswordUtil.verify(dto.getPassword(), user.getPassword())) {
            int failCount = (user.getLoginFailCount() == null ? 0 : user.getLoginFailCount()) + 1;
            user.setLoginFailCount(failCount);
            
            if (failCount >= MAX_LOGIN_FAIL_COUNT) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(LOCK_DURATION_MINUTES));
                userMapper.update(user);
                log.warn("登录失败: 账户已锁定 - username={}, failCount={}", dto.getUsername(), failCount);
                throw new BusinessException("账户已锁定，请" + LOCK_DURATION_MINUTES + "分钟后再试");
            }
            
            userMapper.update(user);
            log.warn("登录失败: 密码错误 - username={}, failCount={}", dto.getUsername(), failCount);
            throw new BusinessException("用户名或密码错误，剩余尝试次数: " + (MAX_LOGIN_FAIL_COUNT - failCount));
        }
        
        if (user.getLoginFailCount() != null && user.getLoginFailCount() > 0) {
            user.setLoginFailCount(0);
            user.setLockedUntil(null);
            userMapper.update(user);
        }
        
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setRole(user.getRole());
        vo.setEmail(user.getEmail());
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
        user.setPassword(PasswordUtil.hashPassword(dto.getPassword()));
        user.setSalt("");
        user.setRole("USER");
        user.setEmail(dto.getEmail());
        user.setCreatedAt(LocalDateTime.now());
        user.setLoginFailCount(0);
        
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
        
        if (!PasswordUtil.verify(dto.getOldPassword(), user.getPassword())) {
            log.warn("修改密码失败: 旧密码错误 - userId={}", userId);
            throw new BusinessException("旧密码错误");
        }
        
        if (!dto.getNewPassword().equals(dto.getConfirmPassword())) {
            log.warn("修改密码失败: 两次密码不一致 - userId={}", userId);
            throw new BusinessException("两次输入的密码不一致");
        }
        
        user.setPassword(PasswordUtil.hashPassword(dto.getNewPassword()));
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
