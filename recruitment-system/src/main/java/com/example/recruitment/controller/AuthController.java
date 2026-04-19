package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.dto.ChangePasswordDTO;
import com.example.recruitment.dto.UserLoginDTO;
import com.example.recruitment.dto.UserRegisterDTO;
import com.example.recruitment.service.UserService;
import com.example.recruitment.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "用户认证", description = "用户登录、注册等认证相关接口")
public class AuthController {

    private final UserService userService;

    @PostMapping("/login")
    @Operation(summary = "用户登录", description = "用户使用用户名和密码登录系统")
    public Result<UserVO> login(@Valid @RequestBody UserLoginDTO dto) {
        log.info("用户登录请求: username={}", dto.getUsername());
        UserVO userVO = userService.login(dto);
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
    @Operation(summary = "修改密码", description = "用户修改登录密码")
    public Result<Void> changePassword(@Valid @RequestBody ChangePasswordDTO dto) {
        Long userId = 1L;
        log.info("修改密码请求: userId={}", userId);
        userService.changePassword(userId, dto);
        log.info("密码修改成功: userId={}", userId);
        return Result.success();
    }

    @GetMapping("/user-info")
    @Operation(summary = "获取用户信息", description = "获取当前登录用户的详细信息")
    public Result<UserVO> getUserInfo() {
        Long userId = 1L;
        log.info("获取用户信息请求: userId={}", userId);
        UserVO userVO = userService.getUserInfo(userId);
        log.info("获取用户信息成功: userId={}, username={}", userVO.getId(), userVO.getUsername());
        return Result.success(userVO);
    }
}

