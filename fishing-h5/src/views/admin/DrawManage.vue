<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { getDrawResults, exportDrawResults, type DrawResult } from '@/api/draw'

const list = ref<DrawResult[]>([])
const loading = ref(false)
const query = ref({ pageNum: 1, pageSize: 20 })
const total = ref(0)

const load = async () => {
  loading.value = true
  const res = await getDrawResults(query.value)
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
  <div class="admin-page">
    <div class="admin-header">
      <el-button icon="Back" circle @click="$router.push('/admin')" />
      <h3>抽号记录</h3>
      <el-button type="success" icon="Download" circle @click="exportExcel" />
    </div>

    <el-table :data="list" v-loading="loading" size="small" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column prop="reservationId" label="预约ID" width="80" />
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="spotCode" label="钓位" width="80" />
      <el-table-column prop="drawTime" label="抽号时间" />
    </el-table>

    <el-pagination
      v-model:current-page="query.pageNum"
      :page-size="query.pageSize"
      :total="total"
      layout="prev, pager, next"
      small
      @current-change="load"
      style="margin-top: 12px; justify-content: center"
    />
  </div>
</template>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 16px;
  background: var(--bg);
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.admin-header h3 {
  font-size: 18px;
  color: var(--primary);
}
</style>
