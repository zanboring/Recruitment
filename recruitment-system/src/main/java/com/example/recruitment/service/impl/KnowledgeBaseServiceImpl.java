package com.example.recruitment.service.impl;

import com.example.recruitment.entity.KnowledgeBase;
import com.example.recruitment.mapper.KnowledgeBaseMapper;
import com.example.recruitment.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 * 优化要点：
 * 1. 使用缓存机制减少重复数据库查询
 * 2. 合并重复的关键词搜索逻辑
 * 3. 优化标签提取算法
 * 4. 添加批量操作支持
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    // 缓存：已启用的知识库列表（10分钟过期）
    private static final Map<String, Object> CACHE = new ConcurrentHashMap<>();
    private static final long CACHE_EXPIRE_MS = 10 * 60 * 1000;

    /**
     * 获取缓存数据，如果过期则返回null
     */
    private <T> T getCache(String key, Class<T> clazz) {
        Object[] cached = (Object[]) CACHE.get(key);
        if (cached != null && cached.length == 2) {
            long timestamp = (Long) cached[0];
            if (System.currentTimeMillis() - timestamp < CACHE_EXPIRE_MS) {
                return clazz.cast(cached[1]);
            }
        }
        return null;
    }

    /**
     * 设置缓存数据
     */
    private void setCache(String key, Object value) {
        CACHE.put(key, new Object[]{System.currentTimeMillis(), value});
    }

    /**
     * 清除指定缓存
     */
    private void clearCache(String key) {
        CACHE.remove(key);
    }

    /**
     * 清除所有缓存
     */
    private void clearAllCache() {
        CACHE.clear();
    }

    @Override
    public KnowledgeBase add(KnowledgeBase knowledgeBase) {
        initDefaults(knowledgeBase);
        knowledgeBaseMapper.insert(knowledgeBase);
        clearAllCache();
        log.info("添加知识库记录: {}", knowledgeBase.getQuestion());
        return knowledgeBase;
    }

    @Override
    public boolean update(KnowledgeBase knowledgeBase) {
        boolean success = knowledgeBaseMapper.update(knowledgeBase) > 0;
        if (success) {
            clearAllCache();
            log.info("更新知识库记录: id={}", knowledgeBase.getId());
        }
        return success;
    }

    @Override
    public boolean delete(Long id) {
        boolean success = knowledgeBaseMapper.deleteById(id) > 0;
        if (success) {
            clearAllCache();
            log.info("删除知识库记录: id={}", id);
        }
        return success;
    }

    @Override
    public KnowledgeBase getById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public List<KnowledgeBase> getAllEnabled() {
        // 尝试从缓存获取
        List<KnowledgeBase> cached = getCache("all_enabled", List.class);
        if (cached != null) {
            return cached;
        }
        // 从数据库查询并缓存
        List<KnowledgeBase> result = knowledgeBaseMapper.selectAllEnabled();
        setCache("all_enabled", result);
        return result;
    }

    @Override
    public List<KnowledgeBase> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEnabled();
        }
        // 关键词搜索使用缓存键
        String cacheKey = "search_" + keyword.trim().hashCode();
        List<KnowledgeBase> cached = getCache(cacheKey, List.class);
        if (cached != null) {
            return cached;
        }
        List<KnowledgeBase> result = knowledgeBaseMapper.searchByKeyword(keyword.trim());
        setCache(cacheKey, result);
        return result;
    }

    @Override
    public KnowledgeBase findSimilar(String question) {
        if (question == null || question.trim().isEmpty()) {
            return null;
        }
        return knowledgeBaseMapper.selectBySimilarQuestion(question.trim());
    }

    @Override
    public void incrementUsage(Long id) {
        knowledgeBaseMapper.incrementUsageCount(id);
    }

    @Override
    @Transactional
    public KnowledgeBase learnFromZhipu(String question, String answer) {
        // 检查是否已存在相似问题
        KnowledgeBase existing = findSimilar(question);
        if (existing != null) {
            // 如果已有回答质量更高或使用次数更多，不覆盖
            if (existing.getQualityScore() >= 2 || existing.getUsageCount() > 5) {
                log.info("知识库已存在相似问题，跳过学习: {}", question);
                return existing;
            }
            // 否则更新为新的高质量回答
            existing.setAnswer(answer);
            existing.setSource("zhipu");
            existing.setQualityScore(2);
            update(existing);
            log.info("更新知识库记录（来自智谱）: {}", question);
            return existing;
        }

        // 创建新记录
        KnowledgeBase newEntry = new KnowledgeBase();
        newEntry.setQuestion(question);
        newEntry.setAnswer(answer);
        newEntry.setTags(extractTags(question));
        newEntry.setSource("zhipu");
        newEntry.setStatus(0);
        newEntry.setQualityScore(2);
        newEntry.setUsageCount(0);

        return add(newEntry);
    }

    @Override
    public int batchAdd(List<KnowledgeBase> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        list.forEach(this::initDefaults);
        int result = knowledgeBaseMapper.batchInsert(list);
        clearAllCache();
        log.info("批量添加知识库记录: {} 条", result);
        return result;
    }

    @Override
    public List<KnowledgeBase> getByPage(int page, int size) {
        int offset = (page - 1) * size;
        return knowledgeBaseMapper.selectByPage(offset, size);
    }

    @Override
    public int getCount() {
        Integer cached = getCache("count", Integer.class);
        if (cached != null) {
            return cached;
        }
        int result = knowledgeBaseMapper.countAll();
        setCache("count", result);
        return result;
    }

    /**
     * 设置知识库记录的默认值
     */
    private void initDefaults(KnowledgeBase kb) {
        if (kb.getStatus() == null) {
            kb.setStatus(1);
        }
        if (kb.getUsageCount() == null) {
            kb.setUsageCount(0);
        }
        if (kb.getQualityScore() == null) {
            kb.setQualityScore(0);
        }
        if (kb.getSource() == null) {
            kb.setSource("manual");
        }
    }

    /**
     * 常见招聘相关关键词集合（优化为静态常量，避免重复创建）
     */
    private static final Set<String> KEYWORD_SET = Set.of(
        "java", "python", "前端", "后端", "测试", "运维", "产品", "算法",
        "长沙", "北京", "上海", "深圳", "成都", "广州", "杭州", "武汉",
        "薪资", "工资", "待遇", "薪酬", "学历", "大专", "本科", "研究生", "硕士",
        "经验", "应届", "实习", "一年", "三年", "五年", "面试", "简历", "hr", "招聘",
        "学习", "培训", "提升", "发展", "java开发", "python开发", "web前端", "安卓", "ios",
        "vue", "react", "spring", "django", "mysql", "redis", "mongodb", "kafka",
        "加班", "996", "工作强度", "工作氛围"
    );

    /**
     * 从问题中提取关键词（优化版）
     */
    private String extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        String lowerText = text.toLowerCase();
        List<String> found = new ArrayList<>();

        // 优先匹配较长的关键词（避免子串匹配问题）
        List<String> sortedKeywords = KEYWORD_SET.stream()
                .sorted((a, b) -> b.length() - a.length())
                .collect(Collectors.toList());

        for (String keyword : sortedKeywords) {
            if (lowerText.contains(keyword)) {
                // 检查是否已包含更具体的关键词
                boolean isRedundant = found.stream()
                        .anyMatch(f -> f.toLowerCase().contains(keyword) || keyword.contains(f.toLowerCase()));
                if (!isRedundant) {
                    found.add(keyword);
                }
            }
            // 限制最多提取8个关键词
            if (found.size() >= 8) {
                break;
            }
        }

        return String.join(",", found);
    }

    /**
     * 从问题中提取标签（用于存储）
     */
    private String extractTags(String question) {
        String keywords = extractKeywords(question);
        // 进一步简化，只取前5个最重要的
        String[] parts = keywords.split(",");
        if (parts.length > 5) {
            keywords = String.join(",", Arrays.copyOfRange(parts, 0, 5));
        }
        return keywords;
    }

    /**
     * 获取AI上下文（优化版）
     * 优化点：
     * 1. 合并重复的关键词搜索逻辑
     * 2. 使用缓存减少数据库查询
     * 3. 优化搜索结果排序
     */
    @Override
    public String getContextForAI(String userQuestion) {
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return "";
        }

        StringBuilder context = new StringBuilder();
        context.append("\n\n【本地知识库参考】\n");

        try {
            // 1. 先尝试精确匹配
            KnowledgeBase exactMatch = findSimilar(userQuestion);
            if (exactMatch != null) {
                context.append("📌 匹配问题：").append(exactMatch.getQuestion()).append("\n");
                context.append("💡 回答：").append(exactMatch.getAnswer()).append("\n\n");
                incrementUsage(exactMatch.getId());
                return context.toString();
            }

            // 2. 提取关键词进行模糊搜索（优化：合并搜索结果去重）
            String keywords = extractKeywords(userQuestion);
            if (!keywords.isEmpty()) {
                List<KnowledgeBase> results = searchByMultipleKeywords(Arrays.asList(keywords.split(",")));

                // 取前3个最相关的
                int count = 0;
                for (KnowledgeBase kb : results) {
                    if (count >= 3) break;
                    context.append("📌 相关问题：").append(kb.getQuestion()).append("\n");
                    context.append("💡 回答：").append(kb.getAnswer()).append("\n\n");
                    incrementUsage(kb.getId());
                    count++;
                }
            }

            if (context.length() > 50) {
                context.append("【以上来自本地知识库，可参考以上回答】\n");
            }

        } catch (Exception e) {
            log.warn("获取知识库上下文失败: {}", e.getMessage());
        }

        return context.toString();
    }

    /**
     * 多关键词搜索（优化版）
     * 合并多个关键词的搜索结果，按质量得分排序去重
     */
    private List<KnowledgeBase> searchByMultipleKeywords(List<String> keywords) {
        Set<KnowledgeBase> resultSet = new LinkedHashSet<>();

        for (String keyword : keywords) {
            String trimmedKeyword = keyword.trim();
            if (!trimmedKeyword.isEmpty()) {
                List<KnowledgeBase> partial = knowledgeBaseMapper.searchByKeyword(trimmedKeyword);
                resultSet.addAll(partial);
            }
        }

        // 按质量得分和使用次数排序
        return resultSet.stream()
                .sorted(Comparator.comparingInt(kb -> {
                    int score = kb.getQualityScore() != null ? kb.getQualityScore() : 0;
                    int usage = kb.getUsageCount() != null ? kb.getUsageCount() : 0;
                    return -(score * 100 + usage);
                }))
                .collect(Collectors.toList());
    }
}