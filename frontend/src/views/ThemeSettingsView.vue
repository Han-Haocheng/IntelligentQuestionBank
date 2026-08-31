<template>
  <div>
    <el-card class="page-card">
      <div class="filter-bar">
        <el-alert type="info" :closable="false" style="flex: 1"
          title="管理多套前端样式: 可新增/编辑/启停/删除主题, 设为默认即全局生效; 所有用户可在右上角「切换界面主题」选择启用的样式。" />
        <el-button type="primary" @click="openAdd"><el-icon><Plus /></el-icon>&nbsp;新增主题</el-button>
      </div>

      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="name" label="主题名称" width="140" />
        <el-table-column label="标识" width="130">
          <template #default="{ row }"><code class="key-code">{{ row.themeKey }}</code></template>
        </el-table-column>
        <el-table-column label="样式预览">
          <template #default="{ row }">
            <span class="sw" :style="{ background: cfg(row).primary }" :title="'主色 ' + cfg(row).primary"></span>
            <span class="sw" :style="{ background: cfg(row).asideBg }" :title="'侧栏 ' + cfg(row).asideBg"></span>
            <span class="sw" :style="{ background: cfg(row).headerBg, border: '1px solid #dcdfe6' }" :title="'顶栏 ' + cfg(row).headerBg"></span>
            <span class="sw" :style="{ background: cfg(row).pageBg, border: '1px solid #dcdfe6' }" :title="'页面背景 ' + cfg(row).pageBg"></span>
            <span class="sw-gradient"
              :style="{ background: 'linear-gradient(135deg, ' + cfg(row).loginFrom + ',' + cfg(row).loginTo + ')' }"
              :title="'登录页渐变 ' + cfg(row).loginFrom + ' → ' + cfg(row).loginTo"></span>
          </template>
        </el-table-column>
        <el-table-column label="启用" width="80" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.enabled === 1" @change="v => toggleStatus(row, v)" />
          </template>
        </el-table-column>
        <el-table-column label="全局默认" width="90" align="center">
          <template #default="{ row }">
            <el-radio :model-value="defaultId" :value="row.id" @change="setDefault(row)">默认</el-radio>
          </template>
        </el-table-column>
        <el-table-column prop="updateTime" label="更新时间" width="180" />
        <el-table-column label="操作" width="110" fixed="right">
          <template #default="{ row }">
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

    <!-- 新增/编辑主题 -->
    <el-dialog v-model="editVisible" :title="form.id ? '编辑主题' : '新增主题'" width="760px"
      append-to-body :close-on-click-modal="false">
      <el-form :model="form" label-width="92px">
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主题名称">
              <el-input v-model="form.name" maxlength="20" placeholder="如: 默认蓝" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="主题标识">
              <el-input v-model="form.themeKey" :disabled="!!form.id"
                placeholder="字母开头, 如 my-blue" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="预设模板">
          <el-select v-model="presetKey" placeholder="选择模板快速填充颜色" style="width: 100%" @change="applyPreset">
            <el-option v-for="p in presets" :key="p.themeKey" :value="p.themeKey" :label="'加载内置模板: ' + p.name" />
          </el-select>
        </el-form-item>

        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="主色"><el-color-picker v-model="form.config.primary" /></el-form-item>
            <el-form-item label="页面背景"><el-color-picker v-model="form.config.pageBg" /></el-form-item>
            <el-form-item label="卡片背景"><el-color-picker v-model="form.config.cardBg" /></el-form-item>
            <el-form-item label="顶栏背景"><el-color-picker v-model="form.config.headerBg" /></el-form-item>
            <el-form-item label="顶栏文字"><el-color-picker v-model="form.config.headerText" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="侧栏背景"><el-color-picker v-model="form.config.asideBg" /></el-form-item>
            <el-form-item label="侧栏文字"><el-color-picker v-model="form.config.asideText" /></el-form-item>
            <el-form-item label="侧栏高亮"><el-color-picker v-model="form.config.asideActive" /></el-form-item>
            <el-form-item label="登录页起色"><el-color-picker v-model="form.config.loginFrom" /></el-form-item>
            <el-form-item label="登录页止色"><el-color-picker v-model="form.config.loginTo" /></el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="圆角">
          <el-input-number v-model="form.config.radius" :min="0" :max="20" />
          <span class="tip">px(按钮/输入框等组件圆角)</span>
        </el-form-item>

        <el-form-item label="启用">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
          <span class="tip">停用的主题用户不可见; 停用/删除当前默认主题时, 默认会自动移交给其他启用主题</span>
        </el-form-item>

        <el-form-item label="效果预览">
          <div class="preview">
            <div class="preview-aside" :style="{ background: form.config.asideBg, color: form.config.asideText }">
              <div class="preview-logo" :style="{ color: form.config.asideActive }">题库管理</div>
              <div class="preview-menu">统计看板</div>
              <div class="preview-menu active" :style="{ color: form.config.asideActive }">题目管理</div>
            </div>
            <div class="preview-right">
              <div class="preview-header" :style="{ background: form.config.headerBg, color: form.config.headerText }">
                <span>顶部栏</span>
              </div>
              <div class="preview-body" :style="{ background: form.config.pageBg }">
                <span class="preview-btn" :style="{ background: form.config.primary }">主要按钮</span>
                <span class="preview-card" :style="{ background: form.config.cardBg }">内容卡片</span>
              </div>
            </div>
          </div>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { themeApi } from '../api'
import { confirmAction } from '../utils/confirm'
import { parseThemeConfig, DEFAULT_THEME } from '../stores/theme'
import { useThemeStore } from '../stores/theme'

