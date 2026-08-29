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
      { path: 'categories', name: 'categories', component: () => import('../views/CategoriesView.vue'), meta: { title: '分类管理' } },
      { path: 'practice', name: 'practice', component: () => import('../views/PracticeView.vue'), meta: { title: '开始练习' } },
      { path: 'records', name: 'records', component: () => import('../views/RecordsView.vue'), meta: { title: '练习记录' } },
      { path: 'wrong', name: 'wrong', component: () => import('../views/WrongBookView.vue'), meta: { title: '错题本' } },
      { path: 'favorites', name: 'favorites', component: () => import('../views/FavoritesView.vue'), meta: { title: '收藏夹' } },
      { path: 'shares', name: 'shares', component: () => import('../views/SharesView.vue'), meta: { title: '共享' } },
      { path: 'profile', name: 'profile', component: () => import('../views/ProfileView.vue'), meta: { title: '个人资料' } },
      { path: 'users', name: 'users', component: () => import('../views/UsersView.vue'), meta: { title: '用户管理', admin: true } }
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
