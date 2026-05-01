-- 知识库表：存储常见招聘问答对，供本地模型使用
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键ID',
    question VARCHAR(500) NOT NULL COMMENT '问题',
    answer TEXT NOT NULL COMMENT '回答',
    tags VARCHAR(500) DEFAULT '' COMMENT '标签，多个用逗号分隔（如：Java,长沙,薪资）',
    source VARCHAR(50) DEFAULT 'manual' COMMENT '来源：manual=手动添加, zhipu=智谱学习, ollama=本地生成',
    usage_count INT DEFAULT 0 COMMENT '使用次数',
    status TINYINT DEFAULT 1 COMMENT '状态：1=启用, 0=禁用',
    quality_score INT DEFAULT 0 COMMENT '质量评分：0=未评分, 1=低, 2=中, 3=高',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI知识库表';

-- 初始化常见招聘问答数据
INSERT INTO knowledge_base (question, answer, tags, source, quality_score) VALUES
-- 薪资相关
('长沙Java后端薪资多少', '根据系统数据，长沙Java后端开发薪资范围大致如下：\n- 应届生/1年以下：6-10K\n- 1-3年经验：10-18K\n- 3-5年经验：15-25K\n- 5年以上：20-35K\n\n实际薪资受公司规模、技术栈、城市区域影响较大。建议关注长沙软件园、高新区企业群。', 'Java,长沙,薪资,后端', 'manual', 3),

('北京Python工程师薪资水平', '北京Python工程师薪资参考：\n- 应届生：10-18K\n- 1-3年：18-30K\n- 3-5年：25-45K\n- 5年以上：35-60K\n\n北京互联网企业集中，薪资普遍高于其他城市，但生活成本也较高。', 'Python,北京,薪资', 'manual', 3),

('前端开发薪资怎么样', '前端开发薪资因城市和经验差异较大：\n- 初级(1年以下)：5-12K\n- 中级(1-3年)：10-20K\n- 高级(3-5年)：18-30K\n- 资深(5年+)：25-45K\n\n热门框架Vue/React薪资通常比传统前端高20-30%。', '前端,薪资,Vue,React', 'manual', 3),

('测试工程师工资高吗', '软件测试工程师薪资水平：\n- 功能测试/实习生：5-10K\n- 自动化测试：12-22K\n- 测试开发/架构：20-40K\n- 测试经理：25-50K\n\n建议向自动化测试、性能测试方向发展，薪资提升明显。', '测试,薪资,自动化', 'manual', 3),

-- 城市选择
('长沙IT行业好不好', '长沙IT行业近年来发展迅速：\n- 优势：房价相对北上深低很多，生活成本适中，美食丰富\n- 劣势：薪资比一线城市低20-40%，大厂数量少\n- 机会：软件园、高新区企业增多，移动互联网、游戏行业发展不错\n- 建议：适合想长期定居二线城市、又想从事IT的人', '长沙,IT,城市选择', 'manual', 3),

('深圳程序员工作机会多吗', '深圳是程序员圣地之一：\n- 机会：腾讯、字节、华为等大厂总部或分部集中\n- 薪资：全国最高，应届生起薪通常15K+\n- 竞争：非常激烈，对学历和技术要求高\n- 生活：房价极高，建议早期租房，后期考虑周边城市', '深圳,城市选择,机会', 'manual', 3),

('成都工作IT怎么样', '成都IT环境特点：\n- 天府软件园聚集大量互联网公司\n- 游戏公司特别多（腾讯游戏、完美世界等）\n- 薪资中等，生活成本适中，房价可接受\n- 适合西南地区求职者', '成都,城市选择,游戏', 'manual', 3),

-- 技能需求
('Java需要学哪些技术栈', 'Java后端核心技术栈：\n- 基础：Java SE、并发、IO、集合源码\n- 框架：Spring Boot、Spring Cloud、MyBatis\n- 数据库：MySQL、Redis、MongoDB\n- 工具：Git、Maven/Docker、Linux\n- 进阶：微服务、消息队列(Kafka/RocketMQ)、搜索引擎\n- 加分：K8s、云原生、性能调优', 'Java,技术栈,学习路线', 'manual', 3),

('Python适合做什么开发', 'Python主要应用领域：\n- Web后端：Django、Flask、FastAPI\n- 数据分析：Pandas、NumPy、Matplotlib\n- 机器学习：TensorFlow、PyTorch\n- 爬虫：Scrapy、Selenium\n- 自动化运维：Ansible、SaltStack\n- 游戏后端：PyGame、Godot\n\n入门简单，但高级岗位竞争激烈。', 'Python,应用领域,技术选型', 'manual', 3),

