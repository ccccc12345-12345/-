<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const form = reactive({
  phone: '',
  code: '',
  password: ''
})

const useCode = ref(true)
const loading = ref(false)
const countdown = ref(0)

const sendCode = () => {
  if (!form.phone || form.phone.length !== 11) {
    ElMessage.warning('请输入正确手机号')
    return
  }
  countdown.value = 60
  const timer = setInterval(() => {
    countdown.value--
    if (countdown.value <= 0) clearInterval(timer)
  }, 1000)
  ElMessage.success('验证码已发送：1234')
}

const handleLogin = async () => {
  if (!form.phone) {
    ElMessage.warning('请输入手机号')
    return
  }
  const credential = useCode.value ? form.code : form.password
  if (!credential) {
    ElMessage.warning(useCode.value ? '请输入验证码' : '请输入密码')
    return
  }
  loading.value = true
  try {
    const res = await login({ username: form.phone, password: credential })
    userStore.setUser({
      token: res.data.token,
      userId: res.data.userId,
      isAdmin: form.phone === 'admin'
    })
    ElMessage.success('登录成功')
    if (form.phone === 'admin') {
      router.push('/admin')
    } else {
      router.push('/')
    }
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>钓鱼场预约</h1>
      <p class="subtitle">选择心仪时段，开启垂钓之旅</p>

      <el-input
        v-model="form.phone"
        placeholder="请输入手机号"
        maxlength="11"
        class="login-input"
      >
        <template #prefix>
          <el-icon><Iphone /></el-icon>
        </template>
      </el-input>

      <div v-if="useCode" class="code-row">
        <el-input v-model="form.code" placeholder="验证码" maxlength="6" class="code-input">
          <template #prefix>
            <el-icon><Message /></el-icon>
          </template>
        </el-input>
        <el-button
          class="code-btn"
          :disabled="countdown > 0"
          @click="sendCode"
        >
          {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
        </el-button>
      </div>

      <el-input
        v-else
        v-model="form.password"
        type="password"
        placeholder="请输入密码"
        class="login-input"
        show-password
      >
        <template #prefix>
          <el-icon><Lock /></el-icon>
        </template>
      </el-input>

      <div class="switch-row" @click="useCode = !useCode">
        <span>{{ useCode ? '使用密码登录' : '使用验证码登录' }}</span>
      </div>

      <el-button class="login-btn" type="primary" :loading="loading" @click="handleLogin">
        登录 / 注册
      </el-button>

      <p class="tip">普通用户：任意手机号 + 验证码 1234<br>管理员：admin + 密码 admin</p>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(160deg, #1a5f7a 0%, #57cc99 100%);
  padding: 20px;
}

.login-card {
  width: 100%;
  background: white;
  border-radius: 24px;
  padding: 32px 24px;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.15);
}

.login-card h1 {
  font-size: 28px;
  color: var(--primary);
  text-align: center;
  margin-bottom: 8px;
}

.subtitle {
  text-align: center;
  color: var(--text-secondary);
  margin-bottom: 28px;
  font-size: 14px;
}

.login-input {
  margin-bottom: 16px;
  height: 48px;
}

.code-row {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.code-input {
  flex: 1;
  height: 48px;
}

.code-btn {
  width: 120px;
  height: 48px;
  border-radius: 12px;
  background: var(--bg);
  border: none;
  color: var(--primary);
  font-weight: 600;
}

.switch-row {
  text-align: right;
  color: var(--primary);
  font-size: 14px;
  margin-bottom: 24px;
}

.login-btn {
  width: 100%;
  height: 48px;
  border-radius: 999px;
  font-size: 16px;
  font-weight: 700;
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  border: none;
}

.tip {
  margin-top: 20px;
  text-align: center;
  color: var(--text-secondary);
  font-size: 12px;
  line-height: 1.6;
}
</style>
