<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <div class="title">岗位城市分布</div>
          <div ref="cityChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="title">企业岗位数量</div>
          <div ref="companyChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <div class="title">技能需求统计</div>
          <div ref="skillChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="title">薪资预测（示例）</div>
          <el-form :model="form" label-width="70px" :inline="true">
            <el-form-item label="城市">
              <el-input v-model="form.city" placeholder="如：北京" style="width: 140px" />
            </el-form-item>
            <el-form-item label="经验">
              <el-input v-model="form.experience" placeholder="如：1-3年" style="width: 140px" />
            </el-form-item>
            <el-form-item label="学历">
              <el-input v-model="form.education" placeholder="如：本科" style="width: 140px" />
            </el-form-item>
            <el-form-item label="技能">
              <el-input v-model="form.skills" placeholder="如：Java,Spring" style="width: 180px" />
            </el-form-item>
            <el-form-item>
              <el-button type="primary" @click="onPredict">预测</el-button>
            </el-form-item>
          </el-form>
          <div class="predict-result">
            预测薪资：<span class="amount">{{ salaryText }}</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue';
import * as echarts from 'echarts';
import { fetchJobStatCity, fetchJobStatCompany, fetchJobStatSkill, predictSalary } from '@/api/job';

const cityChartRef = ref<HTMLDivElement | null>(null);
const companyChartRef = ref<HTMLDivElement | null>(null);
const skillChartRef = ref<HTMLDivElement | null>(null);

const form = reactive({
  city: '',
  experience: '',
  education: '',
  skills: ''
});

const salaryText = ref<string>('—');

const init = async () => {
  const [cityData, companyData, skillData] = await Promise.all([
    fetchJobStatCity(),
    fetchJobStatCompany(),
    fetchJobStatSkill()
  ]);

  if (cityChartRef.value) {
    const chart = echarts.init(cityChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: cityData.map((i: any) => i.name) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: cityData.map((i: any) => i.count) }]
    });
  }

  if (companyChartRef.value) {
    const chart = echarts.init(companyChartRef.value);
    chart.setOption({
      tooltip: {},
      legend: { data: ['岗位数量'] },
      xAxis: { type: 'category', data: companyData.map((i: any) => i.name).slice(0, 10) },
      yAxis: { type: 'value' },
      series: [
        {
          name: '岗位数量',
          type: 'bar',
          data: companyData.map((i: any) => i.count).slice(0, 10)
        }
      ]
    });
  }

  if (skillChartRef.value) {
    const chart = echarts.init(skillChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: skillData.map((i: any) => i.name).slice(0, 12) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: skillData.map((i: any) => i.count).slice(0, 12) }]
    });
  }
};

const onPredict = async () => {
  const result = await predictSalary({
    city: form.city || undefined,
    experience: form.experience || undefined,
    education: form.education || undefined,
    skills: form.skills || undefined
  });
  const val = result == null ? 0 : Number(result);
  salaryText.value = `${val.toFixed(2)} 元`;
};

onMounted(() => {
  init().catch(() => {
    // 页面不因可视化接口失败中断
    salaryText.value = '—';
  });
});
</script>

<style scoped>
.dashboard {
  width: 100%;
}
.title {
  font-weight: 600;
  margin-bottom: 8px;
}
.chart {
  height: 320px;
}
.predict-result {
  margin-top: 16px;
  font-size: 16px;
}
.amount {
  color: #409eff;
  font-weight: 700;
}
</style>

