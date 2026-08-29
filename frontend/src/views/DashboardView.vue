<template>
  <div>
    <el-card class="page-card">
      <div class="stat-cards">
        <el-card class="stat-card"><div class="num">{{ overview.questionCount || 0 }}</div><div class="label">题目总数</div></el-card>
        <el-card class="stat-card"><div class="num">{{ overview.categoryCount || 0 }}</div><div class="label">分类数量</div></el-card>
        <el-card class="stat-card"><div class="num">{{ overview.favoriteCount || 0 }}</div><div class="label">收藏题目</div></el-card>
        <el-card class="stat-card"><div class="num" style="color:#f56c6c">{{ overview.wrongCount || 0 }}</div><div class="label">未掌握错题</div></el-card>
        <el-card class="stat-card"><div class="num">{{ overview.practiceCount || 0 }}</div><div class="label">练习次数</div></el-card>
        <el-card class="stat-card"><div class="num" style="color:#67c23a">{{ overview.accuracy || 0 }}%</div><div class="label">总正确率</div></el-card>
      </div>
      <div class="filter-bar">
        <el-button type="primary" :loading="reportLoading" @click="genReport">
          <el-icon><MagicStick /></el-icon>&nbsp;AI 学情报告
        </el-button>
        <span v-if="reportModel" class="report-model">来源: {{ reportModel }}</span>
      </div>
    </el-card>
    <div class="chart-grid">
      <el-card class="page-card"><div class="chart-box" ref="typeChartRef"></div></el-card>
      <el-card class="page-card"><div class="chart-box" ref="difficultyChartRef"></div></el-card>
      <el-card class="page-card"><div class="chart-box" ref="trendChartRef" style="grid-column: 1 / -1; height: 340px"></div></el-card>
      <el-card class="page-card"><div class="chart-box" ref="wrongChartRef"></div></el-card>
      <el-card class="page-card"><div class="chart-box" ref="categoryChartRef"></div></el-card>
    </div>

    <el-dialog v-model="reportVisible" title="AI 学情报告" width="680px">
      <div v-loading="reportLoading" class="pre-wrap report-content">{{ reportContent }}</div>
      <template #footer>
        <el-button @click="reportVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { ElMessage } from 'element-plus'
import { useRouter } from 'vue-router'
import * as echarts from 'echarts'
import { statsApi } from '../api'
import { aiChat, buildReportPrompt, pushAiHistory, hasApiKey, getAiConfig } from '../utils/ai'

const overview = reactive({})
const typeChartRef = ref()
const difficultyChartRef = ref()
const trendChartRef = ref()
const wrongChartRef = ref()
const categoryChartRef = ref()

const reportVisible = ref(false)
const reportLoading = ref(false)
const reportContent = ref('')
const reportModel = ref('')

let charts = []
const router = useRouter()

function renderBar (el, title, data) {
  const chart = echarts.init(el)
  chart.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    grid: { top: 50, bottom: 30, left: 40, right: 20 },
    xAxis: { type: 'category', data: data.map(d => d.name) },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{ type: 'bar', data: data.map(d => d.value), barMaxWidth: 40, itemStyle: { color: '#409eff' } }]
  })
  return chart
}

function renderPie (el, title, data) {
  const chart = echarts.init(el)
  chart.setOption({
    title: { text: title, left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'item', formatter: '{b}: {c} ({d}%)' },
    legend: { bottom: 0, type: 'scroll' },
    series: [{ type: 'pie', radius: ['38%', '62%'], center: ['50%', '52%'], data }]
  })
  return chart
}

function renderTrend (el, data) {
  const chart = echarts.init(el)
  chart.setOption({
    title: { text: '近14天练习趋势', left: 'center', textStyle: { fontSize: 14 } },
    tooltip: { trigger: 'axis' },
    legend: { top: 26 },
    grid: { top: 60, bottom: 30, left: 45, right: 45 },
    xAxis: { type: 'category', data: data.map(d => d.date.slice(5)) },
    yAxis: [
      { type: 'value', name: '题数', minInterval: 1 },
      { type: 'value', name: '正确率%', max: 100 }
    ],
    series: [
      { name: '练习题数', type: 'bar', data: data.map(d => d.total), itemStyle: { color: '#409eff' } },
      { name: '正确率%', type: 'line', yAxisIndex: 1, data: data.map(d => d.accuracy), smooth: true, itemStyle: { color: '#67c23a' } }
    ]
  })
  return chart
}

async function loadAll () {
  try {
    const ov = await statsApi.overview()
    Object.assign(overview, ov)
    const [typeData, diffData, trendData, wrongData, categoryData] = await Promise.all([
      statsApi.byType(), statsApi.byDifficulty(), statsApi.trend(),
      statsApi.wrongByCategory(), statsApi.byCategory()
    ])
    await nextTick()
    charts = [
      renderPie(typeChartRef.value, '题型分布', typeData),
      renderBar(difficultyChartRef.value, '难度分布', diffData),
      renderTrend(trendChartRef.value, trendData),
      renderBar(wrongChartRef.value, '易错分类 Top5', wrongData),
      renderPie(categoryChartRef.value, '分类题目占比', categoryData)
    ]
  } catch (e) { /* 拦截器已提示 */ }
}

async function genReport () {
  if (!hasApiKey()) {
    ElMessage.warning('尚未配置 AI, 请先到「AI 设置」填写 API Key')
    router.push('/ai-settings')
    return
  }
  reportVisible.value = true
  reportLoading.value = true
  reportContent.value = ''
  try {
    const [overview, wrongByCategory, trend] = await Promise.all([
      statsApi.overview(), statsApi.wrongByCategory(), statsApi.trend()
    ])
    const prompt = buildReportPrompt({ ...overview, wrongByCategory, recentTrend: trend })
    const content = await aiChat(prompt)
    reportContent.value = content
    reportModel.value = getAiConfig().model + ' (本地直连)'
    pushAiHistory({ type: 'report', title: '学情报告', content })
  } catch (e) {
    ElMessage.error(e.message || 'AI 调用失败')
    reportVisible.value = false
  } finally {
    reportLoading.value = false
  }
}

function onResize () {
  charts.forEach(c => c && c.resize())
}

onMounted(() => {
  loadAll()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  window.removeEventListener('resize', onResize)
  charts.forEach(c => c && c.dispose())
})
</script>

<style scoped>
.report-model {
  color: #909399;
  font-size: 12px;
}

.report-content {
  max-height: 420px;
  overflow: auto;
}
</style>
