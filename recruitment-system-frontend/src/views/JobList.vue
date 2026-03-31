<template>
  <el-card>
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="关键词" style="width: 200px" />
      <el-input v-model="query.city" placeholder="城市" style="width: 160px; margin-left: 8px" />
      <el-select v-model="query.status" placeholder="状态" style="width: 140px; margin-left: 8px" clearable>
        <el-option label="新增" value="NEW" />
        <el-option label="在岗" value="ACTIVE" />
        <el-option label="下架" value="OFFLINE" />
      </el-select>
      <el-button type="primary" style="margin-left: 8px" @click="loadData">查询</el-button>
      <el-button style="margin-left: 8px" @click="onRecommend">岗位推荐</el-button>
    </div>
    <el-table :data="list" style="width: 100%">
      <el-table-column prop="title" label="岗位名称" />
      <el-table-column prop="companyName" label="公司" />
      <el-table-column prop="sourceSite" label="来源" width="120" />
      <el-table-column prop="city" label="城市" width="120" />
      <el-table-column prop="jobStatus" label="状态" width="110" />
      <el-table-column prop="experience" label="经验" width="120" />
      <el-table-column prop="education" label="学历" width="120" />
      <el-table-column label="薪资" width="160">
        <template #default="scope">
          <span>{{ scope.row.minSalary }} ~ {{ scope.row.maxSalary }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="skills" label="技能" />
    </el-table>
    <div class="pager">
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next, jumper"
        @current-change="loadData"
      />
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue';
import { fetchJobPage, recommendJobs } from '@/api/job';
import { ElMessageBox } from 'element-plus';

const query = reactive({
  keyword: '',
  city: '',
  companyName: '',
  experience: '',
  education: '',
  status: '',
  pageNum: 1,
  pageSize: 10
});

const list = ref<any[]>([]);
const total = ref(0);

const loadData = async () => {
  const res = await fetchJobPage(query);
  list.value = res.list;
  total.value = res.total;
};

const onRecommend = async () => {
  const skills = await ElMessageBox.prompt('请输入你的技能（逗号分隔）', '岗位推荐', {
    confirmButtonText: '确定',
    cancelButtonText: '取消'
  }).catch(() => null);
  if (!skills || !skills.value) return;
  const data = await recommendJobs({ skills: skills.value, city: query.city });
  list.value = data;
  total.value = data.length;
};

onMounted(() => {
  loadData();
});
</script>

<style scoped>
.toolbar {
  margin-bottom: 12px;
  display: flex;
  align-items: center;
}
.pager {
  margin-top: 12px;
  display: flex;
  justify-content: flex-end;
}
</style>