('前端需要学哪些东西', '前端学习路线：\n- 基础：HTML/CSS/JavaScript三件套\n- 框架：Vue或React（至少掌握一个）\n- 工程化：Webpack/Vite、ESLint、Git\n- Node.js：了解后端开发基础\n- 移动端：Uni-app、Flutter（可选）\n- 进阶：TypeScript、性能优化、浏览器原理', '前端,学习路线,Vue,React', 'manual', 3),

-- 学历经验
('大专学历好找工作吗', '大专学历在IT行业找工作建议：\n- 优势：某些外包公司对学历要求不高，容易积累经验\n- 劣势：大厂简历关难过，正规公司可能卡学历\n- 破局方法：\n  1. 技术栈深入，能解决实际问题\n  2. 积累项目经验，展示实际作品\n  3. 考虑继续教育（成人本科）\n  4. 从小公司起步，逐步跳槽', '大专,学历,求职', 'manual', 3),

('没有工作经验能找到工作吗', '应届生/转行求职策略：\n- 项目经验：自己做完整项目（博客、爬虫、管理系统）\n- 实习经历：哪怕是小公司实习也有帮助\n- 技术博客：写技术文章展示能力\n- GitHub：提交有意义的开源贡献\n- 证书：部分认证有帮助（如阿里云ACP）\n- 面试：准备充分，展示学习能力', '应届生,经验,求职技巧', 'manual', 3),

-- 简历面试
('怎么提高简历通过率', '提高简历通过率的技巧：\n1. 技术栈关键词匹配：看职位要求写技能\n2. 项目经历量化：用数据展示成果（如\"提升性能30%\"）\n3. 格式清晰：基本信息-技能-项目-工作经历\n4. 项目描述STAR法则：Situation-Task-Action-Result\n5. 简历一页纸：突出重点，不要堆砌\n6. 定期更新：删除过时内容', '简历,求职技巧,通过率', 'manual', 3),

('技术面试一般问什么', '技术面试常见内容：\n- Java：集合源码、并发编程、Spring原理、MySQL索引\n- Python：语言特性、爬虫/数据处理经验、Django/Flask\n- 前端：Vue/React原理、手写代码、网络协议\n- 算法：二叉树、链表、动态规划（高频题）\n- 系统设计：高并发、缓存、数据库设计\n\n建议刷LeetCode重点题，准备项目亮点讲解。', '面试,技术面试,算法', 'manual', 3),

('怎么准备面试', '面试准备清单：\n1. 简历内容：每个项目能讲清楚技术细节和难点\n2. 八股文：语言基础、框架原理、设计模式\n3. 算法：hot 100高频题，理解思路而非背题\n4. 系统设计：了解常见架构（缓存、消息队列、数据库分库分表）\n5. 反向提问：准备几个问面试官的问题\n6. 模拟面试：找人练习，复盘改进', '面试,求职技巧,准备', 'manual', 3),

-- 职业发展
('程序员35岁后会失业吗', '关于程序员中年危机：\n- 现实：部分岗位确实存在年龄歧视\n- 破局方向：\n  1. 技术专家：深耕某一领域成为专家\n  2. 技术管理：转向架构师、技术经理\n  3. 转型产品/运营：利用技术背景\n  4. 自由职业：接项目、写技术博客\n  5. 降维打击：从小公司技术负责人做起\n- 建议：早做规划，不要只写代码', '职业发展,35岁,中年危机', 'manual', 3),

('要不要考研', '是否考研的建议：\n- 考研好处：大厂简历关、薪资起点高、系统学习\n- 考研代价：2-3年时间成本，竞争激烈\n- 建议人群：本科一般、想进大厂、喜欢学术\n- 建议工作人群：本科不错、自学能力强、目标明确\n- 折中方案：工作后读非全日制研究生', '考研,学历,职业规划', 'manual', 3),

-- 行业趋势
('AI会取代程序员吗', '关于AI对程序员的影响：\n- 当前：AI辅助编程(GitHub Copilot等)提高效率\n- 短期：重复性编码工作减少，但创意设计、系统架构仍需人\n- 建议：\n  1. 学会使用AI工具提效\n  2. 培养AI难以替代的能力：系统设计、沟通、项目管理\n  3. 关注AI+行业应用领域\n- 结论：会取代部分工作，但不会完全取代程序员', 'AI,程序员,职业趋势,ChatGPT', 'manual', 3);

-- 创建索引加速搜索
CREATE INDEX idx_knowledge_tags ON knowledge_base(tags);
CREATE INDEX idx_knowledge_status ON knowledge_base(status);
CREATE INDEX idx_knowledge_usage ON knowledge_base(usage_count DESC);
