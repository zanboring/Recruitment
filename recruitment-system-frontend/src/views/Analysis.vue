<template>
  <div>
    <el-card>
      <div class="title">数据分析结果</div>
      <p class="summary">{{ summary }}</p>
    </el-card>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <div class="sub-title">热门岗位技能排行</div>
          <div ref="skillChartRef" class="chart" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <div class="sub-title">城市岗位需求分布</div>
          <div ref="cityChartRef" class="chart" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import { fetchAnalysisSummary, fetchJobStatCity, fetchJobStatSkill } from '@/api/job';

const summary = ref('正在分析...');
const skillChartRef = ref<HTMLDivElement | null>(null);
const cityChartRef = ref<HTMLDivElement | null>(null);

const init = async () => {
  const [summaryText, cityData, skillData] = await Promise.all([
    fetchAnalysisSummary(),
    fetchJobStatCity(),
    fetchJobStatSkill()
  ]);
  summary.value = (summaryText as string) || '暂无数据';

  if (skillChartRef.value) {
    const chart = echarts.init(skillChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: (skillData as any[]).map((i: any) => i.name).slice(0, 12) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: (skillData as any[]).map((i: any) => i.count).slice(0, 12) }]
    });
  }
  if (cityChartRef.value) {
    const chart = echarts.init(cityChartRef.value);
    chart.setOption({
      tooltip: {},
      xAxis: { type: 'category', data: (cityData as any[]).map((i: any) => i.name).slice(0, 12) },
      yAxis: { type: 'value' },
      series: [{ type: 'bar', data: (cityData as any[]).map((i: any) => i.count).slice(0, 12) }]
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
