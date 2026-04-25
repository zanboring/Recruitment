package com.example.recruitment;

import com.example.recruitment.common.Result;
import com.example.recruitment.common.ResultCode;
import com.example.recruitment.util.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtUtil 单元测试
 * 覆盖：Token生成、解析、验证、过期处理
 */
@DisplayName("JwtUtil 工具类测试")
class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        // 通过反射注入配置值（避免启动Spring容器）
        ReflectionTestUtils.setField(jwtUtil, "secret",
            "recruitment-system-jwt-secret-key-2024-must-be-at-least-256-bits-long-for-hmac-sha");
        ReflectionTestUtils.setField(jwtUtil, "expiration", 86400000L); // 24小时
    }

    @Test
    @DisplayName("生成Token - 基本功能验证")
    void testGenerateToken() {
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN");

        assertNotNull(token, "生成的Token不应为null");
        assertFalse(token.isEmpty(), "生成的Token不应为空字符串");
        assertTrue(token.length() > 50, "Token长度应足够长（JWT标准格式）");
    }

    @Test
    @DisplayName("从Token解析用户ID")
    void testGetUserIdFromToken() {
        Long expectedUserId = 100L;
        String token = jwtUtil.generateToken(expectedUserId, "testuser", "USER");

        Long userId = jwtUtil.getUserIdFromToken(token);

        assertEquals(expectedUserId, userId, "从Token解析的用户ID应与输入一致");
    }

    @Test
    @DisplayName("从Token解析用户名")
    void testGetUsernameFromToken() {
        String expectedUsername = "zhangsan";
        String token = jwtUtil.generateToken(1L, expectedUsername, "USER");

        String username = jwtUtil.getUsernameFromToken(token);

        assertEquals(expectedUsername, username, "从Token解析的用户名应与输入一致");
    }

    @Test
    @DisplayName("从Token解析用户角色")
    void testGetRoleFromToken() {
        String token = jwtUtil.generateToken(1L, "admin", "ADMIN");

        String role = jwtUtil.getRoleFromToken(token);

        assertEquals("ADMIN", role, "从Token解析的角色应与输入一致");
    }

    @Test
    @DisplayName("验证有效Token - 应返回true")
    void testValidateToken_valid() {
        String token = jwtUtil.generateToken(1L, "test", "USER");

        boolean isValid = jwtUtil.validateToken(token);

        assertTrue(isValid, "有效的Token应返回true");
    }

    @Test
    @DisplayName("验证无效Token - 应返回false")
    void testValidateToken_invalid() {
        // 完全伪造的token
        boolean isValid = jwtUtil.validateToken("this.is.a.fake.token");

        assertFalse(isValid, "无效的Token应返回false");
    }

    @Test
    @DisplayName("验证空Token - 应返回false")
    void testValidateToken_empty() {
        boolean isValid = jwtUtil.validateToken("");
        assertFalse(isValid, "空的Token应返回false");

        isValid = jwtUtil.validateToken(null);
        assertFalse(isValid, "null的Token应返回false");
    }

    @Test
    @DisplayName("解析被篡改的Token - 应返回null")
    void testParseTamperedToken() {
        String validToken = jwtUtil.generateToken(1L, "admin", "ADMIN");
        // 截断并拼接非法字符模拟篡改
        String tampered = validToken.substring(0, Math.max(10, validToken.length() / 2)) + "xxxxx";

        assertNull(jwtUtil.getUserIdFromToken(tampered), "篡改后的Token应无法解析用户ID");
        assertNull(jwtUtil.getUsernameFromToken(tampered), "篡改后的Token应无法解析用户名");
    }

    @Test
    @DisplayName("不同用户生成不同Token")
    void testDifferentUsersDifferentTokens() {
        String token1 = jwtUtil.generateToken(1L, "userA", "USER");
        String token2 = jwtUtil.generateToken(2L, "userB", "USER");

        assertNotEquals(token1, token2, "不同用户应生成不同的Token");
    }

    @Test
    @DisplayName("角色为null时默认USER")
    void testRoleDefaultToUser() {
        String token = jwtUtil.generateToken(1L, "testuser", null);

        String role = jwtUtil.getRoleFromToken(token);
        assertEquals("USER", role, "角色为null时默认应使用USER");
    }
}
