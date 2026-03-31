<template>
  <div class="page">
    <el-card>
      <div class="header">
        <div class="title">数据管理（导入 / 导出）</div>
      </div>

      <el-upload
        :show-file-list="false"
        :http-request="onUpload"
        accept=".xlsx"
        class="upload"
      >
        <el-button type="primary">导入招聘数据（Excel）</el-button>
      </el-upload>

      <el-button type="success" style="margin-left: 12px" @click="onExport">
        导出招聘数据
      </el-button>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue';
import { ElMessage } from 'element-plus';
import http from '@/api/http';

const uploading = ref(false);

const onUpload = async (options: any) => {
  if (uploading.value) return;
  uploading.value = true;
  try {
    const formData = new FormData();
    formData.append('file', options.file);
    await http.post('/data/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    ElMessage.success('导入成功');
  } finally {
    uploading.value = false;
  }
};

const onExport = async () => {
  const res = await http.get('/data/export', { responseType: 'blob' } as any);
  const blob = res as any as Blob;
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = '招聘数据.xlsx';
  a.click();
  URL.revokeObjectURL(url);
};
</script>

<style scoped>
.page {
  width: 100%;
}
.title {
  font-size: 16px;
  font-weight: 600;
}
.header {
  margin-bottom: 16px;
}
</style>

