package com.example.recruitment.service;

import com.example.recruitment.entity.KnowledgeBase;

import java.util.List;

/**
 * 知识库服务接口
 */
public interface KnowledgeBaseService {

    /**
     * 添加知识库记录
     */
    KnowledgeBase add(KnowledgeBase knowledgeBase);

    /**
     * 更新知识库记录
     */
    boolean update(KnowledgeBase knowledgeBase);

    /**
     * 删除知识库记录
     */
    boolean delete(Long id);

    /**
     * 根据ID获取
     */
    KnowledgeBase getById(Long id);

    /**
     * 获取所有启用的知识库
     */
    List<KnowledgeBase> getAllEnabled();

    /**
     * 关键词搜索
     */
    List<KnowledgeBase> search(String keyword);

    /**
     * 根据问题搜索相似记录
     */
    KnowledgeBase findSimilar(String question);

    /**
     * 增加使用次数
     */
    void incrementUsage(Long id);

    /**
     * 从智谱AI学习（保存问答对）
     */
    KnowledgeBase learnFromZhipu(String question, String answer);

    /**
     * 批量添加知识库
     */
    int batchAdd(List<KnowledgeBase> list);

    /**
     * 分页获取知识库
     */
    List<KnowledgeBase> getByPage(int page, int size);

    /**
     * 获取知识库总数
     */
    int getCount();

    /**
     * 获取用于AI上下文的知识库内容
     * 根据用户问题匹配相关问答
     */
    String getContextForAI(String userQuestion);
}
