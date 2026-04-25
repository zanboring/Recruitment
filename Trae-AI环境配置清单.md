# Trae AI 环境配置 & 隐式改动完整清单

> 本文档列出所有**非代码逻辑修改但影响系统运行**的关键配置项。Trae AI 编译运行前必须确认这些条件已满足。

---

## 一、数据库配置（最关键）

### 1.1 MySQL 数据库

**项目使用 MySQL 数据库（不是 H2/SQLite），必须提前创建：**

```sql
CREATE DATABASE recruitment_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 1.2 application.yml 数据库连接（环境变量）

`src/main/resources/application.yml` 第 22-24 行：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/recruitment_db?useUnicode=true&characterEncoding=utf8&characterSetResults=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true
    username: ${DB_USERNAME:root}        # ⚠️ 使用环境变量，默认 root
    password: ${DB_PASSWORD:123456}      # ⚠️ 使用环境变量，默认 123456
```

> **重要**：用户名和密码通过 **Spring 环境变量** `${DB_USERNAME}` / `${DB_PASSWORD}` 读取，冒号后是默认值。如果本地 MySQL 密码不是 `123456`，需要：
> - 方式 A：设置环境变量 `set DB_PASSWORD=你的密码`
> - 方式 B：直接修改 yml 中默认值

### 1.3 没有建表 SQL 脚本

**项目中不存在 `.sql` 建表文件！** 表结构依赖 MyBatis 的 Mapper XML + 实体类推断。如果数据库是全新的，需要手动创建表或通过 ORM 自动建表。

**核心表**（根据实体类推测）：
- `user` — 用户表（id, username, password, salt, role, email, created_at）
- `job` — 岗位表（id, title, company_name, city, salary_min, salary_max, experience, education, source_site, job_status, skills, job_desc, url, publish_time）
- `crawl_task` — 爬虫任务表（id, source_site, keyword, city, status, job_count, created_at, finished_at, message）

### 1.4 初始管理员账号

**没有自动初始化脚本！** 系统启动时不会自动创建 admin 账号。你需要：

1. **先通过注册接口创建账号**：调用 `POST /api/auth/register`（`{"username":"admin","password":"你的密码","email":"admin@test.com"}`）
2. 或直接在数据库中手动 INSERT 一条 user 记录

**密码加密规则**（PasswordUtil.java）：
```java
// SHA-256(salt + password) → Base64编码
// salt = 随机16字节 → Base64编码的字符串（每次注册都不同）
```

---

## 二、JWT 认证体系

### 2.1 JWT 配置（application.yml 第 82-84 行）

```yaml
jwt:
  secret: ${JWT_SECRET:recruitment-system-jwt-secret-key-2024-must-be-at-least-256-bits-long-for-hmac-sha}
  expiration: ${JWT_EXPIRATION:86400000}   # 默认24小时（毫秒）
```

> **注意**：JWT Secret 也使用环境变量 `${JWT_SECRET}`，默认值是一个长字符串。
> - 如果部署到生产环境，**必须设置强随机密钥**
> - JwtUtil.java 第 19 行有 fallback 默认值

### 2.2 认证流程

```
前端请求 → http.ts 拦截器自动附加 Authorization: Bearer {token}
         → 后端 SecurityConfig 拦截所有请求（除 login/register）
         → JwtAuthFilter 从 Header 提取 Token 并验证
         → 通过后将 userId 设置到 SecurityContext
         → Controller 可从 SecurityContext 获取当前用户
```

**关键点**：
- **除 `/api/auth/login` 和 `/api/auth/register` 外，所有接口都需要 Token**
- 前端路由守卫（router/index.ts）：未登录自动跳转 `/login`
- **必须先注册/登录才能看到数据**

### 2.3 CORS 配置（SecurityConfig.java 第 90-111 行）

后端允许的前端地址：

