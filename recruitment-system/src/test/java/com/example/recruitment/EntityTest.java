package com.example.recruitment;

import com.example.recruitment.entity.Job;
import com.example.recruitment.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.Month;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 实体类（Entity）单元测试
 * 验证 getter/setter 正确性及默认值行为
 */
@DisplayName("Entity 实体类测试")
class EntityTest {

    // ==================== User Entity ====================

    @Test
    @DisplayName("User - Setter/Getter 正确性")
    void testUserSettersAndGetters() {
        User user = new User();
        user.setId(1L);
        user.setUsername("zhangsan");
        user.setPassword("hashedPass");
        user.setSalt("randomSalt");
        user.setRole("ADMIN");
        user.setEmail("test@example.com");
        user.setCreatedAt(LocalDateTime.of(2024, Month.JANUARY, 15, 10, 30));

        assertEquals(1L, user.getId());
        assertEquals("zhangsan", user.getUsername());
        assertEquals("hashedPass", user.getPassword());
        assertEquals("randomSalt", user.getSalt());
        assertEquals("ADMIN", user.getRole());
        assertEquals("test@example.com", user.getEmail());
        assertNotNull(user.getCreatedAt());
    }

    @Test
    @DisplayName("User - 默认值均为null")
    void testUserDefaultValues() {
        User user = new User();

        assertNull(user.getId(), "新创建的User ID应为null");
        assertNull(user.getUsername());
        assertNull(user.getPassword());
        assertNull(user.getSalt());
        assertNull(user.getRole());
        assertNull(user.getEmail());
        assertNull(user.getCreatedAt());
    }

    // ==================== Job Entity ====================

    @Test
    @DisplayName("Job - Setter/Getter 正确性")
    void testJobSettersAndGetters() {
        Job job = new Job();
        job.setId(100L);
        job.setTitle("Java后端开发工程师");
        job.setCompanyName("某科技公司");
        job.setCity("长沙");
        job.setExperience("3-5年");
        job.setEducation("本科");
        job.setMinSalary(new BigDecimal("15000"));
        job.setMaxSalary(new BigDecimal("25000"));
        job.setJobStatus("ACTIVE");
        job.setSourceSite("boss");
        job.setCreatedAt(LocalDateTime.now());

        assertEquals(100L, job.getId());
        assertEquals("Java后端开发工程师", job.getTitle());
        assertEquals("某科技公司", job.getCompanyName());
        assertEquals("长沙", job.getCity());
        assertEquals(15000, job.getMinSalary());
        assertEquals(25000, job.getMaxSalary());
        assertEquals("ACTIVE", job.getJobStatus());
    }

    @Test
    @DisplayName("Job - 薪资范围合理性")
    void testJobSalaryRange() {
        Job job = new Job();
        job.setMinSalary(new BigDecimal("8000"));
        job.setMaxSalary(new BigDecimal("20000"));

        assertTrue(job.getMaxSalary().compareTo(job.getMinSalary()) >= 0,
            "最高薪资不应低于最低薪资");

        // 薪资中值计算
        BigDecimal mid = job.getMinSalary().add(job.getMaxSalary()).divide(new BigDecimal("2"));
        assertEquals(new BigDecimal("14000.0"), mid);
    }
}
