<template>
  <div class="data-manage-page">
    <el-row :gutter="20">
      <el-col :span="24">
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

            <el-button type="primary" size="large" @click="startQuickCrawl" :loading="quickCrawling" :disabled="quickCrawling">
              <el-icon><Connection /></el-icon>
              一键实时爬取
            </el-button>

            <el-button type="warning" size="large" @click="showCrawlDialog = true">
              <el-icon><Connection /></el-icon>
              创建爬虫任务
            </el-button>

            <el-button type="info" size="large" @click="loadData">
              <el-icon><Refresh /></el-icon>
              刷新数据
            </el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 数据统计卡片 -->
    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><DataAnalysis /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalJobs }}</div>
              <div class="stat-label">总岗位数</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Success /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ activeJobs }}</div>
              <div class="stat-label">有效岗位</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Warning /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ offlineJobs }}</div>
              <div class="stat-label">已下架</div>
            </div>
          </div>
        </el-card>
      </el-col>
      <el-col :span="6">
        <el-card class="stat-card" shadow="hover">
          <div class="stat-content">
            <div class="stat-icon">
              <el-icon><Timer /></el-icon>
            </div>
            <div class="stat-info">
              <div class="stat-value">{{ totalTasks }}</div>
              <div class="stat-label">爬取任务</div>
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 爬取进度条 -->
    <el-row :gutter="20" style="margin-top: 20px" v-if="quickCrawling">
      <el-col :span="24">
        <el-card class="progress-card" shadow="never">
          <div class="progress-header">
            <el-icon><Loading /></el-icon>
            <span>爬取进度</span>
          </div>
          <el-progress :percentage="crawlProgress" :status="crawlProgress === 100 ? 'success' : 'warning'" />
          <div class="progress-info">
            <span>当前状态: {{ crawlStatus }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="20" style="margin-top: 20px" v-if="quickCrawling || crawlLogs.length > 0">
      <el-col :span="24">
        <el-card class="log-card" shadow="never">
          <template #header>
            <div class="log-header">
              <div class="title">
                <el-icon><Document /></el-icon>
                <span>实时爬取日志</span>
              </div>
              <el-button type="danger" size="small" @click="clearLogs" :disabled="crawlLogs.length === 0">
                <el-icon><Delete /></el-icon>
                清空日志
              </el-button>
            </div>
          </template>
          <div class="log-content" ref="logContentRef">
            <div v-for="(log, index) in crawlLogs" :key="index" class="log-item" :class="log.type">
              <span class="log-time">{{ log.time }}</span>
              <span class="log-message">{{ log.message }}</span>
            </div>
            <div v-if="crawlLogs.length === 0" class="log-empty">
              暂无日志记录
            </div>
          </div>
        </el-card>
      </el-col>
    </el-row>

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

    <el-row :gutter="20" style="margin-top: 20px">
      <el-col :span="24">
        <el-card class="preview-card" shadow="never">
          <template #header>
            <div class="card-header">
              <div class="title">
                <el-icon><View /></el-icon>
                <span>数据预览</span>
              </div>
              <div class="preview-actions">
                <el-input
                  v-model="previewQuery.keyword"
                  placeholder="搜索岗位"
                  clearable
                  style="width: 200px; margin-right: 10px"
                  @keyup.enter="loadPreviewData"
                />
                <el-button type="primary" size="small" @click="loadPreviewData">
                  <el-icon><Search /></el-icon>
                  搜索
                </el-button>
              </div>
            </div>
          </template>

          <el-table :data="previewData" v-loading="previewLoading" stripe max-height="400">
            <el-table-column prop="title" label="岗位名称" width="200" show-overflow-tooltip />
            <el-table-column prop="companyName" label="公司名称" width="180" show-overflow-tooltip />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="experience" label="经验" width="100" />
            <el-table-column prop="education" label="学历" width="100" />
            <el-table-column label="薪资" width="150">
              <template #default="{ row }">
                {{ formatSalary(row.minSalary, row.maxSalary) }}
              </template>
            </el-table-column>
            <el-table-column prop="sourceSite" label="来源" width="100" />
            <el-table-column prop="createdAt" label="创建时间" width="180" />
          </el-table>

          <el-empty v-if="previewData.length === 0 && !previewLoading" description="暂无数据" />
        </el-card>
      </el-col>
    </el-row>

    <el-dialog
      v-model="showCrawlDialog"
      title="创建爬虫任务"
      width="500px"
      :close-on-click-modal="false"
    >
      <el-form :model="crawlForm" label-width="100px">
        <el-form-item label="来源网站">
          <el-select v-model="crawlForm.sourceSite" placeholder="请选择来源网站" style="width: 100%">
            <el-option label="BOSS直聘" value="boss" />
            <el-option label="智联招聘" value="zhaopin" />
            <el-option label="前程无忧" value="51job" />
            <el-option label="猎聘" value="liepin" />
          </el-select>
        </el-form-item>
        <el-form-item label="关键词">
          <el-input v-model="crawlForm.keyword" placeholder="请输入关键词，如：Java开发" />
        </el-form-item>
        <el-form-item label="城市">
          <div class="city-selection">
            <div class="city-actions">
              <el-button type="primary" size="small" @click="selectAllCities">全选</el-button>
              <el-button type="info" size="small" @click="selectInverseCities">反选</el-button>
              <el-button type="danger" size="small" @click="clearCities">清空</el-button>
            </div>
            <el-checkbox-group v-model="crawlForm.cities" style="margin-top: 10px">
              <el-checkbox v-for="city in cityOptions" :key="city.value" :label="city.value" style="margin-right: 15px; margin-bottom: 10px;">{{ city.label }}</el-checkbox>
            </el-checkbox-group>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showCrawlDialog = false">取消</el-button>
        <el-button type="primary" @click="createCrawlTask" :loading="creatingTask">
          创建任务
        </el-button>
      </template>
    </el-dialog>

    <el-dialog
      v-model="showLogDialog"
      title="任务日志"
      width="600px"
    >
      <div class="task-log">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="任务ID">{{ currentTask?.id }}</el-descriptions-item>
          <el-descriptions-item label="来源网站">{{ currentTask?.sourceSite }}</el-descriptions-item>
          <el-descriptions-item label="关键词">{{ currentTask?.keyword }}</el-descriptions-item>
          <el-descriptions-item label="城市">{{ currentTask?.city }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="getStatusType(currentTask?.status)">
              {{ getStatusText(currentTask?.status) }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="岗位数量">{{ currentTask?.jobCount }}</el-descriptions-item>
          <el-descriptions-item label="消息">{{ currentTask?.message }}</el-descriptions-item>
          <el-descriptions-item label="创建时间">{{ currentTask?.createdAt }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ currentTask?.finishedAt || '-' }}</el-descriptions-item>
        </el-descriptions>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted } from 'vue';
import { ElMessage, ElMessageBox } from 'element-plus';
import {
  FolderOpened,
  Upload,
  Download,
  Connection,
  Refresh,
  List,
  View,
  Search,
  Document,
  Delete
} from '@element-plus/icons-vue';
import http from '@/api/http';

const loading = ref(false);
const previewLoading = ref(false);
const creatingTask = ref(false);
const cleaningData = ref(false);
const quickCrawling = ref(false);
const tasks = ref<any[]>([]);
const previewData = ref<any[]>([]);
const showCrawlDialog = ref(false);
const showLogDialog = ref(false);
const currentTask = ref<any>(null);
const crawlLogs = ref<any[]>([]);
const logContentRef = ref<HTMLElement | null>(null);
const quickTaskIds = ref<number[]>([]);
const quickPollTimer = ref<number | null>(null);
const totalJobs = ref(0);
const activeJobs = ref(0);
const offlineJobs = ref(0);
const totalTasks = ref(0);
const crawlProgress = ref(0);
const crawlStatus = ref('准备中');

const cityOptions = [
  { label: '北京', value: '北京' },
  { label: '上海', value: '上海' },
  { label: '广州', value: '广州' },
  { label: '深圳', value: '深圳' },
  { label: '长沙', value: '长沙' },
  { label: '武汉', value: '武汉' },
  { label: '成都', value: '成都' },
  { label: '重庆', value: '重庆' },
  { label: '杭州', value: '杭州' },
  { label: '南京', value: '南京' },
  { label: '西安', value: '西安' }
];

const crawlForm = reactive({
  sourceSite: '',
  keyword: '',
  cities: [] as string[]
});

const previewQuery = reactive({
  keyword: ''
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await http.get('/crawl/tasks');
    tasks.value = res.data || [];
    totalTasks.value = tasks.value.length;
    
    // 加载岗位统计数据
    await loadJobStats();
  } catch (error) {
    ElMessage.error('加载任务列表失败');
  } finally {
    loading.value = false;
  }
};

const loadJobStats = async () => {
  try {
    const res = await http.post('/jobs/page', {
      pageNum: 1,
      pageSize: 1
    });
    totalJobs.value = res.data?.total || 0;
    
    // 加载有效岗位数
    const activeRes = await http.post('/jobs/page', {
      pageNum: 1,
      pageSize: 1,
      status: 'ACTIVE'
    });
    activeJobs.value = activeRes.data?.total || 0;
    
    // 加载已下架岗位数
    const offlineRes = await http.post('/jobs/page', {
      pageNum: 1,
      pageSize: 1,
      status: 'OFFLINE'
    });
    offlineJobs.value = offlineRes.data?.total || 0;
  } catch (error) {
    console.error('加载岗位统计失败:', error);
  }
};

const loadPreviewData = async () => {
  previewLoading.value = true;
  try {
    const res = await http.post('/jobs/page', {
      ...previewQuery,
      pageNum: 1,
      pageSize: 10
    });
    previewData.value = res.data?.list || [];
  } catch (error) {
    ElMessage.error('加载数据预览失败');
  } finally {
    previewLoading.value = false;
  }
};

const onUpload = async (options: any) => {
  try {
    const formData = new FormData();
    formData.append('file', options.file);
    await http.post('/data/import', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    });
    ElMessage.success('导入成功');
    loadPreviewData();
  } catch (error) {
    ElMessage.error('导入失败');
  }
};

