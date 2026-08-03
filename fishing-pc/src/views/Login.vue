<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Finished, Lock, Refresh, Shop, User } from '@element-plus/icons-vue'
import { getCaptcha, login, register, type RegisterParams } from '@/api/auth'
import { ROLE_ADMIN, ROLE_MERCHANT, ROLE_STAFF, ROLE_USER, useUserStore } from '@/store/userStore'
// import { redirectMerchantConsole } from '@/utils/merchantConsole'

const router = useRouter()
const userStore = useUserStore()

const activeTab = ref<'login' | 'register'>('login')
const loading = ref(false)
const captchaLoading = ref(false)
const captchaBase64 = ref('')
const captchaKey = ref('')

const loginForm = reactive({ username: 'merchant', password: 'merchant', captchaCode: '' })
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

const isDemoAccount = (username: string) => {
  return ['18800000001', '18800000002', 'merchant'].includes(username)
}

const demoAccounts = [
  {
    label: '演示商家',
    username: '18800000001',
    password: '123456'
  },
  {
    label: '演示用户',
    username: '18800000002',
    password: '123456'
  },
  {
    label: '默认商家',
    username: 'merchant',
    password: 'merchant'
  }
]

const refreshCaptcha = async () => {
  captchaLoading.value = true
  try {
    const res = await getCaptcha()
    captchaKey.value = res.data.captchaKey
    captchaBase64.value = `data:image/png;base64,${res.data.imageBase64}`
    loginForm.captchaCode = ''
    registerForm.captchaCode = ''
  } catch {
    ElMessage.error('验证码加载失败，请检查后端服务')
  } finally {
    captchaLoading.value = false
  }
}

const fillAccount = (username: string, password: string) => {
  activeTab.value = 'login'
  loginForm.username = username
  loginForm.password = password
  loginForm.captchaCode = ''
  if (isDemoAccount(username)) {
    ElMessage.info('演示账号已填入，点击登录即可进入')
  } else {
    ElMessage.info('账号已填入，请输入验证码后登录')
  }
}

const validateLogin = () => {
  const username = loginForm.username.trim()
  if (!username) {
    ElMessage.warning('请输入账号')
    return false
  }
  if (username.length > 32) {
    ElMessage.warning('账号长度不能超过 32 位')
    return false
  }
  if (!loginForm.password) {
    ElMessage.warning('请输入密码')
    return false
  }
  if (loginForm.password.length < 6 || loginForm.password.length > 16) {
    ElMessage.warning('密码长度应为 6-16 位')
    return false
  }
  if (!isDemoAccount(username)) {
    if (!loginForm.captchaCode.trim()) {
      ElMessage.warning('请输入验证码')
      return false
    }
    if (!captchaKey.value) {
      ElMessage.warning('验证码未加载，请刷新验证码')
      return false
    }
  }
  return true
}

