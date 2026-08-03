<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import {
  getMerchantRestaurantOrders,
  updateMerchantRestaurantOrderStatus,
  type RestaurantOrderVO
} from '@/api/restaurant-merchant'
import { useMerchantPush } from '@/composables/useMerchantPush'
import { usePondStore } from '@/store/pond'
import { formatDateTime } from '@/utils/date'

const pondStore = usePondStore()

const list = ref<RestaurantOrderVO[]>([])
const loading = ref(false)
const statusFilter = ref('')

const statusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '制作中', value: 'cooking' },
  { label: '已配送', value: 'delivered' },
  { label: '已完成', value: 'completed' }
]

const statusFlow: Record<string, { next: string; label: string; type: any }> = {
  pending: { next: 'cooking', label: '接单制作', type: 'primary' },
  cooking: { next: 'delivered', label: '确认送达', type: 'success' },
  delivered: { next: 'completed', label: '完成订单', type: 'warning' }
}

const load = async () => {
  loading.value = true
  try {
    const params: { pondId?: number; status?: string } = {}
    if (pondStore.currentPondId) params.pondId = pondStore.currentPondId
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getMerchantRestaurantOrders(params)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const advanceStatus = async (row: RestaurantOrderVO) => {
  const flow = statusFlow[row.status]
  if (!flow) return
  await updateMerchantRestaurantOrderStatus(row.id, flow.next)
  ElMessage.success('状态更新成功')
  load()
}

const statusLabel = (value: string) => {
  return statusOptions.find(s => s.value === value)?.label || value
}

const statusType = (value: string) => {
  const map: Record<string, any> = {
    pending: 'info',
    cooking: 'warning',
    delivered: 'success',
    completed: ''
  }
  return map[value] || 'info'
}

const formatPrice = (price: number) => (price / 100).toFixed(2)

useMerchantPush({
  events: ['RESTAURANT_ORDER_CREATED', 'RESTAURANT_ORDER_STATUS_CHANGED'],
  onEvent: () => load(),
  fallback: () => load()
})

onMounted(load)
watch(() => pondStore.currentPondId, load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>餐厅订单</h2>
      <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="load">
        <el-option
          v-for="opt in statusOptions"
          :key="opt.value"
          :label="opt.label"
          :value="opt.value"
        />
      </el-select>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="160" />
        <el-table-column prop="pondName" label="鱼塘" width="120" />
        <el-table-column prop="spotCode" label="钓位" width="100">
          <template #default="{ row }">{{ row.spotCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="userNickname" label="用户" width="110">
          <template #default="{ row }">{{ row.userNickname || '-' }}</template>
        </el-table-column>
        <el-table-column prop="userPhone" label="手机号" width="130">
          <template #default="{ row }">{{ row.userPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="菜品" min-width="200">
          <template #default="{ row }">
            <div v-for="item in row.items" :key="item.id" class="item-line">
              {{ item.menuName }} x{{ item.quantity }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ formatPrice(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="statusFlow[row.status]"
              :type="statusFlow[row.status].type"
              size="small"
              @click="advanceStatus(row)"
            >
              {{ statusFlow[row.status].label }}
            </el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && list.length === 0" description="暂无订单" />
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

.item-line {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}
</style>
