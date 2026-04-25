<template>
  <div class="ai-chat-container">
    <el-card class="ai-chat-card">
      <template #header>
        <ChatHeader @show-about="showAbout" />
      </template>

      <!-- 聊天记录 -->
      <ChatMessageList
        ref="chatMessageListRef"
        :messages="messages"
        :loading="loading"
        :error="error"
        @retry="retryLastMessage"
      />

      <!-- 输入区域 -->
      <ChatInput
        ref="chatInputRef"
        :loading="loading"
        @send="handleSend"
        @clear="clearMessages"
      />

      <!-- 常用问题 -->
      <div class="common-questions">
        <h3 class="questions-title">
          <el-icon class="questions-icon"><ChatLineSquare /></el-icon>
          常用问题
        </h3>
        <div class="questions-grid">
          <el-tag
            v-for="(question, index) in commonQuestions"
            :key="index"
            class="question-tag"
            @click="selectQuestion(question)"
            effect="plain"
            :class="{ 'tag-hover': hoveredTag === index }"
            @mouseenter="hoveredTag = index"
            @mouseleave="hoveredTag = -1"
          >
            <el-icon class="tag-icon"><ChatDotRound /></el-icon>
            {{ question }}
          </el-tag>
        </div>
      </div>
      
      <!-- 关于对话框 -->
      <el-dialog
        v-model="aboutDialogVisible"
        title="关于AI智能分析"
        width="500px"
      >
        <div class="about-content">
          <div class="about-logo">
            <el-avatar size="80">
              <el-icon class="about-logo-icon"><ChatDotRound /></el-icon>
            </el-avatar>
          </div>
          <h3>AI智能分析助手</h3>
          <p class="version">版本 1.0.0</p>
          <div class="about-text">
            <p>基于大模型技术的招聘数据分析助手，为您提供专业的招聘相关分析。</p>
            <p>功能特点：</p>
            <ul>
              <li>实时智能问答</li>
              <li>薪资水平分析</li>
              <li>行业趋势预测</li>
              <li>简历优化建议</li>
              <li>面试准备指导</li>
            </ul>
            <p class="copyright">© 2026 招聘系统</p>
          </div>
        </div>
      </el-dialog>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref, nextTick, computed } from 'vue';

// 组件名称（用于 keep-alive include 匹配）
defineOptions({ name: 'AIChat' });
import { ElMessage, ElMessageBox } from 'element-plus';
import { ChatDotRound, ChatLineSquare } from '@element-plus/icons-vue';
import { useUserStore } from '@/store/user';
import { useChatStore } from '@/store/chat';

// 子组件
import ChatHeader from './components/ChatHeader.vue';
import ChatMessageList from './components/ChatMessageList.vue';
import ChatInput from './components/ChatInput.vue';

// Store
const userStore = useUserStore();
const chatStore = useChatStore();

// 从 store 读取响应式状态
const messages = computed(() => chatStore.messages);
const loading = computed(() => chatStore.loading);
const error = computed(() => chatStore.error);

// 关于对话框可见性
const aboutDialogVisible = ref(false);

// 聊天消息列表引用
const chatMessageListRef = ref<any>(null);

// ChatInput 组件引用（用于获取输入框内容）
const chatInputRef = ref<any>(null);

// 悬停的标签索引
const hoveredTag = ref(-1);

// 常用问题
const commonQuestions = [
  '分析长沙Java后端岗位的薪资水平',
  '长沙应届生就业前景如何？',
  '运维岗位的技能要求有哪些？',
  '如何提高简历通过率？',
  '长沙的IT行业发展趋势' 
];

// 滚动到底部
const scrollToBottom = () => {
  if (chatMessageListRef.value?.chatMessages) {
    chatMessageListRef.value.chatMessages.scrollTop = chatMessageListRef.value.chatMessages.scrollHeight;
  }
};

