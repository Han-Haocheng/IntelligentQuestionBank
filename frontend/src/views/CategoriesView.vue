<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <el-input v-model="search" placeholder="搜索分类名称" clearable style="width: 200px" />
        <el-button type="success" @click="openAdd(null)"><el-icon><Plus /></el-icon>&nbsp;新增顶级分类</el-button>
        <el-button size="small" @click="expandAll">展开全部</el-button>
        <el-button size="small" @click="collapseAll">收起全部</el-button>
        <span class="drag-tip">💡 拖动二级分类到顶级分类上=移动归属; 拖到同级分类上=调整顺序; 点击名称=改名</span>
      </div>
      <el-table :data="filteredTree" v-loading="loading" row-key="id"
        :tree-props="{ children: 'children' }"
        :default-expand-all="!!search"
        :expand-row-keys="search ? [] : expandedKeys"
        @expand-change="onExpandChange">
        <el-table-column label="分类名称" min-width="300">
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
        <el-table-column prop="createTime" label="创建时间" width="170" />
        <!-- 每种操作独立一列, 保证一级/二级分类的相同操作始终在同一列 -->
        <el-table-column label="子分类" width="72" align="center">
          <template #default="{ row }">
            <el-tooltip v-if="isTop(row)" content="新增子分类">
              <el-button link type="primary" @click="openAdd(row)"><el-icon><Plus /></el-icon></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="查看题目" width="72" align="center">
          <template #default="{ row }">
            <el-tooltip content="查看该分类下题目">
              <el-button link type="primary" @click="jumpQuestions(row)"><el-icon><View /></el-icon></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="合并" width="72" align="center">
          <template #default="{ row }">
            <el-tooltip content="合并到其他分类">
              <el-button link type="warning" @click="openMerge(row)"><el-icon><Connection /></el-icon></el-button>
            </el-tooltip>
          </template>
        </el-table-column>
        <el-table-column label="删除" width="72" align="center">
          <template #default="{ row }">
            <el-tooltip content="删除分类">
              <el-button link type="danger" @click="remove(row)"><el-icon><Delete /></el-icon></el-button>
            </el-tooltip>
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

    <!-- 合并分类 -->
    <el-dialog v-model="mergeVisible" :title="'合并「' + (mergeFrom ? mergeFrom.name : '') + '」到'" width="440px">
      <el-select v-model="mergeTargetId" filterable placeholder="选择目标分类" style="width: 100%">
        <el-option v-for="c in mergeCandidates" :key="c.id" :value="c.id" :label="c.pathName" />
      </el-select>
      <div class="merge-hint">合并后, 该分类及子级下的题目将全部迁移到目标分类(目标不能是它自身或其子分类)。</div>
      <template #footer>
        <el-button @click="mergeVisible = false">取消</el-button>
        <el-button type="warning" :loading="merging" :disabled="!mergeTargetId" @click="doMerge">合并</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { categoryApi } from '../api'
import { confirmAction } from '../utils/confirm'
import { useCategoryStore } from '../stores/categories'

const router = useRouter()
const categoryStore = useCategoryStore()

const loading = ref(false)
const dialogVisible = ref(false)
const saving = ref(false)
const parentName = ref('无(顶级分类)')
const form = reactive({ id: null, parentId: 0, name: '' })

// 搜索过滤
const search = ref('')
const tree = computed(() => categoryStore.tree)
const filteredTree = computed(() => {
  if (!search.value) return tree.value
  const kw = search.value.trim().toLowerCase()
  const filter = (nodes) => {
    const result = []
    for (const node of nodes || []) {
      const hit = (node.name || '').toLowerCase().includes(kw)
      const children = filter(node.children)
      if (hit || children.length) {
        result.push({ ...node, children: hit ? node.children : children })
      }
    }
    return result
  }
  return filter(tree.value)
})

// 展开状态记忆
const EXPAND_KEY = 'qbank_cat_expanded'
// 注意: Element Plus 树表 treeData 的 key 是字符串, expand-row-keys 需用字符串匹配(数字 id 需 String())
const expandedKeys = ref(((JSON.parse(localStorage.getItem(EXPAND_KEY) || '[]')) || []).map(String))
function persistExpanded () {
  localStorage.setItem(EXPAND_KEY, JSON.stringify(expandedKeys.value))
}
// 树表 expand-change 回调为 (row, expanded: boolean), 据此增量维护展开 key
function onExpandChange (row, expanded) {
  const id = String(row.id)
  const set = new Set(expandedKeys.value)
  if (expanded) {
    set.add(id)
  } else {
    set.delete(id)
  }
  expandedKeys.value = [...set]
  persistExpanded()
}
function expandAll () {
  const collect = (nodes) => {
    const keys = []
    for (const n of nodes || []) {
      if (n.children && n.children.length) {
        keys.push(String(n.id))
        keys.push(...collect(n.children))
      }
    }
    return keys
  }
  expandedKeys.value = collect(tree.value)
  persistExpanded()
}
function collapseAll () {
  expandedKeys.value = []
  persistExpanded()
}

