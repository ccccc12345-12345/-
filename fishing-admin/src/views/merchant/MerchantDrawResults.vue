<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMerchantDrawResults,
  exportMerchantDrawResults,
  getMerchantMissedDraws,
  getMerchantTimeSlots,
  type DrawResult,
  type MissedDraw,
  type TimeSlot
} from '@/api/merchant'
import { usePondStore } from '@/store/pond'
import { formatDateTime } from '@/utils/date'

const pondStore = usePondStore()

const list = ref<DrawResult[]>([])
const missedList = ref<MissedDraw[]>([])
const slots = ref<TimeSlot[]>([])
const loading = ref(false)
const missedLoading = ref(false)
const missedVisible = ref(false)
const query = reactive({
  slotId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20
})
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.slotId) params.slotId = query.slotId
    const res = await getMerchantDrawResults(params)
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

const exportExcel = async () => {
  try {
    const params: any = {}
    if (query.slotId) params.slotId = query.slotId
    const res = await exportMerchantDrawResults(params)
    const blob = new Blob([res.data], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `抽号记录_${new Date().toISOString().split('T')[0]}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch (e: any) {
    ElMessage.error(e.message || '导出失败')
  }
}

const openMissed = async (slotId?: number) => {
  if (!slotId) {
    ElMessage.warning('请选择时段')
    return
  }
  missedVisible.value = true
  missedLoading.value = true
  try {
    const res = await getMerchantMissedDraws(slotId)
    missedList.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    missedLoading.value = false
  }
}

onMounted(() => {
  load()
  loadSlots()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>抽号记录</h2>
      <div>
        <el-button type="warning" icon="WarnTriangleFilled" @click="openMissed(query.slotId)">未参与名单</el-button>
        <el-button type="success" icon="Download" @click="exportExcel">导出 Excel</el-button>
      </div>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="时段">
          <el-select v-model="query.slotId" placeholder="全部时段" clearable style="width: 220px">
            <el-option
              v-for="slot in slots"
              :key="slot.id"
              :label="`${slot.slotDate} ${slot.slotName}`"
              :value="slot.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="query.pageNum = 1; load()">查询</el-button>
          <el-button icon="Refresh" @click="query.slotId = undefined; query.pageNum = 1; load()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="reservationId" label="预约ID" width="100" />
        <el-table-column prop="userPhone" label="手机号" width="130" />
        <el-table-column prop="userNickname" label="昵称" width="120" />
        <el-table-column prop="spotCode" label="钓位编号" width="120" />
        <el-table-column prop="pondName" label="鱼塘" width="120" />
        <el-table-column label="日期/时段" width="150">
          <template #default="{ row }">{{ row.slotDate || '-' }} {{ row.slotName || '' }}</template>
        </el-table-column>
        <el-table-column prop="drawTime" label="抽号时间" min-width="180">
          <template #default="{ row }">{{ formatDateTime(row.drawTime) }}</template>
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

    <el-dialog v-model="missedVisible" title="未参与抽号名单" width="600px">
      <el-table v-loading="missedLoading" :data="missedList" stripe>
        <el-table-column prop="reservationId" label="预约ID" width="100" />
        <el-table-column prop="userPhone" label="手机号" width="130" />
        <el-table-column prop="userNickname" label="昵称" width="120" />
        <el-table-column label="日期/时段" min-width="150">
          <template #default="{ row }">{{ row.slotDate || '-' }} {{ row.slotName || '' }}</template>
        </el-table-column>
      </el-table>
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
  margin: 0;
}

.filter-card {
  margin-bottom: 16px;
}
</style>
