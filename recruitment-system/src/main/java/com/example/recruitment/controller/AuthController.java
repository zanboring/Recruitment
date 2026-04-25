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
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户使用用户名和密码登录系统，返回JWT Token")
    public Result<UserVO> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("用户登录请求: username={}", dto.getUsername());
        UserVO userVO = userService.login(dto);
        // JWT Token 已在 UserServiceImpl.login() 中通过 JwtUtil 生成并设置到 UserVO.token 中
        log.info("用户登录成功: userId={}, username={}", userVO.getId(), userVO.getUsername());
        return Result.success(userVO);
    }

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

