package com.example.recruitment.mapper;

import com.example.recruitment.entity.KnowledgeBase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 知识库 Mapper
 */
@Mapper
public interface KnowledgeBaseMapper {

    /**
     * 插入知识库记录
     */
    int insert(KnowledgeBase knowledgeBase);

    /**
     * 更新知识库记录
     */
    int update(KnowledgeBase knowledgeBase);

    /**
     * 根据ID删除
     */
    int deleteById(Long id);

    /**
     * 根据ID查询
     */
    KnowledgeBase selectById(Long id);

    /**
     * 查询所有启用的知识库记录
     */
    List<KnowledgeBase> selectAllEnabled();

    /**
     * 关键词搜索（匹配问题或标签）
     */
    List<KnowledgeBase> searchByKeyword(@Param("keyword") String keyword);

    /**
     * 根据标签查询
     */
    List<KnowledgeBase> selectByTags(@Param("tags") String tags);

    /**
     * 增加使用次数
     */
    int incrementUsageCount(Long id);

    /**
     * 查询高质量的记录（用于学习）
     */
    List<KnowledgeBase> selectHighQuality(@Param("minScore") Integer minScore);

    /**
     * 批量插入
     */
    int batchInsert(@Param("list") List<KnowledgeBase> list);

    /**
     * 分页查询
     */
    List<KnowledgeBase> selectByPage(@Param("offset") Integer offset, @Param("limit") Integer limit);

    /**
     * 统计总数
     */
    int countAll();

    /**
     * 检查是否存在相似问题
     */
    KnowledgeBase selectBySimilarQuestion(@Param("question") String question);
}
