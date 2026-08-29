<template>
  <div>
    <el-card class="page-card">
      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="收到的共享" name="received" />
        <el-tab-pane label="我发出的" name="sent" />
      </el-tabs>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="questionTitle" label="题目" min-width="240">
          <template #default="{ row }"><span class="question-title-cell">{{ row.questionTitle }}</span></template>
        </el-table-column>
        <el-table-column label="类型" width="110">
          <template #default="{ row }">
            <el-tag size="small" :type="row.shareType === 2 ? 'warning' : 'primary'">
              {{ row.shareType === 2 ? '公开' : '指定用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="tab === 'received'" prop="fromUsername" label="来自" width="120" />
        <el-table-column v-if="tab === 'sent'" label="共享给" width="120">
          <template #default="{ row }">{{ row.shareType === 2 ? '所有人' : (row.toUsername || '-') }}</template>
        </el-table-column>
        <el-table-column prop="message" label="留言" min-width="140" />
        <el-table-column prop="createTime" label="时间" width="170" />
        <el-table-column label="操作" width="130" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">查看题目</el-button>
            <el-button v-if="tab === 'sent'" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pager">
        <el-pagination background layout="total, prev, pager, next" :total="total"
          v-model:current-page="pageNum" :page-size="pageSize" @change="load" />
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="共享题目详情" width="600px">
      <template v-if="current">
        <div class="q-title">{{ current.questionTitle }}</div>
        <template v-if="question">
          <div class="review-line">题型: {{ typeNames[question.type - 1] }}</div>
          <div v-if="question.type === 1 || question.type === 2" class="review-line">
            {{ (question.options || []).map((o, i) => letter(i) + '. ' + o).join('   ') }}
          </div>
          <div class="review-line">参考答案: <b>{{ question.answer }}</b></div>
          <div v-if="question.analysis" class="review-line">解析: {{ question.analysis }}</div>
        </template>
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
import { shareApi, questionApi } from '../api'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const tab = ref('received')
const rows = ref([])
const total = ref(0)
const loading = ref(false)
const pageNum = ref(1)
const pageSize = ref(10)
const detailVisible = ref(false)
const current = ref(null)
const question = ref(null)

function letter (i) { return String.fromCharCode(65 + i) }

async function load () {
  loading.value = true
  try {
    const api = tab.value === 'sent' ? shareApi.sent : shareApi.received
    const data = await api({ pageNum: pageNum.value, pageSize: pageSize.value })
    rows.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

async function openDetail (row) {
  current.value = row
  question.value = await questionApi.get(row.questionId)
  detailVisible.value = true
}

async function cancel (row) {
  await ElMessageBox.confirm('确定取消该共享吗?', '提示', { type: 'warning' })
  await shareApi.cancel(row.id)
  ElMessage.success('已取消')
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
