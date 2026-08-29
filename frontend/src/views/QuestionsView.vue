<template>
  <div class="questions-page">
    <!-- 左侧题库栏 -->
    <el-card class="bank-aside" shadow="never">
      <template #header><b>题库</b></template>
      <div v-loading="bankLoading" class="bank-list">
        <div class="bank-item" :class="{ active: !query.bankId }" @click="selectBank(null)">
          <el-icon class="bank-icon"><Files /></el-icon>
          <span class="bank-name">全部题目</span>
        </div>
        <div v-for="b in banks" :key="b.id" class="bank-item" :class="{ active: query.bankId === b.id }"
          @click="selectBank(b.id)">
          <span class="bank-name" :title="b.name">{{ b.name }}</span>
          <el-tag size="small" type="info">{{ b.questionCount || 0 }}</el-tag>
          <span class="bank-ops" @click.stop>
            <el-dropdown trigger="click" @command="(cmd) => onBankCommand(cmd, b)">
              <el-icon class="bank-more"><MoreFilled /></el-icon>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="edit">编辑</el-dropdown-item>
                  <el-dropdown-item command="share">共享</el-dropdown-item>
                  <el-dropdown-item command="delete" divided>删除</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </span>
        </div>
        <div v-if="!bankLoading && banks.length === 0" class="bank-empty">暂无题库, 点击下方按钮创建</div>
      </div>
      <el-button type="success" plain class="bank-add" @click="openBankEdit()">
        <el-icon><Plus /></el-icon>&nbsp;新建题库
      </el-button>
    </el-card>

    <!-- 右侧题目区 -->
    <el-card class="page-card main-card">
      <div class="filter-bar">
        <el-input v-model="query.keyword" placeholder="搜索题干/标签" clearable style="width: 180px" @keyup.enter="load" />
        <el-select v-model="query.parentCategoryId" placeholder="一级分类" clearable style="width: 120px" @change="onPrimaryCategoryChange">
          <el-option v-for="c in primaryCategories" :key="c.id" :value="c.id" :label="c.name" />
        </el-select>
        <el-select v-model="query.categoryId" placeholder="二级分类" clearable style="width: 120px" :disabled="!query.parentCategoryId">
          <el-option v-for="c in secondaryCategories" :key="c.id" :value="c.id" :label="c.name" />
        </el-select>
        <!-- 题库筛选由左侧题库栏承担, 此处不再重复 -->
        <el-select v-model="query.type" placeholder="题型" clearable style="width: 110px">
          <el-option v-for="(n, i) in typeNames" :key="i" :value="i + 1" :label="n" />
        </el-select>
        <el-select v-model="query.difficulty" placeholder="难度" clearable style="width: 110px">
          <el-option v-for="(n, i) in difficultyNames" :key="i" :value="i + 1" :label="n" />
        </el-select>
        <el-select v-if="userStore.isAdmin" v-model="query.userId" placeholder="全部用户" clearable filterable style="width: 130px">
          <el-option v-for="u in users" :key="u.id" :value="u.id" :label="u.nickname || u.username" />
        </el-select>
        <el-button type="primary" @click="load"><el-icon><Search /></el-icon>&nbsp;搜索</el-button>
        <el-button @click="reset">重置</el-button>
        <div style="flex: 1"></div>
        <el-button type="success" @click="openEdit()"><el-icon><Plus /></el-icon>&nbsp;新增题目</el-button>
        <el-button type="warning" plain @click="importVisible = true"><el-icon><Upload /></el-icon>&nbsp;批量导入</el-button>
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
        <el-table-column label="题库" width="130">
          <template #default="{ row }">{{ row.bankName || '-' }}</template>
        </el-table-column>
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

    <!-- 批量导入 -->
    <ImportDialog v-model="importVisible" @imported="load" />

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
        <el-form-item label="题库">
          <el-select v-model="form.bankId" placeholder="选择题库" clearable style="width: 200px">
            <el-option v-for="b in banks" :key="b.id" :value="b.id" :label="b.name" />
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
        <el-form-item label="权限">
          <div>
            <el-radio-group v-model="shareForm.permission" :disabled="shareForm.shareType === 2">
              <el-radio :value="1">只读</el-radio>
              <el-radio :value="2">可编辑</el-radio>
            </el-radio-group>
            <div v-if="shareForm.shareType === 2" class="share-perm-tip">公开共享固定为只读</div>
          </div>
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

    <!-- 新建/编辑题库对话框 (移植自 BanksView) -->
    <el-dialog v-model="bankEditVisible" :title="bankForm.id ? '编辑题库' : '新建题库'" width="460px">
      <el-form :model="bankForm" label-width="80px">
        <el-form-item label="题库名称" required>
          <el-input v-model="bankForm.name" maxlength="50" show-word-limit />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="bankForm.description" type="textarea" :rows="3" maxlength="500" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bankEditVisible = false">取消</el-button>
        <el-button type="primary" :loading="bankSaving" @click="saveBank">保存</el-button>
      </template>
    </el-dialog>

    <!-- 题库共享对话框 (移植自 BanksView) -->
    <el-dialog v-model="bankShareVisible" title="共享题库" width="440px">
      <el-form label-width="70px">
        <el-form-item label="方式">
          <el-radio-group v-model="bankShareForm.shareType">
            <el-radio :value="3">共享给用户</el-radio>
            <el-radio :value="4">公开共享</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="bankShareForm.shareType === 3" label="用户名">
          <el-input v-model="bankShareForm.toUsername" placeholder="对方的用户名" />
        </el-form-item>
        <el-form-item label="权限">
          <div>
            <el-radio-group v-model="bankShareForm.permission" :disabled="bankShareForm.shareType === 4">
              <el-radio :value="1">只读</el-radio>
              <el-radio :value="2">可编辑</el-radio>
            </el-radio-group>
            <div v-if="bankShareForm.shareType === 4" class="share-perm-tip">公开共享固定为只读</div>
          </div>
        </el-form-item>
        <el-form-item label="留言">
          <el-input v-model="bankShareForm.message" maxlength="200" placeholder="留言(可选)" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="bankShareVisible = false">取消</el-button>
        <el-button type="primary" :loading="bankSharing" @click="doBankShare">确定共享</el-button>
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
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { questionApi, bankApi, favoriteApi, shareApi, userApi } from '../api'
import { useUserStore } from '../stores/user'
import { useCategoryStore } from '../stores/categories'
import { aiChat, buildQuestionPrompt, pushAiHistory, hasApiKey } from '../utils/ai'
import ImportDialog from '../components/ImportDialog.vue'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const difficultyNames = ['入门', '简单', '中等', '较难', '困难']

