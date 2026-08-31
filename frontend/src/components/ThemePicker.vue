<template>
  <el-dialog :model-value="modelValue" title="切换界面主题" width="580px"
    append-to-body @update:model-value="$emit('update:modelValue', $event)">
    <el-alert type="info" :closable="false" style="margin-bottom: 14px"
      title="主题由管理员维护。你可以在下方选择喜欢的样式, 选择仅保存在本机浏览器。" />

    <div class="theme-list">
      <!-- 跟随管理员默认 -->
      <div class="theme-card" :class="{ active: !store.userKey }" @click="pick('')">
        <div class="theme-head">
          <span class="theme-name">跟随管理员默认</span>
          <el-tag v-if="!store.userKey" type="success" size="small">当前</el-tag>
        </div>
        <div class="theme-swatch">
          <span class="sw sw-lg" :style="{ background: swatch(store.active || DEFAULT_THEME).primary }"></span>
          <span class="sw" :style="{ background: swatch(store.active || DEFAULT_THEME).asideBg }"></span>
          <span class="sw" :style="{ background: swatch(store.active || DEFAULT_THEME).headerBg, border: '1px solid #dcdfe6' }"></span>
          <span class="sw" :style="{ background: swatch(store.active || DEFAULT_THEME).pageBg, border: '1px solid #dcdfe6' }"></span>
          <span class="sw-gradient"
            :style="{ background: 'linear-gradient(135deg, ' + swatch(store.active || DEFAULT_THEME).loginFrom + ',' + swatch(store.active || DEFAULT_THEME).loginTo + ')' }"></span>
        </div>
        <div class="theme-desc">使用管理员设置的全局样式({{ (store.active || DEFAULT_THEME).name }})</div>
      </div>

      <!-- 启用的主题 -->
      <div v-for="t in store.enabled" :key="t.themeKey" class="theme-card"
        :class="{ active: store.userKey === t.themeKey }" @click="pick(t.themeKey)">
        <div class="theme-head">
          <span class="theme-name">{{ t.name }}</span>
          <el-tag v-if="store.userKey === t.themeKey" type="success" size="small">当前</el-tag>
          <el-tag v-else-if="t.isDefault === 1" type="info" size="small">默认</el-tag>
        </div>
        <div class="theme-swatch">
          <span class="sw sw-lg" :style="{ background: swatch(t).primary }"></span>
          <span class="sw" :style="{ background: swatch(t).asideBg }"></span>
          <span class="sw" :style="{ background: swatch(t).headerBg, border: '1px solid #dcdfe6' }"></span>
          <span class="sw" :style="{ background: swatch(t).pageBg, border: '1px solid #dcdfe6' }"></span>
          <span class="sw-gradient"
            :style="{ background: 'linear-gradient(135deg, ' + swatch(t).loginFrom + ',' + swatch(t).loginTo + ')' }"></span>
        </div>
        <div class="theme-desc">{{ t.themeKey }}</div>
      </div>
    </div>

    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ElMessage } from 'element-plus'
import { useThemeStore, DEFAULT_THEME, parseThemeConfig } from '../stores/theme'

defineProps({
  modelValue: { type: Boolean, default: false }
})
defineEmits(['update:modelValue'])

const store = useThemeStore()

function swatch (theme) {
  return parseThemeConfig(theme ? theme.config : null)
}

function pick (key) {
  const t = store.switchTo(key)
  ElMessage.success(key ? '已切换为「' + t.name + '」' : '已跟随管理员默认样式')
}
</script>

<style scoped>
.theme-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
  max-height: 380px;
  overflow: auto;
}

.theme-card {
  border: 1px solid #dcdfe6;
  border-radius: 8px;
  padding: 12px;
  cursor: pointer;
  transition: border-color 0.15s, box-shadow 0.15s;
}

.theme-card:hover {
  border-color: var(--q-primary);
}

.theme-card.active {
  border-color: var(--q-primary);
  box-shadow: 0 0 0 2px rgba(64, 158, 255, 0.15);
}

.theme-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.theme-name {
  font-size: 14px;
  font-weight: 600;
  color: #303133;
}

.theme-swatch {
  display: flex;
  gap: 6px;
  align-items: center;
  margin-bottom: 8px;
}

.sw {
  width: 22px;
  height: 22px;
  border-radius: 4px;
  display: inline-block;
}

.sw-lg {
  width: 34px;
}

.sw-gradient {
  flex: 1;
  height: 22px;
  border-radius: 4px;
  display: inline-block;
}

.theme-desc {
  color: #909399;
  font-size: 12px;
}

@media (max-width: 640px) {
  .theme-list {
    grid-template-columns: 1fr;
  }
}
</style>
