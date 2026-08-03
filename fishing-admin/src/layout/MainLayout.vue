<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { usePondStore } from '@/store/pond'
import { ElMessage, ElMessageBox } from 'element-plus'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()
const pondStore = usePondStore()

const userMenus = [
  { path: '/user/booking', title: '场地预约', icon: 'Calendar' },
  { path: '/user/reservations', title: '我的预约', icon: 'Tickets' },
  { path: '/user/spots', title: '我的钓位', icon: 'MapLocation' }
]

const adminMenus = [
  { path: '/admin/dashboard', title: '管理首页', icon: 'HomeFilled' },
  { path: '/admin/reservations', title: '预约管理', icon: 'Tickets' },
  { path: '/admin/checkin', title: '扫码核销', icon: 'FullScreen' },
  { path: '/admin/revenue', title: '收益统计', icon: 'Money' },
  { path: '/admin/slots', title: '时段配置', icon: 'Calendar' },
  { path: '/admin/spots', title: '钓位管理', icon: 'MapLocation' },
  { path: '/admin/draws', title: '抽号记录', icon: 'Trophy' },
  { path: '/admin/ponds', title: '鱼塘管理', icon: 'OfficeBuilding' }
]

const superAdminMenus = [
  { path: '/admin/users', title: '用户管理', icon: 'UserFilled' }
]

const merchantMenus = [
  { path: '/merchant/dashboard', title: '工作台', icon: 'HomeFilled' },
  { path: '/merchant/ponds', title: '鱼塘管理', icon: 'OfficeBuilding', ownerOnly: true },
  { path: '/merchant/time-slots', title: '时段配置', icon: 'Calendar' },
  { path: '/merchant/spots', title: '钓位管理', icon: 'MapLocation' },
  { path: '/merchant/pond-board', title: '钓位看板', icon: 'Grid' },
  { path: '/merchant/checkin', title: '扫码核销', icon: 'FullScreen' },
  { path: '/merchant/reservations', title: '预约管理', icon: 'Tickets' },
  { path: '/merchant/revenue', title: '收益统计', icon: 'Money' },
  { path: '/merchant/draw-results', title: '抽号记录', icon: 'Trophy' },
  { path: '/merchant/shop/products', title: '商品管理', icon: 'Goods' },
  { path: '/merchant/catches', title: '渔获回收', icon: 'Document' },
  { path: '/merchant/restaurant/menus', title: '餐厅菜单', icon: 'ForkSpoon' },
  { path: '/merchant/restaurant/orders', title: '餐厅订单', icon: 'List' },
  { path: '/merchant/staff', title: '员工管理', icon: 'UserFilled', ownerOnly: true }
]

const layoutTitle = computed(() => {
  if (userStore.isAdmin) return '管理员后台'
  if (userStore.isMerchant || userStore.isStaff) return '商家工作台'
  return ''
})

const menus = computed(() => {
  if (userStore.isSuperAdmin) return [...adminMenus, ...superAdminMenus]
  if (userStore.isAdmin) return adminMenus
  if (userStore.isMerchant) return merchantMenus
  if (userStore.isStaff) {
    return merchantMenus.filter(menu => {
      if (menu.ownerOnly) return false
      return getAllowedMerchantPaths(userStore.staffRole).includes(menu.path)
    })
  }
  return userMenus
})
const activePath = computed(() => route.path)

const mobileTabs = [
  { path: '/user/dashboard', title: '首页', icon: '🏠' },
  { path: '/shop', title: '商城', icon: '🛒' },
  { path: '/my-catches', title: '渔获', icon: '🎣' },
  { path: '/restaurant', title: '餐厅', icon: '🍽️' },
  { path: '/user/reservations', title: '我的', icon: '👤' }
]

onMounted(() => {
  if (userStore.isAdmin && pondStore.ponds.length === 0) {
    pondStore.loadPonds(userStore.pondId)
  }
  if ((userStore.isMerchant || userStore.isStaff) && pondStore.ponds.length === 0) {
    pondStore.loadMerchantPonds(userStore.pondId)
  }
})

