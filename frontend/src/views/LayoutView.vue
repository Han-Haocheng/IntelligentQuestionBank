<template>
  <el-container class="layout">
    <el-aside width="210px" class="aside">
      <div class="logo">题库管理系统</div>
      <el-menu :default-active="route.path" router background-color="#001529" text-color="#a6adb4"
        active-text-color="#ffffff" class="menu">
        <el-menu-item index="/dashboard"><el-icon><DataLine /></el-icon>统计看板</el-menu-item>
        <el-menu-item index="/questions"><el-icon><Document /></el-icon>题目管理</el-menu-item>
        <el-menu-item index="/ai-settings"><el-icon><MagicStick /></el-icon>AI 设置</el-menu-item>
        <el-menu-item index="/categories"><el-icon><FolderOpened /></el-icon>分类管理</el-menu-item>
        <el-menu-item index="/practice"><el-icon><EditPen /></el-icon>开始练习</el-menu-item>
        <el-menu-item index="/records"><el-icon><List /></el-icon>练习记录</el-menu-item>
        <el-menu-item index="/wrong"><el-icon><WarningFilled /></el-icon>错题本</el-menu-item>
        <el-menu-item index="/favorites"><el-icon><Star /></el-icon>收藏夹</el-menu-item>
        <el-menu-item index="/shares"><el-icon><Share /></el-icon>共享</el-menu-item>
        <el-menu-item v-if="store.isAdmin" index="/users"><el-icon><UserFilled /></el-icon>用户管理</el-menu-item>
      </el-menu>
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
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const initials = computed(() => {
  const name = store.userInfo ? (store.userInfo.nickname || store.userInfo.username) : '?'
  return name.substring(0, 1)
})

function onCommand (cmd) {
  if (cmd === 'profile') {
    router.push('/profile')
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
}

.logo {
  color: #fff;
  font-size: 17px;
  font-weight: 600;
  text-align: center;
  padding: 20px 0;
  letter-spacing: 2px;
}

.menu {
  border-right: none;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
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
