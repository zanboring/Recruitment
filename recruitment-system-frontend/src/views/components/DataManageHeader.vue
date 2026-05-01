<template>
  <el-card class="action-card" shadow="never">
    <div class="card-header">
      <div class="title">
        <el-icon><FolderOpened /></el-icon>
        <span>数据管理</span>
      </div>
    </div>

    <div class="action-buttons">
      <el-upload
        :show-file-list="false"
        :http-request="onUpload"
        accept=".xlsx"
        :auto-upload="false"
        :on-change="handleFileChange"
      >
        <el-button type="primary" size="large">
          <el-icon><Upload /></el-icon>
          导入招聘数据
        </el-button>
      </el-upload>

      <el-button type="success" size="large" @click="onExport">
        <el-icon><Download /></el-icon>
        导出招聘数据
      </el-button>

      <el-button type="danger" plain size="large" @click="cleanupDatabase" :loading="cleaningData">
        <el-icon><Delete /></el-icon>
        清洗数据库
      </el-button>

      <el-button type="primary" size="large" @click="handleQuickCrawl" :loading="quickCrawling" :disabled="quickCrawling">
        <el-icon><Connection /></el-icon>
        一键实时爬取
      </el-button>

      <el-button type="warning" size="large" @click="handleOpenCrawlDialog">
        <el-icon><Connection /></el-icon>
        创建爬虫任务
      </el-button>

      <el-button type="info" size="large" @click="handleRefreshData">
        <el-icon><Refresh /></el-icon>
        刷新数据
      </el-button>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  FolderOpened,
  Upload,
  Download,
  Connection,
  Refresh,
  Delete
} from '@element-plus/icons-vue';
import http from '@/api/http';

// Props
defineProps<{
  cleaningData: boolean;
  quickCrawling: boolean;
}>();

// Emits
const emit = defineEmits<{
  (e: 'loadData'): void;
  (e: 'startQuickCrawl'): void;
  (e: 'openCrawlDialog'): void;
}>();

// 上传文件
const onUpload = async (options: any) => {
  try {
    const formData = new FormData();
    formData.append('file', options.file);
    await http.post('/data/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    ElMessage.success('导入成功');
    emit('loadData');
  } catch (error) {
    ElMessage.error('导入失败');
  }
};

// 处理文件选择
const handleFileChange = (file: any) => {
  onUpload({ file: file.raw });
};

// 导出数据
const onExport = async () => {
  try {
    const res = await http.get('/data/export', { responseType: 'blob' } as any);
    // 拦截器对 blob 请求直接返回 Blob 对象，无需再取 .data
    const blobData = res instanceof Blob ? res : new Blob([res], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' });
    const url = URL.createObjectURL(blobData);
    const a = document.createElement('a');
    a.href = url;
    a.download = '招聘数据.xlsx';
    a.click();
    URL.revokeObjectURL(url);
    ElMessage.success('导出成功');
  } catch (error) {
    ElMessage.error('导出失败');
  }
};

// 清洗数据库
const cleanupDatabase = async () => {
  try {
    await ElMessageBox.confirm(
      '该操作会清空岗位数据和爬虫任务记录，是否继续？',
      '清洗数据库确认',
      {
        confirmButtonText: '确定清洗',
        cancelButtonText: '取消',
        type: 'warning'
      }
    );

    const deletedCount = await http.post('/data/cleanup');
    ElMessage.success(`清洗完成，已删除 ${deletedCount || 0} 条岗位数据`);
    emit('loadData');
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('数据库清洗失败');
    }
  }
};

// 一键爬取按钮点击
const handleQuickCrawl = () => {
  emit('startQuickCrawl');
};

// 打开创建爬虫任务对话框
const handleOpenCrawlDialog = () => {
  showCrawlDialog.value = true;
};

// 刷新数据按钮点击
const handleRefreshData = () => {
  emit('loadData');
};

// 暴露给父组件
const showCrawlDialog = defineModel<boolean>('showCrawlDialog', { default: false });
</script>

<style scoped>
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-header .title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.action-buttons {
  display: flex;
  gap: 12px;
  margin-top: 16px;
}
</style>