// 点击改名
const renamingId = ref(null)
const renameValue = ref('')

// 合并
const mergeVisible = ref(false)
const mergeFrom = ref(null)
const mergeTargetId = ref(null)
const merging = ref(false)
const mergeCandidates = computed(() => categoryStore.flat.filter(c => c.id !== (mergeFrom.value && mergeFrom.value.id) && c.parentId !== (mergeFrom.value && mergeFrom.value.id)))

function isTop (row) { return !row.parentId || row.parentId === 0 }
function isSub (row) { return row.parentId && row.parentId !== 0 }

function findNode (id, list) {
  for (const item of list || []) {
    if (item.id === id) return item
    const child = findNode(id, item.children)
    if (child) return child
  }
  return null
}

async function load () {
  loading.value = true
  try {
    await categoryStore.fetchTree()
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
    categoryStore.refresh()
  } finally {
    saving.value = false
  }
}

// ==================== 拖拽: 移动归属 / 同级排序 ====================
function onDragStart (e, row) {
  if (!isSub(row)) return
  e.dataTransfer.setData('text/plain', String(row.id))
  e.dataTransfer.effectAllowed = 'move'
}

function onDragOver (e, row) {
  e.preventDefault()
}

async function onDrop (e, row) {
  e.preventDefault()
  const dragId = Number(e.dataTransfer.getData('text/plain'))
  if (!dragId || dragId === row.id) return
  const dragged = findNode(dragId, tree.value)
  if (!dragged || !isSub(dragged)) return
  const name = dragged.name

  if (isTop(row)) {
    // 拖到顶级: 移动归属
    if (dragged.parentId !== row.id) {
      await categoryApi.update({ id: dragId, name, parentId: row.id })
      ElMessage.success('已移动到「' + row.name + '」')
    }
  } else {
    // 拖到同级/其他子级: 先移动归属到目标父级, 再同级排序(插到目标之后)
    const targetParentId = row.parentId
    if (dragged.parentId !== targetParentId) {
      await categoryApi.update({ id: dragId, name, parentId: targetParentId })
    }
    const parent = findNode(targetParentId, tree.value)
    const siblings = (parent && parent.children) ? parent.children.map(c => c.id) : []
    const idx = siblings.indexOf(row.id)
    const from = siblings.indexOf(dragId)
    if (from >= 0) siblings.splice(from, 1)
    const insertAt = idx >= 0 ? idx : siblings.length
    siblings.splice(insertAt, 0, dragId)
    await categoryApi.sort(targetParentId, siblings)
    ElMessage.success('已调整顺序')
  }
  categoryStore.refresh()
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
  categoryStore.refresh()
}

// ==================== 查看题目(跳转联动) ====================
function jumpQuestions (row) {
  router.push('/questions?categoryId=' + row.id)
}

// ==================== 合并分类 ====================
function openMerge (row) {
  mergeFrom.value = row
  mergeTargetId.value = null
  mergeVisible.value = true
}

async function doMerge () {
  if (!mergeTargetId.value) return
  merging.value = true
  try {
    const moved = await categoryApi.merge(mergeFrom.value.id, mergeTargetId.value)
    ElMessage.success('已迁移 ' + moved + ' 道题到目标分类')
    mergeVisible.value = false
    categoryStore.refresh()
  } finally {
    merging.value = false
  }
}

// ==================== 删除(带影响面提示) ====================
async function remove (row) {
  let stats = { questionCount: 0, childCount: 0 }
  try {
    stats = await categoryApi.count(row.id)
  } catch (e) { /* 提示信息仍可展示 */ }
  const msg = '确定删除分类「' + row.name + '」吗?\n' +
    '该分类及子级下共有 ' + (stats.questionCount || 0) + ' 道题、' + (stats.childCount || 0) + ' 个子分类;\n' +
    '删除后这些题目将变为未分类。'
  if (!(await confirmAction(msg, '提示', { confirmButtonText: '删除' }))) return
  await categoryApi.remove(row.id)
  ElMessage.success('删除成功')
  categoryStore.refresh()
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

.merge-hint {
  margin-top: 8px;
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
