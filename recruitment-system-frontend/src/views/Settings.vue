<template>
  <div class="page">
    <el-card>
      <div class="title">爬虫任务设置</div>

      <el-form :model="form" label-width="90px" style="margin-top: 16px">
        <el-form-item label="数据来源">
          <el-checkbox-group v-model="form.sourceSites">
            <el-checkbox label="zhaopin">智联招聘</el-checkbox>
            <el-checkbox label="51job">前程无忧</el-checkbox>
            <el-checkbox label="boss">BOSS直聘</el-checkbox>
          </el-checkbox-group>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="form.keyword" placeholder="Java,Python,前端,测试,网络工程,大数据" />
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

      <el-table :data="tasks" style="margin-top: 16px">
        <el-table-column prop="id" label="任务ID" width="90" />
        <el-table-column prop="sourceSite" label="站点" />
        <el-table-column prop="keyword" label="关键词" />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="status" label="状态" width="110" />
        <el-table-column prop="jobCount" label="处理条数" width="100" />
        <el-table-column prop="message" label="结果" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, reactive, ref } from 'vue';
import { ElMessage } from 'element-plus';
import { createCrawlTask, fetchCrawlTasks, startCrawlTask } from '@/api/crawl';

const form = reactive({
  sourceSites: ['zhaopin', '51job', 'boss'],
  keyword: 'Java,Python,前端,测试,网络工程,大数据',
  city: ''
});

const loading = ref(false);
const taskId = ref<number | null>(null);
const tasks = ref<any[]>([]);
let timer: number | undefined;

const startCrawl = async () => {
  if (!form.sourceSites.length) {
    ElMessage.warning('请至少选择一个数据来源');
    return;
  }
  loading.value = true;
  try {
    const createRes = await createCrawlTask({
      sourceSite: form.sourceSites.join(','),
      keyword: form.keyword,
      city: form.city
    });
    const id = createRes as any as number;
    taskId.value = id;
    await startCrawlTask(id);
    ElMessage.success('爬虫任务已启动');
    await loadTasks();
  } finally {
    loading.value = false;
  }
};

const loadTasks = async () => {
  tasks.value = (await fetchCrawlTasks()) as any[];
};

onMounted(async () => {
  await loadTasks();
  timer = window.setInterval(loadTasks, 5000);
});

onUnmounted(() => {
  if (timer) {
    clearInterval(timer);
  }
});
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

