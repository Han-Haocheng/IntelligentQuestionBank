<template>
  <div>
    <!-- 开始练习 -->
    <el-card v-if="stage === 'setup'" class="page-card" style="max-width: 560px">
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
          <el-input-number v-model="form.count" :min="1" :max="50" />
        </el-form-item>
        <el-form-item label="只练错题">
          <el-switch v-model="form.onlyWrong" />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="starting" @click="start">
          开始练习
        </el-button>
      </el-form>
    </el-card>

    <!-- 答题中 -->
    <el-card v-else-if="stage === 'answering'" class="page-card">
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
    <el-card v-else class="page-card">
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
        <el-button type="primary" @click="stage = 'setup'">再来一轮</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import { practiceApi, categoryApi, bankApi } from '../api'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const difficultyNames = ['入门', '简单', '中等', '较难', '困难']
const route = useRoute()

const stage = ref('setup')
const starting = ref(false)
const flatCategories = ref([])

const form = reactive({ name: '', mode: 1, categoryId: null, bankId: null, type: null, difficulty: null, count: 10, onlyWrong: false })
const banks = ref([])

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

onMounted(async () => {
  const tree = await categoryApi.tree()
  function flatten (list, prefix) {
    const result = []
    for (const item of list) {
      const pathName = prefix ? prefix + ' / ' + item.name : item.name
      result.push({ id: item.id, name: item.name, pathName })
      if (item.children && item.children.length) result.push(...flatten(item.children, pathName))
    }
    return result
  }
  flatCategories.value = flatten(tree, '')
  bankApi.list().then((list) => { banks.value = list })
  if (route.query.onlyWrong === '1') {
    form.mode = 3
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
</style>
