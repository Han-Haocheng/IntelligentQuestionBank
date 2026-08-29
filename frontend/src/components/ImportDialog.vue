<template>
  <el-dialog v-model="visible" title="批量导入题目" width="860px" :close-on-click-modal="false" @closed="onClosed">
    <el-steps :active="step" finish-status="success" align-center style="margin-bottom: 18px">
      <el-step title="上传文件" />
      <el-step title="预览确认" />
      <el-step title="导入结果" />
    </el-steps>

    <!-- 第一步: 上传 -->
    <div v-if="step === 0">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px">
        <template #title>
          两种导入方式, 任选其一:
          <b>模板化导入</b>(.xlsx/.csv 固定列表格) 或 <b>非模板化导入</b>(.md Markdown 自然格式)
        </template>
        <template #default>
          <div class="tpl-links">
            <el-link type="primary" :underline="false" @click="downloadTemplate('xlsx')">下载 Excel 模板</el-link>
            <el-link type="primary" :underline="false" @click="downloadTemplate('csv')">下载 CSV 模板</el-link>
            <el-link type="primary" :underline="false" @click="downloadTemplate('md')">下载 Markdown 模板</el-link>
          </div>
        </template>
      </el-alert>
      <el-form label-width="90px">
        <el-form-item label="归属题库">
          <el-select v-model="bankId" placeholder="不归属(可选)" clearable style="width: 260px">
            <el-option v-for="b in banks" :key="b.id" :value="b.id" :label="b.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属分类">
          <el-select v-model="categoryId" placeholder="不归属(可选)" clearable style="width: 260px">
            <el-option v-for="c in flatCategories" :key="c.id" :value="c.id" :label="c.pathName" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload ref="uploadRef" drag accept=".xlsx,.xls,.csv,.md,.markdown" :limit="1"
            :auto-upload="false" :on-change="onFileChange" :on-exceed="onExceed">
            <div style="padding: 14px 0">
              <el-icon style="font-size: 34px; color: #909399"><UploadFilled /></el-icon>
              <div>拖拽文件到此处, 或点击选择文件</div>
              <div class="sub">模板化: .xlsx / .xls / .csv ｜ 非模板化: .md / .markdown, 单次建议不超过 500 题</div>
            </div>
          </el-upload>
          <div v-if="fileMode" style="margin-top: 8px">
            <el-tag :type="fileMode === 'md' ? 'warning' : 'primary'" size="small">
              {{ fileMode === 'md' ? '非模板化 · Markdown' : '模板化 · 表格' }}
            </el-tag>
            <span class="sub" style="margin-left: 8px">
              {{ fileMode === 'md' ? '按 Markdown 格式解析(## 题型 / ### 题号 / 答案: 等)' : '按固定列表格模板解析(第一列题干, 第二列题型)' }}
            </span>
          </div>
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :disabled="!file" :loading="parsing" @click="doParse">上传并解析</el-button>
      </div>
    </div>

    <!-- 第二步: 预览 -->
    <div v-else-if="step === 1">
      <el-alert v-if="errorCount" type="warning" :closable="false" style="margin-bottom: 12px"
        :title="'共解析 ' + rows.length + ' 行, 其中 ' + errorCount + ' 行存在问题(表格中标红)。导入时将自动跳过这些行。'" />
      <el-alert v-else type="success" :closable="false" style="margin-bottom: 12px"
        :title="'共解析 ' + rows.length + ' 行, 全部校验通过'" />

      <!-- AI 补全缺失的答案/解析 -->
      <div class="ai-box">
        <div class="ai-head">
          <span>AI 补全</span>
          <span class="ai-sub">对缺失答案或解析的行, 调用本地 AI 生成(单次最多 {{ AI_LIMIT }} 行)</span>
        </div>
        <div class="ai-body">
          <el-button type="primary" size="small" :disabled="!aiTargets.length || aiRunning"
            :loading="aiRunning" @click="aiFill">AI 补全 {{ aiTargets.length }} 行</el-button>
          <el-progress v-if="aiRunning || aiDone" :percentage="aiPercent" style="flex: 1; margin-left: 12px" />
          <span v-if="aiDone && !aiRunning" class="ai-done">已处理 {{ aiDoneCount }} 行</span>
        </div>
        <div v-if="aiFailed.length" class="ai-fail">AI 未能补全: 第 {{ aiFailed.join('、') }} 行(可手动在库中补充)</div>
      </div>
      <el-table :data="rows" max-height="380" size="small" border
        :row-class-name="(r) => r.row.errors && r.row.errors.length ? 'row-error' : ''">
        <el-table-column prop="rowNo" label="行" width="50" />
        <el-table-column prop="title" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column label="题型" width="80">
          <template #default="{ row }">{{ typeNames[(row.type || 1) - 1] || row.typeName }}</template>
        </el-table-column>
        <el-table-column prop="answer" label="答案" width="110" show-overflow-tooltip />
        <el-table-column prop="analysis" label="解析" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.analysis || '-' }}</template>
        </el-table-column>
        <el-table-column label="问题" min-width="170">
          <template #default="{ row }">
            <el-tag v-for="(e, i) in row.errors" :key="i" type="danger" size="small" style="margin: 2px">{{ e }}</el-tag>
            <span v-if="!row.errors || !row.errors.length" style="color: #67c23a">✓</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-footer">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :loading="saving" :disabled="!rows.length" @click="doSave">确认导入 {{ okCount }} 行</el-button>
      </div>
    </div>

    <!-- 第三步: 结果 -->
    <div v-else>
      <el-result icon="success" :title="'成功导入 ' + result.successCount + ' 道题'">
        <template #sub-title>
          <div v-if="result.failures && result.failures.length">
            <p>以下 {{ result.failures.length }} 行导入失败:</p>
            <div v-for="(f, i) in result.failures" :key="i" class="fail-line">
              第 {{ f.rowNo }} 行: {{ f.reason }}
            </div>
          </div>
          <p v-else>题目已全部入库</p>
        </template>
      </el-result>
      <div class="dialog-footer">
        <el-button type="primary" @click="finish">完成</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { questionApi, bankApi } from '../api'
