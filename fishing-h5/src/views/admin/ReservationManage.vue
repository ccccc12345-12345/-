<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getAdminReservations, adminCancelReservation, type Reservation } from '@/api/reservation'
import { formatDate, formatTime } from '@/utils/date'

const list = ref<Reservation[]>([])
const loading = ref(false)
const query = ref({ status: '', pageNum: 1, pageSize: 20 })
const total = ref(0)

const load = async () => {
  loading.value = true
  const res = await getAdminReservations(query.value)
  list.value = res.data.records
  total.value = res.data.total
  loading.value = false
}

const cancel = async (row: Reservation) => {
  try {
    await ElMessageBox.confirm('确定取消该预约吗？', '提示', { type: 'warning' })
    await adminCancelReservation(row.id)
    ElMessage.success('取消成功')
    load()
  } catch {}
}

const statusClass = (status: string) => {
  const map: Record<string, string> = {
    '待抽号': 'status-pending',
    '已抽号': 'status-drawn',
    '预约取消': 'status-cancelled',
    '过期失效': 'status-expired'
  }
  return map[status] || ''
}

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <div class="admin-header">
      <el-button icon="Back" circle @click="$router.push('/admin')" />
      <h3>预约记录</h3>
    </div>

    <div class="filter-bar">
      <el-select v-model="query.status" placeholder="状态" clearable size="small" style="width: 120px">
        <el-option label="待抽号" value="待抽号" />
        <el-option label="已抽号" value="已抽号" />
        <el-option label="预约取消" value="预约取消" />
        <el-option label="过期失效" value="过期失效" />
      </el-select>
      <el-button type="primary" size="small" @click="load">查询</el-button>
    </div>

    <el-table :data="list" v-loading="loading" size="small" stripe>
      <el-table-column prop="id" label="ID" width="60" />
      <el-table-column label="日期/时段" width="140">
        <template #default="{ row }">
          <div v-if="row.slot">
            {{ formatDate(row.slot.slotDate) }}<br>
            {{ row.slot.slotName }}
          </div>
        </template>
      </el-table-column>
      <el-table-column prop="userId" label="用户ID" width="80" />
      <el-table-column prop="status" label="状态" width="90">
        <template #default="{ row }">
          <span class="status-tag" :class="statusClass(row.status)">{{ row.status }}</span>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="90">
        <template #default="{ row }">
          <el-button v-if="row.status === '待抽号'" link type="danger" size="small" @click="cancel(row)">取消</el-button>
        </template>
      </el-table-column>
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
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.admin-header h3 {
  font-size: 18px;
  color: var(--primary);
}

.filter-bar {
  display: flex;
  gap: 8px;
  margin-bottom: 12px;
}
</style>
