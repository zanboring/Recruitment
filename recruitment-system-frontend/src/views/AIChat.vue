<template>
  <div class="ai-chat-container">
    <el-card class="ai-chat-card">
      <template #header>
        <div class="card-header">
          <el-avatar class="logo-avatar" size="40">
            <el-icon class="logo-icon"><ChatDotRound /></el-icon>
          </el-avatar>
          <div class="header-content">
            <h2 class="header-title">AI智能分析</h2>
            <p class="header-subtitle">基于大模型的招聘数据分析助手</p>
          </div>
          <div class="header-actions">
            <el-tooltip content="使用AI进行招聘相关分析" placement="left">
              <el-button 
                type="text" 
                icon="InfoFilled" 
                class="info-button"
              />
            </el-tooltip>
            <el-tooltip content="关于" placement="left">
              <el-button 
                type="text" 
                icon="HelpFilled" 
                class="help-button"
                @click="showAbout"
              />
            </el-tooltip>
          </div>
        </div>
      </template>

      <!-- 聊天记录 -->
      <div class="chat-messages" ref="chatMessages">
        <!-- 欢迎消息 -->
        <div v-if="messages.length === 1" class="welcome-message">
          <div class="welcome-content">
            <el-icon class="welcome-icon"><ChatDotRound /></el-icon>
            <div class="welcome-text">
              <h3>欢迎使用AI智能分析助手！</h3>
              <p>我可以帮您分析招聘数据、薪资水平、行业趋势等信息。</p>
              <p>您可以：</p>
              <ul>
                <li>输入您的问题，如：分析长沙Java后端岗位的薪资水平</li>
                <li>点击下方的常用问题快速获取信息</li>
                <li>询问关于招聘、简历、面试的相关问题</li>
              </ul>
            </div>
          </div>
        </div>

        <!-- 聊天消息 -->
        <div
          v-for="(message, index) in messages"
          :key="index"
          :class="[
            'message-item',
            message.role === 'user' ? 'user-message' : 'ai-message'
          ]"
          :style="{
            animationDelay: `${index * 0.1}s`
          }"
        >
          <div class="message-avatar">
            <el-avatar
              :size="40"
              :icon="message.role === 'user' ? User : ChatLineRound"
              :class="message.role === 'user' ? 'user-avatar' : 'ai-avatar'"
              :style="{
                animationDelay: `${index * 0.1 + 0.2}s`
              }"
            />
          </div>
          <div class="message-content">
            <div class="message-text" v-if="message.role === 'user'">{{ message.content }}</div>
            <div class="message-text markdown-body" v-else v-html="renderMarkdown(message.content)"></div>
            <div class="message-time">{{ message.timestamp }}</div>
            <div class="message-status" v-if="message.role === 'ai'">
              <el-icon class="status-icon"><Check /></el-icon>
              <span>AI</span>
            </div>
          </div>
        </div>
        
        <!-- 加载状态 -->
        <div v-if="loading" class="loading-message">
          <el-icon class="is-loading"><Loading /></el-icon>
          <span>AI正在思考...</span>
          <div class="loading-dots">
            <span class="dot"></span>
            <span class="dot"></span>
            <span class="dot"></span>
          </div>
        </div>
        
        <!-- 错误提示 -->
        <div v-if="error" class="error-message">
          <el-icon class="error-icon"><CircleCloseFilled /></el-icon>
          <span>{{ error }}</span>
          <el-button type="primary" size="small" @click="retryLastMessage" class="retry-button">
            重试
          </el-button>
        </div>
      </div>

      <!-- 输入区域 -->
      <div class="chat-input-area">
        <div class="input-container">
          <el-input
            v-model="inputMessage"
            :placeholder="'请输入您的问题，例如：分析长沙Java后端岗位的薪资水平'"
            :rows="3"
            type="textarea"
            @keyup.enter.exact="handleSend"
            :class="{ 'input-focused': inputFocused }"
            @focus="inputFocused = true"
            @blur="inputFocused = false"
            resize="none"
          />
          <div class="input-toolbar">
            <div class="toolbar-left">
              <el-tooltip content="按Enter发送" placement="top">
                <el-button 
                  type="text" 
                  icon="ChatLineSquare"
                  class="toolbar-button"
                />
              </el-tooltip>
              <el-tooltip content="清空输入" placement="top">
                <el-button 
                  type="text" 
                  icon="Delete"
                  class="toolbar-button"
                  @click="inputMessage = ''"
                />
              </el-tooltip>
            </div>
            <div class="toolbar-right">
              <span class="input-stats">{{ inputMessage.length }} 字符</span>
            </div>
          </div>
        </div>
        <div class="input-actions">
          <el-button
            type="primary"
            :loading="loading"
            @click="handleSend"
            :disabled="!inputMessage.trim() || loading"
            class="send-button"
            :class="{ 'send-button-active': inputMessage.trim() && !loading }"
          >
            <el-icon class="send-icon"><ArrowRight /></el-icon>
            发送
          </el-button>
          <el-button 
            @click="clearMessages"
            class="clear-button"
          >
            <el-icon class="clear-icon"><Delete /></el-icon>
            清空对话
          </el-button>
        </div>
      </div>

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
import { ref, computed, nextTick } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import http from '@/api/http';
import { ChatDotRound, User, ChatLineRound, Loading, InfoFilled, ArrowRight, Delete, ChatLineSquare, HelpFilled, CircleCloseFilled, Check } from '@element-plus/icons-vue';
import { marked } from 'marked';

