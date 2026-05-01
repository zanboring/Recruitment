import { defineStore } from 'pinia';

export interface Message {
  role: 'user' | 'ai';
  content: string;
  timestamp: string;
}

// 默认欢迎消息
const WELCOME_MESSAGE: Message = {
  role: 'ai',
  content: '你好！我是招聘系统的AI助手，有什么可以帮助你的吗？',
  timestamp: new Date().toLocaleTimeString()
};

interface ChatState {
  messages: Message[];
  loading: boolean;
  error: string;
  lastMessage: string;
}

export const useChatStore = defineStore('chat', {
  state: (): ChatState => ({
    messages: [WELCOME_MESSAGE],
    loading: false,
    error: '',
    lastMessage: ''
  }),

  getters: {
    isEmpty: (state) => state.messages.length <= 1,
    messageCount: (state) => state.messages.length
  },

  actions: {
    /** 添加用户消息 */
    addUserMessage(content: string) {
      const msg: Message = {
        role: 'user',
        content,
        timestamp: new Date().toLocaleTimeString()
      };
      this.messages.push(msg);
      this.lastMessage = content;
    },

    /** 添加 AI 空白消息（流式填充用） */
    createAiPlaceholder(): number {
      const msg: Message = {
        role: 'ai',
        content: '',
        timestamp: new Date().toLocaleTimeString()
      };
      this.messages.push(msg);
      return this.messages.length - 1; // 返回索引位置
    },

    /** 追加 AI 流式内容 */
    appendAiContent(index: number, chunk: string) {
      if (index >= 0 && index < this.messages.length) {
        this.messages[index].content += chunk;
      }
    },

    /** 设置 AI 完整内容（流式更新用） */
    setAiContent(index: number, content: string) {
      if (index >= 0 && index < this.messages.length) {
        this.messages[index].content = content;
      }
    },

    /** 设置 AI 最终错误内容 */
    setAiError(index: number, errorContent: string) {
      if (index >= 0 && index < this.messages.length) {
        if (this.messages[index].content === '') {
          this.messages[index].content = errorContent;
        }
      }
    },

    /** 设置加载状态 */
    setLoading(loading: boolean) {
      this.loading = loading;
    },

    /** 设置错误信息 */
    setError(error: string) {
      this.error = error;
    },

    /** 清空所有聊天记录（保留欢迎消息） */
    clearMessages() {
      this.messages = [{ ...WELCOME_MESSAGE, timestamp: new Date().toLocaleTimeString() }];
      this.error = '';
      this.lastMessage = '';
    }
  }
});