const handleFileChange = (file: any) => {
  onUpload({ file: file.raw });
};

const onExport = async () => {
  try {
    const res = await http.get('/data/export', { responseType: 'blob' } as any);
    const blob = res.data as Blob;
    const url = URL.createObjectURL(blob);
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

    cleaningData.value = true;
    const deletedCount = await http.post('/data/cleanup');
    ElMessage.success(`清洗完成，已删除 ${deletedCount || 0} 条岗位数据`);
    crawlLogs.value = [];
    quickTaskIds.value = [];
    await loadData();
    await loadPreviewData();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('数据库清洗失败');
    }
  } finally {
    cleaningData.value = false;
  }
};

const selectAllCities = () => {
  crawlForm.cities = cityOptions.map(city => city.value);
};

const selectInverseCities = () => {
  const allValues = cityOptions.map(city => city.value);
  crawlForm.cities = allValues.filter(value => !crawlForm.cities.includes(value));
};

const clearCities = () => {
  crawlForm.cities = [];
};

const createCrawlTask = async () => {
  if (!crawlForm.sourceSite || !crawlForm.keyword || crawlForm.cities.length === 0) {
    ElMessage.warning('请填写完整的任务信息');
    return;
  }

  creatingTask.value = true;
  try {
    await http.post('/crawl/task', {
      ...crawlForm,
      city: crawlForm.cities.join(',')
    });
    ElMessage.success('任务创建成功');
    showCrawlDialog.value = false;
    Object.assign(crawlForm, { sourceSite: '', keyword: '', cities: [] });
    loadData();
  } catch (error) {
    ElMessage.error('任务创建失败');
  } finally {
    creatingTask.value = false;
  }
};