// 聊天记录
const messages = ref([
  {
    role: 'ai',
    content: '你好！我是招聘系统的AI助手，有什么可以帮助你的吗？',
    timestamp: new Date().toLocaleTimeString()
  }
]);

// 输入消息
const inputMessage = ref('');

// 加载状态
const loading = ref(false);

// 错误信息
const error = ref('');

// 关于对话框可见性
const aboutDialogVisible = ref(false);

// 聊天消息容器
const chatMessages = ref<HTMLElement | null>(null);

// 输入框焦点状态
const inputFocused = ref(false);

// 悬停的标签索引
const hoveredTag = ref(-1);

// 最近发送的消息
const lastMessage = ref('');

// 常用问题
const commonQuestions = [
  '分析长沙Java后端岗位的薪资水平',
  '长沙应届生就业前景如何？',
  '运维岗位的技能要求有哪些？',
  '如何提高简历通过率？',
  '长沙的IT行业发展趋势' 
];

// 发送消息
const handleSend = async () => {
  if (!inputMessage.value.trim() || loading.value) return;

  const userMessage = {
    role: 'user' as const,
    content: inputMessage.value.trim(),
    timestamp: new Date().toLocaleTimeString()
  };
  messages.value.push(userMessage);
  lastMessage.value = userMessage.content;

  inputMessage.value = '';
  error.value = '';

  await nextTick();
  scrollToBottom();

  loading.value = true;

  const aiMessage = {
    role: 'ai' as const,
    content: '',
    timestamp: new Date().toLocaleTimeString()
  };
  messages.value.push(aiMessage);

  try {
    const response = await fetch('/api/ai/stream', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        message: userMessage.content
      })
    });

    const reader = response.body?.getReader();
    const decoder = new TextDecoder();

    if (reader) {
      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        const lines = chunk.split('\n');

        for (const line of lines) {
          if (line.startsWith('data:')) {
            const data = line.slice(5).trim();
            if (data && data !== '[DONE]') {
              // 直接使用数据，不解析为JSON
              aiMessage.content += data;
              await nextTick();
              scrollToBottom();
            }
          }
        }
      }
    }
  } catch (err: any) {
    error.value = 'AI回复失败，请稍后重试';
    console.error('AI回复失败:', err);
  } finally {
    loading.value = false;
  }
};

// 重试上一条消息
const retryLastMessage = () => {
  if (lastMessage.value) {
    inputMessage.value = lastMessage.value;
    handleSend();
  }
};

// 滚动到底部
const scrollToBottom = () => {
  if (chatMessages.value) {
    chatMessages.value.scrollTop = chatMessages.value.scrollHeight;
  }
};

// 渲染Markdown内容
const renderMarkdown = (content: string) => {
  return marked(content);
};

