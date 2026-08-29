<template>
  <div class="profile-grid">
    <el-card class="page-card">
      <template #header><b>个人资料</b></template>
      <el-form :model="profileForm" label-width="80px" style="max-width: 420px">
        <el-form-item label="用户名">
          <el-input :value="store.userInfo ? store.userInfo.username : ''" disabled />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" />
        </el-form-item>
        <el-form-item label="邮箱">
          <el-input v-model="profileForm.email" />
        </el-form-item>
        <el-form-item label="头像URL">
          <el-input v-model="profileForm.avatar" placeholder="可选" />
        </el-form-item>
        <el-button type="primary" :loading="saving" @click="saveProfile">保存资料</el-button>
      </el-form>
    </el-card>

    <el-card class="page-card">
      <template #header><b>修改密码</b></template>
      <el-form :model="pwdForm" label-width="80px" style="max-width: 420px">
        <el-form-item label="原密码">
          <el-input v-model="pwdForm.oldPassword" type="password" show-password />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwdForm.newPassword" type="password" show-password placeholder="至少6位" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="pwdForm.confirm" type="password" show-password />
        </el-form-item>
        <el-button type="primary" :loading="changing" @click="changePassword">修改密码</el-button>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { userApi } from '../api'
import { useUserStore } from '../stores/user'

const store = useUserStore()
const saving = ref(false)
const changing = ref(false)

const profileForm = reactive({ nickname: '', email: '', avatar: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })

async function saveProfile () {
  saving.value = true
  try {
    const user = await userApi.update(profileForm)
    store.userInfo = user
    localStorage.setItem('qbank_user', JSON.stringify(user))
    ElMessage.success('资料已保存')
  } finally {
    saving.value = false
  }
}

async function changePassword () {
  if (pwdForm.newPassword.length < 6) {
    ElMessage.warning('新密码至少6位')
    return
  }
  if (pwdForm.newPassword !== pwdForm.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  changing.value = true
  try {
    await userApi.update({ oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    pwdForm.oldPassword = ''
    pwdForm.newPassword = ''
    pwdForm.confirm = ''
  } finally {
    changing.value = false
  }
}

onMounted(() => {
  userApi.info().then((user) => {
    store.userInfo = user
    profileForm.nickname = user.nickname || ''
    profileForm.email = user.email || ''
    profileForm.avatar = user.avatar || ''
  })
})
</script>

<style scoped>
.profile-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

@media (max-width: 900px) {
  .profile-grid {
    grid-template-columns: 1fr;
  }
}
</style>
