<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyReservations, cancelReservation, type Reservation } from '@/api/reservation'
import { drawSpot } from '@/api/draw'
import { formatDate, formatTime, countdownText } from '@/utils/date'

const reservations = ref<Reservation[]>([])
const loading = ref(false)
const drawLoading = ref(false)
const drawVisible = ref(false)
const drawResult = ref('')
const rollingSpot = ref('')
const rollingTimer = ref<number | null>(null)
const currentReservation = ref<Reservation | null>(null)
const countdownStr = ref('')
const notifiedReservationIds = ref<Set<number>>(new Set())
let timer: number | null = null

const list = computed(() => {
  return [...reservations.value].sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyReservations()
    reservations.value = res.data.records || []
  } finally {
    loading.value = false
  }
  updateCountdown()
}

const updateCountdown = () => {
  const pending = reservations.value.find(r => r.status === '待抽号' && r.drawStartTime)
  if (pending && pending.drawStartTime) {
    countdownStr.value = countdownText(pending.drawStartTime)
  }
}

const requestNotificationPermission = () => {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission()
  }
}

const checkDrawNotification = () => {
  if (!('Notification' in window) || Notification.permission !== 'granted') return
  reservations.value.forEach(r => {
    if (r.status !== '待抽号' || !r.drawStartTime) return
    if (notifiedReservationIds.value.has(r.id)) return
    const drawTime = new Date(r.drawStartTime).getTime()
    const now = Date.now()
    const tenMinutes = 10 * 60 * 1000
    if (drawTime - now > 0 && drawTime - now <= tenMinutes) {
      notifiedReservationIds.value.add(r.id)
      new Notification('抽号即将开始', {
        body: `${r.pondName || ''} ${r.slotName || ''} 抽号将在10分钟内开始，请准备一键抽号`,
        icon: '/favicon.svg'
      })
    }
  })
}

const isInWindow = (r: Reservation) => {
  if (!r.drawStartTime || !r.drawEndTime) return false
  const now = Date.now()
  return now >= new Date(r.drawStartTime).getTime() && now <= new Date(r.drawEndTime).getTime()
}

const isBeforeWindow = (r: Reservation) => {
  if (!r.drawStartTime) return false
  return Date.now() < new Date(r.drawStartTime).getTime()
}

const isExpired = (r: Reservation) => {
  if (!r.drawEndTime) return false
  return Date.now() > new Date(r.drawEndTime).getTime()
}

const handleCancel = async (r: Reservation) => {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？', '取消预约', { type: 'warning' })
    await cancelReservation(r.id)
    ElMessage.success('取消成功')
    loadData()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '取消失败')
  }
}

const startRolling = () => {
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789'
  rollingTimer.value = window.setInterval(() => {
    let result = ''
    for (let i = 0; i < 3; i++) {
      result += chars.charAt(Math.floor(Math.random() * chars.length))
    }
    rollingSpot.value = result
  }, 80)
}

const stopRolling = (spotCode: string) => {
  if (rollingTimer.value) {
    clearInterval(rollingTimer.value)
    rollingTimer.value = null
  }
  drawResult.value = spotCode || ''
}

const handleDraw = async (r: Reservation) => {
  drawLoading.value = true
  drawVisible.value = true
  drawResult.value = ''
  rollingSpot.value = ''
  currentReservation.value = r
  startRolling()
  try {
    const res = await drawSpot(r.id)
    setTimeout(() => {
      stopRolling(res.data)
      drawLoading.value = false
    }, 1500)
  } catch (e: any) {
    if (rollingTimer.value) clearInterval(rollingTimer.value)
    drawVisible.value = false
    drawLoading.value = false
    ElMessage.error(e.message || '抽号失败')
  }
}

/**
 * 抽号功能测试按钮：纯前端模拟完整抽号流程
 * 包括随机滚动动画、结果展示、弹窗交互，不依赖后端真实预约
 */
const handleTestDraw = () => {
  const today = new Date().toISOString().split('T')[0]
  const now = new Date().toISOString()
  const mockReservation: Reservation = {
    id: 0,
    userId: 0,
    slotId: 0,
    status: '待抽号',
    createTime: now,
    cancelTime: null,
    cancelReason: null,
    actualFee: null,
    checkInTime: null,
    checkinCode: null,
    pondId: null,
    pondName: null,
    userPhone: null,
    userNickname: null,
    slotDate: today,
    slotName: '测试场次',
    startTime: '08:00:00',
    endTime: '12:00:00',
    drawStartTime: now,
    drawEndTime: new Date(Date.now() + 3600000).toISOString(),
    spotCode: null
  }

  drawLoading.value = true
  drawVisible.value = true
  drawResult.value = ''
  rollingSpot.value = ''
  currentReservation.value = mockReservation
  startRolling()

  // 模拟后端抽号延迟，随机生成 A01 ~ A30 钓位号
  const chars = 'ABCDEFGHIJKLMNOPQRSTUVWXYZ'
  const letter = chars.charAt(Math.floor(Math.random() * chars.length))
  const number = String(Math.floor(Math.random() * 30) + 1).padStart(2, '0')
  const mockSpotCode = `${letter}${number}`

  setTimeout(() => {
    stopRolling(mockSpotCode)
    drawLoading.value = false

    // 抽号完成后，在预约列表插入一条已抽号的测试记录，直观展示结果
    const testRecord: Reservation = {
      id: Date.now(),
      userId: 0,
      slotId: 0,
      status: '已抽号',
      createTime: new Date().toISOString(),
      cancelTime: null,
      cancelReason: null,
      actualFee: null,
      checkInTime: null,
      checkinCode: null,
      pondId: null,
      pondName: null,
      userPhone: null,
      userNickname: null,
      slotDate: today,
      slotName: '测试场次',
      startTime: '08:00:00',
      endTime: '12:00:00',
      drawStartTime: now,
      drawEndTime: new Date(Date.now() + 3600000).toISOString(),
      spotCode: mockSpotCode
    }
    reservations.value.unshift(testRecord)

    ElMessage.success(`测试抽号完成：${mockSpotCode}，已添加到预约列表`)
  }, 1800)
}

