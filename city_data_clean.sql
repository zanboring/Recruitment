-- ================================================
-- 招聘数据城市清洗脚本
-- 路径：C:\Users\zanboring\OneDrive - Ormesby Primary\Desktop\Recruitment outside\Recruitment midle\Recruitment\
-- 目的：修正城市代码、标准化城市名称、清理脏数据
-- 执行前请备份数据库！
-- ================================================

-- 1. 标准化城市名称（去掉"市"后缀）
UPDATE job
SET city = CASE
    WHEN city = '北京市' THEN '北京'
    WHEN city = '上海市' THEN '上海'
    WHEN city = '广州市' THEN '广州'
    WHEN city = '深圳市' THEN '深圳'
    WHEN city = '杭州市' THEN '杭州'
    WHEN city = '南京市' THEN '南京'
    WHEN city = '成都市' THEN '成都'
    WHEN city = '武汉市' THEN '武汉'
    WHEN city = '西安市' THEN '西安'
    WHEN city = '苏州市' THEN '苏州'
    WHEN city = '重庆市' THEN '重庆'
    WHEN city = '天津市' THEN '天津'
    WHEN city = '长沙市' THEN '长沙'
    WHEN city = '郑州市' THEN '郑州'
    WHEN city = '东莞市' THEN '东莞'
    ELSE city
END
WHERE city IN ('北京市', '上海市', '广州市', '深圳市', '杭州市', '南京市',
              '成都市', '武汉市', '西安市', '苏州市', '重庆市', '天津市',
              '长沙市', '郑州市', '东莞市');

-- 2. 清理无效城市数据（将明显的脏数据标记为需要重新爬取）
UPDATE job
SET city = '未知'
WHERE city NOT IN ('北京', '上海', '广州', '深圳', '杭州', '南京', '成都',
                   '武汉', '西安', '苏州', '重庆', '天津', '长沙', '郑州', '东莞', '未知')
   OR city IS NULL
   OR city = ''
   OR LENGTH(city) < 2;

-- 3. 统计清洗后的数据分布
SELECT
    city AS '城市',
    COUNT(*) AS '职位数量'
FROM job
WHERE job_status != 'OFFLINE'
GROUP BY city
ORDER BY COUNT(*) DESC;

-- 4. 查看需要重新爬取的城市
SELECT COUNT(*) AS '需要重新爬取的职位数' FROM job WHERE city = '未知' OR city IS NULL;
