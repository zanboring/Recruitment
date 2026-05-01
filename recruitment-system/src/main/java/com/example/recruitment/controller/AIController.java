package com.example.recruitment.controller;

import com.example.recruitment.common.Result;
import com.example.recruitment.entity.KnowledgeBase;
import com.example.recruitment.service.AIService;
import com.example.recruitment.service.JobService;
import com.example.recruitment.service.KnowledgeBaseService;
import com.example.recruitment.service.OllamaService;
import com.example.recruitment.vo.JobStatVO;
import com.example.recruitment.vo.JobTrendVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import jakarta.annotation.PreDestroy;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@Tag(name = "AI分析", description = "AI大模型问答接口（统一入口）")
public class AIController {

    private final AIService aiService;
    private final OllamaService ollamaService;
    private final JobService jobService;
    private final KnowledgeBaseService knowledgeBaseService;
    private final ExecutorService executor = Executors.newFixedThreadPool(10);

    private static final String SYSTEM_PROMPT_ZHIPU = 
            "你是\"招聘数据分析助手\"，一个专注于互联网招聘市场数据分析和求职建议的AI专家。\n" +
            "\n" +
            "你的核心能力包括：\n" +
            "1. 招聘市场数据分析：分析岗位数量、薪资分布、技能需求、城市热度等维度\n" +
            "2. 求职建议生成：根据数据分析结果，为求职者提供个性化的职业发展建议\n" +
            "3. 薪资趋势解读：解读不同城市、岗位、经验水平的薪资水平和发展空间\n" +
            "4. 技能热度评估：评估各编程语言和技能在当前招聘市场的需求和前景\n" +
            "\n" +
            "【输出格式规范 - 必须严格遵守】\n" +
            "\n" +
            "重要：你必须使用纯文本格式输出，**禁止使用任何 Markdown 标记**！\n" +
            "\n" +
            "具体要求：\n" +
            "1. **标题格式**：用 【标题名称】 的形式，例如：【薪资水平分析】、【市场趋势】\n" +
            "2. **列表格式**：用分行表述，每个要点单独一行，不用任何符号前缀\n" +
            "   例如：\"薪资范围：8-15k\"，而不是 \"- 8-15k\"\n" +
            "3. **重点内容**：用『关键词』或【关键词】标记，不用 **粗体**\n" +
            "4. **数据展示**：用 | 分隔列，但前后不要加多余的 |，如 \"经验 | 薪资 | 占比\"\n" +
            "5. **分段清晰**：每个观点之间空一行，段落不要太长\n" +
            "6. **使用emoji**：适当使用 emoji 增强可读性（💰 📊 🎯 💻 ✅ ⚠️）\n" +
            "\n" +
            "【禁止 - 绝对不允许】\n" +
            "- 绝对禁止：不要使用 - 或 * 或 + 作为列表前缀\n" +
            "- 绝对禁止：不要使用有序列表（1. 2. 3.）\n" +
            "- 绝对禁止：不要输出 # 或 ## 或 ### 这样的标题标记\n" +
            "- 绝对禁止：不要输出 ``` 或 ```语言 这样的代码块标记\n" +
            "- 绝对禁止：不要使用单个 * 或 _ 标记（会变成斜体）\n" +
            "- 绝对禁止：不要输出 ```language 这样的语言标记\n" +
            "- 绝对禁止：不要输出 | ---- | 这样的表格分隔线\n" +
            "\n" +
            "其他原则：\n" +
            "- 基于用户提供的数据进行客观分析，不编造数据\n" +
            "- 使用专业但通俗易懂的语言\n" +
            "- 给出具体可执行的建议，避免空泛的理论\n" +
            "- 当数据不足时明确说明，不要猜测\n" +
            "- 使用中文回答\n" +
            "- 回答要简洁专业，直接输出内容，不要额外的开场白\n" +
            "";

    private static final String SYSTEM_PROMPT_OLLAMA = 
            "你是\"招聘数据分析助手\"，专门回答招聘相关的问题。\n" +
            "\n" +
            "【输出格式 - 必须遵守】\n" +
            "重要：**禁止使用任何 Markdown 标记**！\n" +
            "- 标题用【标题名】格式，如【薪资分析】\n" +
            "- 列表用 - 或数字，如 \"- Java需求大\"\n" +
            "- 不要用 # ## 或 ###\n" +
            "- 不要用 ``` 或 ```语言\n" +
            "- 不要用单个 * 或 _\n" +
            "\n" +
            "【回答范围】\n" +
            "- 薪资问题：某城市/岗位的薪资水平（如：长沙Java薪资）\n" +
            "- 城市分析：某城市IT行业状况（如：长沙IT好不好）\n" +
            "- 技能需求：某技术的市场需求（如：Python好找工作吗）\n" +
            "- 学历经验：学历/经验对找工作的影响\n" +
            "- 简历面试：如何写简历、准备面试\n" +
            "- 职业发展：职业规划、中年危机等\n" +
            "\n" +
            "【回答规则】\n" +
            "1. 简洁为主：回答控制在3-5句话，除非用户要求详细\n" +
            "2. 先引用知识库：优先使用【本地知识库参考】中的内容\n" +
            "3. 结合数据：如果提供了数据库统计信息，适当引用\n" +
            "4. 诚实回答：不确定时直接说\"这个我不太清楚\"\n" +
            "5. 引导使用主API：复杂问题建议切换到主API\n" +
            "";

