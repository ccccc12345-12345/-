<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPonds, type Pond } from '@/api/pond'
import { getUserSlots, type TimeSlot } from '@/api/timeslot'
import { bookSlot, bookSlotDirect, type ReservationVO } from '@/api/reservation'

const router = useRouter()
const loading = ref(false)
const bookingId = ref<number | null>(null)
const errorText = ref('')
const ponds = ref<Pond[]>([])
const slots = ref<TimeSlot[]>([])
const success = ref<ReservationVO | null>(null)

const filters = reactive({
  pondId: undefined as number | undefined,
  slotDate: '',
  slotName: ''
})

const bookablePonds = computed(() => {
  const ids = new Set(slots.value.map((s) => s.pondId).filter(Boolean))
  if (!ids.size) return ponds.value
  return ponds.value.filter((p) => ids.has(p.id))
})

const visibleSlots = computed(() => {
  let list = slots.value
  if (filters.slotName) list = list.filter((slot) => slot.slotName === filters.slotName)
  return list
})

const pondName = (pondId?: number | null) => ponds.value.find((pond) => pond.id === pondId)?.name || '未选择鱼塘'
const formatMoney = (value?: number | null) => `¥${((Number(value || 0)) / 100).toFixed(2)}`
const canBook = (slot: TimeSlot) => Number(slot.remain ?? slot.maxBookings ?? 0) > 0 && slot.status === 1

const getErrorMessage = (error: unknown) => {
  if (error instanceof Error && error.message) return error.message
  return '预约失败，请刷新后重试'
}

const loadPonds = async () => {
  const res = await getPonds()
  ponds.value = res.data || []
}

const loadSlots = async () => {
  loading.value = true
  errorText.value = ''
  try {
    const params: { pondId?: number; slotDate?: string } = {}
    if (filters.pondId) params.pondId = filters.pondId
    if (filters.slotDate) params.slotDate = filters.slotDate
    const res = await getUserSlots(params)
    slots.value = res.data || []
  } catch (error) {
    slots.value = []
    errorText.value = getErrorMessage(error)
  } finally {
    loading.value = false
  }
}

const resetFilters = async () => {
  filters.pondId = undefined
  filters.slotDate = ''
  filters.slotName = ''
  await loadSlots()
}

const submitBooking = async (slot: TimeSlot) => {
  if (!canBook(slot)) return
  try {
    await ElMessageBox.confirm(
      `确认预约 ${pondName(slot.pondId)} ${slot.slotDate} ${slot.slotName}？预约成功后系统会立即分配钓位。`,
      '确认预约',
      {
        type: 'info',
        confirmButtonText: '确认预约',
        cancelButtonText: '再看看'
      }
    )
    bookingId.value = slot.id
    try {
      const res = await bookSlotDirect(slot.id)
      success.value = res.data
      ElMessage.success('预约成功，已分配钓位')
      await loadSlots()
    } catch (error: any) {
      const message = getErrorMessage(error)
      if (String(message).includes('cancel')) return
      if (String(message).includes('暂无可用钓位')) {
        try {
          await ElMessageBox.confirm(
            '当前场次暂无可用钓位，是否改为普通预约参与抽号？抽号成功后系统会自动分配钓位。',
            '自动分配失败',
            {
              type: 'warning',
              confirmButtonText: '普通预约',
              cancelButtonText: '取消'
            }
          )
          const res = await bookSlot(slot.id)
          if (res.data) {
            ElMessage.success('普通预约成功，请等待抽号分配钓位')
            router.push('/user/reservations')
          }
        } catch (innerError: any) {
          if (String(getErrorMessage(innerError)).includes('cancel')) return
          ElMessage.error(getErrorMessage(innerError))
        }
      } else {
        ElMessage.error(message)
      }
    }
  } catch (error: any) {
    if (String(getErrorMessage(error)).includes('cancel')) return
  } finally {
    bookingId.value = null
  }
}

onMounted(async () => {
  await loadPonds()
  await loadSlots()
  if (!filters.pondId && bookablePonds.value.length > 0) {
    filters.pondId = bookablePonds.value[0].id
    await loadSlots()
  }
})
</script>

