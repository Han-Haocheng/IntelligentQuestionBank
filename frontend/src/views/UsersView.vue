<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索用户名/昵称" clearable style="width: 200px" @keyup.enter="load" />
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>&nbsp;搜索</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openAdd"><el-icon><Plus /></el-icon>&nbsp;新增用户</el-button>
      </div>
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="用户名" width="140" />
        <el-table-column prop="nickname" label="昵称" width="140" />
        <el-table-column label="角色" width="100">
          <template #default="{ row }">
            <el-tag :type="row.role === 0 ? 'danger' : 'info'" size="small">{{ row.role === 0 ? '管理员' : '用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-tooltip :content="row.status === 1 ? '禁用' : '启用'">
              <el-button link :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
                <el-icon><Lock v-if="row.status === 1" /><Unlock v-else /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="重置密码">
              <el-button link type="primary" @click="resetPwd(row)"><el-icon><Key /></el-icon></el-button>
            </el-tooltip>
            <el-tooltip content="删除">
              <el-button link type="danger" @click="remove(row)"><el-icon><Delete /></el-icon></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pager">
        <el-pagination background layout="total, sizes, prev, pager, next" :total="total"
          v-model:current-page="pageNum" :page-size="pageSize" :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </el-card>

    <el-dialog v-model="addVisible" title="新增用户" width="420px">
      <el-form :model="addForm" label-width="80px">
        <el-form-item label="用户名">
          <el-input v-model="addForm.username" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="addForm.nickname" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="addForm.password" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="角色">
          <el-radio-group v-model="addForm.role">
            <el-radio :value="1">用户</el-radio>
            <el-radio :value="0">管理员</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="addVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="add">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'
import { confirmAction } from '../utils/confirm'

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const keyword = ref('')
const pageNum = ref(1)
const pageSize = ref(10)
const addVisible = ref(false)
const saving = ref(false)
const addForm = reactive({ username: '', nickname: '', password: '', role: 1 })

async function load () {
  loading.value = true
  try {
    const data = await userApi.list({ keyword: keyword.value || undefined, pageNum: pageNum.value, pageSize: pageSize.value })
    rows.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function openAdd () {
  addForm.username = ''
  addForm.nickname = ''
  addForm.password = ''
  addForm.role = 1
  addVisible.value = true
}

async function add () {
  if (!addForm.username || addForm.password.length < 6) {
    ElMessage.warning('请填写用户名, 密码至少6位')
    return
  }
  saving.value = true
  try {
    await userApi.add(addForm)
    ElMessage.success('新增成功')
    addVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus (row) {
  await userApi.updateStatus(row.id, row.status === 1 ? 0 : 1)
  ElMessage.success(row.status === 1 ? '已禁用' : '已启用')
  load()
}

async function resetPwd (row) {
  if (!(await confirmAction('确定将 ' + row.username + ' 的密码重置为 123456 吗?'))) return
  await userApi.resetPassword(row.id)
  ElMessage.success('已重置为 123456')
}

async function remove (row) {
  if (!(await confirmAction('确定删除用户 ' + row.username + ' 吗?'))) return
  await userApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
