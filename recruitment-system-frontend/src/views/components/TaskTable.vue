<template>
  <el-row :gutter="20" style="margin-top: 20px">
    <el-col :span="24">
      <el-card class="task-card" shadow="never">
        <template #header>
          <div class="card-header">
            <div class="title">
              <el-icon><List /></el-icon>
              <span>爬虫任务列表</span>
            </div>
            <el-tag type="info" size="small">共 {{ tasks.length }} 个任务</el-tag>
          </div>
        </template>

        <el-table :data="tasks" v-loading="loading" stripe>
          <el-table-column type="index" label="序号" width="80" :index="indexMethod" />
          <el-table-column prop="sourceSite" label="来源网站" width="120" />
          <el-table-column prop="keyword" label="关键词" width="150" show-overflow-tooltip />
          <el-table-column prop="city" label="城市" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="getStatusType(row.status)" effect="dark">
                {{ getStatusText(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="jobCount" label="岗位数量" width="100" />
          <el-table-column prop="createdAt" label="创建时间" width="180" />
          <el-table-column prop="finishedAt" label="完成时间" width="180" />
          <el-table-column label="操作" width="280" fixed="right">
            <template #default="{ row }">
              <el-button
                v-if="row.status === 'PENDING'"
                type="primary"
                size="small"
                @click="startTask(row.id)"
              >
                启动
              </el-button>
              <el-button
                v-if="row.status === 'FINISHED'"
                type="success"
                size="small"
                @click="restartTask(row)"
              >
                重新爬取
              </el-button>
              <el-button
                type="info"
                size="small"
                @click="viewTaskLog(row)"
              >
                日志
              </el-button>
              <el-button
                type="danger"
                size="small"
                @click="handleDeleteTask(row)"
              >
                删除
              </el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-if="tasks.length === 0 && !loading" description="暂无爬虫任务" />
      </el-card>
    </el-col>
  </el-row>
</template>

<script setup lang="ts">
import { ElMessage, ElMessageBox } from 'element-plus';
import { List } from '@element-plus/icons-vue';
import http from '@/api/http';

interface Task {
  id: number;
  sourceSite: string;
  keyword: string;
  city: string;
  status: string;
  jobCount: number;
  createdAt: string;
  finishedAt: string;
  message?: string;
}

// Props
defineProps<{
  tasks: Task[];
  loading: boolean;
}>();

// Emits
const emit = defineEmits<{
  (e: 'loadData'): void;
  (e: 'viewTaskLog', task: Task): void;
}>();

const getStatusType = (status: string) => {
  const types: Record<string, any> = {
    PENDING: 'info',
    RUNNING: 'warning',
    FINISHED: 'success',
    FAILED: 'danger'
  };
  return types[status] || 'info';
};

const getStatusText = (status: string) => {
  const texts: Record<string, string> = {
    PENDING: '待执行',
    RUNNING: '执行中',
    FINISHED: '已完成',
    FAILED: '失败'
  };
  return texts[status] || status;
};

const indexMethod = (index: number) => {
  return index + 1;
};

const startTask = async (id: number) => {
  try {
    await http.post(`/crawl/task/${id}/start`);
    ElMessage.success('任务已启动');
    emit('loadData');
  } catch (error) {
    ElMessage.error('任务启动失败');
  }
};

const handleDeleteTask = async (task: Task) => {
  try {
    await ElMessageBox.confirm(
      `确定要删除任务"${task.sourceSite} - ${task.keyword}"吗？`,
      '删除确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
      }
    );
    
    await http.post(`/crawl/task/${task.id}/delete`);
    ElMessage.success('任务已删除');
    emit('loadData');
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  }
};

const restartTask = async (task: Task) => {
  try {
    await ElMessageBox.confirm(
      `确定要重新爬取"${task.sourceSite} - ${task.keyword}"吗？`,
      '重新爬取确认',
      {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'info',
      }
    );
    
    ElMessage.info('正在创建新的爬取任务...');
    
    const newTask = {
      sourceSite: task.sourceSite,
      keyword: task.keyword,
      city: task.city || '长沙'
    };
    
    const taskRes = await http.post('/crawl/task', newTask);
    const taskId = typeof taskRes === 'number' ? taskRes : (taskRes as any).id || taskRes;
    await http.post(`/crawl/task/${taskId}/start`);
    
    ElMessage.success('新爬取任务已创建并启动');
    emit('loadData');
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('重新爬取失败');
    }
  }
};

const viewTaskLog = (task: Task) => {
  emit('viewTaskLog', task);
};
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
</style>
