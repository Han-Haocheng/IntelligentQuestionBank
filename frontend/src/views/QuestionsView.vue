<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索题干/标签" clearable style="width: 200px" @keyup.enter="load" />
        <el-select v-model="query.categoryId" placeholder="分类" clearable style="width: 150px">
          <el-option v-for="c in flatCategories" :key="c.id" :value="c.id" :label="c.pathName" />
        </el-select>
        <el-select v-model="query.type" placeholder="题型" clearable style="width: 120px">
          <el-option v-for="(n, i) in typeNames" :key="i" :value="i + 1" :label="n" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 120px">
          <el-option v-for="(n, i) in difficultyNames" :key="i" :value="i + 1" :label="n" />
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>&nbsp;搜索</el-button>
        <el-button @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openEdit()"><el-icon><Plus /></el-icon>&nbsp;新增题目</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column label="题干" min-width="280">
          <template #default="{ row }">
            <span class="question-title-cell">{{ row.title }}</span>
          </template>
        </el-table-column>
        <el-table-column label="题型" width="90">
          <template #default="{ row }"><el-tag size="small">{{ typeNames[row.type - 1] }}</el-tag></template>
        </el-table-column>
        <el-table-column label="难度" width="110">
          <template #default="{ row }">
            <el-rate :model-value="row.difficulty" disabled size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="110" />
        <el-table-column label="标签" width="160">
          <template #default="{ row }">
            <el-tag v-for="t in splitTags(row.tags)" :key="t" size="small" type="info" style="margin-right:4px">{{ t }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <el-button link type="warning" @click="toggleFav(row)">
              <el-icon><StarFilled v-if="row.favorited" /><Star v-else /></el-icon>
            </el-button>
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" @click="openShare(row)">共享</el-button>
            <el-button link type="primary" :loading="row.aiLoading" @click="analyze(row)">AI分析</el-button>
            <el-button link type="danger" @click="removeOne(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="table-pager">
        <el-pagination background layout="total, prev, pager, next, sizes" :total="total"
          v-model:current-page="query.pageNum" v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]" @change="load" />
      </div>
    </el-card>

    <!-- 编辑对话框 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑题目' : '新增题目'" width="720px" top="6vh">
      <el-form :model="form" label-width="76px">
        <el-form-item label="题型" required>
          <el-radio-group v-model="form.type" @change="onTypeChange">
            <el-radio-button v-for="(n, i) in typeNames" :key="i" :value="i + 1">{{ n }}</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="题干" required>
          <el-input v-model="form.title" type="textarea" :rows="3" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item v-if="isChoice" label="选项" required>
          <div style="width: 100%">
            <div v-for="(opt, i) in form.options" :key="i" class="option-row">
              <el-tag>{{ letter(i) }}</el-tag>
              <el-input v-model="form.options[i]" placeholder="选项内容" />
              <el-button type="danger" link :disabled="form.options.length <= 2"
                @click="form.options.splice(i, 1)"><el-icon><Delete /></el-icon></el-button>
            </div>
            <el-button link type="primary" :disabled="form.options.length >= 6" @click="form.options.push('')">
              <el-icon><Plus /></el-icon>&nbsp;添加选项
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="参考答案" required>
          <el-radio-group v-if="form.type === 1" v-model="form.answer">
            <el-radio v-for="(opt, i) in form.options" :key="i" :value="letter(i)">{{ letter(i) }}</el-radio>
          </el-radio-group>
          <el-checkbox-group v-else-if="form.type === 2" v-model="multiAnswer">
            <el-checkbox v-for="(opt, i) in form.options" :key="i" :value="letter(i)">{{ letter(i) }}</el-checkbox>
          </el-checkbox-group>
          <el-radio-group v-else-if="form.type === 4" v-model="form.answer">
            <el-radio value="对">对</el-radio>
            <el-radio value="错">错</el-radio>
          </el-radio-group>
          <el-input v-else v-model="form.answer"
            :placeholder="form.type === 3 ? '多个空之间用 ||| 分隔' : '参考答案(按文本宽松比对)'" />
        </el-form-item>
        <el-form-item label="答案解析">
          <el-input v-model="form.analysis" type="textarea" :rows="2" placeholder="解析(可选, 建议填写)" />
        </el-form-item>
        <el-form-item label="难度" required>
          <el-select v-model="form.difficulty" style="width: 140px">
            <el-option v-for="(n, i) in difficultyNames" :key="i" :value="i + 1" :label="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" clearable style="width: 200px">
            <el-option v-for="c in flatCategories" :key="c.id" :value="c.id" :label="c.pathName" />
          </el-select>
        </el-form-item>
        <el-form-item label="知识点">
          <el-input v-model="form.tags" placeholder="多个标签用英文逗号分隔, 如: Java基础,关键字" />
        </el-form-item>
        <el-form-item label="来源">
          <el-input v-model="form.source" placeholder="题目来源(可选)" style="width: 240px" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <!-- 共享对话框 -->
    <el-dialog v-model="shareVisible" title="共享题目" width="440px">
      <el-form label-width="70px">
        <el-form-item label="方式">
          <el-radio-group v-model="shareForm.shareType">
            <el-radio :value="1">共享给用户</el-radio>
            <el-radio :value="2">公开共享</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="shareForm.shareType === 1" label="用户名">
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

    <!-- AI 分析对话框 -->
    <el-dialog v-model="aiVisible" title="AI 题目分析" width="680px">
      <div v-loading="aiLoading" class="pre-wrap" style="max-height: 480px; overflow: auto">{{ aiContent }}</div>
      <template #footer>
        <el-button @click="aiVisible = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionApi, categoryApi, favoriteApi, shareApi, aiApi } from '../api'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const difficultyNames = ['入门', '简单', '中等', '较难', '困难']

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const flatCategories = ref([])
const query = reactive({ keyword: '', categoryId: null, type: null, difficulty: null, pageNum: 1, pageSize: 10 })