const userStore = useUserStore()
const categoryStore = useCategoryStore()

const rows = ref([])
const total = ref(0)
const loading = ref(false)
const flatCategories = computed(() => categoryStore.flat)
const query = reactive({ keyword: '', parentCategoryId: null, categoryId: null, bankId: null, type: null, difficulty: null, userId: null, pageNum: 1, pageSize: 10 })
const banks = ref([])
const bankLoading = ref(false)
const users = ref([])
const route = useRoute()
const router = useRouter()
const importVisible = ref(false)

// 题库栏: 新建/编辑/共享 (移植自 BanksView)
const bankEditVisible = ref(false)
const bankSaving = ref(false)
const bankShareVisible = ref(false)
const bankSharing = ref(false)
const bankForm = reactive({ id: null, name: '', description: '' })
const bankShareForm = reactive({ bankId: null, shareType: 3, toUsername: '', permission: 1, message: '' })

const editVisible = ref(false)
const saving = ref(false)
const shareVisible = ref(false)
const sharing = ref(false)
const aiVisible = ref(false)
const aiLoading = ref(false)
const aiContent = ref('')

const multiAnswer = ref([])
const form = reactive({ id: null, type: 1, title: '', options: ['', '', '', ''], answer: '', analysis: '', difficulty: 3, categoryId: null, bankId: null, tags: '', source: '' })
const shareForm = reactive({ questionId: null, shareType: 1, toUsername: '', permission: 1, message: '' })

const isChoice = computed(() => form.type === 1 || form.type === 2)

function letter (i) { return String.fromCharCode(65 + i) }
function splitTags (tags) { return tags ? tags.split(',').filter(t => t) : [] }

function flatten (list, prefix) {
  const result = []
  for (const item of list) {
    const pathName = prefix ? prefix + ' / ' + item.name : item.name
    result.push({ id: item.id, name: item.name, pathName, parentId: item.parentId || 0 })
    if (item.children && item.children.length) {
      result.push(...flatten(item.children, pathName))
    }
  }
  return result
}

const primaryCategories = computed(() => flatCategories.value.filter(c => c.parentId === 0))
const secondaryCategories = computed(() => flatCategories.value.filter(c => c.parentId !== 0 && c.parentId === query.parentCategoryId))

async function loadBanks () {
  bankLoading.value = true
  try {
    banks.value = await bankApi.list()
  } finally {
    bankLoading.value = false
  }
}

async function loadUsers () {
  if (!userStore.isAdmin) return
  try {
    const data = await userApi.list({ pageNum: 1, pageSize: 200 })
    users.value = data.list || []
  } catch (e) {
    users.value = []
  }
}