const startTask = async (id: number) => {
  try {
    await http.post(`/crawl/task/${id}/start`);
    ElMessage.success('任务已启动');
    loadData();
  } catch (error) {
    ElMessage.error('任务启动失败');
  }
};

const startQuickCrawl = async () => {
  stopQuickPolling();
  quickCrawling.value = true;
  crawlLogs.value = [];
  quickTaskIds.value = [];
  crawlProgress.value = 0;
  crawlStatus.value = '准备中';
  
  const defaultCities = ['长沙'];
  const keywords = ['Java后端', '运维', '软件测试', 'Python开发', '计算机相关', '应届生', '校招', '25届', '26届', '无经验', '0-1年', '1-3年', '实习'];
  
  addLog('info', '========================================');
  addLog('info', '开始一键实时爬取所有平台');
  addLog('info', `目标城市：${defaultCities.join('、')}`);
  addLog('info', '目标平台：BOSS、智联招聘、前程无忧、猎聘、拉勾网、牛客网');
  addLog('info', `关键词：${keywords.join(', ')}`);
  addLog('info', '========================================');

  try {
    const sites = [
      { name: 'BOSS', code: 'boss' },
      { name: '智联招聘', code: 'zhaopin' },
      { name: '前程无忧', code: '51job' },
      { name: '猎聘', code: 'liepin' },
      { name: '拉勾网', code: 'lagou' },
      { name: '牛客网', code: 'nowcoder' }
    ];
    
    const city = defaultCities.join(',');
    let totalCreated = 0;
    let totalFailed = 0;

    for (let i = 0; i < sites.length; i++) {
      const site = sites[i];
      addLog('info', `----------------------------------------`);
      addLog('info', `正在爬取 ${site.name}...`);
      crawlStatus.value = `正在爬取 ${site.name}`;
      crawlProgress.value = Math.round((i / sites.length) * 30);

      const task = {
        sourceSite: site.code,
        keyword: keywords.join(','),
        city: city
      };

      try {
        addLog('info', `创建 ${site.name} 爬取任务...`);
        const taskId = await http.post('/crawl/task', task) as number;
        
        if (taskId) {
          totalCreated++;
          quickTaskIds.value.push(taskId);
          addLog('success', `✓ ${site.name} 任务创建成功 (任务ID: ${taskId})`);
          
          // 启动任务
          addLog('info', `启动 ${site.name} 爬取任务...`);
          await http.post(`/crawl/task/${taskId}/start`);
          addLog('success', `✓ ${site.name} 任务已启动`);
        } else {
          totalFailed++;
          addLog('error', `✗ ${site.name} 任务创建失败：无效的响应`);
        }
      } catch (error: any) {
        totalFailed++;
        const errorMsg = error.message || '未知错误';
        addLog('error', `✗ ${site.name} 任务创建失败：${errorMsg}`);
      }

      // 避免请求过于频繁
      await new Promise(resolve => setTimeout(resolve, 3000));
    }

    crawlProgress.value = 40;
    crawlStatus.value = '爬取任务已启动，等待执行完成';
    
    addLog('info', '========================================');
    addLog('success', `所有平台爬取任务已创建完成`);
    addLog('success', `成功创建 ${totalCreated} 个爬取任务`);
    if (totalFailed > 0) {
      addLog('warning', `失败 ${totalFailed} 个爬取任务`);
    }
    addLog('info', '正在后台执行爬取，系统将自动轮询任务状态...');
    addLog('info', '========================================');
    
    ElMessage.success('一键爬取任务已启动，正在后台执行');

    startQuickPolling();

  } catch (error: any) {
    const errorMsg = error.message || '未知错误';
    addLog('error', `爬取失败：${errorMsg}`);
    addLog('error', '请检查网络连接或后端服务是否正常');
    ElMessage.error('爬取失败，请检查网络连接或后端服务');
    quickCrawling.value = false;
  }
};

