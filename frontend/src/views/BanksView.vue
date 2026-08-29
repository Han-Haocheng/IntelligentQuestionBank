<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <div style="flex: 1"></div>
        <el-button type="success" @click="openAdd"><el-icon><Plus /></el-icon>&nbsp;新建题库</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="题库名称" min-width="180" />
        <el-table-column prop="description" label="描述" min-width="220" />
        <el-table-column label="题目数量" width="100">
          <template #default="{ row }">
            <el-tag type="primary" size="small">{{ row.questionCount || 0 }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <el-tooltip content="管理题目">
              <el-button link type="primary" @click="manage(row)"><el-icon><Files /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="共享">
              <el-button link type="primary" @click="openShare(row)"><el-icon><Share /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="编辑">
              <el-button link type="primary" @click="openEdit(row)"><el-icon><Edit /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="删除">
              <el-button link type="danger" @click="remove(row)"><el-icon><Delete /></el-icon></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新建/编辑 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑题库' : '新建题库'" width="460px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="题库名称" required>
          <el-input v-model="form.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 共享 -->
    <el-dialog v-model="shareVisible" title="共享题库" width="440px">
      <el-form label-width="70px">
        <el-form-item label="方式">
          <el-radio-group v-model="shareForm.shareType">
            <el-radio :value="3">共享给用户</el-radio>
            <el-radio :value="4">公开共享</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="shareForm.shareType === 3" label="用户名">
          <el-input v-model="shareForm.toUsername" placeholder="对方的用户名" />
        </el-form-item>
        <el-form-item label="留言">
          <el-input v-model="shareForm.message" maxlength="200" placeholder="留言(可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shareVisible = false">取消</el-button>
        <el-button type="primary" :loading="sharing" @click="doShare">确定共享</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRouter } from 'vue-router'
import { bankApi, shareApi } from '../api'

const router = useRouter()
const rows = ref([])
const loading = ref(false)
const editVisible = ref(false)
const saving = ref(false)
const shareVisible = ref(false)
const sharing = ref(false)

const form = reactive({ id: null, name: '', description: '' })
const shareForm = reactive({ bankId: null, shareType: 3, toUsername: '', message: '' })

async function load () {
  loading.value = true
  try {
    rows.value = await bankApi.list()
  } finally {
    loading.value = false
  }
}

function openAdd () {
  form.id = null
  form.name = ''
  form.description = ''
  editVisible.value = true
}

function openEdit (row) {
  form.id = row.id
  form.name = row.name
  form.description = row.description || ''
  editVisible.value = true
}

async function save () {
  if (!form.name.trim()) { ElMessage.warning('请输入题库名称'); return }
  saving.value = true
  try {
    if (form.id) {
      await bankApi.update({ id: form.id, name: form.name, description: form.description })
    } else {
      await bankApi.add({ name: form.name, description: form.description })
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

function manage (row) {
  router.push({ path: '/questions', query: { bankId: row.id } })
}

function openShare (row) {
  shareForm.bankId = row.id
  shareForm.shareType = 3
  shareForm.toUsername = ''
  shareForm.message = ''
  shareVisible.value = true
}

async function doShare () {
  sharing.value = true
  try {
    await shareApi.share(shareForm)
    ElMessage.success('共享成功')
    shareVisible.value = false
  } finally {
    sharing.value = false
  }
}

async function remove (row) {
  await ElMessageBox.confirm(
    '确定删除题库「' + row.name + '」吗? 库内 ' + (row.questionCount || 0) + ' 道题将保留但不再归属任何题库。',
    '提示', { type: 'warning' })
  await bankApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
