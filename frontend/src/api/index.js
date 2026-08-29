import request from './request'

// ==================== 用户 ====================
export const userApi = {
  login: (data) => request.post('/user/login', data),
  register: (data) => request.post('/user/register', data),
  logout: () => request.post('/user/logout'),
  info: () => request.get('/user/info'),
  update: (data) => request.put('/user/update', data),
  add: (data) => request.post('/user/add', data),
  list: (params) => request.get('/user/list', { params }),
  updateStatus: (id, status) => request.put('/user/status', null, { params: { id, status } }),
  resetPassword: (id) => request.put('/user/reset-password/' + id),
  remove: (id) => request.delete('/user/' + id)
}

// ==================== 分类 ====================
export const categoryApi = {
  tree: () => request.get('/category/tree'),
  list: () => request.get('/category/list'),
  add: (data) => request.post('/category', data),
  update: (data) => request.put('/category', data),
  remove: (id) => request.delete('/category/' + id)
}

// ==================== 题目 ====================
export const questionApi = {
  list: (params) => request.get('/question/list', { params }),
  get: (id) => request.get('/question/' + id),
  add: (data) => request.post('/question', data),
  update: (data) => request.put('/question', data),
  remove: (ids) => request.post('/question/delete', ids)
}

// ==================== 收藏 ====================
export const favoriteApi = {
  toggle: (questionId) => request.post('/favorite/' + questionId + '/toggle'),
  list: (params) => request.get('/favorite/list', { params }),
  remove: (questionId) => request.delete('/favorite/' + questionId)
}

// ==================== 共享 ====================
export const shareApi = {
  share: (data) => request.post('/share', data),
  sent: (params) => request.get('/share/sent', { params }),
  received: (params) => request.get('/share/received', { params }),
  cancel: (id) => request.delete('/share/' + id)
}

// ==================== 练习 ====================
export const practiceApi = {
  start: (data) => request.post('/practice/start', data),
  submit: (data) => request.post('/practice/submit', data),
  records: (params) => request.get('/practice/records', { params }),
  detail: (id) => request.get('/practice/records/' + id),
  remove: (id) => request.delete('/practice/records/' + id)
}

// ==================== 错题本 ====================
export const wrongApi = {
  list: (params) => request.get('/wrong/list', { params }),
  toggleMaster: (questionId) => request.put('/wrong/master/' + questionId),
  remove: (questionId) => request.delete('/wrong/' + questionId)
}

// ==================== 统计 ====================
export const statsApi = {
  overview: () => request.get('/stats/overview'),
  byType: () => request.get('/stats/question-by-type'),
  byDifficulty: () => request.get('/stats/question-by-difficulty'),
  byCategory: () => request.get('/stats/question-by-category'),
  trend: () => request.get('/stats/practice-trend'),
  wrongByCategory: () => request.get('/stats/wrong-by-category')
}

// ==================== AI ====================
export const aiApi = {
  analyzeQuestion: (id) => request.post('/ai/analyze/question/' + id),
  report: () => request.post('/ai/analyze/report'),
  history: (params) => request.get('/ai/history', { params })
}
