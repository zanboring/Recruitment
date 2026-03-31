<template>
  <div>
    <el-card>
      <div class="title">数据对比（新增 / 下架 / 在岗）</div>
      <div ref="statusChartRef" class="chart" />
    </el-card>

    <el-card style="margin-top: 16px">
      <el-table :data="statusList">
        <el-table-column prop="name" label="状态" />
        <el-table-column prop="count" label="数量" width="120" />
        <el-table-column prop="avgSalary" label="平均薪资" width="160" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue';
import * as echarts from 'echarts';
import { fetchJobStatStatus } from '@/api/job';

const statusChartRef = ref<HTMLDivElement | null>(null);
const statusList = ref<any[]>([]);

const labelMap: Record<string, string> = {
  NEW: '新增岗位',
  ACTIVE: '正常在岗',
  OFFLINE: '已下架'
};

const init = async () => {
  const raw = (await fetchJobStatStatus()) as any[];
  statusList.value = raw.map((item) => ({ ...item, name: labelMap[item.name] || item.name }));
  if (!statusChartRef.value) return;
  const chart = echarts.init(statusChartRef.value);
  chart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        type: 'pie',
        radius: ['38%', '70%'],
        data: statusList.value.map((i: any) => ({ name: i.name, value: i.count || 0 }))
      }
    ]
  });
};

onMounted(() => {
  init().catch(() => undefined);
});
</script>

<style scoped>
.title {
  font-weight: 600;
}
.chart {
  height: 360px;
}
</style>
