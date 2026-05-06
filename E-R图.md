# 招聘数据可视化系统 E-R图 SQL

以下是系统的数据库表结构定义，可用于在线工具生成E-R图。

---

## 数据库表结构

### 1. 用户表 (user)

```sql
CREATE TABLE `user` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    `username` VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码(加密)',
    `salt` VARCHAR(50) COMMENT '盐值',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色(ADMIN/USER)',
    `email` VARCHAR(100) COMMENT '邮箱',
    `skills` VARCHAR(500) COMMENT '技能标签',
    `education` VARCHAR(20) COMMENT '学历(大专/本科/硕士/博士)',
    `experience_years` INT COMMENT '工作经验年限',
    `login_fail_count` INT DEFAULT 0 COMMENT '登录失败次数',
    `locked_until` DATETIME COMMENT '账号锁定时间',
    `created_at` DATETIME NOT NULL COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';
```

### 2. 岗位表 (job)

```sql
CREATE TABLE `job` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '岗位ID',
    `title` VARCHAR(100) NOT NULL COMMENT '岗位名称',
    `company_name` VARCHAR(100) NOT NULL COMMENT '公司名称',
    `city` VARCHAR(50) COMMENT '工作城市',
    `min_salary` DECIMAL(10,2) COMMENT '最低薪资',
    `max_salary` DECIMAL(10,2) COMMENT '最高薪资',
    `education` VARCHAR(20) COMMENT '学历要求',
    `experience` VARCHAR(20) COMMENT '经验要求',
    `skills` VARCHAR(500) COMMENT '技能要求',
    `description` TEXT COMMENT '岗位描述',
    `source_site` VARCHAR(50) COMMENT '数据来源(BOSS/智联/前程无忧/猎聘)',
    `source_url` VARCHAR(500) COMMENT '原始链接',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态(ACTIVE/EXPIRED)',
    `crawl_task_id` BIGINT COMMENT '爬取任务ID',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    FOREIGN KEY (`crawl_task_id`) REFERENCES `crawl_task`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位表';
```

### 3. 简历表 (resume)

```sql
CREATE TABLE `resume` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '简历ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `name` VARCHAR(50) NOT NULL COMMENT '姓名',
    `phone` VARCHAR(20) COMMENT '手机号',
    `email` VARCHAR(100) COMMENT '邮箱',
    `education` VARCHAR(20) COMMENT '最高学历',
    `major` VARCHAR(100) COMMENT '专业',
    `experience_years` INT COMMENT '工作年限',
    `skills` VARCHAR(500) COMMENT '技能标签',
    `work_history` TEXT COMMENT '工作经历',
    `project_experience` TEXT COMMENT '项目经验',
    `resume_file_path` VARCHAR(255) COMMENT '简历文件路径',
    `status` VARCHAR(20) DEFAULT 'ACTIVE' COMMENT '状态',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='简历表';
```

### 4. 岗位申请表 (job_application)

```sql
CREATE TABLE `job_application` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `job_id` BIGINT NOT NULL COMMENT '岗位ID',
    `resume_id` BIGINT COMMENT '简历ID',
    `status` VARCHAR(20) DEFAULT 'APPLIED' COMMENT '申请状态(APPLIED/INTERVIEW/OFFER/REJECTED)',
    `apply_time` DATETIME NOT NULL COMMENT '申请时间',
    `interview_time` DATETIME COMMENT '面试时间',
    `remark` VARCHAR(500) COMMENT '备注',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`job_id`) REFERENCES `job`(`id`),
    FOREIGN KEY (`resume_id`) REFERENCES `resume`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位申请表';
```

### 5. 爬虫任务表 (crawl_task)

```sql
CREATE TABLE `crawl_task` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '任务ID',
    `name` VARCHAR(100) NOT NULL COMMENT '任务名称',
    `source_site` VARCHAR(50) NOT NULL COMMENT '爬取站点',
    `keywords` VARCHAR(500) COMMENT '搜索关键词',
    `city` VARCHAR(50) COMMENT '目标城市',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态(PENDING/RUNNING/COMPLETED/FAILED)',
    `total_pages` INT DEFAULT 0 COMMENT '总页数',
    `crawled_pages` INT DEFAULT 0 COMMENT '已爬取页数',
    `total_items` INT DEFAULT 0 COMMENT '总岗位数',
    `success_items` INT DEFAULT 0 COMMENT '成功导入数',
    `fail_items` INT DEFAULT 0 COMMENT '失败数',
    `start_time` DATETIME COMMENT '开始时间',
    `end_time` DATETIME COMMENT '结束时间',
    `error_message` TEXT COMMENT '错误信息',
    `created_at` DATETIME NOT NULL COMMENT '创建时间',
    `updated_at` DATETIME COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫任务表';
```

### 6. 搜索历史表 (search_history)

```sql
CREATE TABLE `search_history` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `keywords` VARCHAR(200) COMMENT '搜索关键词',
    `city` VARCHAR(50) COMMENT '城市',
    `education` VARCHAR(20) COMMENT '学历筛选',
    `experience` VARCHAR(20) COMMENT '经验筛选',
    `min_salary` DECIMAL(10,2) COMMENT '最低薪资',
    `max_salary` DECIMAL(10,2) COMMENT '最高薪资',
    `search_time` DATETIME NOT NULL COMMENT '搜索时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='搜索历史表';
```

### 7. 岗位收藏表 (job_favorite)

```sql
CREATE TABLE `job_favorite` (
    `id` BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '收藏ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `job_id` BIGINT NOT NULL COMMENT '岗位ID',
    `favorite_time` DATETIME NOT NULL COMMENT '收藏时间',
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`),
    FOREIGN KEY (`job_id`) REFERENCES `job`(`id`),
    UNIQUE KEY `uk_user_job` (`user_id`, `job_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='岗位收藏表';
```

---

## 实体关系说明

| 关系 | 实体1 | 实体2 | 关系类型 | 说明 |
|------|-------|-------|----------|------|
| 用户-简历 | user | resume | 1:N | 一个用户可以有多份简历 |
| 用户-申请 | user | job_application | 1:N | 一个用户可以申请多个岗位 |
| 用户-收藏 | user | job_favorite | 1:N | 一个用户可以收藏多个岗位 |
| 用户-搜索 | user | search_history | 1:N | 一个用户可以有多次搜索记录 |
| 岗位-申请 | job | job_application | 1:N | 一个岗位可以被多个用户申请 |
| 岗位-爬虫 | job | crawl_task | N:1 | 多个岗位来自同一个爬虫任务 |
| 简历-申请 | resume | job_application | 1:N | 一份简历可以用于多个申请 |

---

## 使用说明

将上述SQL代码复制到以下在线工具生成E-R图：

1. **DrawSQL** - https://drawsql.app/
2. **dbdiagram.io** - https://dbdiagram.io/
3. **SQLDBM** - https://sqldbm.com/

推荐使用 **dbdiagram.io**，支持直接导入SQL DDL并自动生成ER图。