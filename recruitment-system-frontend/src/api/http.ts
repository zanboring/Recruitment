import axios from 'axios';
import { useUserStore } from '@/store/user';
import { ElMessage } from 'element-plus';

const instance = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json;charset=utf-8'
  }
});

/**
 * 开发环境日志（生产环境自动禁用）
 * 仅记录URL和方法，不打印敏感数据（password/token等）
 */
const devLog = (type: 'request' | 'response' | 'error', info: Record<string, any>) => {
  if (import.meta.env.DEV) {
    const icon = type === 'request' ? '🚀' : type === 'response' ? '✅' : '❌';
    // 脱敏：不打印完整的data和headers
    const safeInfo = {
      url: info.url,
      method: info.method || undefined,
      status: info.status || undefined,
      // 仅在错误时显示简短信息
      message: type === 'error' ? info.message : undefined
    };
    console.log(`${icon} [${type.toUpperCase()}]`, JSON.stringify(safeInfo).replace(/[{}"]/g, ''));
  }
};

instance.interceptors.request.use(
  (config) => {
    // 开发环境日志（已脱敏）
    devLog('request', { 
      url: config.url, 
      method: config.method?.toUpperCase() 
    });
    
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers = config.headers || {};
      (config.headers as any)['Authorization'] = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => {
    devLog('error', { message: error.message });
    return Promise.reject(error);
  }
);

instance.interceptors.response.use(
  (response) => {
    // 开发环境日志（仅记录状态码，不打印完整响应体）
    devLog('response', { 
      url: response.config.url, 
      status: response.status 
    });
    
    // File download: let browser handle blob/arraybuffer directly
    if (response.config && (response.config.responseType === 'blob' || response.config.responseType === 'arraybuffer')) {
      return response.data;
    }
    const res = response.data;
    if (res.code !== 0) {
      ElMessage.error(res.msg || '请求失败');
      return Promise.reject(res);
    }
    return res.data;
  },
  (error) => {
    // 统一错误处理：网络超时/服务器错误等
    const message = error.response?.data?.msg || error.message || '网络错误';
    devLog('error', { message });
    
    // 根据HTTP状态码提供更友好的提示
    if (error.response) {
      const status = error.response.status;
      if (status === 401) {
        ElMessage.error('登录已过期，请重新登录');
        // Token无效或过期，清除本地存储并跳转登录页
        const userStore = useUserStore();
        userStore.logout();
        window.location.href = '/login';
      } else if (status === 403) {
        ElMessage.error('没有权限执行此操作');
      } else if (status === 500) {
        ElMessage.error('服务器内部错误，请稍后重试');
      } else {
        ElMessage.error(message);
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请检查网络连接');
    } else {
      ElMessage.error(message);
    }
    return Promise.reject(error);
  }
);

export default instance;
