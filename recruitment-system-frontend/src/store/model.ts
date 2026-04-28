import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import axios from 'axios';

export const useModelStore = defineStore('model', () => {
  // 当前使用的模型类型: 'primary' | 'local' | 'auto'
  const currentModel = ref<'primary' | 'local' | 'auto'>('primary');
  
  // 主API状态
  const primaryAvailable = ref(false);
  const primaryDescription = ref('');
  
  // 小模型状态
  const localAvailable = ref(false);
  const localStatus = ref('unknown');
  const localDescription = ref('');
  const localVersion = ref('');
  
  // 是否正在加载
  const loading = ref(false);

  // 是否显示切换按钮（仅当小模型可用时显示）
  const showSwitchButton = computed(() => {
    return localAvailable.value;
  });

  // 当前模型显示名称
  const currentModelName = computed(() => {
    switch (currentModel.value) {
      case 'local':
        return '小模型';
      case 'primary':
        return '主API';
      case 'auto':
        return '自动';
      default:
        return '未知';
    }
  });

  // 获取模型状态
  async function fetchStatus() {
    loading.value = true;
    try {
      const response = await axios.get('/api/model/status');
      const data = response.data.data;
      
      if (data.primary) {
        primaryAvailable.value = data.primary.available;
        primaryDescription.value = data.primary.description;
      }
      
      if (data.local) {
        localAvailable.value = data.local.available;
        localStatus.value = data.local.status;
        localDescription.value = data.local.description;
        localVersion.value = data.local.version;
      }
    } catch (error: any) {
      console.error('获取模型状态失败:', error);
      if (error.response) {
        console.error('HTTP状态码:', error.response.status);
        console.error('响应数据:', error.response.data);
      } else if (error.request) {
        console.error('网络请求失败，后端服务可能未启动');
      } else {
        console.error('请求配置错误:', error.message);
      }
      localAvailable.value = false;
    } finally {
      loading.value = false;
    }
  }

  // 切换到主API
  function switchToPrimary() {
    currentModel.value = 'primary';
  }

  // 切换到小模型
  function switchToLocal() {
    if (localAvailable.value) {
      currentModel.value = 'local';
    }
  }

  // 切换到自动模式
  function switchToAuto() {
    currentModel.value = 'auto';
  }

  // 检查是否应该使用本地模型
  function shouldUseLocalModel(): boolean {
    if (currentModel.value === 'local') {
      return localAvailable.value;
    }
    if (currentModel.value === 'auto') {
      // 自动模式：优先使用小模型，如果不可用则回退到主API
      return localAvailable.value;
    }
    return false;
  }

  // 刷新状态
  function refreshStatus() {
    fetchStatus();
  }

  return {
    // 状态
    currentModel,
    primaryAvailable,
    primaryDescription,
    localAvailable,
    localStatus,
    localDescription,
    localVersion,
    loading,
    
    // 计算属性
    showSwitchButton,
    currentModelName,
    
    // 方法
    fetchStatus,
    switchToPrimary,
    switchToLocal,
    switchToAuto,
    shouldUseLocalModel,
    refreshStatus
  };
});