import { aiChat, hasApiKey } from '../utils/ai'
import { useRouter } from 'vue-router'
import { useCategoryStore } from '../stores/categories'
import { TYPE_NAMES as typeNames } from '../utils/constants'

const AI_LIMIT = 50
const router = useRouter()
const emit = defineEmits(['imported'])

const visible = defineModel({ default: false })
const step = ref(0)
const file = ref(null)
const rows = ref([])
const result = ref({})
const parsing = ref(false)
const saving = ref(false)
const bankId = ref(null)
const categoryId = ref(null)
const banks = ref([])
const categoryStore = useCategoryStore()
const flatCategories = computed(() => categoryStore.flat)
const uploadRef = ref()

const errorCount = computed(() => rows.value.filter((r) => r.errors && r.errors.length).length)
const okCount = computed(() => rows.value.length - errorCount.value)

/** 文件导入方式: md=非模板化(Markdown), table=模板化(表格) */
const fileMode = computed(() => {
  if (!file.value) return null
  const n = file.value.name.toLowerCase()
  return n.endsWith('.md') || n.endsWith('.markdown') ? 'md' : 'table'
})

// ---------- AI 补全 ----------
const aiRunning = ref(false)
const aiDone = ref(false)
const aiDoneCount = ref(0)
const aiFailed = ref([])

/** 缺失答案或解析的行(排除本身已校验失败的行), 上限 AI_LIMIT */
const aiTargets = computed(() =>
  rows.value
    .filter((r) => !(r.errors && r.errors.length) && (!r.answer || !r.analysis))
    .slice(0, AI_LIMIT)
)
const aiPercent = computed(() =>
  aiTargets.value.length ? Math.round((aiDoneCount.value / aiTargets.value.length) * 100) : 0
)

function aiPrompt (row) {
  let p = '你是资深教师。请为下面这道题补全缺失的答案和解析。只输出一个 JSON 对象, 两个键分别是 answer 和 analysis, 值均为字符串, 不要输出 JSON 以外的任何内容。\n'
  if (row.answer) p += '答案已存在, 请原样保留在 answer 字段: ' + row.answer + '\n'
  p += '题型: ' + (typeNames[(row.type || 1) - 1]) + '\n'
  p += '题干: ' + row.title + '\n'
  if (row.options && row.options.length) {
    p += '选项:\n'
    row.options.forEach((o, i) => { p += String.fromCharCode(65 + i) + '. ' + o + '\n' })
  }
  if (row.analysis) p += '解析已存在: ' + row.analysis + '\n'
  p += '要求: 选择题 answer 只填选项字母(多选题字母按字母序); 判断题 answer 只填「对」或「错」; 填空题多空用 ||| 分隔。'
  return p
}

