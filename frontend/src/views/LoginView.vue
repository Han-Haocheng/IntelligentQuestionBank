<template>
  <div class="login-page">
    <el-card class="login-card">
      <div class="login-title">
        <h2>智能题库管理系统</h2>
        <p>保存 · 共享 · 分析 · 练习</p>
      </div>
      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form ref="loginFormRef" :model="loginForm" :rules="rules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="loginForm.username" placeholder="用户名" :prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="loginForm.password" type="password" show-password placeholder="密码"
                :prefix-icon="Lock" size="large" @keyup.enter="doLogin" />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doLogin">
              登 录
            </el-button>
            <el-alert class="login-tip" type="info" :closable="false"
              title="默认账号: admin / 123456 (管理员)  demo / 123456 (普通用户)" />
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="register">
          <el-form ref="regFormRef" :model="regForm" :rules="rules" label-width="0">
            <el-form-item prop="username">
              <el-input v-model="regForm.username" placeholder="用户名(至少3个字符)" :prefix-icon="User" size="large" />
            </el-form-item>
            <el-form-item prop="nickname">
              <el-input v-model="regForm.nickname" placeholder="昵称(可选)" :prefix-icon="Postcard" size="large" />
            </el-form-item>
            <el-form-item prop="password">
              <el-input v-model="regForm.password" type="password" show-password placeholder="密码(至少6位)"
                :prefix-icon="Lock" size="large" />
            </el-form-item>
            <el-form-item prop="confirm">
              <el-input v-model="regForm.confirm" type="password" show-password placeholder="确认密码"
                :prefix-icon="Lock" size="large" @keyup.enter="doRegister" />
            </el-form-item>
            <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="doRegister">
              注 册
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>

      <!-- 后端地址设置(远程部署/局域网使用时) -->
      <el-collapse class="backend-collapse">
        <el-collapse-item title="⚙️ 后端地址设置" name="backend">
          <div class="backend-row">
            <el-radio-group v-model="backendMode" size="small">
              <el-radio-button value="default">默认后端(本机)</el-radio-button>
              <el-radio-button value="custom">自定义远程后端</el-radio-button>
            </el-radio-group>
          </div>
          <div v-if="backendMode === 'custom'" class="backend-row">
            <el-input v-model="customBase" size="small" clearable
              placeholder="http://192.168.1.10:8080/api 或 https://example.com/api"
              @keyup.enter="saveBackend" />
          </div>
          <div class="backend-row backend-tip">
            <span class="backend-current">当前: <code>{{ effectiveBase }}</code></span>
            <el-button size="small" link type="primary" @click="saveBackend">应用</el-button>
            <el-button v-if="backendMode === 'custom' && customBase" size="small" link @click="resetBackend">恢复默认</el-button>
          </div>
          <div class="backend-hint">自定义后, 登录/注册及所有请求将发往该地址(保存在本机浏览器); 远程后端需放行跨域(CORS)。</div>
        </el-collapse-item>
      </el-collapse>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock, Postcard } from '@element-plus/icons-vue'
import { userApi } from '../api'
import { useUserStore } from '../stores/user'

const router = useRouter()
const store = useUserStore()

const activeTab = ref('login')
const loading = ref(false)
const loginFormRef = ref()
const regFormRef = ref()

// ============ 后端地址设置 ============
const BACKEND_KEY = 'qbank_api_base'
const backendMode = ref('default')
const customBase = ref('')
const effectiveBase = ref('')

function defaultBase () {
  return import.meta.env.DEV ? '/api' : 'http://localhost:8080/api'
}

/** 应用后端地址(持久化到 localStorage, 供 axios 每次请求动态解析); 返回是否成功 */
function applyBackend () {
  const val = (customBase.value || '').trim().replace(/\/+$/, '')
  if (backendMode.value === 'custom') {
    if (!/^https?:\/\//i.test(val)) {
      ElMessage.warning('请输入完整的 http(s):// 地址')
      return false
    }
    localStorage.setItem(BACKEND_KEY, val)
    customBase.value = val
    effectiveBase.value = val
  } else {
    localStorage.removeItem(BACKEND_KEY)
    customBase.value = ''
    effectiveBase.value = defaultBase()
  }
  return true
}

function saveBackend () {
  if (applyBackend()) ElMessage.success('后端地址已应用')
}

function resetBackend () {
  backendMode.value = 'default'
  customBase.value = ''
  applyBackend()
  ElMessage.success('已恢复默认后端')
}

function readBackend () {
  const v = localStorage.getItem(BACKEND_KEY)
  if (v) {
    backendMode.value = 'custom'
    customBase.value = v
    effectiveBase.value = v
  } else {
    backendMode.value = 'default'
    customBase.value = ''
    effectiveBase.value = defaultBase()
  }
}

onMounted(readBackend)

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', nickname: '', password: '', confirm: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function doLogin () {
  await loginFormRef.value.validate()
  if (!applyBackend()) return
  loading.value = true
  try {
    const data = await userApi.login({ username: loginForm.username, password: loginForm.password })
    store.saveLogin(data.token, data.user)
    ElMessage.success('登录成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

async function doRegister () {
  await regFormRef.value.validate()
  if (!applyBackend()) return
  if (regForm.password.length < 6) {
    ElMessage.warning('密码至少6位')
    return
  }
  if (regForm.password !== regForm.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  loading.value = true
  try {
    const data = await userApi.register({
      username: regForm.username,
      nickname: regForm.nickname,
      password: regForm.password
    })
    store.saveLogin(data.token, data.user)
    ElMessage.success('注册成功, 已自动登录')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f6feb 0%, #6e40c9 100%);
}

.login-card {
  width: 420px;
  padding: 10px 10px 4px;
}

.login-title {
  text-align: center;
  margin-bottom: 10px;
}

.login-title h2 {
  margin: 0;
  color: #303133;
}

.login-title p {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.login-tip {
  margin-top: 14px;
}

.backend-collapse {
  margin-top: 8px;
  border-top: 1px solid #ebeef5;
}

.backend-row {
  margin-bottom: 8px;
}

.backend-tip {
  display: flex;
  align-items: center;
  gap: 4px;
}

.backend-current {
  flex: 1;
  color: #606266;
  font-size: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.backend-current code {
  color: #409eff;
}

.backend-hint {
  color: #909399;
  font-size: 12px;
  line-height: 1.6;
}
</style>
