<template>
  <div>
    <!-- 练习主页: 开始练习 + 练习记录 -->
    <el-tabs v-if="stage === 'setup'" v-model="activeTab" @tab-change="onTabChange">
      <el-tab-pane label="开始练习" name="start">
        <el-card class="page-card" style="max-width: 640px">
          <template #header><b>开始练习</b></template>
          <el-form :model="form" label-width="90px">
            <el-form-item label="练习名称">
              <el-input v-model="form.name" placeholder="留空自动生成" />
            </el-form-item>
            <el-form-item label="练习模式">
              <el-radio-group v-model="form.mode">
                <el-radio-button :value="1">顺序</el-radio-button>
                <el-radio-button :value="2">随机</el-radio-button>
                <el-radio-button :value="3">错题重做</el-radio-button>
              </el-radio-group>
            </el-form-item>
            <el-form-item label="限定分类">
              <el-select v-model="form.categoryId" placeholder="全部分类" clearable style="width: 100%">
                <el-option v-for="c in flatCategories" :key="c.id" :value="c.id" :label="c.pathName" />
              </el-select>
            </el-form-item>
            <el-form-item label="限定题库">
              <el-select v-model="form.bankId" placeholder="全部题库" clearable style="width: 100%">
                <el-option v-for="b in banks" :key="b.id" :value="b.id" :label="b.name" />
              </el-select>
            </el-form-item>
            <el-form-item label="题型">
              <el-select v-model="form.type" placeholder="全部题型" clearable style="width: 100%">
                <el-option v-for="(n, i) in typeNames" :key="i" :value="i + 1" :label="n" />
              </el-select>
            </el-form-item>
            <el-form-item label="难度">
              <el-select v-model="form.difficulty" placeholder="全部难度" clearable style="width: 100%">
                <el-option v-for="(n, i) in difficultyNames" :key="i" :value="i + 1" :label="n" />
              </el-select>
            </el-form-item>
            <el-form-item label="题目数量">
              <div class="count-row">
                <div v-loading="countLoading" class="count-info">
                  当前筛选共 <b :class="{ 'count-zero': availableCount === 0 }">{{ availableCount }}</b> 题
                </div>
                <el-radio-group v-model="countMode" size="small">
                  <el-radio-button value="all">全部</el-radio-button>
                  <el-radio-button value="half">1/2</el-radio-button>
                  <el-radio-button value="quarter">1/4</el-radio-button>
                  <el-radio-button value="custom">自定义</el-radio-button>
                </el-radio-group>
                <el-input-number v-if="countMode === 'custom'" v-model="form.count" :min="1" :max="50" />
              </div>
            </el-form-item>
            <el-form-item label="只练错题">
              <el-switch v-model="form.onlyWrong" />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="starting"
              :disabled="form.count < 1" @click="start">
              开始练习
            </el-button>
          </el-form>
        </el-card>
      </el-tab-pane>
      <el-tab-pane label="练习记录" name="records">
        <el-card class="page-card">
          <el-table :data="recordRows" v-loading="recordLoading" stripe>
            <el-table-column prop="name" label="练习名称" min-width="200" />
            <el-table-column label="模式" width="100">
              <template #default="{ row }">
                <el-tag size="small">{{ ['顺序', '随机', '错题重做'][row.mode - 1] }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="成绩" width="110">
              <template #default="{ row }">{{ row.correct }} / {{ row.total }}</template>
            </el-table-column>
            <el-table-column label="正确率" width="90">
              <template #default="{ row }">{{ rate(row) }}%</template>
            </el-table-column>
            <el-table-column label="用时" width="80">
              <template #default="{ row }">{{ row.duration }}s</template>
            </el-table-column>
            <el-table-column prop="startTime" label="开始时间" width="165" />
            <el-table-column prop="finishTime" label="交卷时间" width="165" />
            <el-table-column label="操作" width="140" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openDetail(row)">详情</el-button>
                <el-button link type="danger" @click="removeRecord(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pager">
            <el-pagination background layout="total, prev, pager, next" :total="recordTotal"
              v-model:current-page="recordPageNum" :page-size="recordPageSize" @change="loadRecords" />
          </div>
        </el-card>
      </el-tab-pane>

      <!-- 错题本(并入练习页) -->
      <el-tab-pane label="错题本" name="wrong">
        <el-card class="page-card">
          <div class="filter-bar">
            <el-select v-model="wrongMastered" placeholder="全部错题" clearable style="width: 150px" @change="loadWrong">
              <el-option :value="0" label="未掌握" />
              <el-option :value="1" label="已掌握" />
            </el-select>
            <div style="flex: 1"></div>
            <el-button type="danger" plain @click="redoWrong">
              <el-icon><RefreshRight /></el-icon>&nbsp;错题重练
            </el-button>
          </div>
          <el-table :data="wrongRows" v-loading="wrongLoading" stripe>
            <el-table-column label="题干" min-width="280">
              <template #default="{ row }"><span class="question-title-cell">{{ row.title }}</span></template>
            </el-table-column>
            <el-table-column label="题型" width="90">
              <template #default="{ row }"><el-tag size="small">{{ typeNames[row.type - 1] }}</el-tag></template>
            </el-table-column>
            <el-table-column prop="categoryName" label="分类" width="110" />
            <el-table-column prop="wrongCount" label="错误次数" width="90">
              <template #default="{ row }"><el-tag type="danger" size="small">{{ row.wrongCount }}</el-tag></template>
            </el-table-column>
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="row.mastered ? 'success' : 'warning'" size="small">{{ row.mastered ? '已掌握' : '未掌握' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="lastWrongTime" label="最近错误" width="170" />
            <el-table-column label="操作" width="210" fixed="right">
              <template #default="{ row }">
                <el-button link type="primary" @click="openWrongDetail(row)">详情</el-button>
                <el-button link :type="row.mastered ? 'warning' : 'success'" @click="toggleMaster(row)">
                  {{ row.mastered ? '恢复' : '掌握' }}
                </el-button>
                <el-button link type="danger" @click="removeWrong(row)">移除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <div class="table-pager">
            <el-pagination background layout="total, prev, pager, next" :total="wrongTotal"
              v-model:current-page="wrongPageNum" :page-size="wrongPageSize" @change="loadWrong" />
          </div>
        </el-card>
        <el-dialog v-model="wrongDetailVisible" title="错题详情" width="600px">
          <template v-if="wrongCurrent">
            <div class="q-title">{{ wrongCurrent.title }}</div>
            <div class="review-line">题型: {{ typeNames[wrongCurrent.type - 1] }} | 分类: {{ wrongCurrent.categoryName || '未分类' }} | 难度: {{ wrongCurrent.difficulty }}</div>
            <div v-if="wrongCurrent.type === 1 || wrongCurrent.type === 2" class="review-line">
              {{ (wrongCurrent.options || []).map((o, i) => letter(i) + '. ' + o).join('   ') }}
            </div>
            <div class="review-line">正确答案: <b>{{ wrongCurrent.answer }}</b></div>
            <div class="review-line">最近错误答案: {{ wrongCurrent.lastAnswer || '(未作答)' }}</div>
            <div v-if="wrongCurrent.analysis" class="review-line">解析: {{ wrongCurrent.analysis }}</div>
          </template>
          <template #footer>
            <el-button @click="wrongDetailVisible = false">关闭</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>

    <!-- 答题中 -->
    <el-card v-if="stage === 'answering'" class="page-card">
      <el-progress :percentage="answeredPercent" :stroke-width="10" style="margin-bottom: 16px" />
      <div class="q-meta">
        <el-tag type="primary">{{ typeNames[current.type - 1] }}</el-tag>
        <el-tag type="warning">难度 {{ current.difficulty }}</el-tag>
        <span class="q-index">第 {{ index + 1 }} / {{ questions.length }} 题</span>
      </div>
      <div class="q-title">{{ current.title }}</div>

      <div v-if="current.type === 1" class="q-body">
        <el-radio-group v-model="answers[current.id]">
          <el-radio v-for="(opt, i) in current.options" :key="i" :value="letter(i)" class="option-line">
            {{ letter(i) }}. {{ opt }}
          </el-radio>
        </el-radio-group>
      </div>
      <div v-else-if="current.type === 2" class="q-body">
        <el-checkbox-group v-model="multiSelect" @change="onMultiChange">
          <el-checkbox v-for="(opt, i) in current.options" :key="i" :value="letter(i)" class="option-line">
            {{ letter(i) }}. {{ opt }}
          </el-checkbox>
        </el-checkbox-group>
      </div>
      <div v-else-if="current.type === 3" class="q-body">
        <el-input v-model="answers[current.id]" placeholder="多个空之间用 ||| 分隔" />
      </div>
      <div v-else-if="current.type === 4" class="q-body">
        <el-radio-group v-model="answers[current.id]">
          <el-radio value="对" class="option-line">对</el-radio>
          <el-radio value="错" class="option-line">错</el-radio>
        </el-radio-group>
      </div>
      <div v-else class="q-body">
        <el-input v-model="answers[current.id]" type="textarea" :rows="3" placeholder="请输入答案" />
      </div>

      <div class="q-actions">
        <el-button :disabled="index === 0" @click="index--">上一题</el-button>
        <el-button v-if="index < questions.length - 1" type="primary" @click="index++">下一题</el-button>
        <el-button type="success" @click="submit">交卷</el-button>
        <el-button @click="quit">放弃</el-button>
      </div>
    </el-card>

    <!-- 结果 -->
    <el-card v-if="stage === 'result'" class="page-card">
      <template #header><b>练习结果</b></template>
      <el-result :icon="scoreIcon" :title="resultTitle"
        :sub-title="'用时 ' + result.record.duration + ' 秒 · 答对 ' + result.record.correct + ' / ' + result.record.total + ' 题'">
      </el-result>
      <div v-for="(a, i) in result.answers" :key="a.id" class="review-item">
        <div class="review-title">
          <el-tag :type="a.isCorrect ? 'success' : 'danger'" size="small">{{ a.isCorrect ? '答对' : '答错' }}</el-tag>
          <el-tag size="small" type="info">{{ typeNames[a.type - 1] }}</el-tag>
          <span>{{ i + 1 }}. {{ a.title }}</span>
        </div>
        <div class="review-line">你的答案: <b :style="{ color: a.isCorrect ? '#67c23a' : '#f56c6c' }">{{ a.userAnswer || '(未作答)' }}</b></div>
        <div class="review-line">正确答案: <b>{{ a.correctAnswer }}</b></div>
        <div v-if="a.analysis" class="review-line">解析: {{ a.analysis }}</div>
      </div>
      <div style="text-align:center; margin-top: 10px">
        <el-button type="primary" @click="backToSetup">再来一轮</el-button>
      </div>
    </el-card>

    <!-- 练习详情 -->
    <el-dialog v-model="detailVisible" title="练习详情" width="700px">
      <div v-for="(a, i) in detailAnswers" :key="a.id" class="review-item">
        <div class="review-title">
          <el-tag :type="a.isCorrect ? 'success' : 'danger'" size="small">{{ a.isCorrect ? '答对' : '答错' }}</el-tag>
          <el-tag size="small" type="info">{{ typeNames[a.type - 1] }}</el-tag>
          <span>{{ i + 1 }}. {{ a.title }}</span>
        </div>
        <div class="review-line">你的答案: {{ a.userAnswer || '(未作答)' }} | 正确答案: {{ a.correctAnswer }}</div>
        <div v-if="a.analysis" class="review-line">解析: {{ a.analysis }}</div>
      </div>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { practiceApi, bankApi, wrongApi } from '../api'
import { useCategoryStore } from '../stores/categories'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const difficultyNames = ['入门', '简单', '中等', '较难', '困难']
const route = useRoute()

const stage = ref('setup')
const starting = ref(false)
const categoryStore = useCategoryStore()
const flatCategories = computed(() => categoryStore.flat)
const activeTab = ref('start')

const form = reactive({ name: '', mode: 1, categoryId: null, bankId: null, type: null, difficulty: null, count: 10, onlyWrong: false })
const banks = ref([])

// 题目数量快捷区
const countMode = ref('custom')
const availableCount = ref(0)
const countLoading = ref(false)

// 练习记录
const recordRows = ref([])
const recordTotal = ref(0)
const recordLoading = ref(false)
const recordPageNum = ref(1)
const recordPageSize = ref(10)
const detailVisible = ref(false)
const detailAnswers = ref([])

// 错题本(并入练习页)
const wrongRows = ref([])
const wrongTotal = ref(0)
const wrongLoading = ref(false)
const wrongMastered = ref(null)
const wrongPageNum = ref(1)
const wrongPageSize = ref(10)
const wrongDetailVisible = ref(false)
const wrongCurrent = ref(null)

const recordId = ref(null)
const questions = ref([])
const answers = reactive({})
const multiSelect = ref([])
const index = ref(0)
const result = ref(null)

const current = computed(() => questions.value[index.value])
const answeredCount = computed(() => questions.value.filter(q => (answers[q.id] || '').trim() !== '').length)
const answeredPercent = computed(() => questions.value.length ? Math.round(answeredCount.value * 100 / questions.value.length) : 0)
const scoreIcon = computed(() => {
  const r = result.value.record
  const rate = r.total ? r.correct * 100 / r.total : 0
  return rate >= 80 ? 'success' : (rate >= 60 ? 'warning' : 'error')
})
const resultTitle = computed(() => {
  const r = result.value.record
  const rate = r.total ? Math.round(r.correct * 100 / r.total) : 0
  return '正确率 ' + rate + '%'
})

function letter (i) { return String.fromCharCode(65 + i) }

function onMultiChange (val) {
  answers[current.value.id] = [...val].sort().join('')
}

watch(index, () => {
  multiSelect.value = current.value && current.value.type === 2 ? (answers[current.value.id] || '').split('') : []
})

// ==================== 题目数量快捷区 ====================
function applyQuickCount () {
  const n = availableCount.value
  if (countMode.value === 'all') {
    form.count = n
  } else if (countMode.value === 'half') {
    form.count = Math.max(1, Math.round(n / 2))
  } else if (countMode.value === 'quarter') {
    form.count = Math.max(1, Math.round(n / 4))
  }
}

async function fetchCount () {
  countLoading.value = true
  try {
    const n = await practiceApi.count({
      categoryId: form.categoryId || undefined,
      bankId: form.bankId || undefined,
      difficulty: form.difficulty || undefined,
      type: form.type || undefined,
      onlyWrong: (form.onlyWrong || form.mode === 3) || undefined
    })
    availableCount.value = Number(n) || 0
    if (countMode.value !== 'custom') applyQuickCount()
  } catch (e) {
    availableCount.value = 0
  } finally {
    countLoading.value = false
  }
}

watch(() => [form.mode, form.categoryId, form.bankId, form.type, form.difficulty, form.onlyWrong], () => {
  if (stage.value === 'setup') fetchCount()
})

watch(countMode, () => {
  if (countMode.value !== 'custom') applyQuickCount()
})

// ==================== 练习记录 (移植自 RecordsView) ====================
function onTabChange (name) {
  if (name === 'records') loadRecords()
  if (name === 'wrong') loadWrong()
}

// ==================== 错题本 (并入练习页) ====================
async function loadWrong () {
  wrongLoading.value = true
  try {
    const data = await wrongApi.list({
      mastered: wrongMastered.value === null ? undefined : wrongMastered.value,
      pageNum: wrongPageNum.value,
      pageSize: wrongPageSize.value
    })
    wrongRows.value = data.list
    wrongTotal.value = Number(data.total)
  } finally {
    wrongLoading.value = false
  }
}

function openWrongDetail (row) {
  wrongCurrent.value = row
  wrongDetailVisible.value = true
}

async function toggleMaster (row) {
  const state = await wrongApi.toggleMaster(row.questionId)
  row.mastered = state
  ElMessage.success(state ? '已标记掌握' : '已恢复未掌握')
}

async function removeWrong (row) {
  await ElMessageBox.confirm('确定从错题本移除该题吗?', '提示', { type: 'warning' })
  await wrongApi.remove(row.questionId)
  ElMessage.success('已移除')
  loadWrong()
}

function redoWrong () {
  // 切回开始练习: 错题重做模式
  activeTab.value = 'start'
  form.mode = 3
  form.onlyWrong = true
  fetchCount()
}

function rate (row) {
  return row.total ? Math.round(row.correct * 100 / row.total) : 0
}

async function loadRecords () {
  recordLoading.value = true
  try {
    const data = await practiceApi.records({ pageNum: recordPageNum.value, pageSize: recordPageSize.value })
    recordRows.value = data.list
    recordTotal.value = Number(data.total)
  } finally {
    recordLoading.value = false
  }
}

async function openDetail (row) {
  const data = await practiceApi.detail(row.id)
  detailAnswers.value = data.answers
  detailVisible.value = true
}

async function removeRecord (row) {
  await ElMessageBox.confirm('确定删除该练习记录吗?', '提示', { type: 'warning' })
  await practiceApi.remove(row.id)
  ElMessage.success('删除成功')
  loadRecords()
}

// ==================== 练习流程 ====================
async function start () {
  starting.value = true
  try {
    const data = await practiceApi.start({
      name: form.name || undefined,
      mode: form.mode,
      categoryId: form.categoryId || undefined,
      bankId: form.bankId || undefined,
      type: form.type || undefined,
      difficulty: form.difficulty || undefined,
      count: form.count,
      onlyWrong: form.onlyWrong
    })
    recordId.value = data.record.id
    questions.value = data.questions
    Object.keys(answers).forEach(k => delete answers[k])
    multiSelect.value = []
    index.value = 0
    stage.value = 'answering'
  } finally {
    starting.value = false
  }
}

async function submit () {
  const unanswered = questions.value.length - answeredCount.value
  if (unanswered > 0) {
    await ElMessageBox.confirm('还有 ' + unanswered + ' 题未作答, 确定交卷吗?', '提示', { type: 'warning' })
  }
  const payload = {
    recordId: recordId.value,
    answers: questions.value
      .filter(q => (answers[q.id] || '').trim() !== '')
      .map(q => ({ questionId: q.id, answer: answers[q.id] }))
  }
  result.value = await practiceApi.submit(payload)
  stage.value = 'result'
  ElMessage.success('已交卷, 答错的题目已加入错题本')
}

async function quit () {
  await ElMessageBox.confirm('确定放弃本次练习吗? 未交卷不会记录成绩。', '提示', { type: 'warning' })
  stage.value = 'setup'
}

function backToSetup () {
  stage.value = 'setup'
  fetchCount()
}

onMounted(async () => {
  await categoryStore.fetchTree()
  bankApi.list().then((list) => { banks.value = list })
  if (route.query.onlyWrong === '1') {
    form.mode = 3
  } else {
    fetchCount()
  }
})
</script>

<style scoped>
.q-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  margin-bottom: 12px;
}

.q-index {
  color: #909399;
  font-size: 13px;
}

.q-title {
  font-size: 17px;
  font-weight: 600;
  line-height: 1.6;
  margin-bottom: 18px;
}

.q-body {
  padding: 6px 0 20px;
}

.option-line {
  display: block;
  margin: 0 0 12px;
}

.q-actions {
  display: flex;
  gap: 10px;
}

.review-item {
  border-top: 1px dashed #e4e7ed;
  padding: 12px 0;
}

.review-title {
  display: flex;
  gap: 8px;
  align-items: center;
  font-weight: 600;
  margin-bottom: 8px;
}

.review-line {
  color: #606266;
  font-size: 13px;
  line-height: 1.8;
}

.count-row {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.count-info {
  color: #606266;
  font-size: 13px;
}

.count-zero {
  color: #f56c6c;
}
</style>
