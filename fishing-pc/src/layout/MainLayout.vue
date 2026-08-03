<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/userStore'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const adminMenus = [
  { path: '/admin/slots', title: '时段配置', icon: 'Calendar' },
  { path: '/admin/spots', title: '钓位管理', icon: 'MapLocation' },
  { path: '/admin/reservations', title: '预约管理', icon: 'Tickets' },
  { path: '/admin/draws', title: '抽号记录', icon: 'Trophy' }
]

const merchantMenus = [
  { path: '/merchant/dashboard', title: '工作台', icon: 'Odometer' },
  { path: '/merchant/ponds', title: '鱼塘管理', icon: 'OfficeBuilding' },
  { path: '/merchant/pond-board', title: '鱼塘看板', icon: 'Monitor' },
  { path: '/merchant/time-slots', title: '场次管理', icon: 'Calendar' },
  { path: '/merchant/reservations', title: '预约管理', icon: 'Tickets' },
  { path: '/merchant/checkin', title: '核销签到', icon: 'Select' },
  { path: '/merchant/shop/products', title: '商品管理', icon: 'Goods' },
  { path: '/merchant/shop/orders', title: '商城订单', icon: 'List' },
  { path: '/merchant/restaurant/menus', title: '餐厅菜单', icon: 'CoffeeCup' },
  { path: '/merchant/restaurant/orders', title: '餐厅订单', icon: 'Dish' },
  { path: '/merchant/catches', title: '渔获回收', icon: 'Ship' },
  { path: '/merchant/revenue', title: '收益统计', icon: 'DataAnalysis' },
  { path: '/merchant/staff', title: '员工管理', icon: 'UserFilled' }
]

const userMenus = [
  { path: '/user/booking', title: '预约', icon: 'Calendar' },
  { path: '/user/reservations', title: '我的预约', icon: 'Tickets' },
  { path: '/user/spots', title: '我的钓位', icon: 'MapLocation' },
  { path: '/restaurant', title: '餐厅', icon: 'Bowl' },
  { path: '/restaurant/orders', title: '餐厅订单', icon: 'Dish' },
  { path: '/user/catches', title: '渔获回收', icon: 'Ship' },
  { path: '/shop', title: '商城', icon: 'ShoppingBag' },
  { path: '/shop/orders', title: '商城订单', icon: 'List' }
]

const activePath = computed(() => route.path)
const pageTitle = computed(() => (route.meta?.title as string) || '')
const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isManagementRoute = computed(() => route.path.startsWith('/admin') || route.path.startsWith('/merchant'))
const managementMenus = computed(() => (isAdminRoute.value ? adminMenus : merchantMenus))
const layoutTitle = computed(() => (isAdminRoute.value ? '平台管理' : '商家后台'))
const roleText = computed(() => {
  if (userStore.isAdmin) return '管理员'
  if (userStore.isMerchant) return '老板'
  if (userStore.isStaff) return '员工'
  return '会员'
})

const activeManagementPath = computed(() => {
  if (route.path.startsWith('/merchant/ponds')) return '/merchant/ponds'
  if (route.path.startsWith('/merchant/shop/orders')) return '/merchant/shop/orders'
  if (route.path.startsWith('/merchant/shop')) return '/merchant/shop/products'
  if (route.path.startsWith('/merchant/restaurant')) {
    if (route.path.includes('/menus')) return '/merchant/restaurant/menus'
    return '/merchant/restaurant/orders'
  }
  return route.path
})

const isActiveUserMenu = (path: string) => {
  if (path === '/shop') return route.path === '/shop' || route.path.startsWith('/shop/product') || route.path.startsWith('/shop/checkout')
  if (path === '/restaurant') return route.path === '/restaurant'
  if (path === '/user/reservations') return route.path === path || route.path.startsWith('/user/reservations/')
  return route.path === path || route.path.startsWith(path + '/')
}

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    ElMessage.success('已退出')
    router.push('/login')
  } catch {}
}
</script>

