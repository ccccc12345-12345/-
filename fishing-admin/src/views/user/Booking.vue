<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUserSlots, type TimeSlot } from '@/api/slots'
import { getMyReservations, bookSlot, type Reservation } from '@/api/reservation'
import { getPonds, type Pond } from '@/api/ponds'
import { formatDate, formatTime } from '@/utils/date'

const router = useRouter()

const slots = ref<TimeSlot[]>([])
const ponds = ref<Pond[]>([])
const myReservations = ref<Reservation[]>([])
const loading = ref(false)
const bookingIds = ref<Set<number>>(new Set())
const query = reactive({ slotDate: '', slotName: '', pondId: null as number | null })

const reservationMap = new Map<number, Reservation>()

const loadData = async () => {
  loading.value = true
  const slotDate = query.slotDate && query.slotDate.trim() ? query.slotDate : undefined
  const params: any = { slotDate }
  if (query.pondId != null) params.pondId = query.pondId
  const [slotsRes, resRes] = await Promise.all([
    getUserSlots(params),
    getMyReservations()
  ])
  slots.value = slotsRes.data
  myReservations.value = resRes.data.records || []
  reservationMap.clear()
  myReservations.value.forEach(r => {
    if (r.status !== '预约取消' && r.status !== '过期失效') {
      reservationMap.set(r.slotId, r)
    }
  })
  loading.value = false
}

const loadPonds = async () => {
  const res = await getPonds()
  ponds.value = res.data || []
  if (ponds.value.length > 0 && query.pondId == null) {
    query.pondId = ponds.value[0].id
  }
}

const getStatus = (slot: TimeSlot) => {
  if (reservationMap.has(slot.id)) return '已预约'
  if ((slot.remain ?? slot.maxBookings) <= 0) return '已满'
  return '可预约'
}

const handleBook = async (slot: TimeSlot) => {
  try {
    await ElMessageBox.confirm(`确定预约 ${slot.slotName} 吗？`, '确认预约', { type: 'info' })
    bookingIds.value.add(slot.id)
    await bookSlot(slot.id)
    ElMessage.success('预约成功')
    await ElMessageBox.alert('预约成功，请前往“我的预约”查看并参与抽号。', '预约成功', {
      confirmButtonText: '前往我的预约',
      type: 'success'
    })
    router.push('/user/reservations')
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '预约失败')
  } finally {
    bookingIds.value.delete(slot.id)
  }
}

const statusType = (status: string) => {
  if (status === '可预约') return 'primary'
  if (status === '已预约') return 'success'
  return 'info'
}

onMounted(async () => {
  await loadPonds()
  loadData()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>场地预约</h2>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="鱼塘">
          <el-select v-model="query.pondId" placeholder="选择鱼塘" clearable style="width: 160px" @change="loadData">
            <el-option
              v-for="pond in ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="query.slotDate" type="date" placeholder="选择日期" value-format="YYYY-MM-DD" clearable />
        </el-form-item>
        <el-form-item label="时段类型">
          <el-select v-model="query.slotName" placeholder="全部" clearable style="width: 140px">
            <el-option label="早场" value="早场" />
            <el-option label="午场" value="午场" />
            <el-option label="晚场" value="晚场" />
            <el-option label="全天场" value="全天场" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadData">查询</el-button>
          <el-button icon="Refresh" @click="query.slotDate = ''; query.slotName = ''; query.pondId = ponds[0]?.id; loadData()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" v-loading="loading">
      <el-table :data="slots" stripe>
        <el-table-column prop="slotDate" label="日期" width="120" />
        <el-table-column prop="slotName" label="时段名称" width="120" />
        <el-table-column label="时间范围" width="180">
          <template #default="{ row }">{{ formatTime(row.startTime) }} - {{ formatTime(row.endTime) }}</template>
        </el-table-column>
        <el-table-column prop="pondName" label="鱼塘" width="120" />
        <el-table-column label="剩余名额" width="150">
          <template #default="{ row }">
            <el-progress :percentage="Math.round(((row.remain ?? row.maxBookings) / row.maxBookings) * 100)" />
            <span>{{ row.remain ?? row.maxBookings }} / {{ row.maxBookings }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(getStatus(row))">{{ getStatus(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="160">
          <template #default="{ row }">
            <el-button
              type="primary"
              size="small"
              :disabled="getStatus(row) !== '可预约' || bookingIds.has(row.id)"
              :loading="bookingIds.has(row.id)"
              @click="handleBook(row)"
            >
              {{ getStatus(row) === '已满' ? '已满' : '立即预约' }}
            </el-button>
          </template>
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
  margin-bottom: 20px;
}

.page-header h2 {
  color: #0f4c75;
}

.filter-card {
  margin-bottom: 16px;
}

.table-card {
  min-height: 400px;
}
</style>