const stopQuickPolling = () => {
  if (quickPollTimer.value != null) {
    window.clearInterval(quickPollTimer.value);
    quickPollTimer.value = null;
  }
};

const updateQuickTasksStatus = async () => {
  try {
    const res = await http.get('/crawl/tasks');
    const allTasks = res.data || [];
    const quickTasks = allTasks.filter(t => quickTaskIds.value.includes(t.id));
    
    // 只更新状态发生变化的任务
    let updated = false;
    tasks.value = tasks.value.map(task => {
      const updatedTask = quickTasks.find(t => t.id === task.id);
      if (updatedTask && updatedTask.status !== task.status) {
        updated = true;
        return updatedTask;
      }
      return task;
    });
    
    return quickTasks;
  } catch (error) {
    console.error('更新任务状态失败:', error);
    return [];
  }
};

const startQuickPolling = () => {
  stopQuickPolling();
  let rounds = 0;
  const totalTasks = quickTaskIds.value.length;
  quickPollTimer.value = window.setInterval(async () => {
    rounds++;
    try {
      const quickTasks = await updateQuickTasksStatus();
      const running = quickTasks.filter(t => t.status === 'RUNNING').length;
      const finished = quickTasks.filter(t => t.status === 'FINISHED' || t.status === 'FAILED').length;
      addLog('info', `轮询状态：运行中 ${running} 个，已结束 ${finished}/${totalTasks}`);

      // 更新爬取进度
      const progress = 40 + Math.round((finished / totalTasks) * 60);
      crawlProgress.value = Math.min(progress, 100);
      
      if (running > 0) {
        crawlStatus.value = `正在爬取中，已完成 ${finished}/${totalTasks} 个任务`;
      } else if (finished < totalTasks) {
        crawlStatus.value = `等待任务执行，已完成 ${finished}/${totalTasks} 个任务`;
      } else {
        crawlStatus.value = '所有任务已完成';
      }

      if (finished === totalTasks || rounds >= 30) {
        stopQuickPolling();
        await loadPreviewData();
        await loadJobStats();
        quickCrawling.value = false;
        crawlProgress.value = 100;
        crawlStatus.value = '爬取完成';
        addLog('success', '✓ 爬取任务轮询结束，数据已刷新');
        addLog('info', '========================================');
        addLog('info', '爬取流程结束');
        addLog('info', '========================================');
      }
    } catch (e) {
      addLog('warning', '轮询任务状态失败，稍后重试');
    }
  }, 8000);
};