<template>
  <section class="page">
    <div class="page-hero fp-spotlight" v-fp-spotlight>
      <div>
        <p class="subtitle">预约钓位</p>
        <h1>选择场次，成功后直接获得钓位凭证</h1>
        <p class="hero-desc">所有数据实时来自后端。刷新页面、重新登录后，你的预约和钓位仍会在“我的预约”和“我的钓位”里显示。</p>
      </div>
      <el-button type="primary" size="large" v-fp-ripple @click="router.push('/user/reservations')">查看我的预约</el-button>
    </div>

    <div class="glass-filter fp-lift">
      <el-select v-model="filters.pondId" clearable placeholder="全部鱼塘" @change="loadSlots">
        <el-option v-for="pond in bookablePonds" :key="pond.id" :label="pond.name" :value="pond.id" />
      </el-select>
      <el-date-picker v-model="filters.slotDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" @change="loadSlots" />
      <el-select v-model="filters.slotName" clearable placeholder="全部场次">
        <el-option label="早场" value="早场" />
        <el-option label="午场" value="午场" />
        <el-option label="晚场" value="晚场" />
        <el-option label="全天场" value="全天场" />
      </el-select>
      <el-button v-fp-ripple @click="resetFilters">重置</el-button>
      <el-button type="primary" v-fp-ripple @click="loadSlots">刷新</el-button>
    </div>

    <el-alert
      v-if="success"
      class="success-card"
      type="success"
      :closable="false"
      show-icon
    >
      <template #title>
        <span>预约成功：{{ success.pondName || pondName(success.pondId) }} · {{ success.slotDate }} {{ success.slotName }} · 钓位 {{ success.spotCode || '待同步' }}</span>
      </template>
      <div class="success-actions">
        <span>核销码：{{ success.checkinCode || '后端生成中' }}</span>
        <el-button type="success" size="small" v-fp-ripple @click="router.push(`/user/reservations/${success.id}`)">查看详情</el-button>
        <el-button size="small" v-fp-ripple @click="router.push({ path: '/restaurant', query: { pondId: success.pondId, reservationId: success.id, spotId: success.spotId } })">去餐厅点餐</el-button>
        <el-button size="small" v-fp-ripple @click="router.push({ path: '/user/catches', query: { reservationId: success.id } })">提交渔获</el-button>
      </div>
    </el-alert>

    <el-alert
      v-if="errorText"
      class="error-card"
      type="error"
      :title="errorText"
      show-icon
      :closable="false"
    >
      <template #default>
        <el-button size="small" v-fp-ripple @click="loadSlots">重新加载</el-button>
      </template>
    </el-alert>

    <el-skeleton v-if="loading" :rows="5" animated />
    <div v-else-if="visibleSlots.length === 0" class="empty-state fp-magnetic" v-fp-magnetic>
      <div class="empty-icon">
        <svg viewBox="0 0 64 64" width="64" height="64" fill="none">
          <circle cx="32" cy="32" r="30" stroke="var(--fp-primary-light)" stroke-width="2" />
          <path d="M20 32h24M32 20v24" stroke="var(--fp-primary-light)" stroke-width="2" stroke-linecap="round" />
        </svg>
      </div>
      <h3>暂无可预约场次</h3>
      <p>当前没有开放预约的鱼塘/场次，或所选筛选条件下没有可用名额。</p>
      <div class="empty-actions">
        <el-button type="primary" v-fp-ripple @click="resetFilters">查看全部可预约场次</el-button>
        <el-button v-fp-ripple @click="router.push('/user/reservations')">查看我的预约</el-button>
      </div>
    </div>

    <div v-else class="slot-grid fp-stagger">
      <article v-for="slot in visibleSlots" :key="slot.id" class="slot-card fp-lift">
        <div class="slot-top">
          <div>
            <h3>{{ slot.slotName }}</h3>
            <p>{{ pondName(slot.pondId) }}</p>
          </div>
          <el-tag :type="canBook(slot) ? 'success' : 'info'">{{ canBook(slot) ? '可预约' : '已满' }}</el-tag>
        </div>
        <div class="slot-time">
          <strong>{{ slot.slotDate }}</strong>
          <span>{{ slot.startTime }} - {{ slot.endTime }}</span>
        </div>
        <div class="slot-metrics">
          <span>剩余 <b>{{ slot.remain ?? slot.maxBookings }}</b> / {{ slot.maxBookings }}</span>
          <span>价格 <b>{{ formatMoney(slot.defaultPrice) }}</b></span>
        </div>
        <div class="slot-note">预约后自动锁定可用钓位，凭核销码到场签到。</div>
        <el-button
          type="primary"
          :disabled="!canBook(slot)"
          :loading="bookingId === slot.id"
          v-fp-ripple
          @click="submitBooking(slot)"
        >
          立即预约并分配钓位
        </el-button>
      </article>
    </div>
  </section>