<template>
  <div v-if="isManagementRoute" class="layout">
    <aside class="sidebar fp-spotlight" v-fp-spotlight>
      <button class="logo" type="button" @click="router.push('/merchant/ponds')">
        <el-icon :size="26"><Aim /></el-icon>
        <span>
          <strong>野钓营地</strong>
          <small>{{ layoutTitle }}</small>
        </span>
      </button>
      <el-menu
        :default-active="activeManagementPath"
        class="sidebar-menu"
        background-color="transparent"
        text-color="var(--fp-menu-text)"
        active-text-color="var(--fp-menu-active-text)"
        router
      >
        <el-menu-item v-for="menu in managementMenus" :key="menu.path" :index="menu.path">
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="breadcrumb">
          <el-icon><Location /></el-icon>
          <span>{{ layoutTitle }}</span>
          <span v-if="pageTitle" class="split">/</span>
          <span v-if="pageTitle" class="current">{{ pageTitle }}</span>
        </div>
        <div class="user-info">
          <el-button v-if="userStore.isMerchant || userStore.isStaff" type="primary" link @click="router.push('/user/booking')">
            <el-icon><ShoppingBag /></el-icon>
            用户端应用
          </el-button>
          <span class="role-pill">{{ roleText }}</span>
          <el-icon><User /></el-icon>
          <span>{{ userStore.username || '未命名账号' }}</span>
          <el-button type="danger" link @click="logout">退出</el-button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>

  <div v-else class="user-layout">
    <header class="user-topbar fp-spotlight" v-fp-spotlight>
      <button class="brand" type="button" @click="router.push('/user/booking')">
        <span class="brand-mark"><el-icon><Aim /></el-icon></span>
        <span class="brand-text">
          <strong>野钓营地</strong>
          <small>预约 · 餐厅 · 回收 · 商城</small>
        </span>
      </button>

      <nav class="user-nav" aria-label="用户端应用栏">
        <button
          v-for="menu in userMenus"
          :key="menu.path"
          class="nav-item"
          :class="{ active: isActiveUserMenu(menu.path) }"
          type="button"
          @click="router.push(menu.path)"
        >
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </button>
      </nav>

      <div class="profile-box">
        <span class="app-label">应用栏</span>
        <el-button v-if="userStore.isMerchant || userStore.isStaff" plain size="small" @click="router.push('/merchant/ponds')">
          <el-icon><Setting /></el-icon>
          商家后台
        </el-button>
        <span class="user-role">{{ roleText }}</span>
        <span class="avatar">{{ (userStore.username || 'U').slice(0, 1).toUpperCase() }}</span>
        <span class="username">{{ userStore.username || '用户' }}</span>
        <el-button type="danger" text size="small" @click="logout">退出</el-button>
      </div>
    </header>

    <main class="user-content">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  min-width: 1180px;
}

.sidebar {
  --fp-menu-text: oklch(78% 0.04 165);
  --fp-menu-active-text: oklch(100% 0 0);

  width: 230px;
  background: linear-gradient(180deg, var(--fp-primary-dark) 0%, oklch(22% 0.05 165) 100%);
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.logo,
.sidebar-menu {
  position: relative;
  z-index: 1;
}

.logo {
  min-height: 72px;
  display: flex;
  align-items: center;
  gap: 12px;
  color: oklch(100% 0 0);
  padding: 0 20px;
  border: 0;
  border-bottom: 1px solid oklch(100% 0 0 / 0.1);
  background: transparent;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--fp-dur-fast) var(--fp-ease-out),
    padding-left var(--fp-dur-fast) var(--fp-ease-out);
}

.logo:hover {
  background-color: oklch(100% 0 0 / 0.04);
  padding-left: 26px;
}

.logo .el-icon {
  transition: transform var(--fp-dur-fast) var(--fp-ease-out);
}

.logo:hover .el-icon {
  transform: rotate(15deg) scale(1.1);
}

.logo strong,
.logo small {
  display: block;
}

.logo strong {
  font-size: 17px;
  line-height: 1.3;
  font-family: var(--fp-font-display);
}

.logo small {
  color: var(--fp-accent-light);
  font-size: 12px;
  margin-top: 2px;
}

.sidebar-menu {
  border-right: none;
  flex: 1;
  padding-top: 10px;
}

