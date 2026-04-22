<template>
  <div class="job-list-container">
    <el-card class="filter-card" shadow="never">
      <div class="filter-section">
        <el-row :gutter="20" align="middle" class="filter-row">
          <el-col :span="5">
            <el-input
              v-model="query.keyword"
              placeholder="岗位名称/公司/技能"
              clearable
              @keyup.enter="loadData"
              size="large"
              style="width: 220px"
            >
              <template #prefix>
                <el-icon><Search /></el-icon>
              </template>
            </el-input>
          </el-col>
          <el-col :span="4">
            <el-select v-model="query.city" placeholder="选择城市" clearable filterable size="large" style="width: 220px">
              <el-option label="北京" value="北京" />
              <el-option label="上海" value="上海" />
              <el-option label="广州" value="广州" />
              <el-option label="深圳" value="深圳" />
              <el-option label="杭州" value="杭州" />
              <el-option label="南京" value="南京" />
              <el-option label="成都" value="成都" />
              <el-option label="武汉" value="武汉" />
              <el-option label="西安" value="西安" />
              <el-option label="苏州" value="苏州" />
              <el-option label="重庆" value="重庆" />
              <el-option label="天津" value="天津" />
              <el-option label="长沙" value="长沙" />
              <el-option label="郑州" value="郑州" />
              <el-option label="东莞" value="东莞" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="query.sourceSite" placeholder="来源网站" clearable size="large" style="width: 220px">
              <el-option label="BOSS直聘" value="BOSS直聘" />
              <el-option label="智联招聘" value="智联招聘" />
              <el-option label="前程无忧" value="前程无忧" />
              <el-option label="猎聘" value="猎聘" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="query.education" placeholder="学历" clearable size="large" style="width: 220px">
              <el-option label="不限" value="不限" />
              <el-option label="大专" value="大专" />
              <el-option label="本科" value="本科" />
              <el-option label="硕士" value="硕士" />
              <el-option label="博士" value="博士" />
            </el-select>
          </el-col>
          <el-col :span="4">
            <el-select v-model="query.experience" placeholder="经验" clearable size="large" style="width: 220px">
              <el-option label="不限" value="经验不限" />
              <el-option label="应届" value="应届" />
              <el-option label="1-3年" value="1-3年" />
              <el-option label="3-5年" value="3-5年" />
              <el-option label="5-10年" value="5-10年" />
            </el-select>
          </el-col>
        </el-row>

        <el-row :gutter="20" align="middle" class="filter-row">
          <el-col :span="6">
            <div class="salary-range">
              <el-input-number
                v-model="query.minSalary"
                :min="0"
                :step="1000"
                placeholder="最低薪资"
                controls-position="right"
                size="large"
                style="width: 120px"
              />
              <span class="salary-separator">-</span>
              <el-input-number
                v-model="query.maxSalary"
                :min="0"
                :step="1000"
                placeholder="最高薪资"
                controls-position="right"
                size="large"
                style="width: 120px"
              />
            </div>
          </el-col>
          <el-col :span="18">
            <el-space>
              <el-button type="primary" size="large" @click="loadData" class="action-btn">
                <el-icon><Search /></el-icon>搜索
              </el-button>
              <el-button size="large" @click="resetFilters">重置</el-button>
              <el-button type="success" size="large" @click="handleAIAnalysis" class="action-btn">
                <el-icon><DataAnalysis /></el-icon>AI分析
              </el-button>
              <el-button type="primary" size="large" @click="startQuickCrawl" :loading="quickCrawling" :disabled="quickCrawling" class="action-btn">
                <el-icon><Connection /></el-icon>一键爬取
              </el-button>
              <el-button type="warning" size="large" @click="exportToExcel" class="action-btn">
                <el-icon><Download /></el-icon>导出Excel
              </el-button>
            </el-space>
          </el-col>
        </el-row>
      </div>
    </el-card>

    <div class="job-cards">
      <el-row :gutter="16" class="result-info-row">
        <el-col :span="12">
          <div class="result-info">
            <el-icon><InfoFilled /></el-icon>
            共找到 <span class="highlight">{{ total }}</span> 个符合条件岗位
          </div>
        </el-col>
        <el-col :span="12" class="result-actions">
          <el-radio-group v-model="query.pageSize" @change="loadData" size="small">
            <el-radio-button :value="12">12</el-radio-button>
            <el-radio-button :value="24">24</el-radio-button>
            <el-radio-button :value="36">36</el-radio-button>
          </el-radio-group>
        </el-col>
      </el-row>

      <el-row :gutter="20" v-loading="loading" class="job-grid">
        <el-col :span="8" v-for="job in list" :key="job.id" class="job-col">
          <el-card class="job-card" shadow="hover" @click="handleJobClick(job)" style="cursor: pointer;">
            <!-- 卡片头部 -->
            <div class="job-card-header">
              <div class="job-title-wrapper">
                <div class="job-title">{{ job.title }}</div>
                <el-tag :type="getStatusType(job.jobStatus)" effect="dark" size="small" round>
                  {{ getStatusText(job.jobStatus) }}
                </el-tag>
              </div>
            </div>

            <!-- 公司信息 -->
            <div class="job-company" v-if="job.companyName && job.companyName !== '未知公司'">
              <el-icon><OfficeBuilding /></el-icon>
              <span class="company-name">{{ job.companyName }}</span>
            </div>
            <div class="job-company" v-else>
              <el-icon><OfficeBuilding /></el-icon>
              <span class="company-name" style="color: #909399;">公司信息待完善</span>
            </div>

            <!-- 薪资信息 -->
            <div class="job-salary">
              <div class="salary-wrapper">
                <span class="salary-value">{{ formatSalary(job.minSalary, job.maxSalary) }}</span>
                <span class="salary-unit">/月</span>
              </div>
            </div>

            <!-- 详细信息 -->
            <div class="job-info-row">
              <div class="info-item">
                <el-icon><Location /></el-icon>
                <span>{{ job.city || '未知' }}</span>
              </div>
              <div class="info-item">
                <el-icon><Reading /></el-icon>
                <span>{{ job.education || '不限' }}</span>
              </div>
              <div class="info-item">
                <el-icon><Clock /></el-icon>
                <span>{{ job.experience || '经验不限' }}</span>
              </div>
            </div>

            <!-- 岗位介绍 -->
            <div class="job-desc" v-if="job.jobDesc">
              <div class="desc-title">
                <el-icon><Document /></el-icon>
                <span>岗位介绍</span>
              </div>
              <div class="desc-content">{{ truncateDesc(job.jobDesc) }}</div>
            </div>

            <!-- 技能标签 -->
            <div class="job-skills">
              <el-tag
                v-for="(skill, index) in (job.skills || '').split(',').slice(0, 4)"
                :key="skill"
                :type="getSkillTagType(index)"
                effect="light"
                size="small"
                class="skill-tag"
              >
                {{ skill }}
              </el-tag>
            </div>

            <!-- 底部信息 -->
            <div class="job-footer">
              <el-tag :type="getSourceType(job.sourceSite)" size="small" effect="plain">
                {{ job.sourceSite || '未知' }}
              </el-tag>
              <span class="publish-time">{{ formatDate(job.publishTime) }}</span>
            </div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="16" v-if="list.length === 0 && !loading">
        <el-col :span="24">
          <el-empty description="暂无岗位数据，请先执行爬虫任务获取数据">
            <el-button type="primary" @click="$router.push('/data')">前往数据管理</el-button>
          </el-empty>
        </el-col>
      </el-row>

      <div class="pagination-container" v-if="total > 0">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[12, 24, 36]"
          :total="total"
          layout="total, sizes, prev, pager, next, jumper"
          background
          size="default"
          @current-change="loadData"
          @size-change="loadData"
        />
      </div>
    </div>

    <!-- AI分析弹窗 -->
    <el-dialog
      v-model="showAIAnalysis"
      title="AI智能分析报告"
      width="85%"
      top="3vh"
      class="ai-dialog"
      :close-on-click-modal="false"
    >
      <div v-loading="aiLoading" class="ai-analysis-content">
        <!-- 分析摘要 -->
        <el-alert
          v-if="aiResult.summary"
          :title="aiResult.summary"
          type="success"
          :closable="false"
          show-icon
          class="analysis-summary"
        />

        <!-- 优质岗位推荐 -->
        <div class="analysis-section">
          <div class="section-header">
            <el-icon><Star /></el-icon>
            <span>优质岗位推荐</span>
          </div>
          <el-table :data="aiResult.qualityJobs" stripe class="analysis-table">
            <el-table-column prop="title" label="岗位名称" width="180" show-overflow-tooltip />
            <el-table-column prop="companyName" label="公司" width="150" show-overflow-tooltip />
            <el-table-column prop="city" label="城市" width="100" />
            <el-table-column prop="salary" label="薪资" width="140" />
            <el-table-column prop="skills" label="技能要求" show-overflow-tooltip />
            <el-table-column prop="recommendReason" label="推荐理由" show-overflow-tooltip />
          </el-table>
        </div>

        <!-- 需求趋势分析 -->
        <div class="analysis-section">
          <div class="section-header">
            <el-icon><TrendCharts /></el-icon>
            <span>需求趋势分析</span>
          </div>
          <el-descriptions :column="3" border v-if="aiResult.trendAnalysis" class="trend-desc">
            <el-descriptions-item label="热门城市">
              <el-tag type="primary">{{ aiResult.trendAnalysis.hotCity }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="热门技能">
              <el-tag type="success">{{ aiResult.trendAnalysis.hotSkill }}</el-tag>
            </el-descriptions-item>
            <el-descriptions-item label="热门岗位">
              <el-tag type="warning">{{ aiResult.trendAnalysis.hotTitle }}</el-tag>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 技能需求排名 -->
        <div class="analysis-section">
          <div class="section-header">
            <el-icon><Histogram /></el-icon>
            <span>技能需求排名</span>
          </div>
          <el-row :gutter="12">
            <el-col :span="6" v-for="(skill, index) in aiResult.skillDemands" :key="skill.skill">
              <el-card shadow="hover" class="skill-card" :class="'skill-rank-' + (Number(index) + 1)">
                <div class="skill-rank">{{ Number(index) + 1 }}</div>
                <div class="skill-name">{{ skill.skill }}</div>
                <div class="skill-count">{{ skill.count }}个岗位</div>
                <el-tag :type="getSkillLevelType(skill.level)" size="small">{{ skill.level }}</el-tag>
              </el-card>
            </el-col>
          </el-row>
        </div>

        <!-- 薪资分析 -->
        <div class="analysis-section">
          <div class="section-header">
            <el-icon><Money /></el-icon>
            <span>薪资分析</span>
          </div>
          <el-descriptions :column="3" border v-if="aiResult.salaryAnalysis" class="salary-desc">
            <el-descriptions-item label="平均薪资">
              <span class="salary-highlight">{{ aiResult.salaryAnalysis.avgSalary }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="最高薪资">
              <span class="salary-highlight">{{ aiResult.salaryAnalysis.topSalary }}</span>
            </el-descriptions-item>
            <el-descriptions-item label="薪资范围">
              <span class="salary-highlight">{{ aiResult.salaryAnalysis.salaryRange }}</span>
            </el-descriptions-item>
          </el-descriptions>
        </div>

        <!-- 求职建议 -->
        <div class="analysis-section">
          <div class="section-header">
            <el-icon><ChatDotRound /></el-icon>
            <span>求职建议</span>
          </div>
          <el-card class="suggestions-card">
            <el-timeline>
              <el-timeline-item
                v-for="(suggestion, index) in aiResult.suggestions"
                :key="index"
                :timestamp="suggestion"
                placement="top"
                :icon="Calendar"
              >
              </el-timeline-item>
            </el-timeline>
          </el-card>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { Search, DataAnalysis, OfficeBuilding, Location, Reading, Clock, InfoFilled, Star, TrendCharts, Histogram, Money, ChatDotRound, Calendar, Connection, Download } from '@element-plus/icons-vue';
import { fetchJobPage, aiAnalysis } from '@/api/job';
import { ElMessage } from 'element-plus';
import http from '@/api/http';

const loading = ref(false);
const list = ref<any[]>([]);
const total = ref(0);
const showAIAnalysis = ref(false);
const aiLoading = ref(false);
const quickCrawling = ref(false);
const aiResult = ref<any>({
  summary: '',
  qualityJobs: [],
  trendAnalysis: {},
  skillDemands: [],
  salaryAnalysis: {},
  suggestions: []
});

const query = reactive({
  keyword: '',
  city: '',
  sourceSite: '',
  education: '',
  experience: '',
  minSalary: undefined as number | undefined,
  maxSalary: undefined as number | undefined,
  status: '',
  pageNum: 1,
  pageSize: 12
});

const loadData = async () => {
  loading.value = true;
  try {
    const res = await fetchJobPage(query);
    list.value = res.list || [];
    total.value = res.total || 0;
  } catch (error) {
    ElMessage.error('加载数据失败');
  } finally {
    loading.value = false;
  }
};

const resetFilters = () => {
  query.keyword = '';
  query.city = '';
  query.sourceSite = '';
  query.education = '';
  query.experience = '';
  query.minSalary = undefined;
  query.maxSalary = undefined;
  query.pageNum = 1;
  loadData();
};

const startQuickCrawl = async () => {
  quickCrawling.value = true;
  ElMessage.info('开始一键爬取长沙岗位数据...');

  try {
    const sites = [
      { name: 'BOSS直聘', code: 'boss' },
      { name: '智联招聘', code: 'zhaopin' },
      { name: '前程无忧', code: '51job' },
      { name: '猎聘', code: 'liepin' }
    ];
    const keywords = ['Java', '前端', 'Python', '大数据', '运维', '测试'];
    const city = '长沙';
    let successCount = 0;

    for (const site of sites) {
      try {
        const task = {
          sourceSite: site.name,
          keyword: keywords.join(','),
          city: city
        };

        const response = await http.post('/crawl/task', task);
        await http.post(`/crawl/task/${response}/start`);
        successCount++;
      } catch (error) {
        console.error(`${site.name} 爬取失败:`, error);
      }

      await new Promise(resolve => setTimeout(resolve, 1000));
    }

    ElMessage.success(`成功创建 ${successCount} 个爬取任务，正在后台执行...`);

    setTimeout(() => {
      loadData();
      ElMessage.success('数据已刷新');
    }, 8000);

  } catch (error) {
    ElMessage.error('爬取失败，请检查网络连接');
  } finally {
    setTimeout(() => {
      quickCrawling.value = false;
    }, 10000);
  }
};

const exportToExcel = async () => {
  try {
    ElMessage.info('正在导出Excel...');

    const response = await http.get('/data/export', {
      responseType: 'blob',
      params: {
        keyword: query.keyword,
        city: query.city,
        sourceSite: query.sourceSite,
        education: query.education,
        experience: query.experience,
        minSalary: query.minSalary,
        maxSalary: query.maxSalary,
        status: query.status
      }
    });

    const blob = new Blob([response.data], {
      type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet'
    });

    const url = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;

    const now = new Date();
    const dateStr = `${now.getFullYear()}${(now.getMonth() + 1).toString().padStart(2, '0')}${now.getDate().toString().padStart(2, '0')}_${now.getHours().toString().padStart(2, '0')}${now.getMinutes().toString().padStart(2, '0')}${now.getSeconds().toString().padStart(2, '0')}`;
    link.download = `招聘数据_${dateStr}.xlsx`;

    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(url);

    ElMessage.success('Excel导出成功');
  } catch (error) {
    ElMessage.error('导出失败，请重试');
  }
};

const handleAIAnalysis = async () => {
  aiLoading.value = true;
  try {
    const filterQuery = {
      keyword: query.keyword,
      city: query.city,
      sourceSite: query.sourceSite,
      education: query.education,
      experience: query.experience,
      minSalary: query.minSalary,
      maxSalary: query.maxSalary,
      status: query.status
    };
    const res = await aiAnalysis(filterQuery);
    aiResult.value = res || aiResult.value;
    showAIAnalysis.value = true;
  } catch (error) {
    ElMessage.error('AI分析失败');
  } finally {
    aiLoading.value = false;
  }
};

onMounted(() => {
  loadData();
});

const getStatusType = (status: string) => {
  const map: Record<string, string> = { 'NEW': 'warning', 'ACTIVE': 'success', 'OFFLINE': 'info' };
  return map[status] || 'info';
};

const getStatusText = (status: string) => {
  const map: Record<string, string> = { 'NEW': '新增', 'ACTIVE': '在岗', 'OFFLINE': '已下架' };
  return map[status] || status;
};

const getSourceType = (source: string) => {
  const map: Record<string, string> = { 'BOSS直聘': 'danger', '智联招聘': 'primary', '前程无忧': 'success', '猎聘': 'warning' };
  return map[source] || 'info';
};

const getSkillLevelType = (level: string) => {
  const map: Record<string, string> = { '非常热门': 'danger', '热门': 'warning', '一般': 'info' };
  return map[level] || 'info';
};

const getSkillTagType = (index: number | string) => {
  const types = ['', 'success', 'warning', 'danger'];
  return types[Number(index) % 4];
};

const formatSalary = (min?: number | string, max?: number | string) => {
  if (!min && !max) return '面议';
  const minNum = Number(min) || 0;
  const maxNum = Number(max) || 0;
  if (minNum && maxNum) return `${minNum}-${maxNum}`;
  if (minNum) return `${minNum}+`;
  if (maxNum) return `${maxNum}以下`;
  return '面议';
};

const formatDate = (date: string) => {
  if (!date) return '未知';
  return date.substring(0, 10);
};

const handleJobClick = (job: any) => {
  if (job.url) {
    window.open(job.url, '_blank');
  } else {
    ElMessage.warning('该岗位暂无详情链接');
  }
};

const truncateDesc = (desc: string) => {
  if (!desc) return '';
  return desc.length > 100 ? desc.substring(0, 100) + '...' : desc;
};
</script>

<style scoped>
.job-list-container {
  padding: 16px;
}

/* 筛选卡片 */
.filter-card {
  border: none;
  border-radius: 16px;
  background: #fff;
  margin-bottom: 20px;
}

.filter-card :deep(.el-card__body) {
  padding: 20px;
}

.filter-section {
  padding: 4px 0;
}

.filter-row {
  margin-bottom: 20px;
}

.filter-row:last-child {
  margin-bottom: 0;
}

.salary-range {
  display: flex;
  align-items: center;
}

.salary-separator {
  margin: 0 8px;
  color: #909399;
  font-weight: 600;
}

.action-btn {
  font-weight: 600;
}

/* 结果信息 */
.result-info-row {
  margin-bottom: 16px;
  align-items: center;
}

.result-info {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #606266;
}

.result-info .el-icon {
  color: #409eff;
  font-size: 16px;
}

.result-info .highlight {
  color: #409eff;
  font-weight: 700;
  font-size: 18px;
}

.result-actions {
  display: flex;
  justify-content: flex-end;
}

/* 岗位卡片网格 */
.job-grid {
  min-height: 300px;
}

.job-col {
  margin-bottom: 20px;
}

.job-card {
  height: 100%;
  border: none;
  border-radius: 16px;
  background: #fff;
  transition: all 0.4s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
}

.job-card:hover {
  transform: translateY(-8px);
  box-shadow: 0 16px 40px rgba(64, 158, 255, 0.15) !important;
}

.job-card :deep(.el-card__body) {
  padding: 20px;
}

.job-card-header {
  margin-bottom: 12px;
}

.job-title-wrapper {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 8px;
}

.job-title {
  font-size: 16px;
  font-weight: 700;
  color: #303133;
  line-height: 1.4;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
}

.job-company {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #909399;
  margin-bottom: 12px;
}

.company-name {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.job-salary {
  margin-bottom: 14px;
}

.salary-wrapper {
  display: flex;
  align-items: baseline;
}

.salary-value {
  font-size: 24px;
  font-weight: 800;
  background: linear-gradient(135deg, #f56c6c 0%, #e6a23c 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.salary-unit {
  font-size: 12px;
  color: #c0c4cc;
  margin-left: 4px;
}

.job-info-row {
  display: flex;
  gap: 16px;
  margin-bottom: 14px;
  flex-wrap: wrap;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #606266;
  background: #f5f7fa;
  padding: 4px 10px;
  border-radius: 6px;
}

.info-item .el-icon {
  color: #909399;
}

.job-desc {
  margin-bottom: 14px;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
}

.desc-title {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #606266;
  margin-bottom: 8px;
}

.desc-content {
  font-size: 12px;
  line-height: 1.4;
  color: #606266;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

.job-skills {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 16px;
}

.skill-tag {
  font-size: 12px;
  border-radius: 4px;
}

.job-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-top: 14px;
  border-top: 1px dashed #ebeef5;
}

.publish-time {
  font-size: 12px;
  color: #c0c4cc;
}

/* 分页 */
.pagination-container {
  display: flex;
  justify-content: center;
  margin-top: 30px;
  padding: 20px 0;
}

/* AI分析弹窗 */
.ai-dialog :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  padding: 20px 24px;
  margin: 0;
}

.ai-dialog :deep(.el-dialog__title) {
  color: #fff;
  font-size: 18px;
  font-weight: 600;
}

.ai-dialog :deep(.el-dialog__headerbtn .el-dialog__close) {
  color: #fff;
}

.ai-analysis-content {
  max-height: 75vh;
  overflow-y: auto;
  padding: 8px;
}

.analysis-summary {
  margin-bottom: 20px;
  border-radius: 12px;
}

.analysis-section {
  margin-bottom: 24px;
}

.section-header {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 16px;
  padding-left: 12px;
  border-left: 4px solid #409eff;
}

.section-header .el-icon {
  font-size: 18px;
  color: #409eff;
}

.analysis-table {
  border-radius: 12px;
}

.skill-card {
  text-align: center;
  padding: 16px;
  border-radius: 12px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}

.skill-card:hover {
  transform: translateY(-4px);
}

.skill-rank {
  position: absolute;
  top: 8px;
  right: 8px;
  width: 24px;
  height: 24px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: #fff;
  border-radius: 50%;
  font-size: 12px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}

.skill-rank-1 { background: linear-gradient(135deg, #fa709a 0%, #fee140 100%); }
.skill-rank-2 { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
.skill-rank-3 { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }

.skill-name {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin: 8px 0 4px;
}

.skill-count {
  font-size: 13px;
  color: #909399;
  margin-bottom: 8px;
}

.trend-desc, .salary-desc {
  border-radius: 12px;
}

.salary-highlight {
  font-size: 15px;
  font-weight: 600;
  color: #f56c6c;
}

.suggestions-card {
  border-radius: 12px;
}

.suggestions-card :deep(.el-card__body) {
  padding: 20px;
}

/* ===== 毕业设计风格增强样式 ===== */
.job-list-container :deep(.el-row) {
  margin-left: 0 !important;
  margin-right: 0 !important;
}

.job-list-container :deep(.el-col) {
  padding-left: 10px !important;
  padding-right: 10px !important;
}

/* 筛选卡片增强 */
.job-list-container :deep(.filter-card) {
  border: none !important;
  border-radius: 16px !important;
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fc 100%) !important;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.06) !important;
}

.job-list-container :deep(.filter-card .el-card__body) {
  padding: 24px !important;
}

/* 输入框增强 */
.job-list-container :deep(.el-input__wrapper) {
  border-radius: 10px !important;
  box-shadow: 0 0 0 1px #dcdfe6 inset !important;
  transition: all 0.3s ease !important;
}

.job-list-container :deep(.el-input__wrapper:hover) {
  box-shadow: 0 0 0 1px #c0c4cc inset !important;
}

.job-list-container :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 3px rgba(102, 126, 234, 0.2) !important;
  border-color: #667eea !important;
}

/* 选择器增强 */
.job-list-container :deep(.el-select) {
  --el-select-input-focus-border-color: #667eea !important;
}

.job-list-container :deep(.el-select .el-input__wrapper) {
  border-radius: 10px !important;
}

.job-list-container :deep(.el-select-dropdown) {
  border-radius: 12px !important;
  border: 1px solid #f0f0f0 !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12) !important;
}

.job-list-container :deep(.el-select-dropdown .el-select-dropdown__item) {
  border-radius: 8px !important;
  margin: 4px 8px !important;
}

.job-list-container :deep(.el-select-dropdown .el-select-dropdown__item.hover),
.job-list-container :deep(.el-select-dropdown .el-select-dropdown__item:hover) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.1), rgba(118, 75, 162, 0.1)) !important;
}

