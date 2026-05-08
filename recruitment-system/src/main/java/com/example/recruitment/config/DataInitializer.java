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
            boolean passwordUpdated = false;
            if (!PasswordUtil.verify(defaultPassword, existingUser.getPassword())) {
                log.info("默认用户已存在但密码不匹配: username={}，将按环境配置更新密码", defaultUsername);
                existingUser.setPassword(PasswordUtil.hashPassword(defaultPassword));
                existingUser.setSalt("");
                passwordUpdated = true;
            }
            if (!defaultRole.equals(existingUser.getRole())) {
                existingUser.setRole(defaultRole);
                passwordUpdated = true;
            }
            if (passwordUpdated) {
                userMapper.update(existingUser);
                log.info("默认用户已更新: username={}, password={}, role={}", defaultUsername, defaultPassword, defaultRole);
            } else {
                log.info("默认用户已存在且密码匹配: username={}", defaultUsername);
            }
        } else {
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

        User adminUser = userMapper.findByUsername("admin");
        if (adminUser != null && !"admin".equals(defaultUsername)) {
            boolean passwordUpdated = false;
            if (!PasswordUtil.verify(defaultPassword, adminUser.getPassword())) {
                log.info("检测到admin用户密码过期，将更新为环境配置密码");
                adminUser.setPassword(PasswordUtil.hashPassword(defaultPassword));
                adminUser.setSalt("");
                passwordUpdated = true;
            }
            if (!"ADMIN".equals(adminUser.getRole())) {
                adminUser.setRole("ADMIN");
                passwordUpdated = true;
            }
            if (passwordUpdated) {
                userMapper.update(adminUser);
                log.info("admin用户已更新: password={}, role=ADMIN", defaultPassword);
            }
        }
    }
}