const editVisible = ref(false)
const saving = ref(false)
const shareVisible = ref(false)
const sharing = ref(false)
const aiVisible = ref(false)
const aiLoading = ref(false)
const aiContent = ref('')

const multiAnswer = ref([])
const form = reactive({ id: null, type: 1, title: '', options: ['', '', '', ''], answer: '', analysis: '', difficulty: 3, categoryId: null, tags: '', source: '' })
const shareForm = reactive({ questionId: null, shareType: 1, toUsername: '', message: '' })

const isChoice = computed(() => form.type === 1 || form.type === 2)

function letter (i) { return String.fromCharCode(65 + i) }
function splitTags (tags) { return tags ? tags.split(',').filter(t => t) : [] }

function flatten (list, prefix) {
  const result = []
  for (const item of list) {
    const pathName = prefix ? prefix + ' / ' + item.name : item.name
    result.push({ id: item.id, name: item.name, pathName })
    if (item.children && item.children.length) {
      result.push(...flatten(item.children, pathName))
    }
  }
  return result
}

async function loadCategories () {
  const tree = await categoryApi.tree()
  flatCategories.value = flatten(tree, '')
}

async function load () {
  loading.value = true
  try {
    const data = await questionApi.list({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || undefined,
      type: query.type || undefined,
      difficulty: query.difficulty || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    rows.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function reset () {
  query.keyword = ''
  query.categoryId = null
  query.type = null
  query.difficulty = null
  query.pageNum = 1
  load()
}

function onTypeChange () {
  form.answer = ''
  multiAnswer.value = []
  if (!isChoice.value) form.options = ['', '', '', '']
}

function openEdit (row) {
  if (row) {
    form.id = row.id
    form.type = row.type
    form.title = row.title
    form.options = row.options && row.options.length ? [...row.options] : ['', '', '', '']
    form.answer = row.answer || ''
    form.analysis = row.analysis || ''
    form.difficulty = row.difficulty
    form.categoryId = row.categoryId
    form.tags = row.tags || ''
    form.source = row.source || ''
    multiAnswer.value = row.type === 2 ? (row.answer || '').split('') : []
  } else {
    form.id = null
    form.type = 1
    form.title = ''
    form.options = ['', '', '', '']
    form.answer = ''
    form.analysis = ''
    form.difficulty = 3
    form.categoryId = null
    form.tags = ''
    form.source = ''
    multiAnswer.value = []
  }
  editVisible.value = true
}

async function save () {
  if (!form.title.trim()) { ElMessage.warning('请输入题干'); return }
  if (form.type === 2) {
    form.answer = [...multiAnswer.value].sort().join('')
  }
  saving.value = true
  try {
    const payload = {
      id: form.id || undefined,
      type: form.type,
      title: form.title,
      options: isChoice.value ? form.options : undefined,
      answer: form.answer,
      analysis: form.analysis,
      difficulty: form.difficulty,
      categoryId: form.categoryId,
      tags: form.tags,
      source: form.source
    }
    if (form.id) {
      await questionApi.update(payload)
    } else {
      await questionApi.add(payload)
    }
    ElMessage.success('保存成功')
    editVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleFav (row) {
  const fav = await favoriteApi.toggle(row.id)
  row.favorited = fav
  ElMessage.success(fav ? '已收藏' : '已取消收藏')
}

async function removeOne (row) {
  await ElMessageBox.confirm('确定删除该题目吗? 相关收藏/共享/错题记录将一并删除。', '提示', { type: 'warning' })
  await questionApi.remove([row.id])
  ElMessage.success('删除成功')
  load()
}

function openShare (row) {
  shareForm.questionId = row.id
  shareForm.shareType = 1
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

async function analyze (row) {
  row.aiLoading = true
  aiVisible.value = true
  aiLoading.value = true
  aiContent.value = ''
  try {
    const res = await aiApi.analyzeQuestion(row.id)
    aiContent.value = res.content
  } catch (e) {
    aiVisible.value = false
  } finally {
    aiLoading.value = false
    row.aiLoading = false
  }
}

onMounted(() => {
  loadCategories()
  load()
})
</script>
