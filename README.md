## 招聘数据可视化系统（recruitment-system）

本项目是基于 Spring Boot 3 + MyBatis + MySQL + Vue3 + Vite 的前后端分离招聘数据可视化系统，包含数据爬取、数据管理、多维度可视化分析、岗位推荐、薪资预测、系统管理等功能。

### 技术栈

- 后端：Spring Boot 3.2.5、MyBatis、PageHelper、MySQL 8.0、Jsoup、EasyExcel
- 前端：Vue3、Vite、Vue Router、Pinia、Axios、Element Plus、ECharts
- JDK：JDK 21
- 构建工具：Maven 3.9.9

### 环境准备

- 已安装 JDK 21
- 已安装 Maven 3.9.9
- 已安装 Node.js 20
- 已安装 MySQL 8.0，并创建数据库 `recruitment_db`

### 数据库初始化

```bash
mysql -u root -p123456 < recruitment_db.sql
```

### 后端启动命令

```bash
cd recruitment-system
mvn clean package
java -jar target/recruitment-system.jar
```

或：

```bash
cd recruitment-system
mvn spring-boot:run
```

### 前端启动命令

```bash
cd recruitment-system-frontend
npm install
npm run dev
```

### 功能页面

- 登录/注册：`/login`
- 首页概览：`/`
- 数据可视化：`/dashboard`
- 岗位列表（搜索/筛选）：`/jobs`
- 数据对比（新增/下架/在岗）：`/compare`
- 数据分析（文字+图表）：`/analysis`
- 爬虫任务：`/settings`

