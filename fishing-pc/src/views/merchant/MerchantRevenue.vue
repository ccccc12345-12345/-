<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchantRevenueSummary, getMerchantRevenueList, exportMerchantRevenue, type RevenueSummary, type RevenueItem } from '@/api/merchant'
import { formatDate } from '@/utils/date'

const today = new Date()
const firstDayOfMonth = new Date(today.getFullYear(), today.getMonth(), 1)

const query = reactive({
  dateRange: [formatDate(firstDayOfMonth), formatDate(today)] as [string, string]
})

const summary = ref<RevenueSummary | null>(null)
const list = ref<RevenueItem[]>([])
const loading = ref(false)
const total = ref(0)
const page = reactive({ pageNum: 1, pageSize: 20 })

const totalIncome = computed(() => {
  return list.value.reduce((sum, item) => sum + (item.totalIncome || 0), 0)
})

const loadSummary = async () => {
  try {
    const res = await getMerchantRevenueSummary()
    summary.value = res.data
  } catch {}
}

const loadList = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: page.pageNum, pageSize: page.pageSize }
    if (query.dateRange && query.dateRange[0]) params.startDate = query.dateRange[0]
    if (query.dateRange && query.dateRange[1]) params.endDate = query.dateRange[1]
    const res = await getMerchantRevenueList(params)
    const data = res.data
    list.value = Array.isArray(data) ? data : (data?.records || [])
    total.value = Array.isArray(data) ? data.length : (data?.total || 0)
  } finally {
    loading.value = false
  }
}

const handleExport = async () => {
  try {
    const params: any = {}
    if (query.dateRange && query.dateRange[0]) params.startDate = query.dateRange[0]
    if (query.dateRange && query.dateRange[1]) params.endDate = query.dateRange[1]
    const res = await exportMerchantRevenue(params)
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
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card income">
          <div class="summary-label">今日收入</div>
          <div class="summary-value">¥{{ summary?.todayIncome?.toFixed(2) ?? '0.00' }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card reservation">
          <div class="summary-label">今日预约数</div>
          <div class="summary-value">{{ summary?.todayReservationCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card checkin">
          <div class="summary-label">今日核销数</div>
          <div class="summary-value">{{ summary?.todayCheckinCount ?? 0 }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :sm="12" :lg="6">
        <el-card class="summary-card occupancy">
          <div class="summary-label">今日上座率</div>
          <div class="summary-value">{{ summary?.occupancyRate ?? 0 }}%</div>
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
        <el-form-item>
          <el-button type="primary" icon="Search" @click="page.pageNum = 1; loadList()">查询</el-button>
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

      <el-pagination
        v-model:current-page="page.pageNum"
        :page-size="page.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="loadList"
      />
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
  margin: 0;
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

.summary-card.income {
  background: linear-gradient(135deg, var(--fp-primary-dark), var(--fp-primary));
}

.summary-card.reservation {
  background: linear-gradient(135deg, var(--fp-primary), var(--fp-primary-light));
}

.summary-card.checkin {
  background: linear-gradient(135deg, var(--fp-accent-dark), oklch(65% 0.13 85));
}

.summary-card.occupancy {
  background: linear-gradient(135deg, oklch(55% 0.12 250), oklch(70% 0.1 250));
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