async function load () {
  loading.value = true
  try {
    const data = await questionApi.list({
      keyword: query.keyword || undefined,
      categoryId: query.categoryId || query.parentCategoryId || undefined,
      bankId: query.bankId || undefined,
      type: query.type || undefined,
      difficulty: query.difficulty || undefined,
      userId: userStore.isAdmin ? (query.userId || undefined) : undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    rows.value = data.list
    total.value = Number(data.total)
  } finally {
    loading.value = false
  }
}

function selectBank (id) {
  query.bankId = id ? Number(id) : null
  query.pageNum = 1
  load()
}

function reset () {
  query.keyword = ''
  query.parentCategoryId = null
  query.categoryId = null
  query.bankId = null
  query.type = null
  query.difficulty = null
  query.userId = null
  query.pageNum = 1
  load()
}

function onPrimaryCategoryChange () {
  query.categoryId = null
  query.pageNum = 1
  load()
}

// ==================== 题库栏操作 (移植自 BanksView) ====================
function onBankCommand (cmd, bank) {
  if (cmd === 'edit') {
    openBankEdit(bank)
  } else if (cmd === 'share') {
    openBankShare(bank)
  } else if (cmd === 'delete') {
    removeBank(bank)
  }
}

function openBankEdit (row) {
  if (row) {
    bankForm.id = row.id
    bankForm.name = row.name
    bankForm.description = row.description || ''
  } else {
    bankForm.id = null
    bankForm.name = ''
    bankForm.description = ''
  }
  bankEditVisible.value = true
}

async function saveBank () {
  if (!bankForm.name.trim()) { ElMessage.warning('请输入题库名称'); return }
  bankSaving.value = true
  try {
    if (bankForm.id) {
      await bankApi.update({ id: bankForm.id, name: bankForm.name, description: bankForm.description })
    } else {
      await bankApi.add({ name: bankForm.name, description: bankForm.description })
    }
    ElMessage.success('保存成功')
    bankEditVisible.value = false
    loadBanks()
  } finally {
    bankSaving.value = false
  }
}

async function removeBank (bank) {
  await ElMessageBox.confirm(
    '确定删除题库「' + bank.name + '」吗? 库内 ' + (bank.questionCount || 0) + ' 道题将保留但不再归属任何题库。',
    '提示', { type: 'warning' })
  await bankApi.remove(bank.id)
  ElMessage.success('删除成功')
  if (query.bankId === bank.id) {
    query.bankId = null
    query.pageNum = 1
  }
  loadBanks()
  load()
}

function openBankShare (bank) {
  bankShareForm.bankId = bank.id
  bankShareForm.shareType = 3
  bankShareForm.toUsername = ''
  bankShareForm.permission = 1
  bankShareForm.message = ''
  bankShareVisible.value = true
}

async function doBankShare () {
  bankSharing.value = true
  try {
    await shareApi.share(bankShareForm)
    ElMessage.success('共享成功')
    bankShareVisible.value = false
  } finally {
    bankSharing.value = false
  }
}

// ==================== 题目操作 ====================
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
    form.bankId = row.bankId
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
    form.bankId = query.bankId || null
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
      bankId: form.bankId,
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
    loadBanks()
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
  loadBanks()
}

function openShare (row) {
  shareForm.questionId = row.id
  shareForm.shareType = 1
  shareForm.toUsername = ''
  shareForm.permission = 1
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
  if (!hasApiKey()) {
    ElMessage.warning('尚未配置 AI, 请先到「AI 设置」填写 API Key')
    router.push('/ai-settings')
    return
  }
  row.aiLoading = true
  aiVisible.value = true
  aiLoading.value = true
  aiContent.value = ''
  try {
    const content = await aiChat(buildQuestionPrompt({
      type: row.type, difficulty: row.difficulty, tags: row.tags,
      title: row.title, options: row.options, answer: row.answer, analysis: row.analysis
    }))
    aiContent.value = content
    pushAiHistory({ type: 'question', title: row.title.substring(0, 40), content })
  } catch (e) {
    ElMessage.error(e.message || 'AI 调用失败')
    aiVisible.value = false
  } finally {
    aiLoading.value = false
    row.aiLoading = false
  }
}

onMounted(async () => {
  await categoryStore.fetchTree()
  loadBanks()
  loadUsers()
  if (route.query.bankId) {
    selectBank(Number(route.query.bankId))
  }
  if (route.query.categoryId) {
    // 分类管理跳转联动: 按分类过滤(一级设 parentCategoryId, 二级两者都设)
    const c = flatCategories.value.find(x => x.id === Number(route.query.categoryId))
    if (c) {
      if (c.parentId === 0) {
        query.parentCategoryId = c.id
      } else {
        query.parentCategoryId = c.parentId
        query.categoryId = c.id
      }
      load()
      return
    }
  }
  load()
})
</script>

<style scoped>
.questions-page {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.bank-aside {
  width: 230px;
  flex-shrink: 0;
}

.bank-aside :deep(.el-card__body) {
  padding: 12px;
}

.main-card {
  flex: 1;
  min-width: 0;
}

.bank-list {
  min-height: 120px;
}

.bank-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  border-radius: 6px;
  cursor: pointer;
  margin-bottom: 2px;
}

.bank-item:hover {
  background: #f5f7fa;
}

.bank-item.active {
  background: #ecf5ff;
  color: #409eff;
}

.bank-item .bank-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 13px;
}

.bank-icon {
  color: #409eff;
}

.bank-ops {
  display: flex;
  opacity: 0;
  transition: opacity 0.15s;
}

.bank-item:hover .bank-ops {
  opacity: 1;
}

.bank-more {
  color: #909399;
  cursor: pointer;
}

.bank-empty {
  color: #909399;
  font-size: 13px;
  text-align: center;
  padding: 20px 0;
}

.bank-add {
  width: 100%;
  margin-top: 10px;
}

.share-perm-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
