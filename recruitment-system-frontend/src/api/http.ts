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

instance.interceptors.request.use(
  (config) => {
    // 打印请求日志
    console.log('🚀 Request:', {
      url: config.url,
      method: config.method,
      params: config.params,
      data: config.data,
      headers: config.headers
    });
    
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers = config.headers || {};
      (config.headers as any)['Authorization'] = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => {
    console.error('❌ Request Error:', error);
    return Promise.reject(error);
  }
);

instance.interceptors.response.use(
  (response) => {
    // 打印响应日志
    console.log('✅ Response:', {
      url: response.config.url,
      status: response.status,
      statusText: response.statusText,
      data: response.data
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
    console.error('❌ Response Error:', {
      message: error.message,
      config: error.config,
      response: error.response
    });
    ElMessage.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default instance;