const store = useThemeStore()
const rows = ref([])
const loading = ref(false)
const defaultId = ref(null)
const editVisible = ref(false)
const saving = ref(false)
const presetKey = ref('')

// 内置预设模板(与 db 种子一致), 供快速填充颜色
const presets = [
  { name: '默认蓝', themeKey: 'default', config: DEFAULT_THEME.config },
  {
    name: '暗夜深蓝', themeKey: 'dark',
    config: { primary: '#1668dc', pageBg: '#0f1420', cardBg: '#1a2233', headerBg: '#1a2233', headerText: '#e6e8eb', asideBg: '#0a0f18', asideText: '#8a94a6', asideActive: '#ffffff', loginFrom: '#0b1e3d', loginTo: '#1668dc', radius: 8 }
  },
  {
    name: '清新绿', themeKey: 'green',
    config: { primary: '#18a058', pageBg: '#f2f8f4', cardBg: '#ffffff', headerBg: '#ffffff', headerText: '#303133', asideBg: '#0f2e1e', asideText: '#9dc8b0', asideActive: '#ffffff', loginFrom: '#0f7a4d', loginTo: '#18a058', radius: 6 }
  }
]

const form = reactive({
  id: null,
  name: '',
  themeKey: '',
  config: { ...DEFAULT_THEME.config, radius: Number(DEFAULT_THEME.config.radius) },
  enabled: 1
})

function cfg (row) {
  return parseThemeConfig(row.config)
}

async function load () {
  loading.value = true
  try {
    rows.value = await themeApi.list()
    const def = rows.value.find(t => t.isDefault === 1)
    defaultId.value = def ? def.id : null
  } finally {
    loading.value = false
  }
}

function openAdd () {
  Object.assign(form, { id: null, name: '', themeKey: '', config: { ...DEFAULT_THEME.config, radius: Number(DEFAULT_THEME.config.radius) }, enabled: 1 })
  presetKey.value = ''
  editVisible.value = true
}

function openEdit (row) {
  Object.assign(form, {
    id: row.id,
    name: row.name,
    themeKey: row.themeKey,
    config: { ...parseThemeConfig(row.config), radius: Number(parseThemeConfig(row.config).radius) },
    enabled: row.enabled
  })
  presetKey.value = ''
  editVisible.value = true
}

function applyPreset () {
  const p = presets.find(x => x.themeKey === presetKey.value)
  if (p) Object.assign(form.config, { ...p.config, radius: Number(p.config.radius) })
}

async function save () {
  const name = form.name.trim()
  if (!name) { ElMessage.warning('请填写主题名称'); return }
  if (name.length > 20) { ElMessage.warning('主题名称不超过20个字符'); return }
  if (!form.id && !/^[a-zA-Z][a-zA-Z0-9_-]{0,49}$/.test(form.themeKey)) {
    ElMessage.warning('主题标识须以字母开头, 由字母/数字/_/-组成(最多50位)')
    return
  }
  saving.value = true
  try {
    const payload = {
      id: form.id || undefined,
      name,
      themeKey: form.themeKey.trim(),
      config: JSON.stringify(form.config),
      enabled: form.enabled
    }
    if (form.id) {
      await themeApi.update(payload)
      ElMessage.success('已保存')
    } else {
      await themeApi.add(payload)
      ElMessage.success('新增成功')
    }
    editVisible.value = false
    await load()
    // 同步本地主题缓存与个人选择校验
    store.loadEnabled()
    store.boot()
  } finally {
    saving.value = false
  }
}

async function toggleStatus (row, enabled) {
  await themeApi.updateStatus(row.id, enabled ? 1 : 0)
  ElMessage.success(enabled ? '已启用' : '已停用')
  await load()
  store.loadEnabled()
}

async function setDefault (row) {
  await themeApi.setDefault(row.id)
  ElMessage.success('「' + row.name + '」已设为全局默认样式')
  await load()
  store.boot()
}

async function remove (row) {
  if (!(await confirmAction('确定删除主题「' + row.name + '」吗?'))) return
  await themeApi.remove(row.id)
  ElMessage.success('删除成功')
  await load()
  store.loadEnabled()
  store.boot()
}

onMounted(load)
</script>

<style scoped>
.key-code {
  font-family: monospace;
  font-size: 12px;
  color: #409eff;
}

.sw {
  width: 20px;
  height: 20px;
  border-radius: 4px;
  display: inline-block;
  vertical-align: middle;
  margin-right: 6px;
}

.sw-gradient {
  width: 60px;
  height: 20px;
  border-radius: 4px;
  display: inline-block;
  vertical-align: middle;
}

.tip {
  margin-left: 10px;
  color: #909399;
  font-size: 12px;
}

.preview {
  display: flex;
  width: 100%;
  height: 140px;
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  overflow: hidden;
}

.preview-aside {
  width: 90px;
  padding: 10px 8px;
  font-size: 12px;
  flex-shrink: 0;
}

.preview-logo {
  font-weight: 600;
  margin-bottom: 10px;
}

.preview-menu {
  padding: 4px 6px;
  margin-bottom: 4px;
  border-radius: 4px;
}

.preview-menu.active {
  background: rgba(255, 255, 255, 0.12);
}

.preview-right {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.preview-header {
  padding: 8px 12px;
  font-size: 12px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.06);
}

.preview-body {
  flex: 1;
  padding: 10px;
  display: flex;
  gap: 8px;
  align-items: flex-start;
}

.preview-btn {
  color: #fff;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 4px;
}

.preview-card {
  font-size: 12px;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #dcdfe6;
}
</style>
