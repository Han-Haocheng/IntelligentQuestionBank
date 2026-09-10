<template>
  <el-container class="layout">
    <el-aside :style="{ width: collapsed ? '88px' : '280px' }" class="aside">
      <div class="logo">
        <Transition name="logo-fade" mode="out-in">
          <span :key="collapsed ? 'mini' : 'full'">{{ collapsed ? '题库' : '题库管理系统' }}</span>
        </Transition>
      </div>
      <el-menu :default-active="route.path" router :collapse="collapsed" class="menu">
        <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon><template #title>统计看板</template></el-menu-item>
        <el-menu-item index="/questions"><el-icon><Document /></el-icon><template #title>题目管理</template></el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/admin"><el-icon><Setting /></el-icon><template #title>管理</template></el-menu-item>
        <el-menu-item index="/practice"><el-icon><EditPen /></el-icon><template #title>练习</template></el-menu-item>
        <el-menu-item index="/my"><el-icon><Star /></el-icon><template #title>我的</template></el-menu-item>
      </el-menu>
      <!-- 侧栏左下角: 折叠/展开按钮(MD3 图标按钮: 40px 圆形 state-layer) -->
      <div class="aside-footer" :class="{ 'footer-center': collapsed }">
        <el-tooltip :content="collapsed ? '展开侧栏' : '折叠侧栏'" placement="right">
          <span class="icon-btn" @click="toggleCollapse">
            <el-icon><Expand v-if="collapsed" /><Fold v-else /></el-icon>
          </span>
        </el-tooltip>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">{{ route.meta.title || '智能题库' }}</span>
        <el-dropdown @command="onCommand">
          <span class="user-chip">
            <el-avatar :size="30" :style="{ background: css.primary }">{{ initials }}</el-avatar>
            <span class="username">{{ store.userInfo ? store.userInfo.nickname || store.userInfo.username : '' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item command="theme">切换界面主题</el-dropdown-item>
              <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>
      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <!-- 切换界面主题(由管理员维护多套样式) -->
  <ThemePicker v-model="themeVisible" />
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import { useThemeStore } from '../stores/theme'
import ThemePicker from '../components/ThemePicker.vue'

const route = useRoute()
const router = useRouter()
const store = useUserStore()
const themeStore = useThemeStore()

// 主题 CSS 变量(供模板取色)
const css = computed(() => themeStore.css)
const themeVisible = ref(false)


// 侧栏折叠状态持久化
const COLLAPSE_KEY = 'qbank_sidebar_collapsed'
const collapsed = ref(localStorage.getItem(COLLAPSE_KEY) === '1')

function toggleCollapse () {
  collapsed.value = !collapsed.value
  localStorage.setItem(COLLAPSE_KEY, collapsed.value ? '1' : '0')
}

const initials = computed(() => {
  const name = store.userInfo ? (store.userInfo.nickname || store.userInfo.username) : '?'
  return name.substring(0, 1)
})

function onCommand (cmd) {
  if (cmd === 'profile') {
    router.push('/my?tab=profile')
  } else if (cmd === 'theme') {
    themeVisible.value = true
  } else if (cmd === 'logout') {
    store.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
// 进入主界面后拉取启用主题: 校验个人选择是否仍可用, 管理员改样式后同步
onMounted(() => {
  themeStore.loadEnabled()
})
</script>

<style scoped>
.layout {
  height: 100%;
}

/* MD3 navigation drawer: 浅色 surface 底, 折叠 88px 适配 56px 胶囊菜单项 */
.aside {
  background: var(--q-aside-bg, var(--md-nav-bg));
  transition: width 0.25s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  color: var(--q-aside-active, var(--md-on-surface));
  font-size: 22px;
  font-weight: 400;
  text-align: center;
  padding: 24px 0 12px;
  letter-spacing: 2px;
  white-space: nowrap;
  overflow: hidden;
  display: flex;
  justify-content: center;
}

.logo span {
  display: inline-block;
  transition: font-size 0.25s ease;
}

.logo-fade-enter-active,
.logo-fade-leave-active {
  transition: opacity 0.15s ease;
}

.logo-fade-enter-from,
.logo-fade-leave-to {
  opacity: 0;
}

.menu {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

/* MD3 top app bar: elevation-0, 1px 分隔线 */
.header {
  background: var(--q-header-bg, var(--md-topbar-bg));
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--md-outline-variant);
}

.aside-footer {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 0 12px 24px;
  transition: padding-left 0.25s ease;
}

/* 折叠时按钮在 88px 栏内居中 */
.aside-footer.footer-center {
  padding-left: 24px;
  justify-content: center;
}

/* MD3 图标按钮: 40px 圆形 + state layer */
.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--q-aside-text, var(--md-nav-text));
  transition: background-color 0.2s ease, color 0.2s ease;
}

.icon-btn:hover {
  background-color: var(--md-primary-state-8);
  color: var(--md-on-surface);
}

.icon-btn .el-icon {
  font-size: 20px;
}

.header-title {
  font-size: 16px;
  font-weight: 500;
  color: var(--q-header-text, var(--md-on-surface));
}

/* MD3 用户胶囊 */
.user-chip {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  border-radius: var(--md-shape-full);
  padding: 4px 12px 4px 4px;
  transition: background-color 0.2s ease;
  outline: none;
}

.user-chip:hover {
  background-color: var(--md-primary-state-8);
}

.username {
  color: var(--q-header-text, var(--md-on-surface));
  font-size: 14px;
}

.main {
  padding: 20px 24px;
  overflow: auto;
}
</style>