const logout = async () => {
  try {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning' })
    userStore.logout()
    ElMessage.success('已退出')
    router.push('/login')
  } catch {}
}

function getAllowedMerchantPaths(staffRole: string): string[] {
  switch (staffRole) {
    case 'checker':
      return ['/merchant/checkin']
    case 'operator':
      return ['/merchant/time-slots', '/merchant/spots', '/merchant/pond-board']
    case 'finance':
      return ['/merchant/revenue', '/merchant/reservations', '/merchant/shop/products', '/merchant/restaurant/orders']
    case 'manager':
      return [
        '/merchant/dashboard', '/merchant/time-slots', '/merchant/spots', '/merchant/pond-board',
        '/merchant/checkin', '/merchant/reservations', '/merchant/revenue', '/merchant/draw-results',
        '/merchant/shop/products', '/merchant/catches', '/merchant/restaurant/menus', '/merchant/restaurant/orders'
      ]
    default:
      return ['/merchant/dashboard']
  }
}
</script>

<template>
  <!-- 管理员/商家/员工：侧边栏 + 顶部栏 -->
  <div v-if="userStore.isAdmin || userStore.isMerchant || userStore.isStaff" class="layout">
    <aside class="sidebar">
      <div class="logo">
        <el-icon :size="28" color="#fff"><Watermelon /></el-icon>
        <span>钓鱼场预约系统</span>
      </div>
      <el-menu
        :default-active="activePath"
        class="sidebar-menu"
        background-color="#0f4c75"
        text-color="#b0c4de"
        active-text-color="#fff"
        router
      >
        <el-menu-item v-for="menu in menus" :key="menu.path" :index="menu.path">
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.title }}</span>
        </el-menu-item>
      </el-menu>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="breadcrumb">{{ layoutTitle }}</div>
        <div class="user-info">
          <template v-if="pondStore.ponds.length > 0">
            <el-select
              v-if="userStore.isSuperAdmin || userStore.isMerchant"
              v-model="pondStore.currentPondId"
              placeholder="选择鱼塘"
              style="width: 160px; margin-right: 16px"
              size="small"
            >
              <el-option
                v-for="pond in pondStore.ponds"
                :key="pond.id"
                :label="pond.name"
                :value="pond.id"
              />
            </el-select>
            <span v-else class="pond-label">
              <el-icon><OfficeBuilding /></el-icon>
              {{ pondStore.currentPond?.name || '未绑定鱼塘' }}
            </span>
          </template>
          <el-icon><User /></el-icon>
          <span>{{ userStore.username || '用户' }}</span>
          <el-button
            v-if="userStore.isMerchant || userStore.isStaff"
            type="primary"
            link
            @click="router.push('/user/dashboard')"
          >
            <el-icon><HomeFilled /></el-icon>
            返回主页
          </el-button>
          <el-button type="danger" link @click="logout">退出登录</el-button>
        </div>
      </header>
      <main class="content">
        <router-view />
      </main>
    </div>
  </div>

  <!-- 普通用户：仅顶部栏 -->
  <div v-else class="user-layout">
    <header class="user-topbar">
      <div class="user-topbar-left">
        <el-icon :size="24" color="#0f4c75" style="margin-right: 8px"><Watermelon /></el-icon>
        <span class="system-name">钓鱼场预约系统</span>
      </div>
      <div class="user-topbar-right">
        <el-button text @click="router.push('/user/dashboard')">
          <el-icon><HomeFilled /></el-icon>
          首页
        </el-button>
        <el-button text @click="router.push('/shop')">
          <el-icon><Goods /></el-icon>
          商城
        </el-button>
        <el-button text @click="router.push('/user/catches')">
          <el-icon><Document /></el-icon>
          渔获
        </el-button>
        <el-button text @click="router.push('/restaurant')">
          <el-icon><ForkSpoon /></el-icon>
          餐厅
        </el-button>
        <el-button
          v-if="userStore.isMerchant"
          text
          @click="router.push('/merchant/dashboard')"
        >
          <el-icon><Setting /></el-icon>
          管理后台
        </el-button>
        <el-divider direction="vertical" />
        <el-icon><User /></el-icon>
        <span class="username">{{ userStore.username || '用户' }}</span>
        <el-button type="danger" plain size="small" @click="logout">退出</el-button>
      </div>
    </header>
    <main class="user-content">
      <router-view />
    </main>

    <nav class="mobile-tab-bar">
      <div
        v-for="tab in mobileTabs"
        :key="tab.path"
        class="mobile-tab"
        :class="{ active: route.path === tab.path || route.path.startsWith(tab.path + '/') }"
        @click="router.push(tab.path)"
      >
        <span class="tab-icon">{{ tab.icon }}</span>
        <span class="tab-title">{{ tab.title }}</span>
      </div>
    </nav>
  </div>
