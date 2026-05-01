package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.entity.KnowledgeBase;
import com.example.recruitment.service.KnowledgeBaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 知识库管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
@Tag(name = "知识库管理", description = "管理AI知识库，支持增删改查和学习功能")
public class KnowledgeBaseController {

    private final KnowledgeBaseService knowledgeBaseService;

    /**
     * 分页获取知识库列表
     */
    @GetMapping("/list")
    @Operation(summary = "获取知识库列表", description = "分页获取知识库记录")
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        List<KnowledgeBase> list = knowledgeBaseService.getByPage(page, size);
        int total = knowledgeBaseService.getCount();

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("size", size);

        return Result.success(data);
    }

    /**
     * 获取所有启用的知识库（用于AI上下文）
     */
    @GetMapping("/all")
    @Operation(summary = "获取所有知识库", description = "获取所有启用的知识库记录")
    public Result<List<KnowledgeBase>> getAll() {
        return Result.success(knowledgeBaseService.getAllEnabled());
    }

    /**
     * 搜索知识库
     */
    @GetMapping("/search")
    @Operation(summary = "搜索知识库", description = "根据关键词搜索知识库")
    public Result<List<KnowledgeBase>> search(@RequestParam String keyword) {
        return Result.success(knowledgeBaseService.search(keyword));
    }

    /**
     * 根据ID获取详情
     */
    @GetMapping("/{id}")
    @Operation(summary = "获取知识库详情", description = "根据ID获取知识库记录")
    public Result<KnowledgeBase> getById(@PathVariable Long id) {
        KnowledgeBase kb = knowledgeBaseService.getById(id);
        if (kb == null) {
            return Result.failed("记录不存在");
        }
        return Result.success(kb);
    }

    /**
     * 添加知识库记录
     */
    @PostMapping
    @Operation(summary = "添加知识库", description = "添加新的知识库记录")
    public Result<KnowledgeBase> add(@RequestBody KnowledgeBase knowledgeBase) {
        if (knowledgeBase.getQuestion() == null || knowledgeBase.getQuestion().trim().isEmpty()) {
            return Result.failed("问题不能为空");
        }
        if (knowledgeBase.getAnswer() == null || knowledgeBase.getAnswer().trim().isEmpty()) {
            return Result.failed("回答不能为空");
        }
        KnowledgeBase result = knowledgeBaseService.add(knowledgeBase);
        return Result.success(result);
    }

    /**
     * 更新知识库记录
     */
    @PutMapping("/{id}")
    @Operation(summary = "更新知识库", description = "更新知识库记录")
    public Result<Void> update(@PathVariable Long id, @RequestBody KnowledgeBase knowledgeBase) {
        knowledgeBase.setId(id);
        boolean success = knowledgeBaseService.update(knowledgeBase);
        if (!success) {
            return Result.failed("更新失败");
        }
        return Result.success(null);
    }

    /**
     * 删除知识库记录
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识库", description = "删除知识库记录")
    public Result<Void> delete(@PathVariable Long id) {
        boolean success = knowledgeBaseService.delete(id);
        if (!success) {
            return Result.failed("删除失败");
        }
        return Result.success(null);
    }

    /**
     * 启用/禁用知识库记录
     */
    @PutMapping("/{id}/status")
    @Operation(summary = "设置状态", description = "启用或禁用知识库记录")
    public Result<Void> setStatus(@PathVariable Long id, @RequestParam int status) {
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setStatus(status);
        boolean success = knowledgeBaseService.update(kb);
        if (!success) {
            return Result.failed("更新状态失败");
        }
        return Result.success(null);
    }

    /**
     * 设置质量评分
     */
    @PutMapping("/{id}/score")
    @Operation(summary = "设置评分", description = "设置知识库记录的质量评分")
    public Result<Void> setScore(@PathVariable Long id, @RequestParam int score) {
        if (score < 0 || score > 3) {
            return Result.failed("评分范围为0-3");
        }
        KnowledgeBase kb = new KnowledgeBase();
        kb.setId(id);
        kb.setQualityScore(score);
        boolean success = knowledgeBaseService.update(kb);
        if (!success) {
            return Result.failed("更新评分失败");
        }
        return Result.success(null);
    }

    /**
     * 学习接口：从智谱AI回答中学习
     * 前端在用户对智谱回答满意时调用
     */
    @PostMapping("/learn")
    @Operation(summary = "学习问答", description = "保存智谱AI的优质回答到本地知识库")
    public Result<KnowledgeBase> learn(@RequestBody LearnRequest request) {
        if (request.getQuestion() == null || request.getQuestion().trim().isEmpty()) {
            return Result.failed("问题不能为空");
        }
        if (request.getAnswer() == null || request.getAnswer().trim().isEmpty()) {
            return Result.failed("回答不能为空");
        }

        try {
            KnowledgeBase result = knowledgeBaseService.learnFromZhipu(
                request.getQuestion().trim(),
                request.getAnswer().trim()
            );
            return Result.success(result);
        } catch (Exception e) {
            log.error("学习失败: {}", e.getMessage());
            return Result.failed("学习失败: " + e.getMessage());
        }
    }

    /**
     * 预览知识库上下文（用于调试）
     */
    @GetMapping("/preview")
    @Operation(summary = "预览上下文", description = "预览给定问题的知识库上下文")
    public Result<String> preview(@RequestParam String question) {
        String context = knowledgeBaseService.getContextForAI(question);
        return Result.success(context);
    }

    /**
     * 知识库统计
     */
    @GetMapping("/stats")
    @Operation(summary = "知识库统计", description = "获取知识库统计信息")
    public Result<Map<String, Object>> getStats() {
        List<KnowledgeBase> all = knowledgeBaseService.getAllEnabled();
        int total = knowledgeBaseService.getCount();
        int enabled = (int) all.size();

        // 统计各来源数量
        long manual = all.stream().filter(kb -> "manual".equals(kb.getSource())).count();
        long zhipu = all.stream().filter(kb -> "zhipu".equals(kb.getSource())).count();
        long ollama = all.stream().filter(kb -> "ollama".equals(kb.getSource())).count();

        // 统计总使用次数
        long totalUsage = all.stream().mapToLong(kb -> kb.getUsageCount() != null ? kb.getUsageCount() : 0).sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("enabled", enabled);
        stats.put("disabled", total - enabled);
        stats.put("manualCount", manual);
        stats.put("zhipuCount", zhipu);
        stats.put("ollamaCount", ollama);
        stats.put("totalUsage", totalUsage);

        return Result.success(stats);
    }

    /**
     * 学习请求体
     */
    public static class LearnRequest {
        private String question;
        private String answer;

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