// 发送消息
const handleSend = async (messageText?: string) => {
  // 优先使用事件传入的消息，否则从 ChatInput 的 modelValue 获取
  let actualContent = '';
  if (messageText && messageText.trim()) {
    actualContent = messageText.trim();
  } else if (chatInputRef.value?.inputMessage?.trim()) {
    // ChatInput 使用 defineModel('inputMessage')，可以通过 ref 访问
    actualContent = chatInputRef.value.inputMessage.trim();
    // 清空输入框
    chatInputRef.value.inputMessage = '';
  }
  
  if (!actualContent || loading.value) return;

  // 添加用户消息到 store
  chatStore.addUserMessage(actualContent);
  if (!messageText) {
    // 清空输入框（由 ChatInput 组件处理）
  }
  chatStore.setError('');

  await nextTick();
  scrollToBottom();

  chatStore.setLoading(true);

  // 创建 AI 占位消息，获取索引
  const aiIndex = chatStore.createAiPlaceholder();

  try {
    const response = await fetch('/api/ai/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${userStore.token}`
      },
      body: JSON.stringify({
        message: actualContent
      })
    });

    // 处理 HTTP 错误状态码
    if (!response.ok) {
      let errorMsg = 'AI服务暂时不可用';
      try {
        const errBody = await response.json();
        errorMsg = errBody.msg || errBody.message || errorMsg;
      } catch (_) {
        // 非 JSON 响应，使用默认错误消息
      }
      chatStore.setAiError(aiIndex, `[错误] ${errorMsg}`);
      chatStore.setError(errorMsg);
      return;
    }

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (reader) {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value, { stream: true });
        const lines = chunk.split('\n');

        for (const line of lines) {
          const trimmedLine = line.trim();
          // 解析 SSE data 行
          if (trimmedLine.startsWith('data:')) {
            const data = trimmedLine.slice(5).trim();
            if (data && data !== '[DONE]') {
              chatStore.appendAiContent(aiIndex, data);
              await nextTick();
              scrollToBottom();
              // 控制输出速度，确保匀速显示
              await new Promise(resolve => setTimeout(resolve, 50));
            }
          }
        }
      }
    }
  } catch (err: any) {
    // 网络中断等异常
    chatStore.setError('AI回复失败，请检查网络连接或稍后重试');
    console.error('AI回复失败:', err);
    chatStore.setAiError(aiIndex, '[连接失败] 无法连接到AI服务，请检查网络或联系管理员');
  } finally {
    chatStore.setLoading(false);
  }
};

// 重试上一条消息
const retryLastMessage = () => {
  if (chatStore.lastMessage) {
    handleSend(chatStore.lastMessage);
  }
};

// 清空消息
const clearMessages = () => {
  ElMessageBox.confirm('确定要清空所有对话记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    chatStore.clearMessages();
    ElMessage.success('对话记录已清空');
  }).catch(() => {
    // 取消操作
  });
};

// 选择常用问题
const selectQuestion = (question: string) => {
  handleSend(question);
};

// 显示关于对话框
const showAbout = () => {
  aboutDialogVisible.value = true;
};
</script>

<style scoped lang="scss">
.ai-chat-container {
  max-width: 1600px;
  margin: 0 auto;
  padding: 20px;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8f0 100%);
  min-height: 100vh;
}

.ai-chat-card {
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.1);
  background: #fff;
  overflow: hidden;
  transition: all 0.3s ease;

  &:hover {
    box-shadow: 0 6px 25px rgba(0, 0, 0, 0.15);
  }
}

.common-questions {
  padding: 0 20px 20px;
}

.questions-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  font-weight: bold;
  margin-bottom: 16px;
  color: #606266;
}

.questions-icon {
  color: #409eff;
}

.questions-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.question-tag {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 18px;
  border-radius: 24px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #dcdfe6;
  background: #ffffff;
  color: #606266;
  position: relative;
  overflow: hidden;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: -100%;
    width: 100%;
    height: 100%;
    background: linear-gradient(90deg, transparent, rgba(64, 158, 255, 0.1), transparent);
    transition: left 0.5s;
  }

  &:hover,
  &.tag-hover {
    background: linear-gradient(135deg, #ecf5ff 0%, #f0f9eb 100%);
    border-color: #c6e2ff;
    color: #409eff;
    transform: translateY(-2px) scale(1.02);
    box-shadow: 0 4px 12px rgba(64, 158, 255, 0.15);

    &::before {
      left: 100%;
    }

    .tag-icon {
      transform: scale(1.1) rotate(5deg);
    }
  }

  &:active {
    transform: translateY(0) scale(0.98);
  }

  .tag-icon {
    font-size: 14px;
    transition: all 0.3s;
  }
}

