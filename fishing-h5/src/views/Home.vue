<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSlots, type TimeSlot } from '@/api/slots'
import { bookSlot, getMyReservations, type Reservation } from '@/api/reservation'
import { useUserStore } from '@/store/user'
import { formatDate, formatTime } from '@/utils/date'

const userStore = useUserStore()

const selectedDate = ref(formatDate(new Date()))
const slots = ref<TimeSlot[]>([])
const myReservations = ref<Reservation[]>([])
const loading = ref(false)
const bookingIds = ref<Set<number>>(new Set())

const maxAdvanceDays = ref(7)

const dateOptions = computed(() => {
  const list = []
  const today = new Date()
  for (let i = 0; i < maxAdvanceDays.value; i++) {
    const d = new Date(today)
    d.setDate(today.getDate() + i)
    list.push({
      value: formatDate(d),
      label: `${d.getMonth() + 1}/${d.getDate()}`,
      week: ['周日', '周一', '周二', '周三', '周四', '周五', '周六'][d.getDay()]
    })
  }
  return list
})

const filteredSlots = computed(() => {
  return slots.value.filter(s => s.slotDate === selectedDate.value)
})

const reservationMap = computed(() => {
  const map = new Map<number, Reservation>()
  myReservations.value.forEach(r => {
    if (r.status !== '预约取消' && r.status !== '过期失效') {
      map.set(r.slotId, r)
    }
  })
  return map
})

const loadSlots = async () => {
  loading.value = true
  try {
    const res = await getSlots({ slotDate: selectedDate.value, pageSize: 100 })
    slots.value = res.data.records
    if (res.data.records.length > 0) {
      maxAdvanceDays.value = res.data.records[0].advanceDays || 7
    }
  } finally {
    loading.value = false
  }
}

const loadMyReservations = async () => {
  const res = await getMyReservations()
  myReservations.value = res.data.records
}

const handleBook = async (slot: TimeSlot) => {
  if (bookingIds.value.has(slot.id)) return
  try {
    await ElMessageBox.confirm(`确定预约 ${slot.slotName} 吗？`, '确认预约', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'info'
    })
    bookingIds.value.add(slot.id)
    await bookSlot(slot.id)
    ElMessage.success('预约成功')
    await loadMyReservations()
  } catch (e: any) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '预约失败')
    }
  } finally {
    bookingIds.value.delete(slot.id)
  }
}

const getSlotStatus = (slot: TimeSlot) => {
  const r = reservationMap.value.get(slot.id)
  if (r) return '已预约'
  if ((slot.remain ?? slot.maxBookings) <= 0) return '已满'
  return '可预约'
}

watch(selectedDate, loadSlots)

onMounted(() => {
  loadSlots()
  loadMyReservations()
  setInterval(() => {
    loadSlots()
  }, 30000)
})
</script>

<template>
  <div class="page home-page">
    <div class="header">
      <h2>选择日期</h2>
      <div v-if="userStore.isAdmin" class="admin-link" @click="$router.push('/admin')">
        管理后台
      </div>
    </div>

    <div class="date-list">
      <div
        v-for="d in dateOptions"
        :key="d.value"
        class="date-item"
        :class="{ active: selectedDate === d.value }"
        @click="selectedDate = d.value"
      >
        <span class="week">{{ d.week }}</span>
        <span class="date">{{ d.label }}</span>
      </div>
    </div>

    <h3 class="section-title">{{ selectedDate }} 可预约时段</h3>

    <div v-if="loading" class="loading">加载中...</div>

    <div v-else-if="filteredSlots.length === 0" class="empty">
      暂无可用时段
    </div>

    <div v-else class="slot-list">
      <div v-for="slot in filteredSlots" :key="slot.id" class="card slot-card">
        <div class="slot-info">
          <div class="slot-name">{{ slot.slotName }}</div>
          <div class="slot-time">{{ formatTime(slot.startTime) }} - {{ formatTime(slot.endTime) }}</div>
          <div class="slot-remain">
            剩余名额：<strong :class="{ full: (slot.remain ?? slot.maxBookings) <= 0 }">
              {{ slot.remain ?? slot.maxBookings }}
            </strong> / {{ slot.maxBookings }}
          </div>
        </div>
        <div class="slot-action">
          <span class="status-tag" :class="{
            'status-pending': getSlotStatus(slot) === '可预约',
            'status-drawn': getSlotStatus(slot) === '已预约',
            'status-cancelled': getSlotStatus(slot) === '已满'
          }">
            {{ getSlotStatus(slot) }}
          </span>
          <button
            class="btn-primary book-btn"
            :disabled="getSlotStatus(slot) !== '可预约' || bookingIds.has(slot.id)"
            @click="handleBook(slot)"
          >
            {{ bookingIds.has(slot.id) ? '预约中' : (getSlotStatus(slot) === '已满' ? '已满' : '立即预约') }}
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.home-page {
  padding-top: 20px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.header h2 {
  font-size: 20px;
  color: var(--primary);
}

.admin-link {
  color: var(--primary);
  font-size: 14px;
  font-weight: 600;
}

.date-list {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 8px;
}

.date-item {
  flex-shrink: 0;
  width: 64px;
  height: 72px;
  background: white;
  border-radius: 16px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.date-item.active {
  background: linear-gradient(135deg, var(--primary), var(--secondary));
  color: white;
}

.date-item .week {
  font-size: 12px;
}

.date-item .date {
  font-size: 18px;
  font-weight: 700;
}

.section-title {
  font-size: 16px;
  margin: 16px 0 12px;
  color: var(--text);
}

.slot-card {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.slot-name {
  font-size: 18px;
  font-weight: 700;
  color: var(--primary);
  margin-bottom: 6px;
}

.slot-time {
  font-size: 14px;
  color: var(--text-secondary);
  margin-bottom: 8px;
}

.slot-remain {
  font-size: 13px;
  color: var(--text-secondary);
}

.slot-remain strong {
  color: var(--secondary);
}

.slot-remain strong.full {
  color: var(--danger);
}

.slot-action {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 10px;
}

.book-btn {
  min-width: 88px;
  padding: 8px 14px;
  font-size: 13px;
}

.loading, .empty {
  text-align: center;
  padding: 40px 0;
  color: var(--text-secondary);
}
</style>