.job-list-container :deep(.el-select-dropdown .el-select-dropdown__item.selected) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: #fff !important;
}

/* 按钮增强 */
.job-list-container :deep(.el-button--primary) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  border: none !important;
  border-radius: 10px !important;
  font-weight: 500 !important;
}

.job-list-container :deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #5a6fd6 0%, #6a4190 100%) !important;
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(102, 126, 234, 0.4) !important;
}

.job-list-container :deep(.el-button--success) {
  background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%) !important;
  border: none !important;
  border-radius: 10px !important;
}

.job-list-container :deep(.el-button--success:hover) {
  transform: translateY(-2px);
  box-shadow: 0 6px 20px rgba(67, 233, 123, 0.4) !important;
}

.job-list-container :deep(.el-button--info) {
  background: linear-gradient(135deg, #909399 0%, #767d88 100%) !important;
  border: none !important;
  border-radius: 10px !important;
}

.job-list-container :deep(.el-button) {
  border-radius: 10px !important;
  transition: all 0.3s ease !important;
}

/* 表格增强 */
.job-list-container :deep(.el-table) {
  border-radius: 16px !important;
  overflow: hidden !important;
  border: 1px solid #f0f0f0 !important;
}

.job-list-container :deep(.el-table th.el-table__cell) {
  background: linear-gradient(135deg, #f8f9fc 0%, #f4f6f9 100%) !important;
  color: #606266 !important;
  font-weight: 600 !important;
  font-size: 14px !important;
}

.job-list-container :deep(.el-table td.el-table__cell) {
  font-size: 14px !important;
  color: #606266 !important;
}

.job-list-container :deep(.el-table tr) {
  transition: all 0.3s ease !important;
}

.job-list-container :deep(.el-table--enable-row-hover .el-table__body tr:hover > td) {
  background: linear-gradient(135deg, rgba(102, 126, 234, 0.05), rgba(118, 75, 162, 0.05)) !important;
}

.job-list-container :deep(.el-table .el-table__header-wrapper th) {
  border-bottom: 2px solid #e4e7ed !important;
}

/* 分页增强 */
.job-list-container :deep(.el-pagination) {
  margin-top: 24px !important;
  justify-content: center !important;
}

.job-list-container :deep(.el-pagination .el-pager li) {
  border-radius: 8px !important;
  margin: 0 4px !important;
  transition: all 0.3s ease !important;
}

.job-list-container :deep(.el-pagination .el-pager li:hover) {
  color: #667eea !important;
}

.job-list-container :deep(.el-pagination .el-pager li.is-active) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%) !important;
  color: #fff !important;
}

