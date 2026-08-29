<template>
  <div>
    <el-card class="page-card">
      <el-tabs v-model="tab" @tab-change="load">
        <el-tab-pane label="收到的共享" name="received" />
        <el-tab-pane label="我发出的" name="sent" />
      </el-tabs>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="共享内容" min-width="220">
          <template #default="{ row }">
            <el-tag v-if="row.bankId" size="small" type="warning" style="margin-right: 6px">题库</el-tag>
            <span class="question-title-cell">{{ row.bankId ? row.bankName : row.questionTitle }}</span>
          </template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="isPublic(row) ? 'warning' : 'primary'">
              {{ isPublic(row) ? '公开' : '指定用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限" width="90">
          <template #default="{ row }">
            <el-tag size="small" :type="row.permission === 2 ? 'success' : 'info'">
              {{ row.permission === 2 ? '可编辑' : '只读' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="tab === 'received'" prop="fromUsername" label="来自" width="110" />
        <el-table-column v-if="tab === 'sent'" label="共享给" width="110">
          <template #default="{ row }">{{ isPublic(row) ? '所有人' : (row.toUsername || '-') }}</template>
        </el-table-column>
        <el-table-column prop="message" label="留言" min-width="120" />
        <el-table-column prop="createTime" label="时间" width="160" />
        <el-table-column label="操作" :width="tab === 'received' ? 220 : 180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">{{ row.bankId ? '查看题库' : '查看题目' }}</el-button>
            <template v-if="tab === 'sent'">
              <el-button v-if="!isPublic(row)" link type="warning" @click="openPerm(row)">改权限</el-button>
              <el-button link type="danger" @click="cancel(row)">取消</el-button>
            </template>
            <template v-else>
              <el-button link :type="row.subscribed === 1 ? 'warning' : 'success'" @click="toggleSubscribe(row)">
                {{ row.subscribed === 1 ? '退订' : '重新订阅' }}
              </el-button>
              <el-button link type="primary" @click="doCopy(row)">拷贝</el-button>
            </template>
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
        <div class="q-title">
          <el-tag v-if="current.bankId" size="small" type="warning" style="margin-right: 6px">题库</el-tag>
          {{ current.bankId ? current.bankName : current.questionTitle }}
        </div>
        <div v-if="current.bankId && !question" class="review-line">
          这是题库共享, 收到的题目可在「题目管理」页按该题库筛选查看。
        </div>
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

    <!-- 修改权限 -->
    <el-dialog v-model="permVisible" title="修改共享权限" width="380px">
      <el-form label-width="70px">
        <el-form-item label="权限">
          <el-radio-group v-model="permForm.permission">
            <el-radio :value="1">只读</el-radio>
            <el-radio :value="2">可编辑</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="permVisible = false">取消</el-button>
        <el-button type="primary" :loading="permSaving" @click="savePerm">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { shareApi, questionApi } from '../api'

const router = useRouter()

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

// 权限修改
const permVisible = ref(false)
const permSaving = ref(false)
const permForm = reactive({ id: null, permission: 1 })

function letter (i) { return String.fromCharCode(65 + i) }

function isPublic (row) {
  return row.shareType === 2 || row.shareType === 4
}

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
  if (row.bankId) {
    router.push({ path: '/questions', query: { bankId: row.bankId } })
    return
  }
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

function openPerm (row) {
  permForm.id = row.id
  permForm.permission = row.permission === 2 ? 2 : 1
  permVisible.value = true
}

async function savePerm () {
  permSaving.value = true
  try {
    await shareApi.updatePermission(permForm.id, permForm.permission)
    ElMessage.success('权限已修改')
    permVisible.value = false
    load()
  } finally {
    permSaving.value = false
  }
}

async function toggleSubscribe (row) {
  const subscribed = row.subscribed !== 1
  await shareApi.subscribe(row.id, subscribed)
  ElMessage.success(subscribed ? '已重新订阅' : '已退订, 退订后不再展示该共享')
  load()
}

async function doCopy (row) {
  await shareApi.copy(row.id)
  if (row.bankId) {
    ElMessage.success('已拷贝为我的题库, 可在「题目管理」页左侧题库栏查看')
  } else {
    ElMessage.success('已拷贝为我的题目')
  }
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
