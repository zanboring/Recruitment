<template>
  <div>
    <el-card>
      <div class="title">数据分析结果</div>
      <p class="summary">{{ summary }}</p>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <div class="sub-title">热门岗位排行</div>
          <div ref="titleChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="sub-title">城市岗位需求分布</div>
          <div ref="cityChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <div class="sub-title">薪资水平统计</div>
          <div ref="salaryChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="sub-title">高频技能词</div>
          <div ref="skillChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import {
  fetchAnalysisSummary,
  fetchJobStatCity,
  fetchJobStatSalaryRange,
  fetchJobStatSkill,
  fetchTopTitles
} from '@/api/job';

const summary = ref('正在分析...');
const titleChartRef = ref<HTMLDivElement | null>(null);
const skillChartRef = ref<HTMLDivElement | null>(null);
const cityChartRef = ref<HTMLDivElement | null>(null);
const salaryChartRef = ref<HTMLDivElement | null>(null);

const init = async () => {
  const [summaryText, cityData, skillData, salaryRangeData, topTitles] = await Promise.all([
    fetchAnalysisSummary(),
    fetchJobStatCity(),
    fetchJobStatSkill(),
    fetchJobStatSalaryRange(),
    fetchTopTitles()
  ]);
  summary.value = (summaryText as string) || '暂无数据';

  if (cityChartRef.value) {
    const chart = echarts.init(cityChartRef.value);
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [
        {
          type: 'pie',
          radius: ['35%', '68%'],
          data: (cityData as any[]).slice(0, 12).map((i: any) => ({
            name: i.name || '未知',
            value: i.count || 0
          }))
        }
      ]
    });
  }

  if (titleChartRef.value) {
    const chart = echarts.init(titleChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: (topTitles as any[]).map((i: any) => i.name).slice(0, 10) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: (topTitles as any[]).map((i: any) => i.count).slice(0, 10), barMaxWidth: 48 }]
    });
  }

  if (salaryChartRef.value) {
    const chart = echarts.init(salaryChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: (salaryRangeData as any[]).map((i: any) => i.name).slice(0, 6) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: (salaryRangeData as any[]).map((i: any) => i.count).slice(0, 6), barMaxWidth: 48 }]
    });
  }

  if (skillChartRef.value) {
    const chart = echarts.init(skillChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: (skillData as any[]).map((i: any) => i.name).slice(0, 12) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: (skillData as any[]).map((i: any) => i.count).slice(0, 12) }]
    });
  }
};

onMounted(() => {
  init().catch(() => {
    summary.value = '暂无可分析数据';
  });
});
</script>

<style scoped>
.title {
  font-weight: 600;
}
.sub-title {
  font-weight: 600;
  margin-bottom: 8px;
}
.summary {
  margin-top: 10px;
  line-height: 1.8;
}
.chart {
  height: 320px;
}
</style>
