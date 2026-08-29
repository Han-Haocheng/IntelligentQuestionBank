<template>
  <div>
    <el-card class="page-card" style="max-width: 680px">
      <template #header><b>AI 设置</b></template>
      <el-alert type="info" :closable="false" style="margin-bottom: 16px"
        title="AI 分析在浏览器本地直连大模型完成, 不经过后端。配置保存在本机浏览器中。" />

      <el-form :model="form" label-width="110px">
        <!-- 服务商(常用接口地址下拉 + 自定义保底) -->
        <el-form-item label="服务商">
          <el-select v-model="providerSel" style="width: 100%" placeholder="选择常用服务商或自定义">
            <el-option v-for="p in providers" :key="p.baseUrl" :value="p.baseUrl"
              :label="showFullUrl ? p.name + ' (' + p.baseUrl + ')' : p.name" />
            <el-option :value="CUSTOM_BASE_URL" label="✏️ 自定义地址" />
          </el-select>
          <div class="format-row">
            <el-tag :type="isCustom ? 'warning' : 'success'" size="small">
              API 格式: {{ apiFormat }}
            </el-tag>
            <span class="tip">{{ formatHint }}</span>
          </div>
        </el-form-item>

        <!-- 自定义地址(保底输入) -->
        <el-form-item v-if="isCustom" label="自定义地址">
          <el-input v-model="form.baseUrl" placeholder="https://example.com/v1 (需 OpenAI 兼容接口)" />
        </el-form-item>

        <!-- 完整地址 显示/隐藏 -->
        <el-form-item label="完整地址">
          <el-switch v-model="showFullUrl" />
          <span class="tip">{{ showFullUrl ? '已显示完整接口地址(下拉与下方同步展开)' : '已隐藏, 下拉仅显示服务商名称' }}</span>
          <div v-if="showFullUrl" class="full-url">{{ form.baseUrl || '(未填写)' }}</div>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" type="password" show-password
            :placeholder="needKey ? '在对应平台申请 API Key' : '本地服务(Ollama)可留空'" />
        </el-form-item>

        <el-form-item label="模型">
          <el-select v-model="form.model" filterable allow-create default-first-option
            style="width: 100%" placeholder="选择模型或直接输入模型名">
            <el-option v-for="m in models" :key="m" :value="m" :label="m" />
          </el-select>
          <div class="model-bar">
            <el-button size="small" :loading="modelLoading" @click="fetchModelsList">拉取模型列表</el-button>
            <span v-if="modelLoading" class="tip">正在从接口拉取模型…</span>
            <span v-else-if="modelError" class="tip model-err">{{ modelError }}</span>
            <span v-else-if="models.length" class="tip">已获取 {{ models.length }} 个模型, 可直接选择; 也可手动输入</span>
            <span v-else class="tip">填写地址(与 Key)后自动拉取; 拉取失败可手动输入模型名</span>
          </div>
        </el-form-item>

        <el-form-item label="开发模式代理">
          <el-switch v-model="form.useProxy" />
          <span class="tip">npm run dev 时经 Vite 代理转发(仅 DeepSeek 默认地址生效), 规避浏览器跨域限制</span>
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
import { ref, reactive, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  AI_PROVIDERS, CUSTOM_BASE_URL,
  getAiConfig, saveAiConfig, testConnection, fetchModels,
  getAiHistory, clearAiHistory
} from '../utils/ai'

const form = reactive({ baseUrl: '', apiKey: '', model: '', useProxy: true })
const saving = ref(false)
const testing = ref(false)
const history = ref([])
const providerSel = ref('')
const showFullUrl = ref(false)
const models = ref([])
const modelLoading = ref(false)
const modelError = ref('')
let fetchTimer = null

const providers = AI_PROVIDERS

const isCustom = computed(() => providerSel.value === CUSTOM_BASE_URL)

const currentProvider = computed(() =>
  providers.find(p => p.baseUrl === form.baseUrl) || null)

const apiFormat = computed(() => {
  if (isCustom.value) return 'OpenAI 兼容(自定义)'
  return (currentProvider.value && currentProvider.value.format) || 'OpenAI 兼容'
})

const formatHint = computed(() => {
  if (isCustom.value) return '自定义地址须为 OpenAI 兼容接口(/chat/completions 与 /models)'
  return currentProvider.value ? currentProvider.value.desc : ''
})

const needKey = computed(() => {
  const base = form.baseUrl || ''
  return base.indexOf('localhost') < 0 && base.indexOf('127.0.0.1') < 0
})

function formatTime (iso) {
  return iso ? iso.replace('T', ' ').substring(0, 19) : ''
}

function syncProviderFromUrl () {
  providerSel.value = providers.some(p => p.baseUrl === form.baseUrl)
    ? form.baseUrl
    : CUSTOM_BASE_URL
}

function load () {
  const cfg = getAiConfig()
  form.baseUrl = cfg.baseUrl
  form.apiKey = cfg.apiKey
  form.model = cfg.model
  form.useProxy = cfg.useProxy !== false
  history.value = getAiHistory()
  syncProviderFromUrl()
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

async function fetchModelsList () {
  const base = (form.baseUrl || '').trim()
  if (!base) {
    modelError.value = '请先填写接口地址'
    return
  }
  modelLoading.value = true
  modelError.value = ''
  try {
    const list = await fetchModels(base, form.apiKey)
    models.value = list
    if (!list.length) {
      modelError.value = '接口未返回模型列表, 可直接手动输入模型名'
    }
  } catch (e) {
    models.value = []
    modelError.value = e.message || '模型列表获取失败'
  } finally {
    modelLoading.value = false
  }
}

// 服务商切换: 选择预设则自动填入地址; 自定义保持用户输入
function onProviderChange (val) {
  if (val !== CUSTOM_BASE_URL) {
    form.baseUrl = val
  }
}

// 地址/Key 变化后防抖自动拉取模型
function scheduleFetchModels () {
  clearTimeout(fetchTimer)
  fetchTimer = setTimeout(() => {
    if ((form.baseUrl || '').trim()) {
      fetchModelsList()
    }
  }, 600)
}

watch(providerSel, onProviderChange)
watch(() => form.baseUrl, scheduleFetchModels)
watch(() => form.apiKey, scheduleFetchModels)

onMounted(() => {
  load()
  scheduleFetchModels()
})

onBeforeUnmount(() => clearTimeout(fetchTimer))
</script>

<style scoped>
.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.model-err {
  color: #f56c6c;
}

.format-row {
  display: flex;
  align-items: center;
  margin-top: 6px;
}

.full-url {
  margin-top: 8px;
  padding: 6px 10px;
  background: #f5f7fa;
  border-radius: 4px;
  font-family: monospace;
  font-size: 12px;
  color: #409eff;
  word-break: break-all;
}

.model-bar {
  display: flex;
  align-items: center;
  margin-top: 6px;
  flex-wrap: wrap;
  row-gap: 4px;
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
