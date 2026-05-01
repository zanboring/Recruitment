-- 创建数据库
CREATE DATABASE IF NOT EXISTS recruitment_db
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

USE recruitment_db;

-- 用户表
DROP TABLE IF EXISTS user;
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码（加密后）',
    salt VARCHAR(64) NOT NULL COMMENT '密码盐',
    role VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：ADMIN/USER',
    email VARCHAR(100) COMMENT '邮箱',
    login_fail_count INT NOT NULL DEFAULT 0 COMMENT '登录失败次数',
    locked_until DATETIME COMMENT '账户锁定截止时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 默认管理员账号（密码需通过环境变量或配置文件设置）
INSERT INTO user (username, password, salt, role, email, created_at)
VALUES ('admin', 'ZDoWXd+YGTGxeMkoxzJp2YevU/WOkv4dqxI+V7mkpQ8=', 'recruitment-salt-001', 'ADMIN', 'admin@example.com', NOW());

-- 企业表
DROP TABLE IF EXISTS company;
CREATE TABLE company (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    name VARCHAR(100) NOT NULL COMMENT '企业名称',
    industry VARCHAR(100) COMMENT '所属行业',
    city VARCHAR(50) COMMENT '所在城市',
    address VARCHAR(255) COMMENT '地址',
    size VARCHAR(50) COMMENT '企业规模',
    website VARCHAR(255) COMMENT '官网',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='企业表';

-- 招聘信息表
DROP TABLE IF EXISTS job;
CREATE TABLE job (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    company_id BIGINT COMMENT '企业ID',
    title VARCHAR(100) NOT NULL COMMENT '岗位名称',
    company_name VARCHAR(120) COMMENT '公司名称',
    source_site VARCHAR(60) COMMENT '来源网站',
    unique_key VARCHAR(255) UNIQUE COMMENT '岗位去重唯一键',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '在岗状态：1在岗，0下架',
    job_key VARCHAR(64) NOT NULL COMMENT '岗位唯一键',
    job_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '岗位状态：NEW/ACTIVE/OFFLINE',
    city VARCHAR(50) COMMENT '工作城市',
    experience VARCHAR(50) COMMENT '经验要求',
    education VARCHAR(50) COMMENT '学历要求',
    min_salary DECIMAL(10,2) COMMENT '最低薪资',
    max_salary DECIMAL(10,2) COMMENT '最高薪资',
    salary_unit VARCHAR(20) DEFAULT 'monthly' COMMENT '薪资单位（月/年）',
    skills VARCHAR(255) COMMENT '技能标签（逗号分隔）',
    job_desc TEXT COMMENT '岗位描述',
    publish_time DATETIME COMMENT '发布时间',
    last_seen_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最近抓取时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    CONSTRAINT fk_job_company FOREIGN KEY (company_id) REFERENCES company(id)
        ON DELETE SET NULL ON UPDATE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='招聘信息表';

CREATE INDEX idx_job_city ON job(city);
CREATE INDEX idx_job_publish_time ON job(publish_time);
CREATE INDEX idx_job_company_id ON job(company_id);
CREATE INDEX idx_job_status ON job(job_status);
CREATE INDEX idx_job_active_status ON job(status);
CREATE INDEX idx_job_source_site ON job(source_site);
CREATE UNIQUE INDEX uk_job_job_key ON job(job_key);

-- 爬虫任务表
DROP TABLE IF EXISTS crawl_task;
CREATE TABLE crawl_task (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    source_site VARCHAR(100) COMMENT '数据来源网站',
    keyword VARCHAR(100) COMMENT '关键词',
    city VARCHAR(50) COMMENT '城市',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '任务状态',
    job_count INT DEFAULT 0 COMMENT '抓取职位数量',
    message VARCHAR(255) COMMENT '任务说明或错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    finished_at DATETIME COMMENT '完成时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='爬虫任务表';

CREATE INDEX idx_crawl_task_created_at ON crawl_task(created_at);

-- 系统日志表
DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) COMMENT '操作用户',
    action VARCHAR(100) COMMENT '操作名称',
    method VARCHAR(100) COMMENT '请求方法',
    uri VARCHAR(200) COMMENT '请求URI',
    ip VARCHAR(50) COMMENT 'IP地址',
    params TEXT COMMENT '请求参数',
    success TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否成功',
    error_msg VARCHAR(500) COMMENT '错误信息',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统日志表';

CREATE INDEX idx_sys_log_created_at ON sys_log(created_at);
CREATE INDEX idx_sys_log_username ON sys_log(username);