| 地址 | 用途 |
|------|------|
| http://localhost:5173 | Vite 开发服务器默认 |
| http://localhost:5174 | 备用端口 |
| http://localhost:5175 | 备用端口 |
| http://localhost:3000 | Node 开发服务器 |
| http://127.0.0.1:5173 | IP 直连 |
| http://127.0.0.1:5174 | IP 直连 |
| http://127.0.0.1:5175 | IP 直连 |

> **如果 Trae AI 使用不同端口，需要在 SecurityConfig.java 中添加对应地址！**

---

## 三、前端环境要求

### 3.1 Node.js & 包管理器

- **Node.js 版本**：建议 >= 18.x（package.json 无明确锁定）
- **包管理器**：npm / pnpm / yarn 均可
- **安装命令**：`npm install`（或 pnpm install）

### 3.2 package.json 关键依赖版本

| 依赖 | 版本 | 说明 |
|------|------|------|
| vue | ^3.4.0 | Vue 3.4+ |
| element-plus | ^2.7.0 | Element Plus 2.7+ |
| vite | ^5.2.0 | Vite 5.2+ |
| axios | ^1.7.0 | HTTP 客户端 |
| pinia | ^2.1.7 | 状态管理 |
| echarts | ^5.5.0 | 图表库 |
| marked | ^18.0.2 | Markdown 渲染（**注意：不包含 marked-gfm**） |
| sass-embedded | ^1.99.0 | Sass 编译器 |
| typescript | ^5.4.0 | TypeScript |

> ⚠️ **marked-gfm 不在 package.json 中！** 已在代码中移除此依赖，改用 marked 内置 GFM 支持。

### 3.3 前端代理配置（vite.config.ts）

```typescript
server: {
  port: 5173,
  proxy: {
    '/api': {
      target: 'http://localhost:8080',  // ⚠️ 后端地址
      changeOrigin: true,
      rewrite: (path) => path            // 直接转发，不改路径
    }
  }
}
```

> **前后端联调前提**：后端必须在 `http://localhost:8080` 运行。

### 3.4 API 路径前缀

- 前端 axios baseURL = `/api`
- 后端 Controller @RequestMapping = `/api/xxx`
- Vite proxy 将 `/api` 转发到 `http://localhost:8080/api`
- **最终实际请求**：`http://localhost:8080/api/xxx`

---

## 四、后端环境要求

### 4.1 Java & Maven

| 项目 | 要求 |
|------|------|
| **JDK 版本** | **Java 22**（pom.xml 明确指定 `<java.version>22>`） |
| **Maven** | 3.8+ |
| **Spring Boot** | 3.2.5 |
| **MyBatis** | 3.0.4 (mybatis-spring-boot-starter) |
| **MySQL Connector** | mysql-connector-j (runtime scope) |

> ⚠️ **Java 22 是硬性要求**，Java 21 或更低版本可能无法编译。

### 4.2 第三方 API 密钥

**智谱 AI（ZhipuAI）配置**（application.yml 第 87-89 行）：

```yaml
zhipuai:
  api:
    key: ${ZHIPUAI_API_KEY:}     # ⚠️ 默认为空！必须配置才能用 AI 功能
```

> 如果要使用 AI 分析功能，**必须设置环境变量 `ZHIPUAI_API_KEY`** 或直接填写 key。
> 不设则 AI 相关接口会返回错误（但不影响其他功能）。

### 4.3 pom.xml 关键点

- **无硬编码 JDK 路径**（之前已移除 fork 和 JAVA_HOME 配置）
- **无 spring-boot-devtools**（热加载依赖未引入）
- **Lombok 1.18.36**（需 IDE 安装 Lombok 插件）
- **WebMagic 0.8.0**（爬虫框架）
- **EasyExcel 3.3.4**（Excel 导出导入）
- **Jsoup 1.17.2**（HTML 解析）

---

## 五、密码加密机制（关键安全点）

### 5.1 PasswordUtil 加密算法

