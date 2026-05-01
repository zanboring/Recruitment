package com.example.recruitment.service.impl;

import com.example.recruitment.entity.KnowledgeBase;
import com.example.recruitment.mapper.KnowledgeBaseMapper;
import com.example.recruitment.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 知识库服务实现
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private final KnowledgeBaseMapper knowledgeBaseMapper;

    @Override
    public KnowledgeBase add(KnowledgeBase knowledgeBase) {
        if (knowledgeBase.getStatus() == null) {
            knowledgeBase.setStatus(1);
        }
        if (knowledgeBase.getUsageCount() == null) {
            knowledgeBase.setUsageCount(0);
        }
        if (knowledgeBase.getQualityScore() == null) {
            knowledgeBase.setQualityScore(0);
        }
        if (knowledgeBase.getSource() == null) {
            knowledgeBase.setSource("manual");
        }
        knowledgeBaseMapper.insert(knowledgeBase);
        log.info("添加知识库记录: {}", knowledgeBase.getQuestion());
        return knowledgeBase;
    }

    @Override
    public boolean update(KnowledgeBase knowledgeBase) {
        return knowledgeBaseMapper.update(knowledgeBase) > 0;
    }

    @Override
    public boolean delete(Long id) {
        return knowledgeBaseMapper.deleteById(id) > 0;
    }

    @Override
    public KnowledgeBase getById(Long id) {
        return knowledgeBaseMapper.selectById(id);
    }

    @Override
    public List<KnowledgeBase> getAllEnabled() {
        return knowledgeBaseMapper.selectAllEnabled();
    }

    @Override
    public List<KnowledgeBase> search(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return getAllEnabled();
        }
        return knowledgeBaseMapper.searchByKeyword(keyword.trim());
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
            existing.setQualityScore(2); // 标记为中等质量（待人工审核）
            update(existing);
            log.info("更新知识库记录（来自智谱）: {}", question);
            return existing;
        }

        // 提取关键词作为标签
        String tags = extractTags(question);

        // 创建新记录
        KnowledgeBase newEntry = new KnowledgeBase();
        newEntry.setQuestion(question);
        newEntry.setAnswer(answer);
        newEntry.setTags(tags);
        newEntry.setSource("zhipu");
        newEntry.setStatus(0); // 默认禁用，需要人工审核
        newEntry.setQualityScore(2); // 标记为中等质量
        newEntry.setUsageCount(0);

        return add(newEntry);
    }

    @Override
    public int batchAdd(List<KnowledgeBase> list) {
        if (list == null || list.isEmpty()) {
            return 0;
        }
        // 设置默认值
        for (KnowledgeBase kb : list) {
            if (kb.getStatus() == null) kb.setStatus(1);
            if (kb.getUsageCount() == null) kb.setUsageCount(0);
            if (kb.getQualityScore() == null) kb.setQualityScore(0);
            if (kb.getSource() == null) kb.setSource("manual");
        }
        return knowledgeBaseMapper.batchInsert(list);
    }

    @Override
    public List<KnowledgeBase> getByPage(int page, int size) {
        int offset = (page - 1) * size;
        return knowledgeBaseMapper.selectByPage(offset, size);
    }

    @Override
    public int getCount() {
        return knowledgeBaseMapper.countAll();
    }

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

            // 2. 提取关键词进行模糊搜索
            String keywords = extractKeywords(userQuestion);
            if (!keywords.isEmpty()) {
                List<KnowledgeBase> results = new ArrayList<>();
                for (String keyword : keywords.split(",")) {
                    List<KnowledgeBase> partial = knowledgeBaseMapper.searchByKeyword(keyword.trim());
                    for (KnowledgeBase kb : partial) {
                        if (!results.contains(kb)) {
                            results.add(kb);
                        }
                    }
                }

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
     * 从问题中提取关键词
     */
    private String extractKeywords(String text) {
        if (text == null || text.isEmpty()) {
            return "";
        }

        // 常见招聘相关关键词
        Set<String> keywordSet = Set.of(
            "java", "python", "前端", "后端", "测试", "运维", "产品", "算法",
            "长沙", "北京", "上海", "深圳", "成都", "广州", "杭州", "武汉",
            "薪资", "工资", "待遇", "薪酬",
            "学历", "大专", "本科", "研究生", "硕士",
            "经验", "应届", "实习", "一年", "三年", "五年",
            "面试", "简历", "hr", "招聘",
            "学习", "培训", "提升", "发展",
            "java开发", "python开发", "web前端", "安卓", "ios",
            "vue", "react", "spring", "django",
            "mysql", "redis", "mongodb", "kafka",
            "加班", "996", "工作强度", "工作氛围"
        );

        List<String> found = new ArrayList<>();
        String lowerText = text.toLowerCase();

        for (String keyword : keywordSet) {
            if (lowerText.contains(keyword)) {
                found.add(keyword);
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
            keywords = String.join(",", java.util.Arrays.copyOfRange(parts, 0, 5));
        }
        return keywords;
    }
}
