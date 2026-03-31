import axios from 'axios';
import { useUserStore } from '@/store/user';
import { ElMessage } from 'element-plus';

const instance = axios.create({
  baseURL: 'http://localhost:8080/api',
  timeout: 10000
});

instance.interceptors.request.use(
  (config) => {
    const userStore = useUserStore();
    if (userStore.token) {
      config.headers = config.headers || {};
      (config.headers as any)['Authorization'] = `Bearer ${userStore.token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

instance.interceptors.response.use(
  (response) => {
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
    ElMessage.error(error.message || '网络错误');
    return Promise.reject(error);
  }
);

export default instance;

