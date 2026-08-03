<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register, getCaptcha, type RegisterParams } from '@/api/auth'
import { useUserStore, ROLE_USER, ROLE_MERCHANT, ROLE_ADMIN, ROLE_STAFF } from '@/store/user'
import { usePondStore } from '@/store/pond'
import type { FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()
const pondStore = usePondStore()

const activeTab = ref<'login' | 'register'>('login')
const registerRole = ref<'user' | 'merchant'>('user')
const loading = ref(false)
const captchaLoading = ref(false)
const captchaBase64 = ref('')
const captchaKey = ref('')

const loginFormRef = ref<FormInstance>()
const registerFormRef = ref<FormInstance>()

const loginForm = reactive({ username: '', password: '', captchaCode: '' })
const registerForm = reactive<RegisterParams>({
  phone: '',
  nickname: '',
  password: '',
  confirmPassword: '',
  role: 'user',
  inviteCode: '',
  captchaKey: '',
  captchaCode: ''
})

const loginRules = reactive<FormRules>({
  username: [
    { required: true, message: '请输入账号', trigger: 'blur' },
    { min: 3, max: 32, message: '账号长度 3-32 位', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 16, message: '密码长度 6-16 位', trigger: 'blur' }
  ],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
})

const validateConfirmPassword = (_rule: any, value: string, callback: Function) => {
  if (value !== registerForm.password) {
    callback(new Error('两次密码不一致'))
  } else {
    callback()
  }
}

const registerRules = reactive<FormRules>({
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: /^1\d{10}$/, message: '手机号格式错误', trigger: 'blur' }
  ],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { pattern: /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d]{6,16}$/, message: '密码需 6-16 位字母和数字组合', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请再次输入密码', trigger: 'blur' },
    { validator: validateConfirmPassword, trigger: 'blur' }
  ],
  inviteCode: [{ required: false }],
  captchaCode: [{ required: true, message: '请输入验证码', trigger: 'blur' }]
})

const refreshCaptcha = async () => {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    captchaBase64.value = 'data:image/png;base64,' + res.data.imageBase64
    loginForm.captchaCode = ''
    registerForm.captchaCode = ''
  } catch (e: any) {
    ElMessage.error('验证码加载失败')
  } finally {
    captchaLoading.value = false
  }
}

