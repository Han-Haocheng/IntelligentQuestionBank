import { createRouter, createWebHashHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

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
      { path: 'ai-settings', name: 'ai-settings', component: () => import('../views/AiSettingsView.vue'), meta: { title: 'AI 设置' } },
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
  return true
})

export default router
