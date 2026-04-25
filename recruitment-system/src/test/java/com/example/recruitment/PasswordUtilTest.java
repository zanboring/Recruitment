package com.example.recruitment;

import com.example.recruitment.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * PasswordUtil 密码工具类单元测试
 */
@DisplayName("PasswordUtil 密码工具类测试")
class PasswordUtilTest {

    @Test
    @DisplayName("generateSalt - 生成盐值长度正确")
    void testGenerateSalt_length() {
        String salt = PasswordUtil.generateSalt();

        assertNotNull(salt, "生成的盐值不应为null");
        // Base64编码16字节 → 约24字符
        assertTrue(salt.length() >= 20 && salt.length() <= 28,
            "盐值长度应在Base64编码后的合理范围内，实际: " + salt.length());
    }

    @RepeatedTest(5)
    @DisplayName("generateSalt - 每次生成不同的盐值")
    void testGenerateSalt_uniqueness() {
        String salt1 = PasswordUtil.generateSalt();
        String salt2 = PasswordUtil.generateSalt();
        assertNotEquals(salt1, salt2, "每次生成的盐值应不同（密码学安全随机）");
    }

    @Test
    @DisplayName("hashPassword - 相同输入+相同盐值产生相同哈希")
    void testHashPassword_consistent() {
        String password = "MyPassword123";
        String salt = "testSalt123";

        String hash1 = PasswordUtil.hashPassword(password, salt);
        String hash2 = PasswordUtil.hashPassword(password, salt);

        assertEquals(hash1, hash2, "相同密码+相同盐值应产生相同的哈希");
        assertFalse(hash1.isEmpty(), "哈希结果不应为空");
    }

    @Test
    @DisplayName("hashPassword - 不同盐值产生不同哈希")
    void testHashPassword_differentSalts() {
        String password = "SamePassword";

        String hash1 = PasswordUtil.hashPassword(password, "saltA");
        String hash2 = PasswordUtil.hashPassword(password, "saltB");

        assertNotEquals(hash1, hash2, "不同盐值应对同密码产生不同哈希");
    }

    @Test
    @DisplayName("hashPassword - 不同密码产生不同哈希")
    void testHashPassword_differentPasswords() {
        String salt = "sameSalt";

        String hash1 = PasswordUtil.hashPassword("password1", salt);
        String hash2 = PasswordUtil.hashPassword("password2", salt);

        assertNotEquals(hash1, hash2, "不同密码应产生不同哈希");
    }

    @Test
    @DisplayName("hashPassword - 空密码不应抛异常")
    void testHashPassword_emptyPassword() {
        String salt = PasswordUtil.generateSalt();
        assertDoesNotThrow(() -> PasswordUtil.hashPassword("", salt),
            "空密码应能正常处理而不抛异常");
    }

    @Test
    @DisplayName("hashPassword - 哈希结果不包含原始密码明文")
    void testHashPassword_noPlainText() {
        String password = "SuperSecretPass2024";
        String salt = "someSalt";
        String hash = PasswordUtil.hashPassword(password, salt);

        assertFalse(hash.contains(password), "哈希结果中不应包含原始密码的明文");
        assertFalse(hash.contains(salt) || (hash.contains(salt) && !hash.equals(hash)),
            "注意：SHA-256(salt+password)理论上可能包含salt子串，但概率极低");
    }
}