const handleLogin = async () => {
  if (!validateLogin()) return
  loading.value = true
  try {
    const username = loginForm.username.trim()
    const isDemo = isDemoAccount(username)
    const res = await login({
      username,
      password: loginForm.password,
      captchaKey: isDemo ? 'demo' : captchaKey.value,
      captchaCode: isDemo ? 'demo' : loginForm.captchaCode.trim()
    })
    const role = res.data.role ?? ROLE_USER
    userStore.setUser({
      token: res.data.token,
      userId: res.data.userId,
      username,
      role,
      adminType: res.data.adminType,
      pondId: res.data.pondId,
      staffId: res.data.staffId,
      merchantId: res.data.merchantId,
      staffRole: res.data.staffRole
    })
    ElMessage.success('登录成功')

    if (role === ROLE_ADMIN) {
      router.push('/admin/slots')
    } else if (role === ROLE_MERCHANT || role === ROLE_STAFF) {
      router.push('/merchant/ponds')
    } else {
      router.push('/user/booking')
    }
  } catch {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const validateRegister = () => {
  const phoneReg = /^1\d{10}$/
  const passwordReg = /^(?=.*[a-zA-Z])(?=.*\d)[a-zA-Z\d]{6,16}$/
  if (!phoneReg.test(registerForm.phone)) {
    ElMessage.warning('请输入 11 位手机号')
    return false
  }
  if (!registerForm.nickname.trim()) {
    ElMessage.warning('请输入昵称')
    return false
  }
  if (!passwordReg.test(registerForm.password)) {
    ElMessage.warning('密码需要 6-16 位字母和数字组合')
    return false
  }
  if (registerForm.password !== registerForm.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return false
  }
  if (registerForm.role === 'merchant' && !registerForm.inviteCode?.trim()) {
    ElMessage.warning('商家注册需要邀请码')
    return false
  }
  if (!registerForm.captchaCode.trim()) {
    ElMessage.warning('请输入验证码')
    return false
  }
  if (!captchaKey.value) {
    ElMessage.warning('验证码未加载，请刷新验证码')
    return false
  }
  return true
}

const handleRegister = async () => {
  if (!validateRegister()) return
  loading.value = true
  try {
    await register({
      ...registerForm,
      phone: registerForm.phone.trim(),
      nickname: registerForm.nickname.trim(),
      inviteCode: registerForm.inviteCode?.trim(),
      captchaKey: captchaKey.value,
      captchaCode: registerForm.captchaCode.trim()
    })
    ElMessage.success('注册成功，请登录')
    activeTab.value = 'login'
    loginForm.username = registerForm.phone
    loginForm.password = ''
    loginForm.captchaCode = ''
    refreshCaptcha()
  } catch {
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const switchTab = (tab: 'login' | 'register') => {
  activeTab.value = tab
  refreshCaptcha()
}

onMounted(refreshCaptcha)
</script>

<template>
  <div class="login-page">
    <section class="login-shell fp-spotlight fp-magnetic" v-fp-spotlight v-fp-magnetic>
      <div class="brand-panel">
        <div class="brand-overlay">
          <div class="brand-mark">
            <el-icon><Shop /></el-icon>
            <span>野钓营地</span>
          </div>
          <h1>钓鱼预约、商城购买、商家管理一体化平台</h1>
          <p>默认商家账号已就绪，普通用户可注册后使用手机号登录。</p>
          <div class="brand-stats">
            <span>钓位预约</span>
            <span>渔具商城</span>
            <span>商家后台</span>
          </div>
        </div>
      </div>

      <div class="login-card">
        <div class="login-card-head">
          <p class="eyebrow">Web 控制台</p>
          <h2>登录钓鱼平台</h2>
          <p>默认商家账号已自动填入，输入验证码即可进入商品管理。</p>
        </div>

        <div class="quick-row">
          <button
            v-for="account in demoAccounts"
            :key="account.username"
            class="quick-account fp-ripple"
            type="button"
            v-fp-ripple
            @click="fillAccount(account.username, account.password)"
          >
            <el-icon><Finished /></el-icon>
            <span>
              {{ account.label }}
              <small>{{ account.username }} / {{ account.password }}</small>
            </span>
          </button>
        </div>

        <div class="tabs">
          <button class="tab-item" :class="{ active: activeTab === 'login' }" type="button" @click="switchTab('login')">
            登录
          </button>
          <button class="tab-item" :class="{ active: activeTab === 'register' }" type="button" @click="switchTab('register')">
            注册
          </button>
        </div>

        <el-form v-show="activeTab === 'login'" :model="loginForm" label-position="top" class="login-form" @keyup.enter="handleLogin">
          <el-form-item label="账号">
            <el-input v-model="loginForm.username" placeholder="请输入手机号 / merchant" size="large" clearable maxlength="32">
              <template #prefix><el-icon><User /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="loginForm.password" type="password" placeholder="请输入 6-16 位密码" size="large" show-password>
              <template #prefix><el-icon><Lock /></el-icon></template>
            </el-input>
          </el-form-item>
          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input v-model="loginForm.captchaCode" placeholder="输入右侧验证码" size="large" maxlength="10" />
              <button class="captcha-img" type="button" :disabled="captchaLoading" @click="refreshCaptcha">
                <img v-if="captchaBase64" :src="captchaBase64" alt="验证码" />
                <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
              </button>
              <el-button circle :icon="Refresh" :loading="captchaLoading" @click="refreshCaptcha" />
            </div>
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" v-fp-ripple @click="handleLogin">
            登录
          </el-button>
        </el-form>

        <el-form v-show="activeTab === 'register'" :model="registerForm" label-position="top" class="login-form" @keyup.enter="handleRegister">
          <el-form-item label="注册身份">
            <el-radio-group v-model="registerForm.role">
              <el-radio value="user">普通用户</el-radio>
              <el-radio value="merchant">商家</el-radio>
            </el-radio-group>
          </el-form-item>
          <el-form-item label="手机号">
            <el-input v-model="registerForm.phone" placeholder="请输入手机号" size="large" maxlength="11" clearable />
          </el-form-item>
          <el-form-item label="昵称">
            <el-input v-model="registerForm.nickname" placeholder="请输入昵称" size="large" maxlength="32" clearable />
          </el-form-item>
          <el-form-item v-if="registerForm.role === 'merchant'" label="商家邀请码">
            <el-input v-model="registerForm.inviteCode" placeholder="演示邀请码：FISH2024" size="large" clearable />
          </el-form-item>
          <el-form-item label="密码">
            <el-input v-model="registerForm.password" type="password" placeholder="6-16 位字母和数字组合" size="large" show-password />
          </el-form-item>
          <el-form-item label="确认密码">
            <el-input v-model="registerForm.confirmPassword" type="password" placeholder="再次输入密码" size="large" show-password />
          </el-form-item>
          <el-form-item label="验证码">
            <div class="captcha-row">
              <el-input v-model="registerForm.captchaCode" placeholder="输入右侧验证码" size="large" maxlength="10" />
              <button class="captcha-img" type="button" :disabled="captchaLoading" @click="refreshCaptcha">
                <img v-if="captchaBase64" :src="captchaBase64" alt="验证码" />
                <span v-else>{{ captchaLoading ? '加载中' : '刷新' }}</span>
              </button>
              <el-button circle :icon="Refresh" :loading="captchaLoading" @click="refreshCaptcha" />
            </div>
          </el-form-item>
          <el-button type="primary" size="large" class="login-btn" :loading="loading" v-fp-ripple @click="handleRegister">
            注册
          </el-button>
        </el-form>

        <div class="account-tip">
          <span>商家：merchant / merchant</span>
          <span>普通用户：注册后使用手机号和密码登录</span>
          <span>管理员：使用已分配的 6-16 位密码登录</span>
        </div>
      </div>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px;
  background:
    radial-gradient(circle at 12% 20%, rgba(31, 106, 88, 0.14) 0%, transparent 24%),
    radial-gradient(circle at 88% 80%, rgba(87, 160, 120, 0.12) 0%, transparent 26%),
    linear-gradient(180deg, #e8f2ec 0%, #dcece2 50%, #d0e6d8 100%);
  color: #172521;
}

.login-shell {
  width: min(1120px, 100%);
  min-height: 680px;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  background: #ffffff;
  border: 1px solid rgba(19, 53, 45, 0.12);
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 24px 70px rgba(18, 40, 34, 0.16);
  transition: box-shadow 0.3s var(--fp-ease-out), transform 0.3s var(--fp-ease-out);
}

.login-shell:hover {
  box-shadow: 0 32px 90px rgba(18, 40, 34, 0.22);
}

.brand-panel,
.login-card {
  position: relative;
  z-index: 1;
}

.brand-panel {
  min-height: 100%;
  background:
    linear-gradient(180deg, rgba(12, 34, 30, 0.08), rgba(12, 34, 30, 0.78)),
    linear-gradient(135deg, #0f3b31, #1f6a58 55%, #57a078);
}

.brand-overlay {
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  padding: 48px;
  color: #fff;
}

.brand-mark {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  width: fit-content;
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.16);
  border: 1px solid rgba(255, 255, 255, 0.26);
  font-weight: 700;
}

.brand-panel h1 {
  max-width: 520px;
  margin: 24px 0 14px;
  font-size: 38px;
  line-height: 1.2;
  font-weight: 800;
}

.brand-panel p {
  max-width: 460px;
  color: rgba(255, 255, 255, 0.86);
  font-size: 16px;
  line-height: 1.7;
}

.brand-stats {
  display: flex;
  gap: 10px;
  margin-top: 28px;
  flex-wrap: wrap;
}

.brand-stats span {
  padding: 8px 12px;
  border-radius: 6px;
  background: rgba(255, 255, 255, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.22);
  color: #fff7df;
  font-weight: 700;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    background-color var(--fp-dur-fast) var(--fp-ease-out);
}

.brand-stats span:hover {
  transform: translateY(-2px);
  background: rgba(255, 255, 255, 0.25);
}

.login-card {
  padding: 48px 44px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.login-card-head {
  margin-bottom: 22px;
}

.eyebrow {
  margin: 0 0 8px;
  color: #b5742a;
  font-size: 13px;
  font-weight: 800;
}

.login-card h2 {
  margin: 0 0 8px;
  color: #143c35;
  font-size: 28px;
  font-weight: 800;
}

.login-card-head p:last-child {
  margin: 0;
  color: #63716c;
  line-height: 1.6;
}

.quick-row {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 8px;
  margin-bottom: 20px;
}

.quick-account {
  min-height: 46px;
  border: 1px solid #dbe5df;
  border-radius: 6px;
  background: #f8fbf9;
  color: #26443d;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  cursor: pointer;
  font-weight: 700;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out),
    border-color var(--fp-dur-fast) var(--fp-ease-out),
    background-color var(--fp-dur-fast) var(--fp-ease-out);
}

.quick-account span {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  line-height: 1.25;
}

.quick-account small {
  color: #6c7b75;
  font-size: 12px;
  font-weight: 600;
}

.quick-account:hover {
  border-color: #2f6f5e;
  color: #1e594b;
  background: #eef8f1;
  transform: translateY(-2px);
  box-shadow: 0 8px 18px rgba(31, 106, 88, 0.12);
}

.quick-account:active {
  transform: translateY(1px);
}

.tabs {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
  padding: 4px;
  background: #eef3f0;
  border-radius: 8px;
  margin-bottom: 22px;
}

.tab-item {
  min-height: 42px;
  border: 0;
  border-radius: 6px;
  background: transparent;
  color: #5d6c66;
  cursor: pointer;
  font-size: 15px;
  font-weight: 800;
  transition: color var(--fp-dur-fast) var(--fp-ease-out),
    background-color var(--fp-dur-fast) var(--fp-ease-out),
    transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out);
}

.tab-item:hover:not(.active) {
  color: #1e594b;
  background: rgba(31, 106, 88, 0.06);
  transform: translateY(-1px);
}

.tab-item.active {
  background: #ffffff;
  color: #17483e;
  box-shadow: 0 1px 8px rgba(20, 56, 46, 0.12);
}

.tab-item.active:hover {
  transform: translateY(-1px);
  box-shadow: 0 4px 14px rgba(20, 56, 46, 0.16);
}

.login-form .el-form-item {
  margin-bottom: 17px;
}

.captcha-row {
  width: 100%;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 118px 40px;
  align-items: center;
  gap: 10px;
}

.captcha-img {
  width: 118px;
  height: 40px;
  padding: 0;
  border: 1px solid #dbe5df;
  border-radius: 6px;
  overflow: hidden;
  background: #f7faf8;
  color: #5f6c67;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out),
    border-color var(--fp-dur-fast) var(--fp-ease-out);
}

.captcha-img:hover {
  transform: translateY(-1px);
  border-color: #2f6f5e;
  box-shadow: 0 6px 14px rgba(31, 106, 88, 0.1);
}

.captcha-img:disabled {
  cursor: wait;
}

.captcha-img img {
  width: 100%;
  height: 100%;
  display: block;
  object-fit: cover;
}

.login-btn {
  width: 100%;
  min-height: 44px;
  margin-top: 4px;
  --el-button-bg-color: #1f6a58;
  --el-button-border-color: #1f6a58;
  --el-button-hover-bg-color: #185848;
  --el-button-hover-border-color: #185848;
}

.account-tip {
  margin-top: 18px;
  display: flex;
  flex-direction: column;
  gap: 5px;
  color: #73807a;
  font-size: 13px;
  line-height: 1.5;
}

@media (max-width: 920px) {
  .login-page {
    padding: 16px;
  }

  .login-shell {
    grid-template-columns: 1fr;
  }

  .brand-panel {
    min-height: 300px;
  }

  .brand-overlay {
    padding: 32px;
  }

  .brand-panel h1 {
    font-size: 28px;
  }
}

@media (max-width: 560px) {
  .login-card {
    padding: 28px 20px;
  }

  .quick-row {
    grid-template-columns: 1fr;
  }

  .captcha-row {
    grid-template-columns: minmax(0, 1fr) 104px 38px;
    gap: 8px;
  }

  .captcha-img {
    width: 104px;
  }
}
</style>
