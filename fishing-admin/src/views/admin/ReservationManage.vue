<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminReservations, adminCancelReservation, exportReservations, updateActualFee, type Reservation } from '@/api/reservation'
import { formatDate, formatTime, formatDateTime } from '@/utils/date'

const list = ref<Reservation[]>([])
const loading = ref(false)
const query = reactive({
  status: '',
  phone: '',
  userId: undefined as number | undefined,
  slotId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20
})
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const res = await getAdminReservations(query)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const cancel = async (row: Reservation) => {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？', '提示', { type: 'warning' })
    await adminCancelReservation(row.id)
    ElMessage.success('取消成功')
    load()
  } catch {}
}

const editActualFee = async (row: Reservation) => {
  try {
    const { value } = await ElMessageBox.prompt('请输入实际收费金额', '修改实际收费', {
      inputPattern: /^\d+(\.\d{1,2})?$/,
      inputErrorMessage: '金额格式不正确，最多两位小数',
      inputValue: row.actualFee != null ? String(row.actualFee) : ''
    })
    const fee = Number(value)
    if (isNaN(fee) || fee < 0) {
      ElMessage.error('金额不能为负数')
      return
    }
    await updateActualFee(row.id, fee)
    ElMessage.success('修改成功')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '修改失败')
  }
}

const exportExcel = async () => {
  try {
    const res = await exportReservations({
      phone: query.phone || undefined,
      userId: query.userId,
      slotId: query.slotId,
      status: query.status
    })
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

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>预约管理</h2>
      <el-button type="success" icon="Download" @click="exportExcel">导出 Excel</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 140px">
            <el-option label="待抽号" value="待抽号" />
            <el-option label="已抽号" value="已抽号" />
            <el-option label="预约取消" value="预约取消" />
            <el-option label="过期失效" value="过期失效" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model="query.phone" placeholder="请输入手机号" clearable style="width: 160px" />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input-number v-model="query.userId" placeholder="用户ID" :controls="false" style="width: 140px" />
        </el-form-item>
        <el-form-item label="时段ID">
          <el-input-number v-model="query.slotId" placeholder="时段ID" :controls="false" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="load">查询</el-button>
          <el-button icon="Refresh" @click="query.status = ''; query.phone = ''; query.userId = undefined; query.slotId = undefined; load()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="日期/时段" width="150">
          <template #default="{ row }">
            <div v-if="row.slotDate">{{ formatDate(row.slotDate) }} {{ row.slotName }}</div>
            <div v-else>-</div>
          </template>
        </el-table-column>
        <el-table-column label="时间范围" width="150">
          <template #default="{ row }">{{ row.startTime && row.endTime ? formatTime(row.startTime) + ' - ' + formatTime(row.endTime) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="pondName" label="鱼塘" width="120" />
        <el-table-column prop="userPhone" label="手机号" width="130" />
        <el-table-column prop="userId" label="用户ID" width="90" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }"><el-tag :type="statusType(row.status)">{{ row.status }}</el-tag></template>
        </el-table-column>
        <el-table-column label="钓位" width="100">
          <template #default="{ row }">{{ row.spotCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="实际收费" width="130">
          <template #default="{ row }">
            <div>{{ row.actualFee != null ? `¥${row.actualFee}` : '-' }}</div>
          </template>
        </el-table-column>
        <el-table-column label="核销时间" width="160">
          <template #default="{ row }">{{ row.checkInTime ? formatDateTime(row.checkInTime) : '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="editActualFee(row)">修改金额</el-button>
            <el-button v-if="row.status === '待抽号'" link type="danger" @click="cancel(row)">取消预约</el-button>
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
}

.filter-card {
  margin-bottom: 16px;
}
</style>
