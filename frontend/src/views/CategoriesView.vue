<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <el-button type="success" @click="openAdd(null)"><el-icon><Plus /></el-icon>&nbsp;新增顶级分类</el-button>
      </div>
      <el-table :data="tree" v-loading="loading" row-key="id" default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column prop="name" label="分类名称" min-width="260" />
        <el-table-column prop="sort" label="排序" width="90" />
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button v-if="!row.parentId || row.parentId === 0" link type="primary" @click="openAdd(row)">
              <el-icon><Plus /></el-icon>&nbsp;子分类
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑分类' : '新增分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级分类">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" maxlength="20" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '../api'

const tree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const parentName = ref('无(顶级分类)')
const form = reactive({ id: null, parentId: 0, name: '', sort: 0 })

async function load () {
  loading.value = true
  try {
    tree.value = await categoryApi.tree()
  } finally {
    loading.value = false
  }
}

function openAdd (parent) {
  form.id = null
  form.parentId = parent ? parent.id : 0
  form.name = ''
  form.sort = 0
  parentName.value = parent ? parent.name : '无(顶级分类)'
  dialogVisible.value = true
}

function openEdit (row) {
  form.id = row.id
  form.parentId = row.parentId || 0
  form.name = row.name
  form.sort = row.sort || 0
  parentName.value = form.parentId === 0 ? '无(顶级分类)' : '(保持不变)'
  dialogVisible.value = true
}

async function save () {
  if (!form.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  saving.value = true
  try {
    if (form.id) {
      await categoryApi.update({ id: form.id, name: form.name, sort: form.sort })
    } else {
      await categoryApi.add({ name: form.name, parentId: form.parentId, sort: form.sort })
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function remove (row) {
  await ElMessageBox.confirm('确定删除分类「' + row.name + '」吗?', '提示', { type: 'warning' })
  await categoryApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>