.job-list-container :deep(.el-pagination .btn-prev),
.job-list-container :deep(.el-pagination .btn-next) {
  border-radius: 8px !important;
}

.job-list-container :deep(.el-pagination__total) {
  color: #909399 !important;
}

/* 标签增强 */
.job-list-container :deep(.el-tag) {
  border-radius: 20px !important;
  border: none !important;
  padding: 0 12px !important;
  font-weight: 500 !important;
  font-size: 12px !important;
}

.job-list-container :deep(.el-tag--primary) {
  background: linear-gradient(135deg, #ecf5ff 0%, #d9ecff 100%) !important;
  color: #409eff !important;
}

.job-list-container :deep(.el-tag--success) {
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%) !important;
  color: #67c23a !important;
}

.job-list-container :deep(.el-tag--warning) {
  background: linear-gradient(135deg, #fdf6ec 0%, #faecd8 100%) !important;
  color: #e6a23c !important;
}

.job-list-container :deep(.el-tag--info) {
  background: linear-gradient(135deg, #f4f4f5 0%, #e9e9eb 100%) !important;
  color: #909399 !important;
}

/* 详情卡片增强 */
.job-list-container :deep(.detail-card) {
  border: none !important;
  border-radius: 16px !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08) !important;
}

.job-list-container :deep(.detail-card .el-card__header) {
  background: linear-gradient(135deg, #fafbfc 0%, #f4f6f9 100%) !important;
  border-bottom: 1px solid #f0f0f0 !important;
  border-radius: 16px 16px 0 0 !important;
  padding: 16px 20px !important;
}

.job-list-container :deep(.detail-card .el-card__body) {
  padding: 20px !important;
}

/* 建议卡片增强 */
.job-list-container :deep(.suggestions-card) {
  border: none !important;
  border-radius: 16px !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.08) !important;
  background: linear-gradient(135deg, #ffffff 0%, #fafbfc 100%) !important;
}

/* 对话框增强 */
.job-list-container :deep(.el-dialog) {
  border-radius: 20px !important;
  overflow: hidden !important;
}

.job-list-container :deep(.el-dialog__header) {
  background: linear-gradient(135deg, #fafbfc 0%, #f4f6f9 100%) !important;
  padding: 20px !important;
  border-bottom: 1px solid #f0f0f0 !important;
}

.job-list-container :deep(.el-dialog__title) {
  font-size: 18px !important;
  font-weight: 600 !important;
  color: #303133 !important;
}

.job-list-container :deep(.el-dialog__body) {
  padding: 24px !important;
}

.job-list-container :deep(.el-dialog__footer) {
  padding: 16px 20px !important;
  border-top: 1px solid #f0f0f0 !important;
}

/* 消息提示增强 */
.job-list-container :deep(.el-message) {
  border-radius: 12px !important;
  border: none !important;
  box-shadow: 0 8px 30px rgba(0, 0, 0, 0.12) !important;
}

.job-list-container :deep(.el-message--success) {
  background: linear-gradient(135deg, #f0f9eb 0%, #e1f3d8 100%) !important;
  border-left: 4px solid #67c23a !important;
}

.job-list-container :deep(.el-message--error) {
  background: linear-gradient(135deg, #fef0f0 0%, #ffe1e1 100%) !important;
  border-left: 4px solid #f56c6c !important;
}

/* 空状态增强 */
.job-list-container :deep(.el-empty__description) {
  color: #909399 !important;
  font-size: 14px !important;
}

/* 加载状态增强 */
.job-list-container :deep(.el-loading-mask) {
  background: rgba(255, 255, 255, 0.9) !important;
  border-radius: 16px !important;
}

.job-list-container :deep(.el-loading-spinner .circular) {
  stroke: #667eea !important;
}

.job-list-container :deep(.el-loading-spinner .el-loading-text) {
  color: #667eea !important;
}
</style>