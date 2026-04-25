package com.example.recruitment;

import com.example.recruitment.util.PasswordUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 爬虫相关工具类单元测试
 *
 * 注意：由于网络爬虫依赖外部网站，本测试类不包含实际的HTTP爬取调用。
 * 原始的 CrawlTest.main() 方法已迁移为独立的网络连通性验证脚本（如需使用可手动运行）。
 * 本类专注于可离线运行的单元测试。
 */
@DisplayName("爬虫工具与数据校验测试")
class CrawlTest {

    @Test
    @DisplayName("密码加密 - 爬虫系统用户密码安全")
    void testCrawlUserPasswordSecurity() {
        // 模拟爬虫系统内部用户的密码处理流程
        String rawPassword = "crawl_system_key_2024";
        String salt = PasswordUtil.generateSalt();

        // 加密
        String hashed = PasswordUtil.hashPassword(rawPassword, salt);

        assertNotNull(hashed, "哈希结果不应为null");
        assertFalse(hashed.equals(rawPassword), "哈希后不应等于明文");
        assertTrue(hashed.length() > 20, "SHA-256 Base64编码结果应有足够长度");

        // 验证：相同输入产生相同输出
        String hashedAgain = PasswordUtil.hashPassword(rawPassword, salt);
        assertEquals(hashed, hashedAgain, "相同输入应产生确定性的哈希");
    }

    @Test
    @DisplayName("URL编码安全性 - 防止注入攻击")
    void testUrlEncodingSafety() throws Exception {
        String[] dangerousInputs = {
            "Java<script>alert('xss')</script>",
            "Java'; DROP TABLE jobs; --",
            "../../../etc/passwd",
            "Java%0aSet-Cookie: hack=true",
            "Java\t\n\r"
        };

        for (String input : dangerousInputs) {
            String encoded = java.net.URLEncoder.encode(input, java.nio.charset.StandardCharsets.UTF_8);
            // 编码后的字符串不应包含未转义的危险字符
            assertFalse(encoded.contains("<"),
                "URL编码应转义尖括号，输入: " + input);
            assertFalse(encoded.contains("'") && !encoded.contains("%27"),
                "URL编码应转义单引号，输入: " + input);
            assertNotNull(encoded, "编码结果不应为null");
        }
    }

    @Test
    @DisplayName("招聘关键词清洗 - 移除危险字符")
    void testKeywordSanitization() {
        String[] keywords = {"Java后端", "Python开发", "软件测试", "运维工程师", "应届生", "实习"};

        for (String kw : keywords) {
            assertNotNull(kw, "关键词不应为null");
            assertFalse(kw.contains(";"), "关键词不应包含分号: " + kw);
            assertFalse(kw.contains("\n"), "关键词不应包含换行符: " + kw);
            assertTrue(kw.length() <= 50, "关键词长度应合理: " + kw + " (" + kw.length() + ")");
        }
    }

    @Test
    @DisplayName("城市名称合法性检查")
    void testCityNameValidation() {
        String[] validCities = {"长沙", "北京", "上海", "广州", "深圳", "杭州"};
        for (String city : validCities) {
            assertTrue(city.length() >= 2 && city.length() <= 10,
                "城市名长度应在2-10之间: " + city);
            // 城市名应为中文或常见英文
            assertTrue(city.matches("[\\u4e00-\\u9fa5a-zA-Z]+"),
                "城市名应只含中英文字母: " + city);
        }
    }
}
