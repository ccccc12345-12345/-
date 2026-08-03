<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { getMyShopOrders, payShopOrder, statusLabels, type ShopOrder } from '@/api/shop'
import { ElMessage } from 'element-plus'

type OrderStatus = ShopOrder['status']

const router = useRouter()

const statusOptions: { label: string; value: ShopOrder['status'] | 'all' }[] = [
  { label: '全部', value: 'all' },
  { label: '待支付', value: 'pending_pay' },
  { label: '已支付', value: 'paid' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const state = reactive({
  activeStatus: 'all' as ShopOrder['status'] | 'all',
  orders: [] as ShopOrder[],
  loading: false,
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const loadOrders = async () => {
  state.loading = true
  try {
    const res = await getMyShopOrders({ pageNum: state.pageNum, pageSize: state.pageSize })
    let records = res.data?.records || []
    if (state.activeStatus !== 'all') {
      records = records.filter((o) => o.status === state.activeStatus)
    }
    state.orders = records
    state.total = res.data?.total || 0
  } finally {
    state.loading = false
  }
}

const onStatusChange = () => {
  state.pageNum = 1
  loadOrders()
}

const onPageChange = (page: number) => {
  state.pageNum = page
  loadOrders()
}

const payOrder = async (order: ShopOrder) => {
  try {
    await payShopOrder(order.id)
    ElMessage.success('支付成功')
    loadOrders()
  } catch (err: any) {
    ElMessage.error(err?.message || '支付失败')
  }
}

const formatPrice = (price?: number | null) => {
  if (price == null) return '0.00'
  return (price / 100).toFixed(2)
}

const statusType = (status: ShopOrder['status']) => {
  switch (status) {
    case 'paid':
    case 'completed':
      return 'success'
    case 'pending_pay':
      return 'warning'
    case 'cancelled':
      return 'info'
    default:
      return ''
  }
}

onMounted(loadOrders)
</script>

<template>
  <div class="orders-page">
    <div class="orders-header">
      <h2 class="orders-title">我的订单</h2>
      <el-button text @click="router.push('/shop')">继续购物</el-button>
    </div>

    <div class="status-tabs">
      <div
        v-for="item in statusOptions"
        :key="item.value"
        class="status-tab"
        :class="{ active: state.activeStatus === item.value }"
        @click="state.activeStatus = item.value; onStatusChange()"
      >
        {{ item.label }}
      </div>
    </div>

    <div v-loading="state.loading" class="orders-list">
      <div v-for="order in state.orders" :key="order.id" class="order-card">
        <div class="order-head">
          <span class="order-no">订单号：{{ order.orderNo }}</span>
          <el-tag :type="statusType(order.status)">{{ statusLabels[order.status] }}</el-tag>
        </div>

        <div class="order-items">
          <div v-for="item in order.items" :key="item.id" class="order-item">
            <el-image
              :src="item.productImageUrl || 'https://placehold.co/120x120/e8f4f8/0f4c75?text=No+Image'"
              fit="cover"
              class="item-image"
            />
            <div class="item-info">
              <div class="item-name">{{ item.productName || '-' }}</div>
              <div class="item-price">¥{{ formatPrice(item.unitPrice) }} × {{ item.quantity }}</div>
            </div>
            <div class="item-subtotal">¥{{ formatPrice(item.subtotal) }}</div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-total">
            合计：<span class="total-price">¥{{ formatPrice(order.totalAmount) }}</span>
          </div>
          <el-button
            v-if="order.status === 'pending_pay'"
            type="primary"
            size="small"
            @click="payOrder(order)"
          >
            去支付
          </el-button>
        </div>
      </div>
    </div>

    <div v-if="!state.loading && state.orders.length === 0" class="empty-tip">
      <el-empty description="暂无订单" />
    </div>

    <div v-if="state.total > state.pageSize" class="pagination-bar">
      <el-pagination
        v-model:current-page="state.pageNum"
        :page-size="state.pageSize"
        :total="state.total"
        layout="prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.orders-page {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
}

.orders-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.orders-title {
  margin: 0;
  font-size: 20px;
  color: #303133;
}

.status-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
}

.status-tab {
  flex-shrink: 0;
  padding: 6px 14px;
  border-radius: 16px;
  background: #f5f7fa;
  color: #606266;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.status-tab.active {
  background: #0f4c75;
  color: #fff;
}

.orders-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  background: #fff;
  border-radius: 12px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 12px;
  border-bottom: 1px solid #ebeef5;
}

.order-no {
  color: #909399;
  font-size: 13px;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-item {
  display: flex;
  align-items: center;
  gap: 12px;
}

.item-image {
  width: 70px;
  height: 70px;
  border-radius: 8px;
  flex-shrink: 0;
}

.item-info {
  flex: 1;
  min-width: 0;
}

.item-name {
  font-size: 14px;
  color: #303133;
  margin-bottom: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  color: #909399;
  font-size: 13px;
}

.item-subtotal {
  color: #e8a838;
  font-size: 16px;
  font-weight: 700;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
}

.order-total {
  color: #303133;
  font-size: 14px;
}

.total-price {
  color: #e8a838;
  font-size: 20px;
  font-weight: 700;
}

.empty-tip {
  margin-top: 40px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 600px) {
  .orders-page {
    padding: 12px;
  }
}
</style>