const addLog = (type: string, message: string) => {
  const now = new Date();
  const time = `${now.getHours().toString().padStart(2, '0')}:${now.getMinutes().toString().padStart(2, '0')}:${now.getSeconds().toString().padStart(2, '0')}`;

  crawlLogs.value.push({
    type,
    time,
    message
  });

  if (logContentRef.value) {
    setTimeout(() => {
      if (logContentRef.value) {
        logContentRef.value.scrollTop = logContentRef.value.scrollHeight;
      }
    }, 100);
  }
};

const clearLogs = () => {
  crawlLogs.value = [];
  ElMessage.success('日志已清空');
};

const handleDeleteTask = async (task: any) => {
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
    loadData();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('删除失败');
    }
  }
};

const restartTask = async (task: any) => {
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
    
    const taskId = await http.post('/crawl/task', newTask);
    await http.post(`/crawl/task/${taskId}/start`);
    
    ElMessage.success('新爬取任务已创建并启动');
    loadData();
  } catch (error: any) {
    if (error !== 'cancel') {
      ElMessage.error('重新爬取失败');
    }
  }
};

const viewTaskLog = (task: any) => {
  currentTask.value = task;
  showLogDialog.value = true;
};

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

const formatSalary = (min?: number, max?: number) => {
  if (!min && !max) return '面议';
  const minK = min ? (min / 1000).toFixed(1) : '';
  const maxK = max ? (max / 1000).toFixed(1) : '';
  if (min && max) return `${minK}-${maxK}K`;
  if (min) return `${minK}K+`;
  if (max) return `${maxK}K以下`;
  return '面议';
};

