<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getRevenueSummary, getRevenueList, exportRevenue, type RevenueItem, type RevenueSummary } from '@/api/revenue'
import { usePondStore } from '@/store/pond'
import { formatDate } from '@/utils/date'

const pondStore = usePondStore()

const today = new Date()
const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1)

const query = reactive({
  dateRange: [formatDate(firstDayOfMonth), formatDate(today)] as [string, string],
  pondId: null as number | null
})

const summary = ref<RevenueSummary | null>(null)
const list = ref<RevenueItem[]>([])
const loading = ref(false)

const totalIncome = computed(() => {
  return list.value.reduce((sum, item) => sum + (item.totalIncome || 0), 0)
})

const loadSummary = async () => {
  try {
    const res = await getRevenueSummary()
    summary.value = res.data
  } catch {}
}

const loadList = async () => {
  loading.value = true
  try {
    const params: any = {}
    if (query.dateRange && query.dateRange[0]) params.startDate = query.dateRange[0]
    if (query.dateRange && query.dateRange[1]) params.endDate = query.dateRange[1]
    if (query.pondId != null) params.pondId = query.pondId
    const res = await getRevenueList(params)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  try {
    const params: any = {}
    if (query.dateRange && query.dateRange[0]) params.startDate = query.dateRange[0]
    if (query.dateRange && query.dateRange[1]) params.endDate = query.dateRange[1]
    if (query.pondId != null) params.pondId = query.pondId
    const res = await exportRevenue(params)
    const blob = new Blob([res as any], { type: 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = `收益统计_${params.startDate || ''}_${params.endDate || ''}.xlsx`
    document.body.appendChild(link)
    link.click()
    document.body.removeChild(link)
    URL.revokeObjectURL(url)
    ElMessage.success('导出成功')
  } catch {
    ElMessage.error('导出失败')
  }
}

onMounted(() => {
  loadSummary()
  loadList()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>收益统计</h2>
    </div>

    <el-row :gutter="16" class="summary-cards">
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card today">
          <div class="summary-label">今日收入</div>
          <div class="summary-value">¥{{ summary?.today?.toFixed(2) ?? '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card week">
          <div class="summary-label">本周收入</div>
          <div class="summary-value">¥{{ summary?.week?.toFixed(2) ?? '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="8">
        <el-card class="summary-card month">
          <div class="summary-label">本月收入</div>
          <div class="summary-value">¥{{ summary?.month?.toFixed(2) ?? '0.00' }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="日期范围">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
          />
        </el-form-item>
        <el-form-item label="鱼塘">
          <el-select v-model="query.pondId" placeholder="全部鱼塘" clearable style="width: 160px">
            <el-option
              v-for="pond in pondStore.ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="loadList">查询</el-button>
          <el-button icon="Download" @click="handleExport">导出Excel</el-button>
        </el-form-item>
      </el-form>
      <div class="total-bar">
        当前筛选总收入：<strong>¥{{ totalIncome.toFixed(2) }}</strong>
      </div>
    </el-card>

    <el-card v-loading="loading" class="table-card">
      <el-table :data="list" stripe>
        <el-table-column prop="slotDate" label="日期" width="120" sortable />
        <el-table-column prop="slotName" label="时段" width="100" />
        <el-table-column prop="pondName" label="鱼塘" width="120" />
        <el-table-column prop="totalCount" label="预约人数" width="100" />
        <el-table-column prop="checkinCount" label="核销人数" width="100" />
        <el-table-column label="上座率" width="110">
          <template #default="{ row }">{{ row.occupancyRate }}%</template>
        </el-table-column>
        <el-table-column label="总收入" min-width="120">
          <template #default="{ row }">¥{{ (row.totalIncome || 0).toFixed(2) }}</template>
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

.summary-cards {
  margin-bottom: 16px;
}

.summary-card {
  text-align: center;
  margin-bottom: 16px;
  border-radius: 12px;
  color: white;
}

.summary-card.today {
  background: linear-gradient(135deg, #0f4c75, #3282b8);
}

.summary-card.week {
  background: linear-gradient(135deg, #409eff, #79bbff);
}

.summary-card.month {
  background: linear-gradient(135deg, #67c23a, #95d475);
}

.summary-label {
  font-size: 14px;
  opacity: 0.9;
  margin-bottom: 8px;
}

.summary-value {
  font-size: 28px;
  font-weight: 700;
}

.filter-card {
  margin-bottom: 16px;
}

.total-bar {
  margin-top: 12px;
  color: #606266;
  font-size: 14px;
}

.total-bar strong {
  color: #f56c6c;
  font-size: 18px;
}

.table-card {
  min-height: 400px;
}
</style>
