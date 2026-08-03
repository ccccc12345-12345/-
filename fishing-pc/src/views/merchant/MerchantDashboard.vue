<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { dashboardStats, getMerchantReservations, type DashboardStats, type Reservation } from '@/api/merchant'
import { useMerchantPush } from '@/composables/useMerchantPush'
import { formatDate, formatDateTime } from '@/utils/date'

const router = useRouter()

const stats = ref<DashboardStats | null>(null)
const recentReservations = ref<Reservation[]>([])
const loading = ref(false)

const shortcuts = [
  { title: '扫码核销', path: '/merchant/checkin', icon: 'FullScreen', color: '#67c23a' },
  { title: '钓位看板', path: '/merchant/pond-board', icon: 'Grid', color: '#3282b8' },
  { title: '发布时段', path: '/merchant/time-slots', icon: 'Calendar', color: '#e6a23c' }
]

const loadStats = async () => {
  try {
    const res = await dashboardStats(undefined)
    stats.value = res.data
  } catch (e: any) {
    ElMessage.error(e.message || '加载统计失败')
  }
}

const loadReservations = async () => {
  loading.value = true
  try {
    const res = await getMerchantReservations({
      pondId: undefined,
      pageNum: 1,
      pageSize: 5
    })
    recentReservations.value = res.data.records || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载预约失败')
  } finally {
    loading.value = false
  }
}

const refresh = () => {
  loadStats()
  loadReservations()
}

useMerchantPush({
  events: ['DASHBOARD_REFRESH', 'RESERVATION_CREATED', 'RESERVATION_CHECKED_IN', 'RESTAURANT_ORDER_CREATED'],
  onEvent: () => refresh(),
  fallback: () => refresh()
})

onMounted(refresh)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>商家工作台</h2>
      <div class="text-sm text-gray-500">{{ formatDate(new Date()) }}</div>
    </div>

    <el-row :gutter="16" class="stats-row">
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card income">
          <div class="stat-label">今日收入</div>
          <div class="stat-value">¥{{ stats?.todayIncome?.toFixed(2) ?? '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card reservations">
          <div class="stat-label">今日预约数</div>
          <div class="stat-value">{{ stats?.todayReservationCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card checkins">
          <div class="stat-label">今日核销数</div>
          <div class="stat-value">{{ stats?.todayCheckinCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="stat-card occupancy">
          <div class="stat-label">上座率</div>
          <div class="stat-value">{{ stats?.occupancyRate ?? 0 }}%</div>
        </el-card>
      </el-col>
    </el-row>

    <el-row :gutter="16" class="shortcut-row">
      <el-col v-for="item in shortcuts" :key="item.path" :xs="24" :sm="8">
        <div class="shortcut-card" :style="{ background: item.color }" @click="router.push(item.path)">
          <el-icon :size="32"><component :is="item.icon" /></el-icon>
          <span>{{ item.title }}</span>
        </div>
      </el-col>
    </el-row>

    <el-card v-loading="loading" class="recent-card">
      <template #header>
        <div class="card-header">
          <span>最近预约</span>
          <el-button link type="primary" @click="router.push('/merchant/reservations')">查看全部</el-button>
        </div>
      </template>
      <el-table :data="recentReservations" stripe>
        <el-table-column label="日期/时段" width="160">
          <template #default="{ row }">{{ row.slotDate }} {{ row.slotName }}</template>
        </el-table-column>
        <el-table-column prop="userPhone" label="手机号" width="130" />
        <el-table-column prop="userNickname" label="昵称" width="120" />
        <el-table-column prop="spotCode" label="钓位" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" min-width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<style scoped>
.page-container {
  padding-bottom: 20px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.page-header h2 {
  color: #0f4c75;
  margin: 0;
}

.stats-row {
  margin-bottom: 16px;
}

.stat-card {
  margin-bottom: 16px;
  border-radius: 12px;
  color: white;
}

.stat-card :deep(.el-card__body) {
  padding: 20px;
}

.stat-card.income {
  background: linear-gradient(135deg, #0f4c75, #3282b8);
}

.stat-card.reservations {
  background: linear-gradient(135deg, #409eff, #79bbff);
}

.stat-card.checkins {
  background: linear-gradient(135deg, #67c23a, #95d475);
}

.stat-card.occupancy {
  background: linear-gradient(135deg, #e6a23c, #f3d19e);
}

.stat-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.stat-value {
  font-size: 28px;
  font-weight: 700;
}

.shortcut-row {
  margin-bottom: 16px;
}

.shortcut-card {
  height: 80px;
  border-radius: 12px;
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 12px;
  cursor: pointer;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  transition: transform 0.2s;
  margin-bottom: 16px;
  font-size: 16px;
  font-weight: 600;
}

.shortcut-card:hover {
  transform: translateY(-3px);
}

.recent-card {
  min-height: 300px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
</style>
