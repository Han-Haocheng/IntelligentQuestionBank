<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <el-button type="success" @click="openAdd(null)"><el-icon><Plus /></el-icon>&nbsp;新增顶级分类</el-button>
        <span class="drag-tip">💡 拖动二级分类到顶级分类上可移动归属; 点击分类名称可直接改名</span>
      </div>
      <el-table :data="tree" v-loading="loading" row-key="id" default-expand-all
        :tree-props="{ children: 'children' }">
        <el-table-column label="分类名称" min-width="340">
          <template #default="{ row }">
            <el-input v-if="renamingId === row.id" v-model="renameValue" size="small"
              style="width: 200px" maxlength="20" @keyup.enter="confirmRename(row)" @blur="confirmRename(row)" />
            <span v-else class="cat-name" :class="{ 'cat-sub': isSub(row) }"
              :draggable="isSub(row)" @click="startRename(row)"
              @dragstart="onDragStart($event, row)"
              @dragover="onDragOver($event, row)"
              @drop="onDrop($event, row)">
              <el-icon v-if="isSub(row)" class="drag-handle"><Rank /></el-icon>
              {{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="createTime" label="创建时间" width="180" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button v-if="isTop(row)" link type="primary" @click="openAdd(row)">
              <el-icon><Plus /></el-icon>&nbsp;子分类
            </el-button>
            <el-button link type="danger" @click="remove(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 新增分类(顶级或子级) -->
    <el-dialog v-model="dialogVisible" :title="form.parentId ? '新增子分类' : '新增顶级分类'" width="420px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="上级分类">
          <el-input :value="parentName" disabled />
        </el-form-item>
        <el-form-item label="分类名称" required>
          <el-input v-model="form.name" maxlength="20" show-word-limit @keyup.enter="save" />
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
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { categoryApi } from '../api'

const tree = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const parentName = ref('无(顶级分类)')
const form = reactive({ id: null, parentId: 0, name: '' })

// 点击改名
const renamingId = ref(null)
const renameValue = ref('')

function isTop (row) { return !row.parentId || row.parentId === 0 }
function isSub (row) { return row.parentId && row.parentId !== 0 }

function findName (id, list) {
  for (const item of list || []) {
    if (item.id === id) return item.name
    const child = findName(id, item.children)
    if (child) return child
  }
  return null
}

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
  parentName.value = parent ? parent.name : '无(顶级分类)'
  dialogVisible.value = true
}

async function save () {
  if (!form.name.trim()) { ElMessage.warning('请输入分类名称'); return }
  saving.value = true
  try {
    await categoryApi.add({ name: form.name, parentId: form.parentId })
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

// ==================== 拖拽移动二级分类 ====================
function onDragStart (e, row) {
  if (!isSub(row)) return
  e.dataTransfer.setData('text/plain', String(row.id))
  e.dataTransfer.effectAllowed = 'move'
}

function onDragOver (e, row) {
  if (isTop(row)) e.preventDefault()  // 仅顶级分类可作为放置目标
}

async function onDrop (e, row) {
  if (!isTop(row)) return
  e.preventDefault()
  const dragId = Number(e.dataTransfer.getData('text/plain'))
  if (!dragId || dragId === row.id) return
  const name = findName(dragId, tree.value)
  if (!name) return
  await categoryApi.update({ id: dragId, name, parentId: row.id })
  ElMessage.success('已移动到「' + row.name + '」')
  load()
}

// ==================== 点击改名 ====================
function startRename (row) {
  renamingId.value = row.id
  renameValue.value = row.name
}

async function confirmRename (row) {
  if (renamingId.value !== row.id) return
  renamingId.value = null
  const name = renameValue.value.trim()
  if (!name || name === row.name) return
  if (name.length > 20) { ElMessage.warning('分类名称不能超过20个字符'); return }
  await categoryApi.update({ id: row.id, name })
  ElMessage.success('已改名')
  load()
}

async function remove (row) {
  await ElMessageBox.confirm('确定删除分类「' + row.name + '」吗?', '提示', { type: 'warning' })
  await categoryApi.remove(row.id)
  ElMessage.success('删除成功')
  load()
}

onMounted(load)
</script>

<style scoped>
.drag-tip {
  color: #909399;
  font-size: 12px;
  margin-left: 12px;
}

.cat-name {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.cat-sub {
  padding-left: 18px;
}

.cat-sub:hover .drag-handle {
  visibility: visible;
}

.drag-handle {
  visibility: hidden;
  color: #909399;
  cursor: grab;
}
</style>