// 清空消息
const clearMessages = () => {
  ElMessageBox.confirm('确定要清空所有对话记录吗？', '提示', {
    confirmButtonText: '确定',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(() => {
    messages.value = [
      {
        role: 'ai',
        content: '你好！我是招聘系统的AI助手，有什么可以帮助你的吗？',
        timestamp: new Date().toLocaleTimeString()
      }
    ];
    error.value = '';
    lastMessage.value = '';
    ElMessage.success('对话记录已清空');
  }).catch(() => {
    // 取消操作
  });
};

// 选择常用问题
const selectQuestion = (question: string) => {
  inputMessage.value = question;
  handleSend();
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

.card-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 24px;
  background: linear-gradient(90deg, #409eff 0%, #667eea 100%);
  color: white;
  border-bottom: none;
  position: relative;
  overflow: hidden;

  // 背景装饰
  &::before {
    content: '';
    position: absolute;
    top: 0;
    right: 0;
    width: 200px;
    height: 200px;
    background: rgba(255, 255, 255, 0.1);
    border-radius: 50%;
    transform: translate(50%, -50%);
  }

  .logo-avatar {
    background: rgba(255, 255, 255, 0.2);
    border: 2px solid rgba(255, 255, 255, 0.3);
    flex-shrink: 0;

    .logo-icon {
      font-size: 24px;
      color: white;
    }
  }

  .header-content {
    flex: 1;

    .header-title {
      font-size: 24px;
      font-weight: bold;
      margin: 0 0 4px 0;
      letter-spacing: 1px;
    }

    .header-subtitle {
      font-size: 14px;
      opacity: 0.9;
      margin: 0;
    }
  }

  .header-actions {
    display: flex;
    gap: 8px;

    .info-button,
    .help-button {
      color: white;
      opacity: 0.8;
      transition: all 0.3s;

      &:hover {
        opacity: 1;
        transform: scale(1.1);
      }
    }
  }
}

.chat-messages {
  height: 550px;
  overflow-y: auto;
  padding: 24px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  position: relative;

  // 颗粒化背景
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-image: radial-gradient(#e0e0e0 1px, transparent 1px);
    background-size: 20px 20px;
    opacity: 0.1;
    pointer-events: none;
  }

  // 欢迎消息
  .welcome-message {
    display: flex;
    justify-content: center;
    margin-bottom: 30px;

    .welcome-content {
      background: linear-gradient(135deg, #ffffff 0%, #f0f9eb 100%);
      border: 1px solid #e6f7ff;
      border-radius: 16px;
      padding: 24px;
      max-width: 600px;
      width: 100%;
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.05);
      display: flex;
      gap: 16px;

      .welcome-icon {
        font-size: 48px;
        color: #409eff;
        flex-shrink: 0;
        margin-top: 8px;
      }

      .welcome-text {
        flex: 1;

        h3 {
          color: #1a1a1a;
          margin: 0 0 12px 0;
          font-size: 18px;
        }

        p {
          color: #606266;
          margin: 8px 0;
          line-height: 1.5;
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
      }
    }
  }

  // 聊天消息
  .message-item {
    display: flex;
    margin-bottom: 20px;
    max-width: 80%;
    animation: fadeIn 0.3s ease-out;

    &.user-message {
      margin-left: auto;
      flex-direction: row-reverse;

      .message-content {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
        border-radius: 18px 18px 4px 18px;
        align-items: flex-end;
        box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
      }

      .message-time {
        color: rgba(255, 255, 255, 0.7);
      }
    }

    &.ai-message {
      margin-right: auto;

      .message-content {
        background: #ffffff;
        border: 1px solid #e4e7ed;
        border-radius: 18px 18px 18px 4px;
        align-items: flex-start;
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
      }
    }

    .message-avatar {
      margin: 0 12px;
      flex-shrink: 0;

      .user-avatar {
        background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
        color: white;
      }

      .ai-avatar {
        background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
        color: white;
      }
    }

    .message-content {
        display: flex;
        flex-direction: column;
        padding: 16px 20px;
        word-wrap: break-word;
        position: relative;
        transform-origin: center;
        animation: messageSlideIn 0.3s ease-out;

        &::before {
          content: '';
          position: absolute;
          width: 12px;
          height: 12px;
          background: inherit;
          top: 16px;
          animation: messageTailFadeIn 0.3s ease-out 0.1s both;
        }

        .user-message &::before {
          right: -6px;
          border-radius: 0 0 0 100%;
        }

        .ai-message &::before {
          left: -6px;
          border-radius: 0 0 100% 0;
        }

        .message-text {
          font-size: 14px;
          line-height: 1.6;
          margin-bottom: 8px;
          word-break: break-word;
          animation: messageTextFadeIn 0.3s ease-out 0.2s both;
        }

        .message-time {
          font-size: 12px;
          color: #909399;
          align-self: flex-end;
          margin-top: 4px;
          animation: messageTimeFadeIn 0.3s ease-out 0.3s both;
        }

        .message-status {
          display: flex;
          align-items: center;
          gap: 4px;
          font-size: 11px;
          color: #409eff;
          align-self: flex-start;
          margin-top: 4px;
          animation: messageStatusFadeIn 0.3s ease-out 0.4s both;

          .status-icon {
            font-size: 10px;
          }
        }
      }
  }

  // 加载状态
  .loading-message {
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 16px;
    color: #606266;
    background: #f5f7fa;
    border-radius: 12px;
    margin: 0 auto;
    max-width: 200px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);

    .is-loading {
      margin-right: 12px;
      color: #409eff;
    }

    .loading-dots {
      display: flex;
      gap: 4px;
      margin-left: 8px;

      .dot {
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: #409eff;
        animation: loadingDots 1.5s infinite ease-in-out;

        &:nth-child(2) {
          animation-delay: 0.2s;
        }

        &:nth-child(3) {
          animation-delay: 0.4s;
        }
      }
    }
  }

  // 错误提示
  .error-message {
    display: flex;
    align-items: center;
    gap: 12px;
    padding: 16px;
    background: #fef0f0;
    border: 1px solid #fbc4c4;
    border-radius: 12px;
    margin: 0 auto 20px;
    max-width: 500px;
    width: 100%;
    animation: fadeIn 0.3s ease-out;

    .error-icon {
      font-size: 20px;
      color: #f56c6c;
      flex-shrink: 0;
    }

    span {
      flex: 1;
      color: #f56c6c;
      font-size: 14px;
    }

    .retry-button {
      flex-shrink: 0;
    }
  }
}

.chat-input-area {
  margin: 20px;

  .input-container {
    background: #ffffff;
    border: 2px solid #e4e7ed;
    border-radius: 12px;
    overflow: hidden;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
    position: relative;

    &:hover {
      box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
      transform: translateY(-1px);
    }

    &:focus-within {
      border-color: #409eff;
      box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
      transform: translateY(-1px);
    }

    .el-textarea {
      margin-bottom: 0;
      border: none;
      border-radius: 0;

      .el-textarea__inner {
        min-height: 120px;
        font-size: 14px;
        line-height: 1.5;
        border: none;
        resize: none;
        padding: 16px;
        border-radius: 12px 12px 0 0;
        transition: all 0.3s;

        &:focus {
          box-shadow: none;
        }
      }
    }

    .input-toolbar {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 12px 16px;
      border-top: 1px solid #ebeef5;
      background: #fafafa;
      transition: all 0.3s;

      .toolbar-left {
        display: flex;
        gap: 8px;

        .toolbar-button {
          color: #909399;
          transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
          border-radius: 4px;
          padding: 4px;

          &:hover {
            color: #409eff;
            transform: scale(1.1);
            background: rgba(64, 158, 255, 0.1);
          }
        }
      }

      .toolbar-right {
        .input-stats {
          font-size: 12px;
          color: #909399;
          transition: all 0.3s;

          &.typing {
            color: #409eff;
            font-weight: 500;
          }
        }
      }
    }
  }

  .input-actions {
    display: flex;
    justify-content: flex-end;
    gap: 12px;
    margin-top: 16px;

    .send-button {
      display: flex;
      align-items: center;
      gap: 8px;
      border-radius: 8px;
      padding: 12px 24px;
      font-weight: 500;
      transition: all 0.3s;
      background: #409eff;
      border: none;
      box-shadow: 0 2px 4px rgba(64, 158, 255, 0.2);

      &:hover:not(:disabled) {
        background: #66b1ff;
        transform: translateY(-2px);
        box-shadow: 0 4px 12px rgba(64, 158, 255, 0.3);
      }

      &:active:not(:disabled) {
        transform: translateY(0);
      }

      &.send-button-active {
        background: #667eea;
        box-shadow: 0 4px 12px rgba(102, 126, 234, 0.3);
      }

      .send-icon {
        font-size: 16px;
      }
    }

    .clear-button {
      display: flex;
      align-items: center;
      gap: 8px;
      border-radius: 8px;
      padding: 12px 24px;
      font-weight: 500;
      transition: all 0.3s;
      border: 1px solid #dcdfe6;
      background: #ffffff;

      &:hover {
        background: #f5f7fa;
        border-color: #c0c4cc;
        transform: translateY(-2px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      }

      &:active {
        transform: translateY(0);
      }

      .clear-icon {
        font-size: 16px;
      }
    }
  }
}

.common-questions {
  padding: 0 20px 20px;

  .questions-title {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
    font-weight: bold;
    margin-bottom: 16px;
    color: #606266;

    .questions-icon {
      color: #409eff;
    }
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
}

/* Markdown样式 */
.markdown-body {
  font-size: 14px;
  line-height: 1.6;
  color: #333;
  word-wrap: break-word;

  h1, h2, h3, h4, h5, h6 {
    margin-top: 16px;
    margin-bottom: 8px;
    font-weight: bold;
    color: #1a1a1a;
  }

  h1 { font-size: 20px; border-bottom: 1px solid #eee; padding-bottom: 8px; }
  h2 { font-size: 18px; border-bottom: 1px solid #eee; padding-bottom: 6px; }
  h3 { font-size: 16px; }

  p {
    margin: 8px 0;
  }

  ul, ol {
    padding-left: 20px;
    margin: 8px 0;
  }

  li {
    margin: 4px 0;
  }

  code {
    background-color: #f5f5f5;
    padding: 2px 4px;
    border-radius: 3px;
    font-family: Consolas, Monaco, 'Andale Mono', monospace;
    font-size: 13px;
    color: #e83e8c;
  }

  pre {
    background-color: #f5f5f5;
    padding: 12px;
    border-radius: 4px;
    overflow-x: auto;
    margin: 8px 0;

    code {
      background-color: transparent;
      padding: 0;
      color: #333;
    }
  }

  blockquote {
    border-left: 4px solid #ddd;
    padding-left: 12px;
    margin: 8px 0;
    color: #666;
  }

  strong {
    font-weight: bold;
    color: #1a1a1a;
  }

  em {
    font-style: italic;
  }

  table {
    border-collapse: collapse;
    width: 100%;
    margin: 8px 0;

    th, td {
      border: 1px solid #ddd;
      padding: 8px;
      text-align: left;
    }

    th {
      background-color: #f5f5f5;
      font-weight: bold;
    }
  }
}

/* 滚动条样式 */
.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
  transition: background 0.3s;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #a8a8a8;
}

/* 动画效果 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes loadingDots {
  0%, 60%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  30% {
    transform: scale(1.2);
    opacity: 1;
  }
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

/* 动画效果增强 */
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes messageSlideIn {
  from {
    opacity: 0;
    transform: scale(0.95) translateY(10px);
  }
  to {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes messageTailFadeIn {
  from {
    opacity: 0;
    transform: scale(0.5);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

@keyframes messageTextFadeIn {
  from {
    opacity: 0;
    transform: translateY(5px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes messageTimeFadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes messageStatusFadeIn {
  from {
    opacity: 0;
    transform: translateX(-5px);
  }
  to {
    opacity: 1;
    transform: translateX(0);
  }
}

@keyframes loadingDots {
  0%, 60%, 100% {
    transform: scale(0.8);
    opacity: 0.5;
  }
  30% {
    transform: scale(1.2);
    opacity: 1;
  }
}

/* 滚动条样式增强 */
.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #c1c1c1;
  border-radius: 4px;
  transition: background 0.3s;

  &:hover {
    background: #a8a8a8;
  }
}

/* 输入框焦点效果 */
.input-container:focus-within {
  border-color: #409eff;
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.2);
}

/* 常用问题标签增强 */
.question-tag {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover,
  &.tag-hover {
    transform: translateY(-2px) scale(1.02);
  }
}
</style>