</template>

<style scoped>
.page {
  width: min(1240px, calc(100% - 32px));
  margin: 22px auto 48px;
}

.page-hero {
  display: flex;
  justify-content: space-between;
  gap: var(--fp-space-5);
  align-items: flex-end;
  padding: var(--fp-space-6) var(--fp-space-8);
  margin-bottom: var(--fp-space-6);
  border-radius: var(--fp-radius);
  background: linear-gradient(135deg, var(--fp-primary-dark) 0%, var(--fp-primary) 100%);
  color: #fff;
  box-shadow: var(--fp-shadow-lg);
  position: relative;
  overflow: hidden;
}

.page-hero h1 {
  margin: 0;
  font-size: 30px;
  line-height: 1.25;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.subtitle {
  margin: 0 0 8px;
  color: var(--fp-accent-light);
  font-weight: 900;
  font-size: 13px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.hero-desc {
  max-width: 760px;
  margin: 10px 0 0;
  color: oklch(95% 0.01 165);
  line-height: 1.6;
}

.glass-filter {
  display: flex;
  align-items: center;
  gap: var(--fp-space-4);
  flex-wrap: wrap;
  padding: var(--fp-space-4) var(--fp-space-5);
  margin-bottom: var(--fp-space-5);
  border-radius: var(--fp-radius);
  background: var(--fp-surface);
  box-shadow: var(--fp-shadow-sm);
  backdrop-filter: blur(10px);
}

.success-card,
.error-card {
  margin-bottom: var(--fp-space-5);
  border-radius: var(--fp-radius-sm);
}

.success-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 8px;
}

.empty-state {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: var(--fp-space-4);
  padding: var(--fp-space-8);
  border-radius: var(--fp-radius);
  background: var(--fp-surface);
  box-shadow: var(--fp-shadow-md);
  text-align: center;
}

.empty-state h3 {
  margin: 0;
  font-size: 22px;
  color: var(--fp-text);
  font-family: var(--fp-font-display);
}

.empty-state p {
  margin: 0;
  max-width: 420px;
  color: var(--fp-text-secondary);
  line-height: 1.6;
}

.empty-actions {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  margin-top: var(--fp-space-2);
}

.empty-icon {
  width: 96px;
  height: 96px;
  display: grid;
  place-items: center;
  border-radius: 50%;
  background: var(--fp-primary-soft);
}

.slot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
  gap: var(--fp-space-5);
}

.slot-card {
  min-height: 280px;
  display: flex;
  flex-direction: column;
  gap: var(--fp-space-4);
  padding: var(--fp-space-5);
  border: 1px solid var(--fp-border);
  border-radius: var(--fp-radius);
  background: var(--fp-surface);
  box-shadow: var(--fp-shadow-sm);
}

.slot-top,
.slot-metrics {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.slot-top h3 {
  margin: 0 0 4px;
  font-size: 20px;
  color: var(--fp-text);
}

.slot-top p,
.slot-note {
  margin: 0;
  color: var(--fp-text-secondary);
}

.slot-time {
  display: grid;
  gap: 6px;
  padding: 12px;
  border-radius: var(--fp-radius-sm);
  background: var(--fp-primary-soft);
  color: var(--fp-text);
}

.slot-time strong {
  font-family: var(--fp-font-display);
}

.slot-metrics b {
  color: var(--fp-primary);
}

.slot-card .el-button {
  margin-top: auto;
}

@media (max-width: 720px) {
  .page-hero {
    align-items: stretch;
    flex-direction: column;
  }

  .glass-filter :deep(.el-select),
  .glass-filter :deep(.el-date-editor) {
    width: 100%;
  }
}
</style>
