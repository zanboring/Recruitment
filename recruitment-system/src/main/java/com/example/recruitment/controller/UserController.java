package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.entity.User;
import com.example.recruitment.service.UserService;
import com.example.recruitment.vo.UserVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "用户管理", description = "用户账户管理接口（管理员专用）")
public class UserController {

    private final UserService userService;

    @GetMapping
    @Operation(summary = "查询用户列表", description = "分页查询用户列表，支持用户名搜索")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> listUsers(
            @RequestParam(required = false) String username,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        
        List<UserVO> users = userService.listUsers(username, pageNum, pageSize);
        long total = userService.countUsers();
        
        Map<String, Object> result = new HashMap<>();
        result.put("list", users);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        
        return Result.success(result);
    }

    @GetMapping("/{id}")
    @Operation(summary = "查询用户详情", description = "根据ID查询用户详细信息")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO user = userService.getUserById(id);
        return Result.success(user);
    }

    @PutMapping("/{id}")
    @Operation(summary = "更新用户信息", description = "更新用户基本信息（用户名、邮箱、角色等）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> updateUser(@PathVariable Long id, @RequestBody User user) {
        userService.updateUser(id, user);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除用户", description = "根据ID删除用户账户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success();
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "切换用户状态", description = "启用或禁用用户账户")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> toggleUserStatus(@PathVariable Long id, @RequestParam boolean enabled) {
        userService.toggleUserStatus(id, enabled);
        return Result.success();
    }
}
