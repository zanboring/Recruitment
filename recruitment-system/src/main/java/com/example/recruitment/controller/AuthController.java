package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.dto.ChangePasswordDTO;
import com.example.recruitment.dto.UserLoginDTO;
import com.example.recruitment.dto.UserRegisterDTO;
import com.example.recruitment.service.UserService;
import com.example.recruitment.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private final UserService userService;

    // 默认用户凭证来源：允许通过环境变量覆盖（Spring Boot 会映射 app.default-user.*）
    @Value("${app.default-user.username:${DB_USERNAME:admin}}")
    private String defaultUsername;

    @Value("${app.default-user.password:${DB_PASSWORD:admin123}}")
    private String defaultPassword;

    @Value("${spring.profiles.active:dev}")
    private String activeProfile;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户使用用户名和密码登录系统，返回JWT Token")
    public Result<UserVO> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("用户登录请求: username={}", dto.getUsername());
        UserVO userVO = userService.login(dto);
        // JWT Token 已在 UserServiceImpl.login() 中通过 JwtUtil 生成并设置到 UserVO.token 中
        log.info("用户登录成功: userId={}, username={}", userVO.getId(), userVO.getUsername());
        return Result.success(userVO);
    }

    /**
     * 自动登录（用于本地/开发环境避免反复跳转登录页）
     * <p>
     * 使用后端已配置的默认用户凭证完成认证，并返回新的 JWT。
     * </p>
     */
    @PostMapping("/auto-login")
    @Operation(summary = "自动登录（开发环境）", description = "使用后端默认用户凭证自动获取JWT Token，避免401导致的二次跳转")
    public Result<UserVO> autoLogin() {
        // 生产环境不建议开启：会把“默认用户密码”用于自动登录。
        if ("prod".equalsIgnoreCase(activeProfile)) {
            return Result.failed("自动登录在生产环境已禁用");
        }

        UserLoginDTO dto = new UserLoginDTO();
        dto.setUsername(defaultUsername);
        dto.setPassword(defaultPassword);
        return Result.success(userService.login(dto));
    }

    /**
     * 获取默认用户名（用于登录页自动填充，不暴露密码）
     * <p>
     * 仅返回环境变量中的默认用户名，供前端登录表单预填，用户仍需手动输入密码或通过 auto-login 免登录。
     * </p>
     */
    @GetMapping("/default-username")
    @Operation(summary = "获取默认用户名", description = "返回环境变量中的默认用户名，用于登录页自动填充（不暴露密码）")
    public Result<Map<String, String>> defaultUsername() {
        if ("prod".equalsIgnoreCase(activeProfile)) {
            return Result.failed("该接口在生产环境已禁用");
        }
        Map<String, String> data = new HashMap<>();
        data.put("username", defaultUsername);
        return Result.success(data);
    }

    /**
     * [安全优化] 已注释：默认账号密码接口
     * 原接口会将密码明文返回前端，存在信息泄露风险。
     * 现已通过 auto-login 接口实现免登录，无需暴露密码。
     * 如需恢复，请同时取消 SecurityConfig 中 /api/auth/default-credentials 的 permitAll 配置。
     */
    // @GetMapping("/default-credentials")
    // public Result<Map<String, String>> defaultCredentials() {
    //     if ("prod".equalsIgnoreCase(activeProfile)) {
    //         return Result.failed("默认账号密码在生产环境不可用");
    //     }
    //     Map<String, String> data = new HashMap<>();
    //     data.put("username", defaultUsername);
    //     data.put("password", defaultPassword);
    //     return Result.success(data);
    // }

    @PostMapping("/register")
    @Operation(summary = "用户注册", description = "新用户注册账号")
    public Result<Void> register(@Valid @RequestBody UserRegisterDTO dto) {
        log.info("用户注册请求: username={}, email={}", dto.getUsername(), dto.getEmail());
        userService.register(dto);
        log.info("用户注册成功: username={}", dto.getUsername());
        return Result.success();
    }

    @PostMapping("/change-password")
    @Operation(summary = "修改密码", description = "用户修改登录密码（需JWT认证）")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto, HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.failed("请先登录");
        }
        log.info("修改密码请求: userId={}", userId);
        userService.changePassword(userId, dto);
        log.info("密码修改成功: userId={}", userId);
        return Result.success();
    }

    @GetMapping("/user-info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息（需JWT认证）")
    public Result<UserVO> getUserInfo(HttpServletRequest request) {
        Long userId = getCurrentUserId(request);
        if (userId == null) {
            return Result.failed("请先登录");
        }
        log.info("获取用户信息请求: userId={}", userId);
        UserVO userVO = userService.getUserInfo(userId);
        log.info("获取用户信息成功: userId={}, username={}", userVO.getId(), userVO.getUsername());
        return Result.success(userVO);
    }

    /**
     * 从 Spring Security Context 中获取当前已认证用户的 ID。
     * <p>
     * 用户通过 JwtAuthFilter 认证后，其 userId 被设置到 Authentication.principal 中。
     * </p>
     *
     * @param request HTTP请求（保留兼容性）
     * @return 当前用户ID，未认证返回null
     */
    private Long getCurrentUserId(HttpServletRequest request) {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getPrincipal() != null) {
                Object principal = authentication.getPrincipal();
                if (principal instanceof Number) {
                    return ((Number) principal).longValue();
                } else if (principal instanceof String) {
                    return Long.parseLong((String) principal);
                }
            }
        } catch (Exception e) {
            log.warn("从SecurityContext获取用户ID失败: {}", e.getMessage());
        }
        return null;
    }
}

