<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <el-select v-model="mastered" placeholder="全部错题" clearable style="width: 150px" @change="load">
          <el-option :value="0" label="未掌握" />
          <el-option :value="1" label="已掌握" />
        </el-select>
        <div style="flex: 1"></div>
        <el-button type="danger" plain @click="redoWrong">
          <el-icon><RefreshRight /></el-icon>&nbsp;错题重练
        </el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
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
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-tooltip :content="row.favorited ? '取消收藏' : '收藏'">
              <el-button link :type="row.favorited ? 'warning' : 'info'" @click="toggleFav(row)">
                <el-icon><StarFilled v-if="row.favorited" /><Star v-else /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="详情">
              <el-button link type="primary" @click="openDetail(row)"><el-icon><View /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip :content="row.mastered ? '恢复未掌握' : '标记掌握'">
              <el-button link :type="row.mastered ? 'warning' : 'success'" @click="toggleMaster(row)">
                <el-icon><RefreshLeft v-if="row.mastered" /><CircleCheck v-else /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="移除">
              <el-button link type="danger" @click="remove(row)"><el-icon><Delete /></el-icon></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="pageNum" :page-size="pageSize" @change="load" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="错题详情" width="600px">
      <template v-if="current">
        <div class="q-title">{{ current.title }}</div>
        <div class="review-line">题型: {{ typeNames[current.type - 1] }} | 分类: {{ current.categoryName || '未分类' }} | 难度: {{ current.difficulty }}</div>
        <div v-if="current.type === 1 || current.type === 2" class="review-line">
          {{ (current.options || []).map((o, i) => letter(i) + '. ' + o).join('   ') }}
        </div>
        <div class="review-line">正确答案: <b>{{ current.answer }}</b></div>
        <div class="review-line">最近错误答案: {{ current.lastAnswer || '(未作答)' }}</div>
        <div v-if="current.analysis" class="review-line">解析: {{ current.analysis }}</div>
      </template>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { wrongApi, favoriteApi } from '../api'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const router = useRouter()

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const mastered = ref(null)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const current = ref(null)

function letter (i) { return String.fromCharCode(65 + i) }

async function load () {
  loading.value = true
  try {
    const data = await wrongApi.list({
      mastered: mastered.value === null ? undefined : mastered.value,
      pageNum: pageNum.value,
      pageSize: pageSize.value
    })
    rows.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function openDetail (row) {
  current.value = row
  detailVisible.value = true
}

async function toggleFav (row) {
  const fav = await favoriteApi.toggle(row.questionId)
  row.favorited = fav
  ElMessage.success(fav ? '已收藏' : '已取消收藏')
}

async function toggleMaster (row) {
  const state = await wrongApi.toggleMaster(row.questionId)
  row.mastered = state
  ElMessage.success(state ? '已标记掌握' : '已恢复未掌握')
}

async function remove (row) {
  await ElMessageBox.confirm('确定从错题本移除该题吗?', '提示', { type: 'warning' })
  await wrongApi.remove(row.questionId)
  ElMessage.success('已移除')
  load()
}

function redoWrong () {
  router.push('/practice?onlyWrong=1')
}

onMounted(load)
</script>

<style scoped>
.q-title {
  font-size: 16px;
  font-weight: 600;
  margin-bottom: 10px;
}

.review-line {
  color: #606266;
  font-size: 13px;
  line-height: 1.9;
}
</style>