onMounted(() => {
  loadData();
  loadPreviewData();
});

onUnmounted(() => {
  stopQuickPolling();
});
</script>

<style scoped>
.data-manage-page {
  padding: 20px;
}

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

/* 数据统计卡片 */
.stat-card {
  border: none !important;
  border-radius: 16px !important;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fc 100%) !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06) !important;
  transition: all 0.3s ease !important;
}

.stat-card:hover {
  transform: translateY(-4px) !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12) !important;
}

.stat-content {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 0;
}

.stat-icon {
  width: 48px;
  height: 48px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  color: #fff;
}

.stat-card:nth-child(1) .stat-icon {
  background: linear-gradient(135deg, #4f46e5 0%, #6366f1 100%);
}

.stat-card:nth-child(2) .stat-icon {
  background: linear-gradient(135deg, #10b981 0%, #34d399 100%);
}

.stat-card:nth-child(3) .stat-icon {
  background: linear-gradient(135deg, #f59e0b 0%, #fbbf24 100%);
}

.stat-card:nth-child(4) .stat-icon {
  background: linear-gradient(135deg, #ef4444 0%, #f87171 100%);
}

.stat-icon :deep(.el-icon) {
  color: #fff !important;
}

.stat-info {
  flex: 1;
}

.stat-value {
  font-size: 24px;
  font-weight: 800;
  color: #303133;
  line-height: 1.2;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 4px;
}

/* 进度条卡片 */
.progress-card {
  border: none !important;
  border-radius: 16px !important;
  background: #fff !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06) !important;
}

.progress-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
}

.progress-info {
  margin-top: 12px;
  font-size: 14px;
  color: #606266;
  text-align: center;
}

.preview-actions {
  display: flex;
  align-items: center;
}

.task-log {
  padding: 10px 0;
}

.log-card {
  border: none;
  border-radius: 12px;
  background: #1a1a2e;
}

.log-card :deep(.el-card__header) {
  background: #16213e;
  border-bottom: 1px solid #0f3460;
  padding: 16px 20px;
}

.log-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.log-header .title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  font-weight: 600;
  color: #e94560;
}

.log-content {
  background: #0f0f23;
  border-radius: 8px;
  padding: 16px;
  height: 400px;
  overflow-y: auto;
  font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
  font-size: 13px;
}

.log-content::-webkit-scrollbar {
  width: 8px;
}

.log-content::-webkit-scrollbar-track {
  background: #1a1a2e;
  border-radius: 4px;
}

.log-content::-webkit-scrollbar-thumb {
  background: #e94560;
  border-radius: 4px;
}

.log-content::-webkit-scrollbar-thumb:hover {
  background: #ff6b6b;
}

.log-item {
  display: flex;
  gap: 12px;
  padding: 8px 0;
  border-bottom: 1px solid #1a1a2e;
  color: #e0e0e0;
}

.log-item:last-child {
  border-bottom: none;
}

.log-time {
  color: #888;
  font-size: 12px;
  min-width: 70px;
}

.log-message {
  flex: 1;
  word-break: break-all;
}

.log-item.info .log-message {
  color: #4a9eff;
}

.log-item.success .log-message {
  color: #4ade80;
}

.log-item.error .log-message {
  color: #f87171;
}

.log-empty {
  text-align: center;
  color: #666;
  padding: 40px 0;
  font-size: 14px;
}

.city-selection {
  width: 100%;
}

.city-actions {
  display: flex;
  gap: 10px;
  margin-bottom: 10px;
}

.city-actions .el-button {
  flex: 1;
  max-width: 100px;
}

.el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.el-checkbox {
  margin-right: 15px !important;
  margin-bottom: 10px !important;
}
</style>

