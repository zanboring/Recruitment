<template>
  <div class="chat-messages" ref="chatMessages">
    <!-- 欢迎消息 -->
    <WelcomeMessage v-if="messages.length === 1" />

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
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { User, ChatLineRound, Loading, CircleCloseFilled, Check } from '@element-plus/icons-vue';
import WelcomeMessage from './WelcomeMessage.vue';
import { renderMarkdown } from '../utils/markdownRenderer';

interface Message {
  role: 'user' | 'ai';
  content: string;
  timestamp: string;
}

// Props
defineProps<{
  messages: Message[];
  loading: boolean;
  error: string;
}>();

// Emits
const emit = defineEmits<{
  (e: 'retry'): void;
}>();

// 聊天消息容器
const chatMessages = ref<HTMLElement | null>(null);

const retryLastMessage = () => {
  emit('retry');
};

// 暴露方法给父组件
defineExpose({
  chatMessages
});
</script>

<style scoped>
.chat-messages {
  height: 550px;
  overflow-y: auto;
  padding: 24px;
  border-bottom: 1px solid #ebeef5;
  background: #fafafa;
  position: relative;
}

.chat-messages::before {
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

/* 聊天消息 */
.message-item {
  display: flex;
  margin-bottom: 20px;
  max-width: 80%;
  animation: fadeIn 0.3s ease-out;
}

.message-item.user-message {
  margin-left: auto;
  flex-direction: row-reverse;
}

.message-item.user-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  border-radius: 18px 18px 4px 18px;
  align-items: flex-end;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.message-item.user-message .message-time {
  color: rgba(255, 255, 255, 0.7);
}

.message-item.ai-message {
  margin-right: auto;
}

.message-item.ai-message .message-content {
  background: #ffffff;
  border: 1px solid #e4e7ed;
  border-radius: 18px 18px 18px 4px;
  align-items: flex-start;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.message-avatar {
  margin: 0 12px;
  flex-shrink: 0;
}

.user-avatar {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.ai-avatar {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.message-content {
  display: flex;
  flex-direction: column;
  padding: 16px 20px;
  word-wrap: break-word;
  position: relative;
  transform-origin: center;
  animation: messageSlideIn 0.3s ease-out;
}

.message-content::before {
  content: '';
  position: absolute;
  width: 12px;
  height: 12px;
  background: inherit;
  top: 16px;
  animation: messageTailFadeIn 0.3s ease-out 0.1s both;
}

.user-message .message-content::before {
  right: -6px;
  border-radius: 0 0 0 100%;
}

.ai-message .message-content::before {
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
}

.status-icon {
  font-size: 10px;
}

/* 加载状态 */
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
}

.is-loading {
  margin-right: 12px;
  color: #409eff;
}

.loading-dots {
  display: flex;
  gap: 4px;
  margin-left: 8px;
}

.dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #409eff;
  animation: loadingDots 1.5s infinite ease-in-out;
}

.dot:nth-child(2) {
  animation-delay: 0.2s;
}

.dot:nth-child(3) {
  animation-delay: 0.4s;
}

/* 错误提示 */
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
}

.error-icon {
  font-size: 20px;
  color: #f56c6c;
  flex-shrink: 0;
}

.error-message span {
  flex: 1;
  color: #f56c6c;
  font-size: 14px;
}

.retry-button {
  flex-shrink: 0;
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

/* 动画 */
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
</style>
