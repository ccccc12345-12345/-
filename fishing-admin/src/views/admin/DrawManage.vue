<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDrawResults, exportDrawResults, type DrawResult } from '@/api/draw'
import { formatDateTime } from '@/utils/date'

const list = ref<DrawResult[]>([])
const loading = ref(false)
const query = reactive({
  userId: undefined as number | undefined,
  slotId: undefined as number | undefined,
  pageNum: 1,
  pageSize: 20
})
const total = ref(0)

const load = async () => {
  loading.value = true
  const res = await getDrawResults(query)
  list.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

const exportExcel = () => {
  exportDrawResults()
  ElMessage.success('开始导出')
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>抽号记录</h2>
      <el-button type="success" icon="Download" @click="exportExcel">导出 Excel</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="用户ID">
          <el-input-number v-model="query.userId" placeholder="用户ID" :controls="false" style="width: 140px" />
        </el-form-item>
        <el-form-item label="时段ID">
          <el-input-number v-model="query.slotId" placeholder="时段ID" :controls="false" style="width: 140px" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="load">查询</el-button>
          <el-button icon="Refresh" @click="query.userId = undefined; query.slotId = undefined; load()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="reservationId" label="预约ID" width="100" />
        <el-table-column prop="userId" label="用户ID" width="100" />
        <el-table-column prop="slotId" label="时段ID" width="100" />
        <el-table-column prop="spotCode" label="钓位编号" width="120" />
        <el-table-column prop="pondName" label="鱼塘" width="120" />
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
