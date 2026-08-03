<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMerchantReservations,
  cancelMerchantReservation,
  exportMerchantReservations,
  getMerchantTimeSlots,
  type Reservation,
  type TimeSlot
} from '@/api/merchant'
import { useMerchantPush } from '@/composables/useMerchantPush'
import { usePondStore } from '@/store/pond'
import { formatDateTime } from '@/utils/date'

const pondStore = usePondStore()

const list = ref<Reservation[]>([])
const slots = ref<TimeSlot[]>([])
const loading = ref(false)
const total = ref(0)
const query = reactive({
  pondId: undefined as number | undefined,
  slotDate: '',
  slotId: undefined as number | undefined,
  status: '',
  pageNum: 1,
  pageSize: 20
})

const load = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.pondId) params.pondId = query.pondId
    if (query.slotDate) params.slotDate = query.slotDate
    if (query.slotId) params.slotId = query.slotId
    if (query.status) params.status = query.status
    const res = await getMerchantReservations(params)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadSlots = async () => {
  try {
    const res = await getMerchantTimeSlots({ pageSize: 100 })
    slots.value = res.data.records || []
  } catch {}
}

const cancel = async (row: Reservation) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入取消原因', '取消预约', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      inputPattern: /\S+/,
      inputErrorMessage: '原因不能为空'
    })
    await cancelMerchantReservation(row.id, value)
    ElMessage.success('取消成功')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '取消失败')
  }
}

const exportExcel = async () => {
  try {
    const params: any = {}
    if (query.pondId) params.pondId = query.pondId
    if (query.slotDate) params.slotDate = query.slotDate
    if (query.slotId) params.slotId = query.slotId
    if (query.status) params.status = query.status
    const res = await exportMerchantReservations(params)
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `预约记录_${new Date().toISOString().split('T')[0]}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

const statusType = (status: string) => {
  const map: Record<string, string> = { '待抽号': 'warning', '已抽号': 'success', '预约取消': 'info', '过期失效': 'danger' }
  return map[status] || 'info'
}

useMerchantPush({
  events: ['RESERVATION_CREATED', 'RESERVATION_STATUS_CHANGED', 'RESERVATION_CHECKED_IN'],
  onEvent: () => load(),
  fallback: () => load()
})

onMounted(() => {
  load()
  loadSlots()
})
watch(() => pondStore.currentPondId, (val) => {
  query.pondId = val ?? undefined
  query.pageNum = 1
  load()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>预约管理</h2>
      <el-button type="success" icon="Download" @click="exportExcel">导出 Excel</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="鱼塘">
          <el-select v-model="pondStore.currentPondId" placeholder="全部鱼塘" clearable style="width: 160px">
            <el-option
              v-for="pond in pondStore.ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="query.slotDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="query.slotId" placeholder="全部时段" clearable style="width: 180px">
            <el-option
              v-for="slot in slots"
              :key="slot.id"
              :label="`${slot.slotDate} ${slot.slotName}`"
              :value="slot.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待抽号" value="待抽号" />
            <el-option label="已抽号" value="已抽号" />
            <el-option label="预约取消" value="预约取消" />
            <el-option label="过期失效" value="过期失效" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="query.pageNum = 1; load()">查询</el-button>
          <el-button icon="Refresh" @click="query.slotDate = ''; query.slotId = undefined; query.status = ''; query.pageNum = 1; load()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="日期/时段" width="150">
          <template #default="{ row }">
            <div v-if="row.slotDate">{{ row.slotDate }} {{ row.slotName }}</div>
            <div v-else>-</div>
          </template>
        </el-table-column>
        <el-table-column label="时间范围" width="150">
          <template #default="{ row }">{{ row.startTime && row.endTime ? row.startTime + ' - ' + row.endTime : '-' }}</template>
        </el-table-column>
        <el-table-column prop="pondName" label="鱼塘" width="120" />
        <el-table-column prop="userPhone" label="手机号" width="130" />
        <el-table-column prop="userNickname" label="昵称" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="钓位" width="100">
          <template #default="{ row }">{{ row.spotCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="实收金额" width="120">
          <template #default="{ row }">{{ row.actualFee != null ? `¥${row.actualFee}` : '-' }}</template>
        </el-table-column>
        <el-table-column label="核销时间" width="160">
          <template #default="{ row }">{{ row.checkInTime ? formatDateTime(row.checkInTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待抽号'" link type="danger" @click="cancel(row)">取消</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
      />
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

.filter-card {
  margin-bottom: 16px;
}
</style>