const handleLogin = async () => {
  if (!loginFormRef.value) return
  const valid = await loginFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    const res = await login({
      username: loginForm.username,
      password: loginForm.password,
      captchaKey: captchaKey.value,
      captchaCode: loginForm.captchaCode
    })
    const role = res.data.role ?? ROLE_USER
    const adminType = res.data.adminType ?? 0
    const boundPondId = res.data.pondId ?? null
    userStore.setUser({
      token: res.data.token,
      userId: res.data.userId,
      username: loginForm.username,
      role,
      adminType,
      pondId: boundPondId,
      staffId: res.data.staffId,
      merchantId: res.data.merchantId,
      staffRole: res.data.staffRole
    })
    ElMessage.success('登录成功')

    if (role === ROLE_ADMIN) {
      await pondStore.loadPonds(boundPondId)
      router.push('/admin/dashboard')
    } else if (role === ROLE_MERCHANT) {
      await pondStore.loadMerchantPonds(boundPondId)
      router.push('/merchant/dashboard')
    } else if (role === ROLE_STAFF) {
      await pondStore.loadMerchantPonds(boundPondId)
      redirectByStaffRole(res.data.staffRole)
    } else {
      router.push('/user/booking')
    }
  } catch (e: any) {
    ElMessage.error(e.message || '登录失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!registerFormRef.value) return
  // 商家注册时强制校验邀请码
  registerRules.inviteCode = registerRole.value === 'merchant'
    ? [{ required: true, message: '请输入商家邀请码', trigger: 'blur' }]
    : [{ required: false }]

  const valid = await registerFormRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    registerForm.role = registerRole.value
    registerForm.captchaKey = captchaKey.value
    await register(registerForm)
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.phone
    loginForm.password = ''
    loginForm.captchaCode = ''
    refreshCaptcha()
  } catch (e: any) {
    ElMessage.error(e.message || '注册失败')
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const redirectByStaffRole = (staffRole?: string) => {
  switch (staffRole) {
    case 'checker':
      router.push('/merchant/checkin')
      break
    case 'operator':
      router.push('/merchant/pond-board')
      break
    case 'finance':
      router.push('/merchant/revenue')
      break
    case 'manager':
      router.push('/merchant/dashboard')
      break
    default:
      router.push('/merchant/dashboard')
  }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <h1>钓鱼场预约系统</h1>
      <p class="subtitle">电脑端管理平台</p>

      <div class="tabs">
        <div
          class="tab-item"
          :class="{ active: activeTab === 'login' }"
          @click="activeTab = 'login'"
        >登录</div>
        <div
          class="tab-item"
          :class="{ active: activeTab === 'register' }"
          @click="activeTab = 'register'"
        >注册</div>
      </div>

      <!-- 登录表单 -->
      <el-form
        v-show="activeTab === 'login'"
        ref="loginFormRef"
        :model="loginForm"
        :rules="loginRules"
        label-position="top"
        class="login-form"
        @keyup.enter="handleLogin"
      >
        <el-form-item label="手机号" prop="username">
          <el-input v-model="loginForm.username" placeholder="请输入手机号" size="large" clearable maxlength="11">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="loginForm.password" type="password" placeholder="请输入密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="loginForm.captchaCode" placeholder="请输入验证码" size="large" maxlength="6" />
            <div class="captcha-img" @click="refreshCaptcha">
              <img v-if="captchaBase64" :src="captchaBase64" alt="验证码">
              <div v-else class="captcha-placeholder">加载中</div>
            </div>
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleLogin">
          登录
        </el-button>
      </el-form>

      <!-- 注册表单 -->
      <el-form
        v-show="activeTab === 'register'"
        ref="registerFormRef"
        :model="registerForm"
        :rules="registerRules"
        label-position="top"
        class="login-form"
        @keyup.enter="handleRegister"
      >
        <el-form-item label="注册身份" prop="role">
          <el-radio-group v-model="registerRole" size="large">
            <el-radio :value="'user'">普通用户</el-radio>
            <el-radio :value="'merchant'">商家</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="registerForm.phone" placeholder="请输入手机号" size="large" maxlength="11" clearable>
            <template #prefix><el-icon><Iphone /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="registerForm.nickname" placeholder="请输入昵称" size="large" clearable maxlength="32">
            <template #prefix><el-icon><User /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item v-if="registerRole === 'merchant'" label="邀请码" prop="inviteCode">
          <el-input v-model="registerForm.inviteCode" placeholder="测试码：FISH2024" size="large" clearable>
            <template #prefix><el-icon><DocumentChecked /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="密码" prop="password">
          <el-input v-model="registerForm.password" type="password" placeholder="6-16 位字母和数字组合" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" size="large" show-password>
            <template #prefix><el-icon><Lock /></el-icon></template>
          </el-input>
        </el-form-item>
        <el-form-item label="验证码" prop="captchaCode">
          <div class="captcha-row">
            <el-input v-model="registerForm.captchaCode" placeholder="请输入验证码" size="large" maxlength="6" />
            <div class="captcha-img" @click="refreshCaptcha">
              <img v-if="captchaBase64" :src="captchaBase64" alt="验证码">
              <div v-else class="captcha-placeholder">加载中</div>
            </div>
          </div>
        </el-form-item>
        <el-button type="primary" size="large" class="login-btn" :loading="loading" @click="handleRegister">
          注册
        </el-button>
      </el-form>

      <div class="tip">
        <p>默认商家账号：merchant / merchant</p>
        <p>商家邀请码：FISH2024</p>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #0f4c75 0%, #3282b8 50%, #bbe1fa 100%);
}

.login-card {
  width: 420px;
  background: white;
  border-radius: 16px;
  padding: 40px;
  box-shadow: 0 20px 50px rgba(0, 0, 0, 0.2);
}

.login-card h1 {
  text-align: center;
  color: #0f4c75;
  font-size: 28px;
  margin-bottom: 8px;
}

.subtitle {
  text-align: center;
  color: #909399;
  margin-bottom: 24px;
}

.tabs {
  display: flex;
  margin-bottom: 24px;
  border-bottom: 1px solid #e4e7ed;
}

.tab-item {
  flex: 1;
  text-align: center;
  padding: 12px 0;
  cursor: pointer;
  color: #606266;
  font-weight: 600;
  position: relative;
}

.tab-item.active {
  color: #0f4c75;
}

.tab-item.active::after {
  content: '';
  position: absolute;
  bottom: -1px;
  left: 20%;
  width: 60%;
  height: 2px;
  background: #0f4c75;
}

.login-form .el-form-item {
  margin-bottom: 20px;
}

.login-btn {
  width: 100%;
  margin-top: 8px;
}

.tip {
  margin-top: 24px;
  text-align: center;
  color: #909399;
  font-size: 13px;
  line-height: 1.8;
}

.captcha-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.captcha-row .el-input {
  flex: 1;
}

.captcha-img {
  width: 120px;
  height: 40px;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  flex-shrink: 0;
  border: 1px solid #dcdfe6;
  background: #f5f7fa;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.captcha-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
  font-size: 13px;
}

/* 移动端适配 */
@media (max-width: 576px) {
  .login-page {
    align-items: flex-start;
    padding-top: 40px;
    background: linear-gradient(135deg, #0f4c75 0%, #3282b8 100%);
  }

  .login-card {
    width: calc(100% - 32px);
    max-width: 420px;
    padding: 24px 20px;
    border-radius: 12px;
  }

  .login-card h1 {
    font-size: 22px;
  }

  .subtitle {
    font-size: 13px;
    margin-bottom: 16px;
  }

  .tabs {
    margin-bottom: 16px;
  }

  .login-form .el-form-item {
    margin-bottom: 14px;
  }

  .captcha-row {
    gap: 8px;
  }

  .captcha-img {
    width: 100px;
    height: 36px;
  }
}
</style>
