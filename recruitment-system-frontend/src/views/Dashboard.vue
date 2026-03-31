<template>
  <div class="dashboard">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card>
          <div class="title">各城市岗位数量</div>
          <div ref="cityChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="title">薪资区间分布</div>
          <div ref="salaryChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="24">
        <el-card>
          <div class="title">学历 / 经验要求统计</div>
          <div ref="requirementChartRef" class="chart chart-lg" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import {
  fetchJobStatCity,
  fetchJobStatSalaryRange,
  fetchJobStatEducation,
  fetchJobStatExperience
} from '@/api/job';

const cityChartRef = ref<HTMLDivElement | null>(null);
const salaryChartRef = ref<HTMLDivElement | null>(null);
const requirementChartRef = ref<HTMLDivElement | null>(null);

const init = async () => {
  const [cityData, salaryRangeData, eduData, expData] = await Promise.all([
    fetchJobStatCity(),
    fetchJobStatSalaryRange(),
    fetchJobStatEducation(),
    fetchJobStatExperience()
  ]);

  if (cityChartRef.value) {
    const chart = echarts.init(cityChartRef.value);
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['35%', '68%'],
          data: cityData.map((i: any) => ({ name: i.name || '未知', value: i.count || 0 }))
        }
      ]
    });
  }

  if (salaryChartRef.value) {
    const chart = echarts.init(salaryChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: salaryRangeData.map((i: any) => i.name || '未知') },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: salaryRangeData.map((i: any) => i.count || 0), barMaxWidth: 48 }]
    });
  }

  if (requirementChartRef.value) {
    const chart = echarts.init(requirementChartRef.value);
    chart.setOption({
      tooltip: {},
      legend: { data: ['学历要求', '经验要求'] },
      xAxis: { type: 'category', data: buildAxis(eduData, expData) },
      yAxis: { type: 'value' },
      series: [
        { name: '学历要求', type: 'bar', data: toSeries(buildAxis(eduData, expData), eduData) },
        { name: '经验要求', type: 'bar', data: toSeries(buildAxis(eduData, expData), expData) }
      ]
    });
  }
};

const buildAxis = (a: any[], b: any[]) => {
  const set = new Set<string>();
  a.forEach((i: any) => set.add(i.name || '未知'));
  b.forEach((i: any) => set.add(i.name || '未知'));
  return Array.from(set);
};

const toSeries = (axis: string[], source: any[]) => {
  const map = new Map<string, number>();
  source.forEach((i: any) => map.set(i.name || '未知', i.count || 0));
  return axis.map((key) => map.get(key) || 0);
};

onMounted(() => {
  init().catch(() => undefined);
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
.chart-lg {
  height: 360px;
}
</style>

