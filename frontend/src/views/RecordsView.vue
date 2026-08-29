<template>
  <div>
    <el-card class="page-card">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="练习名称" min-width="200" />
        <el-table-column label="模式" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ ['顺序', '随机', '错题重做'][row.mode - 1] }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="成绩" width="120">
          <template #default="{ row }">{{ row.correct }} / {{ row.total }}</template>
        </el-table-column>
        <el-table-column label="正确率" width="100">
          <template #default="{ row }">{{ rate(row) }}%</template>
        </el-table-column>
        <el-table-column label="用时" width="90">
          <template #default="{ row }">{{ row.duration }}s</template>
        </el-table-column>
        <el-table-column prop="startTime" label="开始时间" width="170" />
        <el-table-column prop="finishTime" label="交卷时间" width="170" />
        <el-table-column label="操作" width="100" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="详情">
              <el-button link type="primary" @click="openDetail(row)"><el-icon><View /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="删除">
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
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { practiceApi } from '../api'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const detailAnswers = ref([])

function rate (row) {
  return row.total ? Math.round(row.correct * 100 / row.total) : 0
}

async function load () {
  loading.value = true
  try {
    const data = await practiceApi.records({ pageNum: pageNum.value, pageSize: pageSize.value })
    rows.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function openDetail (row) {
  const data = await practiceApi.detail(row.id)
  detailAnswers.value = data.answers
  detailVisible.value = true
}

async function remove (row) {
  await ElMessageBox.confirm('确定删除该练习记录吗?', '提示', { type: 'warning' })
  await practiceApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