function parseAiJson (text) {
  let s = (text || '').trim()
  const fence = s.indexOf('{')
  const end = s.lastIndexOf('}')
  if (fence < 0 || end <= fence) return null
  try {
    return JSON.parse(s.substring(fence, end + 1))
  } catch (e) {
    return null
  }
}

async function aiFill () {
  if (!hasApiKey()) {
    ElMessage.warning('尚未配置 AI, 请先到「AI 设置」填写 API Key')
    router.push('/ai-settings')
    return
  }
  const targets = aiTargets.value.slice()
  if (!targets.length) {
    ElMessage.info('没有需要补全的行')
    return
  }
  aiRunning.value = true
  aiDone.value = false
  aiDoneCount.value = 0
  aiFailed.value = []
  try {
    for (const row of targets) {
      try {
        const obj = parseAiJson(await aiChat(aiPrompt(row)))
        if (!obj) throw new Error('bad json')
        if (!row.answer && obj.answer) row.answer = String(obj.answer).trim()
        if (!row.analysis && obj.analysis) row.analysis = String(obj.analysis).trim()
        if (!row.answer && !row.analysis) throw new Error('empty')
      } catch (e) {
        aiFailed.value.push(row.rowNo)
      }
      aiDoneCount.value++
    }
  } finally {
    aiRunning.value = false
    aiDone.value = true
  }
}

watch(visible, (v) => {
  if (v) {
    step.value = 0
    file.value = null
    rows.value = []
    bankId.value = null
    categoryId.value = null
    aiRunning.value = false
    aiDone.value = false
    aiDoneCount.value = 0
    aiFailed.value = []
    if (!banks.value.length) {
      bankApi.list().then((list) => { banks.value = list })
    }
    if (!categoryStore.loaded) {
      categoryStore.fetchTree()
    }
  }
})

function onFileChange (f) {
  file.value = f.raw || null
}

function onExceed () {
  ElMessage.warning('只能选择一个文件, 请先移除已选文件')
}

const TEMPLATE_MIME = {
  xlsx: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
  csv: 'text/csv;charset=UTF-8',
  md: 'text/markdown;charset=UTF-8'
}

async function downloadTemplate (type) {
  const blob = await questionApi.importTemplate(type)
  const url = URL.createObjectURL(new Blob([blob], { type: TEMPLATE_MIME[type] || 'application/octet-stream' }))
  const a = document.createElement('a')
  a.href = url
  a.download = 'question-import-template.' + type
  a.click()
  URL.revokeObjectURL(url)
}

async function doParse () {
  parsing.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)
    rows.value = await questionApi.importParse(fd)
    if (!rows.value.length) {
      ElMessage.warning('未解析到有效数据行')
      return
    }
    step.value = 1
  } finally {
    parsing.value = false
  }
}

async function doSave () {
  saving.value = true
  try {
    const valid = rows.value.filter((r) => !r.errors || !r.errors.length)
    result.value = await questionApi.importSave({
      rows: valid,
      categoryId: categoryId.value,
      bankId: bankId.value
    })
    step.value = 2
  } finally {
    saving.value = false
  }
}

function finish () {
  visible.value = false
  emit('imported')
}

function onClosed () {
  if (uploadRef.value) uploadRef.value.clearFiles()
  file.value = null
  rows.value = []
}
</script>

<style scoped>
.ai-box {
  border: 1px solid #e4e7ed;
  border-radius: 6px;
  padding: 10px 14px;
  margin-bottom: 12px;
  background: #f5f7fa;
}

.ai-head {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 8px;
}

.ai-sub {
  color: #909399;
  font-weight: 400;
  font-size: 12px;
  margin-left: 8px;
}

.ai-body {
  display: flex;
  align-items: center;
}

.ai-done {
  margin-left: 10px;
  color: #67c23a;
  font-size: 12px;
}

.ai-fail {
  margin-top: 8px;
  color: #e6a23c;
  font-size: 12px;
}

.dialog-footer {
  margin-top: 16px;
  text-align: right;
}

.tpl-links {
  display: flex;
  gap: 18px;
  margin-top: 4px;
}

.sub {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}

:deep(.row-error) {
  background: #fef0f0 !important;
}

.fail-line {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.8;
}
</style>
