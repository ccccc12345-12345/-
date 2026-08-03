<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'

const router = useRouter()
const userStore = useUserStore()

const menus = [
  { path: '/admin/slots', title: '时段管理', icon: 'Calendar', color: '#1a5f7a' },
  { path: '/admin/spots', title: '钓位管理', icon: 'MapLocation', color: '#57cc99' },
  { path: '/admin/reservations', title: '预约记录', icon: 'Tickets', color: '#f6d365' },
  { path: '/admin/draws', title: '抽号记录', icon: 'Trophy', color: '#ff6b6b' }
]

const logout = () => {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <div class="admin-home">
    <div class="admin-header">
      <h2>管理后台</h2>
      <div class="actions">
        <el-button type="primary" plain size="small" @click="$router.push('/')">返回前台</el-button>
        <el-button type="danger" plain size="small" @click="logout">退出登录</el-button>
      </div>
    </div>

    <div class="menu-grid">
      <div
        v-for="menu in menus"
        :key="menu.path"
        class="menu-card"
        :style="{ background: menu.color }"
        @click="$router.push(menu.path)"
      >
        <el-icon :size="32"><component :is="menu.icon" /></el-icon>
        <span class="menu-title">{{ menu.title }}</span>
      </div>
    </div>
  </div>
</template>

<style scoped>
.admin-home {
  min-height: 100vh;
  padding: 20px;
  background: var(--bg);
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
}

.admin-header h2 {
  color: var(--primary);
  font-size: 20px;
}

.actions {
  display: flex;
  gap: 8px;
}

.menu-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
}

.menu-card {
  height: 120px;
  border-radius: 20px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: white;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
}

.menu-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
