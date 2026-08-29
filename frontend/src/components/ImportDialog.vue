<template>
  <el-dialog v-model="visible" title="批量导入题目" width="860px" :close-on-click-modal="false" @closed="onClosed">
    <el-steps :active="step" finish-status="success" align-center style="margin-bottom: 18px">
      <el-step title="上传文件" />
      <el-step title="预览确认" />
      <el-step title="导入结果" />
    </el-steps>

    <!-- 第一步: 上传 -->
    <div v-if="step === 0">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
        title="支持 .xlsx / .csv, 第一列题干、第二列题型为必填。建议先下载模板按格式填写。">
        <template #default>
          <el-link type="primary" :underline="false" @click="downloadTemplate">下载 Excel 导入模板</el-link>
        </template>
      </el-alert>
      <el-form label-width="90px">
        <el-form-item label="归属题库">
          <el-select v-model="bankId" placeholder="不归属(可选)" clearable style="width: 260px">
            <el-option v-for="b in banks" :key="b.id" :value="b.id" :label="b.name" />
          </el-select>
        </el-form-item>
        <el-form-item label="归属分类">
          <el-select v-model="categoryId" placeholder="不归属(可选)" clearable style="width: 260px">
            <el-option v-for="c in flatCategories" :key="c.id" :value="c.id" :label="c.pathName" />
          </el-select>
        </el-form-item>
        <el-form-item label="文件">
          <el-upload ref="uploadRef" drag accept=".xlsx,.xls,.csv" :limit="1"
            :auto-upload="false" :on-change="onFileChange" :on-exceed="onExceed">
            <div style="padding: 14px 0">
              <el-icon style="font-size: 34px; color: #909399"><UploadFilled /></el-icon>
              <div>拖拽文件到此处, 或点击选择文件</div>
              <div class="sub">仅支持 .xlsx / .xls / .csv, 单次建议不超过 500 行</div>
            </div>
          </el-upload>
        </el-form-item>
      </el-form>
      <div class="dialog-footer">
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :disabled="!file" :loading="parsing" @click="doParse">上传并解析</el-button>
      </div>
    </div>

    <!-- 第二步: 预览 -->
    <div v-else-if="step === 1">
      <el-alert v-if="errorCount" type="warning" :closable="false" style="margin-bottom: 12px"
        :title="'共解析 ' + rows.length + ' 行, 其中 ' + errorCount + ' 行存在问题(表格中标红)。导入时将自动跳过这些行。'" />
      <el-alert v-else type="success" :closable="false" style="margin-bottom: 12px"
        :title="'共解析 ' + rows.length + ' 行, 全部校验通过'" />
      <el-table :data="rows" max-height="380" size="small" border
        :row-class-name="(r) => r.row.errors && r.row.errors.length ? 'row-error' : ''">
        <el-table-column prop="rowNo" label="行" width="50" />
        <el-table-column prop="title" label="题干" min-width="220" show-overflow-tooltip />
        <el-table-column label="题型" width="80">
          <template #default="{ row }">{{ typeNames[(row.type || 1) - 1] || row.typeName }}</template>
        </el-table-column>
        <el-table-column prop="answer" label="答案" width="110" show-overflow-tooltip />
        <el-table-column prop="analysis" label="解析" min-width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.analysis || '-' }}</template>
        </el-table-column>
        <el-table-column label="问题" min-width="170">
          <template #default="{ row }">
            <el-tag v-for="(e, i) in row.errors" :key="i" type="danger" size="small" style="margin: 2px">{{ e }}</el-tag>
            <span v-if="!row.errors || !row.errors.length" style="color: #67c23a">✓</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="dialog-footer">
        <el-button @click="step = 0">上一步</el-button>
        <el-button type="primary" :loading="saving" :disabled="!rows.length" @click="doSave">确认导入 {{ okCount }} 行</el-button>
      </div>
    </div>

    <!-- 第三步: 结果 -->
    <div v-else>
      <el-result icon="success" :title="'成功导入 ' + result.successCount + ' 道题'">
        <template #sub-title>
          <div v-if="result.failures && result.failures.length">
            <p>以下 {{ result.failures.length }} 行导入失败:</p>
            <div v-for="(f, i) in result.failures" :key="i" class="fail-line">
              第 {{ f.rowNo }} 行: {{ f.reason }}
            </div>
          </div>
          <p v-else>题目已全部入库</p>
        </template>
      </el-result>
      <div class="dialog-footer">
        <el-button type="primary" @click="finish">完成</el-button>
      </div>
    </div>
  </el-dialog>
</template>

<script setup>
import { ref, computed, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { questionApi, bankApi, categoryApi } from '../api'

const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
const emit = defineEmits(['imported'])

const visible = defineModel({ default: false })
const step = ref(0)
const file = ref(null)
const rows = ref([])
const result = ref({})
const parsing = ref(false)
const saving = ref(false)
const bankId = ref(null)
const categoryId = ref(null)
const banks = ref([])
const flatCategories = ref([])
const uploadRef = ref()

const errorCount = computed(() => rows.value.filter((r) => r.errors && r.errors.length).length)
const okCount = computed(() => rows.value.length - errorCount.value)

watch(visible, (v) => {
  if (v) {
    step.value = 0
    file.value = null
    rows.value = []
    bankId.value = null
    categoryId.value = null
    if (!banks.value.length) {
      bankApi.list().then((list) => { banks.value = list })
    }
    if (!flatCategories.value.length) {
      categoryApi.tree().then((tree) => { flatCategories.value = flatten(tree, '') })
    }
  }
})

function flatten (list, prefix) {
  const out = []
  for (const item of list) {
    const pathName = prefix ? prefix + ' / ' + item.name : item.name
    out.push({ id: item.id, name: item.name, pathName })
    if (item.children && item.children.length) out.push(...flatten(item.children, pathName))
  }
  return out
}

function onFileChange (f) {
  file.value = f.raw || null
}

function onExceed () {
  ElMessage.warning('只能选择一个文件, 请先移除已选文件')
}

async function downloadTemplate () {
  const blob = await questionApi.importTemplate()
  const url = URL.createObjectURL(new Blob([blob], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' }))
  const a = document.createElement('a')
  a.href = url
  a.download = 'question-import-template.xlsx'
  a.click()
  URL.revokeObjectURL(url)
}

async function doParse () {
  parsing.value = true
  try {
    const fd = new FormData()
    fd.append('file', file.value)
    rows.value = await questionApi.importParse(fd)
    if (!rows.value.length) {
      ElMessage.warning('未解析到有效数据行')
      return
    }
    step.value = 1
  } finally {
    parsing.value = false
  }
}

async function doSave () {
  saving.value = true
  try {
    const valid = rows.value.filter((r) => !r.errors || !r.errors.length)
    result.value = await questionApi.importSave({
      rows: valid,
      categoryId: categoryId.value,
      bankId: bankId.value
    })
    step.value = 2
  } finally {
    saving.value = false
  }
}

function finish () {
  visible.value = false
  emit('imported')
}

function onClosed () {
  if (uploadRef.value) uploadRef.value.clearFiles()
  file.value = null
  rows.value = []
}
</script>

<style scoped>
.dialog-footer {
  margin-top: 16px;
  text-align: right;
}

.sub {
  color: #909399;
  font-size: 12px;
  margin-top: 6px;
}

:deep(.row-error) {
  background: #fef0f0 !important;
}

.fail-line {
  color: #f56c6c;
  font-size: 12px;
  line-height: 1.8;
}
</style>