    private String buildEnhancedMessage(String userMessage) {
        try {
            StringBuilder context = new StringBuilder();

            // 1. 先添加知识库上下文
            String knowledgeContext = knowledgeBaseService.getContextForAI(userMessage);
            if (!knowledgeContext.isEmpty()) {
                context.append(knowledgeContext);
            }

            // 2. 添加数据库统计信息
            context.append("\n\n【当前数据库统计信息（实时）】\n");

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

            List<JobStatVO> salaryStats = jobService.statBySalaryRange();
            if (!salaryStats.isEmpty()) {
                context.append("💰 薪资分布: ");
                for (JobStatVO s : salaryStats) {
                    context.append(s.getName()).append(":").append(s.getCount()).append("个  ");
                }
                context.append("\n");
            }

            List<JobStatVO> eduStats = jobService.statByEducation();
            if (!eduStats.isEmpty()) {
                context.append("🎓 学历要求: ");
                for (JobStatVO e : eduStats) {
                    context.append(e.getName()).append("(").append(e.getCount()).append(") ");
                }
                context.append("\n");
            }

            List<JobStatVO> expStats = jobService.statByExperience();
            if (!expStats.isEmpty()) {
                context.append("📊 经验要求: ");
                for (JobStatVO e : expStats) {
                    context.append(e.getName()).append("(").append(e.getCount()).append(") ");
                }
                context.append("\n");
            }

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

    @GetMapping("/status")
    @Operation(summary = "获取AI服务状态", description = "获取Ollama和智谱API的可用状态")
    public Result<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();

        Map<String, Object> ollamaStatus = new HashMap<>();
        ollamaStatus.put("available", ollamaService.isAvailable());
        ollamaStatus.put("model", ollamaService.getModelName());
        ollamaStatus.put("status", ollamaService.isAvailable() ? "健康" : "不可用");
        ollamaStatus.put("description", ollamaService.getHealthDescription());
        status.put("ollama", ollamaStatus);

        Map<String, Object> zhipuStatus = new HashMap<>();
        zhipuStatus.put("available", aiService.isApiKeyConfigured());
        zhipuStatus.put("model", "glm-4");
        zhipuStatus.put("description", aiService.isApiKeyConfigured() ? "API Key已配置" : "未配置API Key");
        status.put("zhipu", zhipuStatus);

        return Result.success(status);
    }

    @PostMapping("/chat")
    @Operation(summary = "AI聊天（同步）", description = "通过AI服务进行智能问答（同步模式）")
    public Result<Map<String, Object>> chat(@RequestBody AIChatRequest request) {
        log.info("AI聊天请求 - 消息: {}, useLocalModel: {}", request.getMessage(), request.getUseLocalModel());
        
        try {
            String message = request.getMessage();
            if (message == null || message.trim().isEmpty()) {
                return Result.failed("消息内容不能为空");
            }

            String response;
            String usedModel;
            Boolean useLocal = request.getUseLocalModel();
            boolean ollamaAvailable = ollamaService.isAvailable();
            boolean zhipuAvailable = aiService.isApiKeyConfigured();

            if (useLocal == null) {
                if (ollamaAvailable) {
                    try {
                        String enhancedMessage = buildEnhancedMessage(message);
                        response = ollamaService.chatSync(SYSTEM_PROMPT_OLLAMA, enhancedMessage);
                        usedModel = "ollama";
                        log.info("自动模式：使用Ollama响应");
                    } catch (Exception e) {
                        log.warn("Ollama调用失败，切换到智谱API: {}", e.getMessage());
                        if (zhipuAvailable) {
                            String enhancedMessage = buildEnhancedMessage(message);
                            response = aiService.chatSync(SYSTEM_PROMPT_ZHIPU, enhancedMessage);
                            usedModel = "zhipu_fallback";
                        } else {
                            return Result.failed("AI服务暂时不可用");
                        }
                    }
                } else if (zhipuAvailable) {
                    String enhancedMessage = buildEnhancedMessage(message);
                    response = aiService.chatSync(SYSTEM_PROMPT_ZHIPU, enhancedMessage);
                    usedModel = "zhipu";
                    log.info("自动模式：Ollama不可用，使用智谱API");
                } else {
                    return Result.failed("AI服务暂时不可用");
                }
            } else if (useLocal) {
                if (!ollamaAvailable) {
                    return Result.failed("本地模型服务不可用，请确保Ollama已启动");
                }
                String enhancedMessage = buildEnhancedMessage(message);
                response = ollamaService.chatSync(SYSTEM_PROMPT_OLLAMA, enhancedMessage);
                usedModel = "ollama";
                log.info("强制使用Ollama响应");
            } else {
                if (!zhipuAvailable) {
                    return Result.failed("智谱API未配置，请联系管理员设置ZHIPUAI_API_KEY");
                }
                String enhancedMessage = buildEnhancedMessage(message);
                response = aiService.chatSync(SYSTEM_PROMPT_ZHIPU, enhancedMessage);
                usedModel = "zhipu";
                log.info("强制使用智谱API响应");
            }

            Map<String, Object> result = new HashMap<>();
            result.put("response", response);
            result.put("usedModel", usedModel);
            result.put("success", true);

            return Result.success(result);

        } catch (Exception e) {
            log.error("AI聊天失败", e);
            return Result.failed("AI聊天失败: " + e.getMessage());
        }
    }

    @PostMapping("/stream")
    @Operation(summary = "AI流式聊天", description = "统一入口，支持智能路由选择AI服务")
    public SseEmitter streamChat(@RequestBody AIChatRequest request) {
        SseEmitter emitter = new SseEmitter(120000L);

        executor.execute(() -> {
            try {
                String message = request.getMessage();
                Boolean useLocal = request.getUseLocalModel();
                boolean ollamaAvailable = ollamaService.isAvailable();
                boolean zhipuAvailable = aiService.isApiKeyConfigured();

                log.info("流式聊天请求 - 消息: {}, useLocalModel: {}, ollama可用: {}, 智谱可用: {}", 
                    message, useLocal, ollamaAvailable, zhipuAvailable);

                if (useLocal == null) {
                    if (ollamaAvailable) {
                        try {
                            emitter.send(SseEmitter.event().data("[模型] 使用本地大模型 (Ollama)\n\n"));
                            String enhancedMessage = buildEnhancedMessage(message);
                            ollamaService.chatStream(SYSTEM_PROMPT_OLLAMA, enhancedMessage, emitter);
                            emitter.complete();
                            return;
                        } catch (Exception e) {
                            log.warn("Ollama流式调用失败，切换到智谱API: {}", e.getMessage());
                            if (zhipuAvailable) {
                                emitter.send(SseEmitter.event().data("[切换] 本地模型响应失败，已切换到主API\n\n"));
                                String enhancedMessage = buildEnhancedMessage(message);
                                aiService.chatStream(SYSTEM_PROMPT_ZHIPU, enhancedMessage, emitter);
                                emitter.complete();
                                return;
                            }
                        }
                    }
                    
                    if (zhipuAvailable) {
                        if (!ollamaAvailable) {
                            emitter.send(SseEmitter.event().data("[模型] 本地模型未运行，使用主API\n\n"));
                        }
                        String enhancedMessage = buildEnhancedMessage(message);
                        aiService.chatStream(SYSTEM_PROMPT_ZHIPU, enhancedMessage, emitter);
                        emitter.complete();
                        return;
                    }
                    
                    emitter.send(SseEmitter.event().data("[错误] AI服务暂时不可用"));
                    emitter.complete();
                    
                } else if (useLocal) {
                    if (!ollamaAvailable) {
                        emitter.send(SseEmitter.event().data("[错误] 本地模型服务不可用，请确保Ollama已启动"));
                        emitter.complete();
                        return;
                    }
                    emitter.send(SseEmitter.event().data("[模型] 使用本地大模型 (Ollama)\n\n"));
                    String enhancedMessage = buildEnhancedMessage(message);
                    ollamaService.chatStream(SYSTEM_PROMPT_OLLAMA, enhancedMessage, emitter);
                    emitter.complete();
                    
                } else {
                    if (!zhipuAvailable) {
                        emitter.send(SseEmitter.event().data("[错误] 未配置智谱AI API Key，请联系管理员在环境变量中设置 ZHIPUAI_API_KEY"));
                        emitter.complete();
                        return;
                    }
                    emitter.send(SseEmitter.event().data("[模型] 使用智谱API (GLM-4)\n\n"));
                    String enhancedMessage = buildEnhancedMessage(message);
                    aiService.chatStream(SYSTEM_PROMPT_ZHIPU, enhancedMessage, emitter);
                    emitter.complete();
                }

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
        private Boolean useLocalModel;

        public String getMessage() {
            return message;
        }

        public void setMessage(String message) {
            this.message = message;
        }

        public Boolean getUseLocalModel() {
            return useLocalModel;
        }

        public void setUseLocalModel(Boolean useLocalModel) {
            this.useLocalModel = useLocalModel;
        }
    }

    @PreDestroy
    public void shutdown() {
        executor.shutdown();
    }
}