```
注册/修改密码时：
1. 生成随机 Salt（16字节 SecureRandom → Base64字符串）
2. 密码哈希 = Base64( SHA-256( salt字节明文 + passwordUTF8字节 ) )
3. 存储：(salt, hash)

登录验证时：
1. 根据 username 查出用户的 salt
2. 用相同算法计算 输入密码的 hash
3. 对比存储的 hash
```

> **不是 BCrypt/PBKDF2！是自定义的 SHA-256+Salt+Base64 方案。**
> 这意味着不能用 BCryptPasswordEncoder 替代。

### 5.2 默认测试账号密码

如果需要手动插入测试数据，可以用以下方式生成密码哈希：

```java
String salt = PasswordUtil.generateSalt();
String hashedPwd = PasswordUtil.hashPassword("123456", salt);
// 然后在数据库中 INSERT 用户记录时存入 salt 和 hashedPwd
```

---

## 六、日志与文件输出

### 6.1 日志配置

```yaml
logging:
  file:
    name: logs/recruitment-system.log    # 日志输出到项目根目录/logs/
    max-size: 10MB                       # 单文件最大10MB
    max-history: 30                      # 保留30天
```

> **确保项目目录有写入权限**，否则日志写不进去。

### 6.2 Excel 导出路径

导出的 xlsx 文件通过浏览器下载（Blob URL），不落盘。

---

## 七、启动顺序检查清单

按以下顺序依次确认，缺一不可：

| 序号 | 检查项 | 操作 |
|------|--------|------|
| 1 | MySQL 服务是否运行？ | `mysql -u root -p` 能否连上 |
| 2 | recruitment_db 数据库是否存在？ | 不存在就 `CREATE DATABASE recruitment_db ...` |
| 3 | 数据库中是否有表？ | 新库需要建表或让 ORM 创建 |
| 4 | 数据库中是否有用户账号？ | 没有→ 先注册一个 |
| 5 | Java 是否为 22？ | `java -version` 确认 |
| 6 | 后端能否编译？ | `mvn compile` |
| 7 | 后端能否启动？ | `mvn spring-boot:run`（看控制台有无报错） |
| 8 | 前端依赖是否装好？ | `npm install` |
| 9 | 前端能否编译？ | `npm run build` 或 `npm run dev` |
| 10 | 前端能否访问？ | 打开 http://localhost:5173/ |
| 11 | 能否登录？ | 注册/登录 → 看 Dashboard 有无数据 |
| 12 | AI 功能是否可用？（可选） | 设置了 ZHIPUAI_API_KEY 就能用 |

---

## 八、常见问题速查

| 问题 | 原因 | 解决方案 |
|------|------|---------|
| 启动报 `Communications link failure` | MySQL 未启动或端口不对 | 启动 MySQL，检查 3306 端口 |
| 报 `Access denied for user 'root'@'localhost'` | DB_PASSWORD 不对 | 设置环境变量或改 yml 默认值 |
| 前端白屏 / 控制台报错 | npm 依赖没装好 | 删除 node_modules 重装 `rm -rf node_modules && npm install` |
| 登录后立即被踢回登录页 | Token 过期或无效 | 检查 jwt.secret 配置是否一致 |
| 所有接口返回 401 | 未登录或 Token 过期 | 先调 login 接口获取 Token |
| AI 分析接口报错 | ZHIPUAI_API_KEY 未设置 | 设置环境变量或在 yml 中填入 |
| 编译报 `class file has wrong version 62.0` | Java 版本低于 22 | 升级 JDK 到 22 |
| `Success is not exported` | Element Plus 版本太旧 | `npm update element-plus` 或替换为 CircleCheck |
| `marked-gfm not found` | 缺少 marked-gfm 依赖 | 已修复：代码不再依赖它 |
| 导出 Excel 文件为空/损坏 | blob response.data undefined | 已修复：见前端修改清单 |
