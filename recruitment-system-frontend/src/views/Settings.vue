<template>
  <div class="page">
    <el-card>
      <div class="title">系统设置（爬虫任务）</div>

      <el-form :model="form" label-width="90px" style="margin-top: 16px">
        <el-form-item label="数据来源">
          <el-input v-model="form.sourceSite" placeholder="如：某招聘网站" />
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keyword" placeholder="如：Java 开发工程师" />
        </el-form-item>
        <el-form-item label="城市">
          <el-input v-model="form.city" placeholder="如：北京" />
        </el-form-item>

        <el-form-item>
          <el-button type="primary" :loading="loading" @click="startCrawl">
            启动爬虫任务
          </el-button>
        </el-form-item>
      </el-form>

      <div v-if="taskId" class="task">
        当前任务ID：<span class="task-id">{{ taskId }}</span>
      </div>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import http from '@/api/http';

const form = reactive({
  sourceSite: 'example-job-site',
  keyword: '',
  city: ''
});

const loading = ref(false);
const taskId = ref<number | null>(null);

const startCrawl = async () => {
  if (!form.keyword) {
    ElMessage.warning('请输入关键词');
    return;
  }
  loading.value = true;
  try {
    const createRes = await http.post('/crawl/task', {
      sourceSite: form.sourceSite,
      keyword: form.keyword,
      city: form.city
    });
    const id = createRes as any as number;
    taskId.value = id;
    await http.post(`/crawl/task/${id}/start`);
    ElMessage.success('爬虫任务已启动');
  } finally {
    loading.value = false;
  }
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
.task {
  margin-top: 16px;
}
.task-id {
  color: #409eff;
  font-weight: 700;
}
</style>

