<template>
  <div>
    <el-card class="page-card" style="max-width: 640px">
      <template #header><b>AI 设置</b></template>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px"
        title="AI 分析在浏览器本地直连大模型完成, 不经过后端。配置保存在本机浏览器中。" />
      <el-form :model="form" label-width="110px">
        <el-form-item label="接口地址">
          <el-input v-model="form.baseUrl" placeholder="https://api.deepseek.com" />
        </el-form-item>
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password placeholder="在 platform.deepseek.com 申请" />
        </el-form-item>
        <el-form-item label="模型">
          <el-input v-model="form.model" placeholder="deepseek-chat" />
        </el-form-item>
        <el-form-item label="开发模式代理">
          <el-switch v-model="form.useProxy" />
          <span class="tip">npm run dev 时经 Vite 代理转发, 规避浏览器跨域限制</span>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="save">保存配置</el-button>
          <el-button :loading="testing" @click="test">测试连接</el-button>
          <el-button type="danger" link @click="clear">清空 AI 历史</el-button>
        </el-form-item>
      </el-form>
      <el-divider>最近 AI 记录(本地保存, 最多 50 条)</el-divider>
      <el-empty v-if="!history.length" description="暂无记录" :image-size="60" />
      <div v-for="(h, i) in history" :key="i" class="hist-item">
        <div class="hist-head">
          <el-tag size="small" type="primary">{{ h.type === 'report' ? '学情报告' : '题目分析' }}</el-tag>
          <span class="hist-title">{{ h.title }}</span>
          <span class="hist-time">{{ formatTime(h.time) }}</span>
        </div>
        <div class="hist-body">{{ h.content }}</div>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getAiConfig, saveAiConfig, testConnection, getAiHistory, clearAiHistory } from '../utils/ai'

const form = reactive({ baseUrl: '', apiKey: '', model: '', useProxy: true })
const saving = ref(false)
const testing = ref(false)
const history = ref([])

function formatTime (iso) {
  return iso ? iso.replace('T', ' ').substring(0, 19) : ''
}

function load () {
  const cfg = getAiConfig()
  form.baseUrl = cfg.baseUrl
  form.apiKey = cfg.apiKey
  form.model = cfg.model
  form.useProxy = cfg.useProxy !== false
  history.value = getAiHistory()
}

async function save () {
  saving.value = true
  try {
    saveAiConfig({ ...form })
    ElMessage.success('配置已保存到本机')
  } finally {
    saving.value = false
  }
}

async function test () {
  testing.value = true
  try {
    saveAiConfig({ ...form })
    const reply = await testConnection()
    ElMessage.success('连接成功, 模型回复: ' + reply)
  } catch (e) {
    ElMessage.error(e.message || '连接失败')
  } finally {
    testing.value = false
  }
}

function clear () {
  clearAiHistory()
  history.value = []
  ElMessage.success('已清空')
}

onMounted(load)
</script>

<style scoped>
.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.hist-item {
  padding: 10px 0;
  border-bottom: 1px solid #ebeef5;
}

.hist-head {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.hist-title {
  flex: 1;
  font-size: 13px;
  color: #303133;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.hist-time {
  color: #909399;
  font-size: 12px;
}

.hist-body {
  color: #606266;
  font-size: 12px;
  line-height: 1.7;
  max-height: 120px;
  overflow: auto;
  white-space: pre-wrap;
}
</style>
