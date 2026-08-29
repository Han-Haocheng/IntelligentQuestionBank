import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'

const routes = [
  { path: '/login', name: 'login', component: () => import('../views/LoginView.vue') },
  {
    path: '/',
    component: () => import('../views/LayoutView.vue'),
    redirect: '/dashboard',
    children: [
      { path: 'dashboard', name: 'dashboard', component: () => import('../views/DashboardView.vue'), meta: { title: '统计看板' } },
      { path: 'questions', name: 'questions', component: () => import('../views/QuestionsView.vue'), meta: { title: '题目管理' } },
      { path: 'banks', redirect: '/questions' },
      // AI 设置已并入「我的」页, 保留旧路径重定向(题目/AI 分析等处仍会跳转)
      { path: 'ai-settings', redirect: '/my?tab=ai-settings' },
      { path: 'admin', name: 'admin', component: () => import('../views/AdminView.vue'), meta: { title: '管理', admin: true } },
      { path: 'categories', redirect: '/admin' },
      { path: 'users', redirect: '/admin' },
      { path: 'practice', name: 'practice', component: () => import('../views/PracticeView.vue'), meta: { title: '练习' } },
      { path: 'records', redirect: '/practice' },
      { path: 'wrong', redirect: '/practice' },
      { path: 'my', name: 'my', component: () => import('../views/MyView.vue'), meta: { title: '我的' } },
      { path: 'favorites', redirect: '/my' },
      { path: 'shares', redirect: '/my' },
      { path: 'profile', redirect: '/my' }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to) => {
  const token = localStorage.getItem('qbank_token')
  if (to.path !== '/login' && !token) {
    ElMessage.warning('请先登录')
    return '/login'
  }
  if (to.path === '/login' && token) {
    return '/dashboard'
  }
  // 管理员页面鉴权: 侧栏只是隐藏入口, 直访地址同样拦截
  if (to.meta.admin) {
    const store = useUserStore()
    if (!store.isAdmin) {
      ElMessage.warning('无权限访问该页面')
      return '/dashboard'
    }
  }
  return true
})

export default router
