package com.example.recruitment.config;

import com.example.recruitment.entity.User;
import com.example.recruitment.mapper.UserMapper;
import com.example.recruitment.util.PasswordUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;

    @Value("${app.default-user.username:${DB_USERNAME:admin}}")
    private String defaultUsername;

    @Value("${app.default-user.password:${DB_PASSWORD:admin123}}")
    private String defaultPassword;

    @Value("${app.default-user.email:admin@example.com}")
    private String defaultEmail;

    @Value("${app.default-user.role:ADMIN}")
    private String defaultRole;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("检查并初始化默认用户...");
        
        User existingUser = userMapper.findByUsername(defaultUsername);
        if (existingUser != null) {
            // 如果数据库里已有默认用户，但密码与环境配置不一致，则更新为环境配置密码
            if (!PasswordUtil.verify(defaultPassword, existingUser.getPassword())) {
                log.info("默认用户已存在但密码不匹配: username={}，将按环境配置更新密码", defaultUsername);
                existingUser.setPassword(PasswordUtil.hashPassword(defaultPassword));
                existingUser.setSalt("");
                existingUser.setRole(defaultRole);
                userMapper.update(existingUser);
            } else {
                log.info("默认用户已存在且密码匹配: username={}", defaultUsername);
            }
            return;
        }

        log.info("创建默认用户: username={}", defaultUsername);
        
        User user = new User();
        user.setUsername(defaultUsername);
        user.setPassword(PasswordUtil.hashPassword(defaultPassword));
        user.setSalt("");
        user.setRole(defaultRole);
        user.setEmail(defaultEmail);
        user.setCreatedAt(LocalDateTime.now());
        user.setLoginFailCount(0);
        
        userMapper.insert(user);
        
        log.info("默认用户创建成功: userId={}, username={}", user.getId(), user.getUsername());
    }
}