/* Markdown样式 - ChatGPT风格美化 */
.markdown-body {
  font-size: 14.5px;
  line-height: 1.75;
  color: #2d2d2d;
  word-wrap: break-word;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;

  h1, h2, h3, h4, h5, h6 {
    margin-top: 20px;
    margin-bottom: 10px;
    font-weight: 700;
    line-height: 1.4;
    color: #1a1a2e;
  }

  h1 { 
    font-size: 22px; 
    border-bottom: 3px solid #667eea; 
    padding-bottom: 10px;
    background: linear-gradient(135deg, #667eea, #764ba2);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
  }
  h2 { 
    font-size: 19px; 
    border-bottom: 2px solid #e8edf5;
    padding-bottom: 8px;
    display: flex;
    align-items: center;
    gap: 8px;
  }
  h3 { 
    font-size: 16.5px;
    color: #333;
    display: inline-flex;
    align-items: center;
    gap: 6px;
  }
  h3::before {
    content: '';
    width: 4px;
    height: 18px;
    background: linear-gradient(180deg, #667eea, #764ba2);
    border-radius: 2px;
  }

  p {
    margin: 12px 0;
    text-align: justify;
  }

  ul, ol {
    padding-left: 22px;
    margin: 12px 0;
  }

  li {
    margin: 7px 0;
    position: relative;
  }

  li::marker {
    color: #667eea;
    font-weight: bold;
  }

  code {
    background-color: #f4f3f8;
    padding: 2px 7px;
    border-radius: 5px;
    font-family: 'Fira Code', Consolas, Monaco, 'Andale Mono', monospace;
    font-size: 13px;
    color: #d63384;
    border: 1px solid #e9e8ef;
  }

  pre {
    background: linear-gradient(135deg, #282c34, #21252b);
    padding: 16px;
    border-radius: 10px;
    overflow-x: auto;
    margin: 14px 0;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    border: 1px solid #3e4451;

    code {
      background-color: transparent;
      padding: 0;
      color: #abb2bf;
      font-size: 13px;
      line-height: 1.6;
    }
  }

  blockquote {
    border-left: 4px solid #667eea;
    padding: 12px 18px;
    margin: 14px 0;
    color: #556680;
    background: linear-gradient(135deg, #f8f9ff, #fff);
    border-radius: 0 10px 10px 0;
    box-shadow: 0 2px 8px rgba(102, 126, 234, 0.06);
  }

  blockquote p {
    margin: 4px 0 !important;
  }

  strong {
    font-weight: 700;
    color: #c7254e;
  }

  em {
    font-style: italic;
    color: #50a14f;
  }

  table {
    border-collapse: separate;
    width: 100%;
    margin: 14px 0;
    border-spacing: 0;
    border-radius: 10px;
    overflow: hidden;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.07);

    th, td {
      padding: 11px 15px;
      text-align: left;
      font-size: 13.5px;
    }

    th {
      background: linear-gradient(135deg, #667eea, #764ba2);
      color: white;
      font-weight: 600;
      letter-spacing: 0.3px;
      border: none;
    }

    td {
      border-bottom: 1px solid #f0f0f5;
      color: #444;
    }

    tbody tr:hover {
      background-color: #fafbff !important;
    }

    tbody tr:last-child td {
      border-bottom: none;
    }
}
}

/* 打字机光标动画 */
.typing-cursor::after {
  content: '|';
  animation: blink 1s step-end infinite;
  color: #667eea;
  font-weight: bold;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}

/* 关于对话框 */
.about-content {
  text-align: center;
  padding: 20px 0;

  .about-logo {
    margin-bottom: 20px;

    .about-logo-icon {
      font-size: 40px;
      color: #409eff;
    }
  }

  h3 {
    margin: 0 0 8px 0;
    color: #1a1a1a;
  }

  .version {
    margin: 0 0 20px 0;
    color: #909399;
    font-size: 14px;
  }

  .about-text {
    text-align: left;
    max-width: 400px;
    margin: 0 auto;

    p {
      margin: 8px 0;
      line-height: 1.5;
      color: #606266;
    }

    ul {
      padding-left: 20px;
      margin: 12px 0;

      li {
        margin: 4px 0;
        color: #606266;
        line-height: 1.4;
      }
    }

    .copyright {
      margin-top: 20px;
      color: #909399;
      font-size: 12px;
      text-align: center;
    }
  }
}

/* 响应式设计 */
@media (max-width: 768px) {
  .ai-chat-container {
    padding: 10px;
  }

  .chat-messages {
    height: 400px;
    padding: 16px;
  }

  .message-item {
    max-width: 90%;
  }

  .ai-chat-card {
    border-radius: 8px;
  }

  .card-header {
    padding: 16px;

    .header-title {
      font-size: 20px;
    }

    .logo-avatar {
      size: 32px;

      .logo-icon {
        font-size: 20px;
      }
    }
  }

  .welcome-content {
    flex-direction: column;
    text-align: center;

    .welcome-icon {
      font-size: 36px;
    }
  }

  .input-actions {
    flex-direction: column;

    .send-button,
    .clear-button {
      width: 100%;
      justify-content: center;
    }
  }
}
</style>