.sidebar-menu :deep(.el-menu-item) {
  border-radius: 10px;
  margin: 4px 10px;
  height: 46px;
  line-height: 46px;
  font-weight: 700;
  transition: background-color var(--fp-dur-fast) var(--fp-ease-out),
    color var(--fp-dur-fast) var(--fp-ease-out),
    transform var(--fp-dur-fast) var(--fp-ease-out),
    padding-left var(--fp-dur-fast) var(--fp-ease-out);
}

.sidebar-menu :deep(.el-menu-item:hover) {
  transform: translateX(4px);
  padding-left: 24px !important;
}

.sidebar-menu :deep(.el-menu-item .el-icon) {
  color: inherit;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out);
}

.sidebar-menu :deep(.el-menu-item:hover .el-icon) {
  transform: scale(1.15);
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: var(--fp-bg);
  overflow: hidden;
}

.topbar {
  min-height: 68px;
  background: var(--fp-surface);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 0 26px;
  border-bottom: 1px solid var(--fp-border);
  box-shadow: var(--fp-shadow-sm);
  flex-shrink: 0;
}

.breadcrumb,
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
}

.breadcrumb {
  font-size: 16px;
  font-weight: 800;
  color: var(--fp-primary);
  font-family: var(--fp-font-display);
}

.breadcrumb .split {
  color: var(--fp-accent);
}

.breadcrumb .current {
  color: var(--fp-text-secondary);
}

.user-info {
  color: var(--fp-text-secondary);
}

.role-pill {
  padding: 4px 10px;
  border-radius: 6px;
  background: var(--fp-accent-light);
  color: var(--fp-accent-dark);
  font-size: 12px;
  font-weight: 800;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out);
}

.user-info:hover .role-pill {
  transform: translateY(-1px);
  box-shadow: 0 4px 10px oklch(70% 0.12 85 / 0.22);
}

.content {
  flex: 1;
  padding: 24px;
  overflow: auto;
}

.user-layout {
  position: relative;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background:
    radial-gradient(circle at 8% 18%, oklch(38% 0.09 165 / 0.08) 0%, transparent 24%),
    radial-gradient(circle at 92% 82%, oklch(70% 0.12 85 / 0.08) 0%, transparent 26%),
    linear-gradient(180deg, oklch(98% 0.01 100) 0%, oklch(96% 0.01 100) 50%, var(--fp-bg) 100%);
  color: var(--fp-text);
}

.user-layout::before {
  content: '';
  position: fixed;
  inset: 0 0 auto 0;
  height: 260px;
  pointer-events: none;
  background:
    linear-gradient(90deg, var(--fp-primary-dark) 0%, var(--fp-primary) 45%, var(--fp-primary-light) 100%);
  clip-path: polygon(0 0, 100% 0, 100% 58%, 0 86%);
  z-index: 0;
}

.user-topbar {
  position: sticky;
  top: 0;
  min-height: 80px;
  background: var(--fp-surface);
  backdrop-filter: blur(20px);
  display: grid;
  grid-template-columns: minmax(190px, auto) minmax(360px, 1fr) auto;
  align-items: center;
  gap: 18px;
  margin: 16px auto 0;
  width: min(1320px, calc(100% - 40px));
  padding: 0 18px;
  border: 1px solid var(--fp-border);
  border-radius: var(--fp-radius);
  box-shadow: var(--fp-shadow-md);
  flex-shrink: 0;
  z-index: 100;
}

.brand,
.user-nav,
.profile-box {
  position: relative;
  z-index: 1;
}

.brand {
  min-width: 0;
  display: inline-flex;
  align-items: center;
  gap: 12px;
  border: 0;
  padding: 0;
  background: transparent;
  color: var(--fp-primary-dark);
  cursor: pointer;
  white-space: nowrap;
}

.brand-mark {
  width: 48px;
  height: 48px;
  border-radius: var(--fp-radius-sm);
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--fp-primary-dark) 0%, var(--fp-primary) 100%);
  color: var(--fp-accent);
  box-shadow: 0 12px 28px oklch(28% 0.06 165 / 0.26);
  transition: transform var(--fp-dur-fast) var(--fp-ease-out), box-shadow var(--fp-dur-fast) var(--fp-ease-out);
}

