<template>
  <div>
    <el-card class="page-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="题干" min-width="280">
          <template #default="{ row }"><span class="question-title-cell">{{ row.title }}</span></template>
        </el-table-column>
        <el-table-column label="题型" width="90">
          <template #default="{ row }"><el-tag size="small">{{ typeNames[row.type - 1] }}</el-tag></template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column label="难度" width="80">
          <template #default="{ row }">{{ row.difficulty }}</template>
        </el-table-column>
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="详情">
              <el-button link type="primary" @click="openDetail(row)"><el-icon><View /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="取消收藏">
              <el-button link type="danger" @click="remove(row)"><el-icon><StarFilled /></el-icon></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="pageNum" :page-size="pageSize" @change="load" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="题目详情" width="600px">
      <template v-if="current">
        <div class="q-title">{{ current.title }}</div>
        <div class="review-line">题型: {{ typeNames[current.type - 1] }} | 分类: {{ current.categoryName || '未分类' }}</div>
        <div v-if="current.type === 1 || current.type === 2" class="review-line">
          {{ (current.options || []).map((o, i) => letter(i) + '. ' + o).join('   ') }}
        </div>
        <div class="review-line">参考答案: <b>{{ current.answer }}</b></div>
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
import { ElMessage } from 'element-plus'
import { favoriteApi } from '../api'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const current = ref(null)

function letter (i) { return String.fromCharCode(65 + i) }

async function load () {
  loading.value = true
  try {
    const data = await favoriteApi.list({ pageNum: pageNum.value, pageSize: pageSize.value })
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

async function remove (row) {
  await favoriteApi.remove(row.id)
  ElMessage.success('已取消收藏')
  load()
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
