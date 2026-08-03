<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyReservations, cancelReservation, type Reservation } from '@/api/reservation'
import { formatDate, formatTime, countdownText } from '@/utils/date'

const router = useRouter()

const reservations = ref<Reservation[]>([])
const loading = ref(false)
const countdownTarget = ref('')
const countdownStr = ref('')
let timer: number | null = null

const sortedReservations = computed(() => {
  return [...reservations.value].sort((a, b) => {
    return new Date(b.createTime).getTime() - new Date(a.createTime).getTime()
  })
})

const isInDrawWindow = (r: Reservation) => {
  if (!r.slot) return false
  const now = new Date().getTime()
  const start = new Date(r.slot.drawStartTime).getTime()
  const end = new Date(r.slot.drawEndTime).getTime()
  return now >= start && now <= end
}

const isBeforeDraw = (r: Reservation) => {
  if (!r.slot) return false
  const now = new Date().getTime()
  const start = new Date(r.slot.drawStartTime).getTime()
  return now < start
}

const loadReservations = async () => {
  loading.value = true
  try {
    const res = await getMyReservations()
    reservations.value = res.data.records
    updateCountdown()
  } finally {
    loading.value = false
  }
}

const updateCountdown = () => {
  const pending = reservations.value.find(r => r.status === '待抽号' && r.slot)
  if (pending && pending.slot) {
    const now = new Date().getTime()
    const start = new Date(pending.slot.drawStartTime).getTime()
    const end = new Date(pending.slot.drawEndTime).getTime()
    if (now < start) {
      countdownTarget.value = pending.slot.drawStartTime
      countdownStr.value = countdownText(pending.slot.drawStartTime)
    } else if (now <= end) {
      countdownTarget.value = pending.slot.drawEndTime
      countdownStr.value = '抽号进行中'
    }
  }
}

const handleCancel = async (r: Reservation) => {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？', '取消预约', {
      confirmButtonText: '确定',
      cancelButtonText: '再想想',
      type: 'warning'
    })
    await cancelReservation(r.id)
    ElMessage.success('取消成功')
    loadReservations()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '取消失败')
    }
  }
}

const goDraw = (r: Reservation) => {
  router.push(`/draw/${r.id}`)
}

const statusClass = (status: string) => {
  const map: Record<string, string> = {
    '待抽号': 'status-pending',
    '已抽号': 'status-drawn',
    '预约取消': 'status-cancelled',
    '过期失效': 'status-expired'
  }
  return map[status] || ''
}

onMounted(() => {
  loadReservations()
  timer = window.setInterval(() => {
    updateCountdown()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<template>
  <div class="page reservations-page">
    <h2 class="page-title">我的预约</h2>

    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="sortedReservations.length === 0" class="empty">
      暂无预约记录
    </div>

    <div v-else class="reservation-list">
      <div v-for="r in sortedReservations" :key="r.id" class="card reservation-card">
        <div class="card-header">
          <span class="date">{{ r.slot ? formatDate(r.slot.slotDate) : '' }}</span>
          <span class="status-tag" :class="statusClass(r.status)">{{ r.status }}</span>
        </div>

        <div class="card-body" v-if="r.slot">
          <div class="info">
            <div class="name">{{ r.slot.slotName }}</div>
            <div class="time">{{ formatTime(r.slot.startTime) }} - {{ formatTime(r.slot.endTime) }}</div>
          </div>
          <div v-if="r.status === '已抽号' && r.drawResult" class="spot-code">
            {{ r.drawResult.spotCode }}
          </div>
        </div>

        <div class="card-footer">
          <template v-if="r.status === '待抽号'">
            <template v-if="isInDrawWindow(r)">
              <button class="draw-btn" @click="goDraw(r)">一键抽号</button>
            </template>
            <template v-else-if="isBeforeDraw(r)">
              <div class="countdown">
                <el-icon><Timer /></el-icon>
                <span>距抽号开始 {{ countdownStr }}</span>
              </div>
              <el-button size="small" type="danger" plain @click="handleCancel(r)">取消预约</el-button>
            </template>
          </template>
          <span v-else-if="r.status === '过期失效'" class="expired-tip">已过期，请重新预约</span>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.page-title {
  font-size: 20px;
  color: var(--primary);
  margin-bottom: 16px;
}

.reservation-card {
  position: relative;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.card-header .date {
  font-size: 14px;
  color: var(--text-secondary);
}

.card-body {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.card-body .name {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 4px;
}

.card-body .time {
  font-size: 13px;
  color: var(--text-secondary);
}

.spot-code {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--secondary), var(--primary));
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  min-height: 44px;
}

.countdown {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--accent);
  font-size: 13px;
  font-weight: 600;
}

.expired-tip {
  color: var(--text-secondary);
  font-size: 13px;
}

.loading, .empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-secondary);
}
</style>
