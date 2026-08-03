<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminReservations, adminCancelReservation, type ReservationRecord } from '@/api/reservation'
import { getSlots, type TimeSlot } from '@/api/timeslot'
import { formatDate } from '@/utils/date'

const list = ref<ReservationRecord[]>([])
const slots = ref<TimeSlot[]>([])
const loading = ref(false)
const query = reactive({
  status: '',
  phone: '',
  slotId: undefined as number | undefined,
  startDate: '',
  endDate: '',
  pageNum: 1,
  pageSize: 20
})
const total = ref(0)
const cancelDialogVisible = ref(false)
const cancelReason = ref('')
const cancelId = ref<number | null>(null)

const loadSlots = async () => {
  const res = await getSlots({ pageSize: 1000 })
  slots.value = res.data.records
}

const load = async () => {
  loading.value = true
  const params: any = {
    status: query.status || undefined,
    phone: query.phone || undefined,
    slotId: query.slotId,
    startDate: query.startDate || undefined,
    endDate: query.endDate || undefined,
    pageNum: query.pageNum,
    pageSize: query.pageSize
  }
  const res = await getAdminReservations(params)
  list.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

const openCancel = (row: ReservationRecord) => {
  cancelId.value = row.id
  cancelReason.value = ''
  cancelDialogVisible.value = true
}

const confirmCancel = async () => {
  if (!cancelId.value) return
  if (!cancelReason.value.trim()) {
    ElMessage.warning('请输入取消原因')
    return
  }
  await adminCancelReservation(cancelId.value, cancelReason.value)
  ElMessage.success('取消成功')
  cancelDialogVisible.value = false
  load()
}

const exportExcel = () => {
  const params = new URLSearchParams()
  if (query.status) params.append('status', query.status)
  if (query.phone) params.append('phone', query.phone)
  if (query.slotId) params.append('slotId', String(query.slotId))
  if (query.startDate) params.append('startDate', query.startDate)
  if (query.endDate) params.append('endDate', query.endDate)
  window.open(`/api/admin/reservations/export?${params.toString()}`)
  ElMessage.success('开始导出')
}

const statusType = (status: string) => {
  const map: Record<string, string> = { '待抽号': 'warning', '已抽号': 'success', '预约取消': 'info', '过期失效': 'danger' }
  return map[status] || 'info'
}

onMounted(() => {
  loadSlots()
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
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="query.startDate"
            type="date"
            placeholder="开始日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
          />
          <span style="margin: 0 8px">-</span>
          <el-date-picker
            v-model="query.endDate"
            type="date"
            placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 140px"
          />
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="query.slotId" placeholder="全部" clearable style="width: 160px">
            <el-option
              v-for="slot in slots"
              :key="slot.id"
              :label="`${slot.slotDate} ${slot.slotName}`"
              :value="slot.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="用户手机号" clearable style="width: 160px" />
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
          <el-button icon="Refresh" @click="query.status = ''; query.phone = ''; query.slotId = undefined; query.startDate = ''; query.endDate = ''; query.pageNum = 1; load()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="日期/时段" width="180">
          <template #default="{ row }">
            <div>{{ row.slotDate || '-' }}</div>
            <div style="color:#909399;font-size:12px">{{ row.slotName || '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">{{ row.userPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="用户昵称" width="130">
          <template #default="{ row }">{{ row.userNickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="钓位" width="100">
          <template #default="{ row }">{{ row.spotCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="取消原因" min-width="140">
          <template #default="{ row }">{{ row.cancelReason || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === '待抽号'" link type="danger" @click="openCancel(row)">手动取消</el-button>
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

    <el-dialog v-model="cancelDialogVisible" title="手动取消预约" width="400px">
      <el-form label-width="80px">
        <el-form-item label="取消原因">
          <el-input v-model="cancelReason" type="textarea" :rows="3" placeholder="请输入取消原因" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="cancelDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="confirmCancel">确认</el-button>
      </template>
    </el-dialog>
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
}

.filter-card {
  margin-bottom: 16px;
}
</style>
