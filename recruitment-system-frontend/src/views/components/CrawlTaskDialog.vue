<template>
  <el-dialog
    v-model="visible"
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
            <el-checkbox v-for="city in cityOptions" :key="city.value" :value="city.value" style="margin-right: 15px; margin-bottom: 10px;">{{ city.label }}</el-checkbox>
          </el-checkbox-group>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" @click="createCrawlTask" :loading="creatingTask">
        创建任务
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue';
import { ElMessage } from 'element-plus';
import http from '@/api/http';

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

// Props & Emits
const visible = defineModel<boolean>('visible', { default: false });
defineProps<{
  creatingTask: boolean;
}>();

const emit = defineEmits<{
  (e: 'created'): void;
}>();

const crawlForm = reactive({
  sourceSite: '',
  keyword: '',
  cities: [] as string[]
});

const submitLoading = ref(false);

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
  if (submitLoading.value) return;
  
  if (!crawlForm.sourceSite || !crawlForm.keyword || crawlForm.cities.length === 0) {
    ElMessage.warning('请填写完整的任务信息');
    return;
  }

  submitLoading.value = true;
  try {
    await http.post('/crawl/task', {
      ...crawlForm,
      city: crawlForm.cities.join(',')
    });
    ElMessage.success('任务创建成功');
    visible.value = false;
    Object.assign(crawlForm, { sourceSite: '', keyword: '', cities: [] });
    emit('created');
  } catch (error: unknown) {
    ElMessage.error('任务创建失败');
  } finally {
    submitLoading.value = false;
  }
};
</script>

<style scoped>
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
