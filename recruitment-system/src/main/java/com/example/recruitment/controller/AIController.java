package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.service.AIService;
import com.example.recruitment.service.JobService;
import com.example.recruitment.vo.JobStatVO;
import com.example.recruitment.vo.JobTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI分析", description = "AI大模型问答接口")
public class AIController {

    private final AIService aiService;
    private final JobService jobService;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    /**
     * System Prompt - 定义AI角色和专业知识背景
     */
    private static final String SYSTEM_PROMPT = """
            你是"招聘数据分析助手"，一个专注于互联网招聘市场数据分析和求职建议的AI专家。
            
            你的核心能力包括：
            1. 招聘市场数据分析：分析岗位数量、薪资分布、技能需求、城市热度等维度
            2. 求职建议生成：根据数据分析结果，为求职者提供个性化的职业发展建议
            3. 薪资趋势解读：解读不同城市、岗位、经验水平的薪资水平和发展空间
            4. 技能热度评估：评估各编程语言和技能在当前招聘市场的需求和前景
            
            【输出格式规范 - 必须严格遵守】
            
            你必须使用标准 Markdown 格式输出，具体要求：
            1. **标题层级**：主标题用 ##，子标题用 ###，不要使用 #
            2. **列表**：使用 - 或 数字编号列表，保持缩进一致
            3. **重点内容**：使用 **粗体** 标记关键词和重要数据
            4. **数据展示**：使用表格 | 展示对比数据
            5. **分段清晰**：每个观点之间空一行，段落不要太长
            6. **使用emoji**：适当使用 emoji 增强可读性（💰 📊 🎯 💻 ✅ ⚠️）
            7. **代码**：技术术语用 `反引号` 包裹
            
            回答结构建议：
            - 开头：一句话总结你的分析结论
            - 主体：分 2-4 个小节详细展开，每节有小标题
            - 数据支撑：引用系统提供的真实数据
            - 结尾：给出 2-3 条可执行的建议
            
            其他原则：
            - 基于用户提供的数据进行客观分析，不编造数据
            - 使用专业但通俗易懂的语言
            - 给出具体可执行的建议，避免空泛的理论
            - 当数据不足时明确说明，不要猜测
            - 使用中文回答
            """;

