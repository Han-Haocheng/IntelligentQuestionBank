<template>
  <el-container class="layout">
    <el-aside :width="collapsed ? '64px' : '210px'" class="aside">
      <div class="logo" :class="{ 'logo-mini': collapsed }">{{ collapsed ? '题库' : '题库管理系统' }}</div>
      <el-menu :default-active="route.path" router :collapse="collapsed" :collapse-transition="false"
        background-color="#001529" text-color="#a6adb4" active-text-color="#ffffff" class="menu">
        <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon><template #title>统计看板</template></el-menu-item>
        <el-menu-item index="/questions"><el-icon><Document /></el-icon><template #title>题目管理</template></el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/admin"><el-icon><Setting /></el-icon><template #title>管理</template></el-menu-item>
        <el-menu-item index="/practice"><el-icon><EditPen /></el-icon><template #title>练习</template></el-menu-item>
        <el-menu-item index="/my"><el-icon><Star /></el-icon><template #title>我的</template></el-menu-item>
      </el-menu>
      <!-- 侧栏左下角: 折叠/展开按钮 -->
      <div class="aside-footer" :class="{ 'footer-center': collapsed }">
        <el-tooltip :content="collapsed ? '展开侧栏' : '折叠侧栏'" placement="right">
          <el-icon class="collapse-btn" @click="toggleCollapse">
            <Expand v-if="collapsed" /><Fold v-else />
          </el-icon>
        </el-tooltip>
      </div>
    </el-aside>
    <el-container>
      <el-header class="header">
        <span class="header-title">{{ route.meta.title || '智能题库' }}</span>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-avatar :size="30" style="background:#409eff">{{ initials }}</el-avatar>
            <span class="username">{{ store.userInfo ? store.userInfo.nickname || store.userInfo.username : '' }}</span>
            <el-icon><ArrowDown /></el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
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
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

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
  } else if (cmd === 'logout') {
    store.logout()
    ElMessage.success('已退出登录')
    router.push('/login')
  }
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #001529;
  /* 无宽度动画: 内容(logo/菜单/底部按钮)随 collapsed 状态同拍切换, 避免动画不同步造成的抖动 */
  overflow: hidden;
  display: flex;
  flex-direction: column;
}

.logo {
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  text-align: center;
  padding: 20px 0;
  letter-spacing: 2px;
  white-space: nowrap;
  overflow: hidden;
}

.logo-mini {
  font-size: 15px;
  padding: 22px 0;
}

.menu {
  border-right: none;
  flex: 1;
  min-height: 0;
  overflow-y: auto;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
}

.aside-footer {
  display: flex;
  align-items: center;
  justify-content: flex-start;
  padding: 12px 0 12px 24px;
  border-top: 1px solid rgba(255, 255, 255, 0.08);
}

.aside-footer.footer-center {
  justify-content: center;
  padding-left: 0;
}

.collapse-btn {
  font-size: 18px;
  color: #a6adb4;
  cursor: pointer;
  transition: color 0.15s;
}

.collapse-btn:hover {
  color: #fff;
}

.header-title {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.username {
  color: #303133;
  font-size: 14px;
}

.main {
  padding: 16px;
  overflow: auto;
}
</style>
