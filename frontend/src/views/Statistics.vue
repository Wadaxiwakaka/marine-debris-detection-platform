<template>
  <div class="statistics-container">
    <n-card class="stats-card">
      <template #header>
        <div>数据统计</div>
      </template>
      <div class="stats-section">
        <div class="stats-grid">
          <n-card class="chart-card">
            <template #header>
              <div>垃圾类型分布</div>
            </template>
            <div class="card-body">
              <div v-if="garbageStore.statistics.length === 0">
                <n-empty description="暂无数据" />
              </div>
              <div v-else ref="pieChartRef" class="echarts-box"></div>
            </div>
          </n-card>

          <n-card class="chart-card">
            <template #header>
              <div>时间趋势分析</div>
            </template>
            <div class="card-body">
              <div v-if="garbageStore.timeRangeStatistics.length === 0">
                <n-empty description="暂无数据" />
              </div>
              <div v-else ref="lineChartRef" class="echarts-box"></div>
            </div>
          </n-card>
        </div>

        <div class="time-range-selector">
          <h3>选择时间范围：</h3>
          <n-date-picker v-model:value="timeRange.start" type="date" placeholder="开始日期" />
          <span class="separator">至</span>
          <n-date-picker v-model:value="timeRange.end" type="date" placeholder="结束日期" />
          <n-button type="primary" @click="fetchTimeRangeStatistics">查询</n-button>
          <n-button @click="clearDateRange">清空</n-button>
        </div>
      </div>
    </n-card>
  </div>
</template>

<script setup lang="ts">
/**
 * Statistics.vue - 数据统计页面组件
 * 
 * 此组件提供垃圾识别数据的可视化统计分析：
 * 1. 垃圾类型分布饼图
 * 2. 时间趋势分析折线图
 * 3. 时间范围筛选功能
 * 
 * 使用 ECharts 图表库进行数据可视化展示。
 */

import { ref, onMounted, watch, nextTick } from 'vue'
import { useGarbageStore } from '../stores/garbageStore'
import * as echarts from 'echarts'

const garbageStore = useGarbageStore()

const pieChartRef = ref<HTMLDivElement | null>(null)
const lineChartRef = ref<HTMLDivElement | null>(null)
let pieChartInstance: echarts.ECharts | null = null
let lineChartInstance: echarts.ECharts | null = null

const timeRange = ref({
  start: null as number | null,
  end: null as number | null
})

const renderPieChart = () => {
  nextTick(() => {
    if (!pieChartRef.value) return
    if (!pieChartInstance) {
      pieChartInstance = echarts.init(pieChartRef.value)
    }
    const data = garbageStore.statistics.map((item: any) => ({
      value: item.value ?? item.count ?? 0,
      name: item.name ?? item.typeName ?? `类型${item.typeId ?? ''}`
    }))
    pieChartInstance.setOption({
      tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
      legend: { orient: 'vertical', left: 'left' },
      series: [{
        type: 'pie',
        radius: '60%',
        data,
        emphasis: {
          itemStyle: { shadowBlur: 10, shadowOffsetX: 0, shadowColor: 'rgba(0,0,0,0.5)' }
        }
      }]
    })
  })
}

const renderLineChart = () => {
  nextTick(() => {
    if (!lineChartRef.value) return
    if (!lineChartInstance) {
      lineChartInstance = echarts.init(lineChartRef.value)
    }
    const dates = garbageStore.timeRangeStatistics.map((item: any) =>
      item.date ?? item.startTime ?? item.timestamp ?? ''
    )
    const counts = garbageStore.timeRangeStatistics.map((item: any) =>
      item.count ?? item.value ?? 0
    )
    lineChartInstance.setOption({
      tooltip: { trigger: 'axis' },
      xAxis: { type: 'category', data: dates, axisLabel: { rotate: 30 } },
      yAxis: { type: 'value', name: '数量' },
      series: [{
        type: 'line',
        data: counts,
        smooth: true,
        areaStyle: { opacity: 0.2 },
        lineStyle: { color: '#18a058' },
        itemStyle: { color: '#18a058' }
      }]
    })
  })
}

watch(() => garbageStore.statistics, (val) => {
  if (val && val.length > 0) renderPieChart()
}, { deep: true })

watch(() => garbageStore.timeRangeStatistics, (val) => {
  if (val && val.length > 0) renderLineChart()
}, { deep: true })

const fetchTimeRangeStatistics = () => {
  let startTime = ''
  let endTime = ''
  if (timeRange.value.start && timeRange.value.end) {
    const formatDate = (ts: number) => {
      const d = new Date(ts)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}T00:00:00`
    }
    const formatDateEnd = (ts: number) => {
      const d = new Date(ts)
      const year = d.getFullYear()
      const month = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${year}-${month}-${day}T23:59:59`
    }
    startTime = formatDate(timeRange.value.start)
    endTime = formatDateEnd(timeRange.value.end)
  }
  // 同时更新两个图表的数据
  garbageStore.fetchStatisticsByTimeRange(startTime, endTime)
  garbageStore.fetchStatisticsByType(startTime, endTime)
}

const clearDateRange = () => {
  timeRange.value.start = null
  timeRange.value.end = null
  // 同时更新两个图表的数据
  garbageStore.fetchStatisticsByTimeRange('', '')
  garbageStore.fetchStatisticsByType('', '')
}

onMounted(async () => {
  await garbageStore.fetchStatisticsByType('', '')
  if (garbageStore.statistics.length > 0) renderPieChart()

  // 初始加载所有数据
  fetchTimeRangeStatistics()
})
</script>

<style scoped>
.statistics-container {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-section {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.stats-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 20px;
}

.chart-card {
  height: 420px;
}

.card-body {
  height: 340px;
  display: flex;
  align-items: center;
  justify-content: center;
}

.echarts-box {
  width: 100%;
  height: 340px;
}

.time-range-selector {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-top: 20px;
}

.separator {
  margin: 0 10px;
}

@media (max-width: 768px) {
  .stats-grid {
    grid-template-columns: 1fr;
  }
}
</style>