const closeDrawDialog = () => {
  drawVisible.value = false
  loadData()
}

const statusType = (status: string) => {
  const map: Record<string, string> = {
    '待抽号': 'warning',
    '已抽号': 'success',
    '预约取消': 'info',
    '过期失效': 'danger',
    '已作废': 'danger'
  }
  return map[status] || 'info'
}

const isVoided = (r: Reservation) => r.status === '已作废'

const formatDateTime = (time?: string | null) => {
  return time ? time.replace('T', ' ').substring(0, 19) : '-'
}

const viewSpot = (r: Reservation) => {
  ElMessageBox.alert(`您的钓位编号为：${r.spotCode || '-'}`, '钓位详情', { confirmButtonText: '知道了' })
}

onMounted(() => {
  requestNotificationPermission()
  loadData()
  timer = window.setInterval(() => {
    updateCountdown()
    checkDrawNotification()
  }, 1000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
  if (rollingTimer.value) clearInterval(rollingTimer.value)
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的预约</h2>
      <el-button type="primary" icon="Pointer" @click="handleTestDraw">抽号效果测试</el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column label="日期" width="120">
          <template #default="{ row }">{{ row.slotDate ? formatDate(row.slotDate) : '-' }}</template>
        </el-table-column>
        <el-table-column label="时段名称" width="120">
          <template #default="{ row }">{{ row.slotName || '-' }}</template>
        </el-table-column>
        <el-table-column label="时间范围" width="160">
          <template #default="{ row }">{{ row.startTime && row.endTime ? formatTime(row.startTime) + ' - ' + formatTime(row.endTime) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="pondName" label="鱼塘" width="120" />
        <el-table-column label="状态" width="140">
          <template #default="{ row }">
            <div class="status-cell">
              <el-tag :type="statusType(row.status)">{{ row.status }}</el-tag>
              <span v-if="isVoided(row)" class="void-timestamp">
                {{ formatDateTime(row.cancelTime) }}
              </span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="钓位号" width="120">
          <template #default="{ row }">
            <span v-if="row.spotCode" class="spot-text">{{ row.spotCode }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <template v-if="isVoided(row)">
              <el-alert
                class="void-alert"
                title="该时段已被管理员删除，此预约已作废"
                type="error"
                :closable="false"
                show-icon
              />
            </template>
            <template v-else>
              <template v-if="row.status === '待抽号'">
                <el-button v-if="isInWindow(row)" type="warning" size="small" icon="Pointer" @click="handleDraw(row)">一键抽号</el-button>
                <span v-else-if="isBeforeWindow(row)" class="countdown">{{ countdownStr }}</span>
                <el-tag v-else-if="isExpired(row)" type="danger">已失效</el-tag>
                <el-button v-if="isBeforeWindow(row)" type="danger" link size="small" @click="handleCancel(row)">取消预约</el-button>
              </template>
              <el-button v-if="row.status === '已抽号'" type="success" size="small" @click="viewSpot(row)">查看钓位</el-button>
            </template>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="drawVisible"
      title="抽号结果"
      width="420px"
      align-center
      :close-on-click-modal="false"
      @closed="closeDrawDialog"
    >
      <div class="draw-box">
        <div class="draw-result">
          <span v-if="drawLoading || !drawResult">{{ rollingSpot }}</span>
          <span v-else>{{ drawResult }}</span>
        </div>
        <p v-if="drawLoading" class="draw-tip">正在抽取最佳钓位...</p>
        <p v-else class="draw-tip">恭喜您获得钓位 <strong>{{ drawResult || '-' }}</strong></p>
        <div v-if="currentReservation && !drawLoading" class="draw-info">
          <p><strong>日期：</strong>{{ currentReservation.slotDate ? formatDate(currentReservation.slotDate) : '-' }}</p>
          <p><strong>时段：</strong>{{ currentReservation.slotName || '-' }}</p>
          <p><strong>时间：</strong>{{ currentReservation.startTime && currentReservation.endTime ? formatTime(currentReservation.startTime) + ' - ' + formatTime(currentReservation.endTime) : '-' }}</p>
        </div>
      </div>
      <template #footer>
        <el-button @click="closeDrawDialog">关闭</el-button>
        <el-button type="primary" icon="Camera" :disabled="drawLoading" @click="ElMessage.success('请使用浏览器截图或右键保存')">截图保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding-bottom: 20px;
}

.page-header {
  margin-bottom: 20px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.page-header h2 {
  color: #0f4c75;
  margin: 0;
}

.spot-text {
  font-weight: 700;
  color: #43a047;
}

.countdown {
  color: #f9a825;
  font-weight: 600;
  margin-right: 12px;
}

.status-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.void-timestamp {
  font-size: 12px;
  color: #f56c6c;
}

.void-alert {
  padding: 0;
  background: transparent;
}

.void-alert :deep(.el-alert__title) {
  font-size: 12px;
}

.draw-box {
  text-align: center;
  padding: 20px 0;
}

.draw-result {
  font-size: 72px;
  font-weight: 800;
  color: #f9a825;
  margin-bottom: 16px;
  min-height: 90px;
  letter-spacing: 4px;
}

.draw-tip {
  color: #606266;
  margin-bottom: 16px;
}

.draw-info {
  background: #f5f7fa;
  border-radius: 8px;
  padding: 16px;
  text-align: left;
  line-height: 1.8;
  color: #606266;
}
</style>
