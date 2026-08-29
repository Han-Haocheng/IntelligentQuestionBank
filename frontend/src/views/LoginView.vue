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
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
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

const loginForm = reactive({ username: '', password: '' })
const regForm = reactive({ username: '', nickname: '', password: '', confirm: '' })

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function doLogin () {
  await loginFormRef.value.validate()
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
</style>
