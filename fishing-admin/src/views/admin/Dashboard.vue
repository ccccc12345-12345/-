<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()

const menus = [
  { path: '/admin/reservations', title: '预约管理', icon: 'Tickets', color: '#0f4c75' },
  { path: '/admin/slots', title: '时段配置', icon: 'Calendar', color: '#3282b8' },
  { path: '/admin/spots', title: '钓位管理', icon: 'MapLocation', color: '#43a047' },
  { path: '/admin/draws', title: '抽号记录', icon: 'Trophy', color: '#f9a825' }
]
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>管理首页</h2></div>

    <el-row :gutter="16">
      <el-col v-for="menu in menus" :key="menu.path" :xs="24" :sm="12" :md="12" :lg="6">
        <div class="menu-card" :style="{ background: menu.color }" @click="router.push(menu.path)">
          <el-icon :size="40"><component :is="menu.icon" /></el-icon>
          <span class="title">{{ menu.title }}</span>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped>
.page-container {
  padding-bottom: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  color: #0f4c75;
}

.menu-card {
  height: 160px;
  border-radius: 16px;
  color: white;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  cursor: pointer;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12);
  transition: transform 0.2s;
}

.menu-card:hover {
  transform: translateY(-4px);
}

.menu-card .title {
  font-size: 18px;
  font-weight: 600;
}
</style>