</template>

<style scoped>
.layout {
  display: flex;
  height: 100vh;
  min-width: 1200px;
}

.sidebar {
  width: 220px;
  background: #0f4c75;
  flex-shrink: 0;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: white;
  font-size: 16px;
  font-weight: 700;
  border-bottom: 1px solid rgba(255, 255, 255, 0.1);
}

.sidebar-menu {
  border-right: none;
  flex: 1;
}

.main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: #f5f7fa;
  overflow: hidden;
}

.topbar {
  height: 60px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
}

.breadcrumb {
  font-size: 16px;
  font-weight: 600;
  color: #0f4c75;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #606266;
}

.pond-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-right: 16px;
  color: #0f4c75;
  font-weight: 600;
}

.content {
  flex: 1;
  padding: 20px;
  overflow: auto;
}

@media (max-width: 768px) {
  .layout {
    min-width: auto;
    flex-direction: column;
  }
  .sidebar {
    width: 100%;
    height: auto;
    flex-direction: row;
    align-items: center;
    padding: 0 12px;
  }
  .logo {
    height: 48px;
    border-bottom: none;
    flex-shrink: 0;
    padding: 0 8px;
  }
  .sidebar-menu {
    display: flex;
    flex-direction: row;
    overflow-x: auto;
    padding: 4px 0;
  }
  .sidebar-menu :deep(.el-menu-item) {
    height: 40px;
    line-height: 40px;
    padding: 0 12px !important;
  }
  .main {
    width: 100%;
    overflow: auto;
  }
  .topbar {
    height: auto;
    min-height: 48px;
    padding: 8px 12px;
    flex-wrap: wrap;
    gap: 8px;
  }
  .user-info {
    flex-wrap: wrap;
    justify-content: flex-end;
  }
  .content {
    padding: 12px;
  }
}

/* ====== 普通用户布局 ====== */
.user-layout {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f0f2f5;
  overflow: hidden;
}

.user-topbar {
  height: 56px;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  flex-shrink: 0;
  z-index: 100;
}

.user-topbar-left {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: 700;
  color: #0f4c75;
}

.user-topbar-right {
  display: flex;
  align-items: center;
  gap: 10px;
  color: #606266;
  font-size: 14px;
}

.user-topbar-right .username {
  font-weight: 500;
  color: #303133;
}

.user-content {
  flex: 1;
  overflow: auto;
  padding: 0;
  padding-bottom: 64px;
}

.mobile-tab-bar {
  display: none;
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  height: 56px;
  background: #fff;
  border-top: 1px solid #ebeef5;
  z-index: 200;
  justify-content: space-around;
  align-items: center;
}

.mobile-tab {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  color: #7a8b7b;
  cursor: pointer;
  font-size: 11px;
  flex: 1;
  height: 100%;
}

.mobile-tab.active {
  color: #1b6b4a;
}

.tab-icon {
  font-size: 20px;
  line-height: 1;
}

@media (max-width: 768px) {
  .user-topbar {
    padding: 0 12px;
  }
  .user-topbar-left .system-name {
    font-size: 15px;
  }
  .mobile-tab-bar {
    display: flex;
  }
}
</style>