.brand:hover .brand-mark {
  transform: translateY(-1px);
  box-shadow: 0 16px 32px oklch(28% 0.06 165 / 0.32);
}

.brand-text {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
}

.brand-text strong {
  color: var(--fp-primary-dark);
  font-size: 20px;
  font-weight: 900;
  line-height: 1.2;
  font-family: var(--fp-font-display);
}

.brand-text small {
  margin-top: 3px;
  color: var(--fp-text-secondary);
  font-size: 12px;
  font-weight: 800;
}

.user-nav {
  justify-self: center;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  min-height: 50px;
  padding: 5px;
  border: 1px solid var(--fp-border);
  border-radius: var(--fp-radius-sm);
  background: oklch(97% 0.01 100 / 0.8);
  overflow-x: auto;
  max-width: 100%;
  scrollbar-width: none;
}

.user-nav::-webkit-scrollbar {
  display: none;
}

.nav-item {
  flex-shrink: 0;
  min-height: 40px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 0 14px;
  border: 0;
  border-radius: 10px;
  background: transparent;
  color: var(--fp-text-secondary);
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: color var(--fp-dur-fast) var(--fp-ease-out),
    background-color var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out),
    transform var(--fp-dur-fast) var(--fp-ease-out);
}

.nav-item:hover {
  color: var(--fp-primary-dark);
  background: oklch(38% 0.09 165 / 0.06);
  transform: translateY(-2px);
}

.nav-item.active {
  color: #fff;
  background: linear-gradient(135deg, var(--fp-primary-dark) 0%, var(--fp-primary) 100%);
  box-shadow: 0 10px 22px oklch(28% 0.06 165 / 0.22);
}

.nav-item.active:hover {
  transform: translateY(-2px);
  box-shadow: 0 14px 28px oklch(28% 0.06 165 / 0.3);
}

.nav-item .el-icon {
  transition: transform var(--fp-dur-fast) var(--fp-ease-out);
}

.nav-item:hover .el-icon {
  transform: scale(1.2) rotate(-5deg);
}

.profile-box {
  justify-self: end;
  display: inline-flex;
  align-items: center;
  gap: 8px;
  color: var(--fp-text-secondary);
  white-space: nowrap;
  min-height: 46px;
  padding: 5px 8px 5px 12px;
  border: 1px solid var(--fp-border);
  border-radius: var(--fp-radius-sm);
  background: var(--fp-surface);
}

.app-label {
  padding: 4px 8px;
  border-radius: 6px;
  background: var(--fp-primary-dark);
  color: var(--fp-accent);
  font-size: 12px;
  font-weight: 900;
}

.user-role {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--fp-accent-light);
  color: var(--fp-accent-dark);
  font-size: 12px;
  font-weight: 900;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--fp-primary-dark) 0%, var(--fp-primary) 100%);
  color: var(--fp-accent);
  font-weight: 900;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out);
}

.profile-box:hover .avatar {
  transform: scale(1.08);
  box-shadow: 0 0 0 2px oklch(70% 0.12 85 / 0.35);
}

.username {
  max-width: 120px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-weight: 800;
  color: var(--fp-text);
}

.user-content {
  position: relative;
  z-index: 1;
  flex: 1;
  min-height: 0;
  padding-top: 4px;
}

@media (max-width: 1180px) {
  .user-topbar {
    grid-template-columns: 1fr;
    align-items: start;
    padding: 14px 16px;
  }

  .user-nav {
    justify-self: stretch;
  }

  .profile-box {
    justify-self: start;
  }
}

@media (max-width: 980px) {
  .layout {
    min-width: auto;
    flex-direction: column;
  }

  .sidebar {
    width: 100%;
  }

  .logo {
    min-height: 58px;
  }

  .topbar {
    min-height: 56px;
    height: auto;
    padding: 10px 14px;
    flex-wrap: wrap;
  }

  .content {
    padding: 12px;
  }

  .brand-text small,
  .username,
  .user-role,
  .app-label {
    display: none;
  }

  .user-topbar {
    width: calc(100% - 20px);
    margin-top: 10px;
  }

  .nav-item {
    padding: 0 11px;
  }
}
</style>
