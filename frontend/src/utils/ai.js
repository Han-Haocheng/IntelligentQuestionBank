// ============================================================
// 前端本地 AI 客户端 (OpenAI 兼容接口, 默认适配 DeepSeek)
// 配置持久化在 localStorage, 纯浏览器直连, 不经过后端
// ============================================================

const CONFIG_KEY = 'qbank_ai_config'
const HISTORY_KEY = 'qbank_ai_history'
const MAX_HISTORY = 50

export const DEFAULT_AI_CONFIG = {
  baseUrl: 'https://api.deepseek.com',
  apiKey: '',
  model: 'deepseek-chat',
  // 开发模式(vite)下走本地代理规避 CORS; Electron 生产环境无跨域限制
  useProxy: true
}

export function getAiConfig () {
  try {
    const raw = localStorage.getItem(CONFIG_KEY)
    if (raw) {
      return { ...DEFAULT_AI_CONFIG, ...JSON.parse(raw) }
    }
  } catch (e) { /* 配置损坏则重置 */ }
  return { ...DEFAULT_AI_CONFIG }
}

export function saveAiConfig (cfg) {
  localStorage.setItem(CONFIG_KEY, JSON.stringify({ ...getAiConfig(), ...cfg }))
}

export function hasApiKey () {
  return !!getAiConfig().apiKey
}

function resolveUrl (cfg) {
  let base = cfg.baseUrl || DEFAULT_AI_CONFIG.baseUrl
  if (base.endsWith('/')) base = base.slice(0, -1)
  const isLocalApi = base.indexOf('localhost') >= 0 || base.indexOf('127.0.0.1') >= 0
  const useProxy = cfg.useProxy !== false && import.meta.env.DEV && !isLocalApi
  return (useProxy ? '/ai-proxy' : base) + '/chat/completions'
}

/**
 * 调用大模型对话, 返回回复文本
 * @param {string} prompt
 * @param {{signal?: AbortSignal}} opts
 */
export async function aiChat (prompt, opts = {}) {
  const cfg = getAiConfig()
  if (!cfg.apiKey) {
    throw new Error('未配置 AI API Key, 请先在「AI 设置」中配置')
  }
  const resp = await fetch(resolveUrl(cfg), {
    method: 'POST',
    signal: opts.signal,
    headers: {
      'Content-Type': 'application/json',
      Authorization: 'Bearer ' + cfg.apiKey
    },
    body: JSON.stringify({
      model: cfg.model || DEFAULT_AI_CONFIG.model,
      stream: false,
      messages: [{ role: 'user', content: prompt }]
    })
  })
  if (!resp.ok) {
    let detail = ''
    try {
      const err = await resp.json()
      detail = (err.error && err.error.message) || ''
    } catch (e) { /* 忽略解析失败 */ }
    throw new Error('AI 接口请求失败(' + resp.status + ')' + (detail ? ': ' + detail : ''))
  }
  const data = await resp.json()
  const content = data.choices && data.choices[0] && data.choices[0].message && data.choices[0].message.content
  if (!content) {
    throw new Error('AI 返回内容为空')
  }
  return content
}

/** 快速测试配置是否可用 */
export async function testConnection () {
  const reply = await aiChat('请只回复两个字母: OK')
  return reply.trim().substring(0, 20)
}

// ---------------- AI 历史(本地) ----------------

export function getAiHistory () {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY)) || []
  } catch (e) {
    return []
  }
}

export function pushAiHistory (entry) {
  const list = getAiHistory()
  list.unshift({ time: new Date().toISOString(), model: getAiConfig().model, ...entry })
  localStorage.setItem(HISTORY_KEY, JSON.stringify(list.slice(0, MAX_HISTORY)))
}

export function clearAiHistory () {
  localStorage.removeItem(HISTORY_KEY)
}

// ---------------- 提示词构建 ----------------

export function buildQuestionPrompt (q) {
  const typeNames = ['单选题', '多选题', '填空题', '判断题', '简答题']
  let s = '你是一名资深教师。请对下面这道题目进行分析，用中文分点输出：\n'
  s += '1. 考查知识点；2. 难度评估(1-5)及理由；3. 题目表述清晰度与改进建议；4. 解题思路提示(不要直接给出完整答案)。\n\n'
  s += '题型: ' + (typeNames[(q.type || 1) - 1]) + '\n'
  s += '难度: ' + (q.difficulty == null ? '-' : q.difficulty) + '\n'
  s += '知识点标签: ' + (q.tags || '无') + '\n'
  s += '题干: ' + q.title + '\n'
  if (q.options && q.options.length) {
    s += '选项:\n'
    q.options.forEach((o, i) => { s += String.fromCharCode(65 + i) + '. ' + o + '\n' })
  }
  s += '参考答案: ' + (q.answer || '无') + '\n'
  s += '解析: ' + (q.analysis || '无')
  return s
}

export function buildReportPrompt (stats) {
  let s = '你是一名学习顾问。请根据以下练习数据，用中文分点输出学情报告：\n'
  s += '1. 总体表现评价；2. 薄弱知识点分析；3. 具体学习建议；4. 后续练习计划建议。\n\n'
  s += '题库题目数: ' + (stats.questionCount || 0) + '\n'
  s += '练习次数: ' + (stats.practiceCount || 0) + '\n'
  s += '当前错题数: ' + (stats.wrongCount || 0) + '\n'
  s += '总正确率: ' + (stats.accuracy == null ? '-' : stats.accuracy + '%') + '\n'
  if (stats.wrongByCategory && stats.wrongByCategory.length) {
    s += '错题最多的分类:\n'
    stats.wrongByCategory.forEach((x) => { s += '- ' + x.name + ': ' + x.value + '\n' })
  }
  if (stats.recentTrend && stats.recentTrend.length) {
    s += '近13天练习(日期 答题数 答对数):\n'
    stats.recentTrend.forEach((x) => { s += '- ' + x.date + ' ' + x.total + ' ' + (x.correct || 0) + '\n' })
  }
  return s
}