    /** 构建带数据库统计上下文的增强消息 */
    private String buildEnhancedMessage(String userMessage) {
        try {
            StringBuilder context = new StringBuilder();
            context.append("\n\n【当前数据库统计信息（实时）】\n");

            // 城市统计 TOP5
            List<JobStatVO> cityStats = jobService.statByCity();
            if (!cityStats.isEmpty()) {
                context.append("📍 热门城市TOP5: ");
                for (int i = 0; i < Math.min(5, cityStats.size()); i++) {
                    JobStatVO c = cityStats.get(i);
                    context.append(c.getName()).append("(").append(c.getCount()).append("个岗位");
                    if (c.getAvgSalary() != null) {
                        context.append(",均薪").append(String.format("%.0f元", c.getAvgSalary()));
                    }
                    context.append(")");
                    if (i < Math.min(5, cityStats.size()) - 1) context.append("、");
                }
                context.append("\n");
            }

            // 技能统计 TOP8
            List<JobStatVO> skillStats = jobService.statBySkill();
            if (!skillStats.isEmpty()) {
                context.append("💻 热门技能TOP8: ");
                for (int i = 0; i < Math.min(8, skillStats.size()); i++) {
                    JobStatVO s = skillStats.get(i);
                    context.append(s.getName()).append("(").append(s.getCount()).append(")");
                    if (i < Math.min(8, skillStats.size()) - 1) context.append("、");
                }
                context.append("\n");
            }

            // 薪资区间统计
            List<JobStatVO> salaryStats = jobService.statBySalaryRange();
            if (!salaryStats.isEmpty()) {
                context.append("💰 薪资分布: ");
                for (JobStatVO s : salaryStats) {
                    context.append(s.getName()).append(":").append(s.getCount()).append("个  ");
                }
                context.append("\n");
            }

            // 学历要求统计
            List<JobStatVO> eduStats = jobService.statByEducation();
            if (!eduStats.isEmpty()) {
                context.append("🎓 学历要求: ");
                for (JobStatVO e : eduStats) {
                    context.append(e.getName()).append("(").append(e.getCount()).append(") ");
                }
                context.append("\n");
            }

            // 经验要求统计
            List<JobStatVO> expStats = jobService.statByExperience();
            if (!expStats.isEmpty()) {
                context.append("📊 经验验要求: ");
                for (JobStatVO e : expStats) {
                    context.append(e.getName()).append("(").append(e.getCount()).append(") ");
                }
                context.append("\n");
            }

            // 7日趋势
            JobTrendVO trend = jobService.jobTrendLast7Days();
            if (trend != null) {
                long last7 = trend.getLast7Days() != null ? trend.getLast7Days() : 0;
                long prev7 = trend.getPrev7Days() != null ? trend.getPrev7Days() : 0;
                context.append("📈 近期趋势: 最近7天新增").append(last7)
                       .append("个岗位, 前7天新增").append(prev7).append("个岗位");
                if (prev7 > 0) {
                    double ratio = (double) last7 / prev7;
                    if (ratio >= 1.15) context.append("(↑上升)");
                    else if (ratio <= 0.85) context.append("(↓回落)");
                    else context.append("(→平稳)");
                }
                context.append("\n");
            }

            // 热门岗位TOP5
            List<JobStatVO> titleStats = jobService.statTopTitles();
            if (!titleStats.isEmpty()) {
                context.append("🔥 热门岗位TOP5: ");
                for (int i = 0; i < Math.min(5, titleStats.size()); i++) {
                    context.append(titleStats.get(i).getName());
                    if (i < Math.min(5, titleStats.size()) - 1) context.append("、");
                }
                context.append("\n");
            }

            context.append("【以上数据来自系统数据库，请在回答中参考这些真实数据】\n");
            return userMessage + context.toString();

        } catch (Exception e) {
            log.warn("构建数据库上下文失败，使用原始消息: {}", e.getMessage());
            return userMessage;
        }
    }

    @PostMapping("/chat")
    @Operation(summary = "AI聊天", description = "通过ZhipuAI进行智能问答")
    public Result<String> chat(@RequestBody AIChatRequest request) {
        log.info("AI聊天请求");
        try {
            String enhancedMessage = buildEnhancedMessage(request.getMessage());
            String response = aiService.chatSync(SYSTEM_PROMPT, enhancedMessage);
            return Result.success(response);
        } catch (Exception e) {
            log.error("AI聊天失败", e);
            return Result.failed("AI聊天失败: " + e.getMessage());
        }
    }

    @PostMapping("/stream")
    @Operation(summary = "AI流式聊天", description = "通过ZhipuAI进行流式智能问答")
    public SseEmitter streamChat(@RequestBody AIChatRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);

        executor.execute(() -> {
            try {
                // 前置检查：API Key 是否已配置
                if (!aiService.isApiKeyConfigured()) {
                    emitter.send(SseEmitter.event().data("[错误] 未配置智谱AI API Key，请联系管理员在环境变量中设置 ZHIPUAI_API_KEY"));
                    emitter.complete();
                    return;
                }

                String enhancedMessage = buildEnhancedMessage(request.getMessage());
                aiService.chatStream(SYSTEM_PROMPT, enhancedMessage, emitter);
                emitter.complete();
            } catch (Exception e) {
                log.error("流式聊天失败", e);
                try {
                    emitter.send(SseEmitter.event().data("[错误] AI服务异常: " + e.getMessage()));
                } catch (Exception ignored) {}
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    public static class AIChatRequest {
        private String message;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
