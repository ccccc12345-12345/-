<script setup lang="ts">
import { computed, onMounted, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { Goods } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getMyShopOrders, payShopOrder, statusLabels, type ShopOrder } from '@/api/shop'

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
  error: '',
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const visibleOrders = computed(() => {
  if (state.activeStatus === 'all') return state.orders
  return state.orders.filter((order) => order.status === state.activeStatus)
})

const loadOrders = async () => {
  state.loading = true
  state.error = ''
  try {
    const res = await getMyShopOrders({ pageNum: state.pageNum, pageSize: state.pageSize })
    state.orders = res.data?.records || []
    state.total = res.data?.total || state.orders.length
  } catch (err: any) {
    state.orders = []
    state.total = 0
    state.error = err?.message || '商城订单加载失败，请确认后端服务已启动后重试'
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
      <div>
        <p class="eyebrow">Shop Orders</p>
        <h1>我的商城订单</h1>
      </div>
      <el-button type="primary" plain @click="router.push('/shop')">继续购买</el-button>
    </div>

    <div class="status-tabs">
      <button
        v-for="item in statusOptions"
        :key="item.value"
        class="status-tab"
        :class="{ active: state.activeStatus === item.value }"
        type="button"
        @click="state.activeStatus = item.value; onStatusChange()"
      >
        {{ item.label }}
      </button>
    </div>

    <el-alert v-if="state.error" class="error-alert" type="error" :title="state.error" show-icon :closable="false">
      <template #default>
        <el-button size="small" type="danger" plain @click="loadOrders">重试</el-button>
      </template>
    </el-alert>

    <section v-loading="state.loading" class="orders-list">
      <article v-for="order in visibleOrders" :key="order.id" class="order-card">
        <div class="order-head">
          <div>
            <strong>订单号：{{ order.orderNo }}</strong>
            <span>{{ order.createTime }}</span>
          </div>
          <el-tag :type="statusType(order.status)">{{ statusLabels[order.status] || order.status }}</el-tag>
        </div>

        <div class="order-items">
          <div v-for="item in order.items || []" :key="item.id" class="order-item">
            <el-image v-if="item.productImageUrl" :src="item.productImageUrl" fit="cover" class="item-image" />
            <div v-else class="item-image item-visual">
              <el-icon><Goods /></el-icon>
            </div>
            <div class="item-info">
              <div class="item-name">{{ item.productName || '商城商品' }}</div>
              <div class="item-price">¥{{ formatPrice(item.unitPrice) }} x {{ item.quantity }}</div>
            </div>
            <div class="item-subtotal">¥{{ formatPrice(item.subtotal) }}</div>
          </div>
        </div>

        <div class="order-footer">
          <div class="order-total">
            合计 <span>¥{{ formatPrice(order.totalAmount) }}</span>
          </div>
          <el-button v-if="order.status === 'pending_pay'" type="primary" @click="payOrder(order)">去支付</el-button>
        </div>
      </article>
    </section>

    <div v-if="!state.loading && !state.error && visibleOrders.length === 0" class="empty-tip">
      <el-empty description="暂无商城订单">
        <el-button type="primary" plain @click="router.push('/shop')">去商城看看</el-button>
      </el-empty>
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
  width: min(920px, calc(100% - 32px));
  margin: 0 auto;
  padding: 26px 0 40px;
}

.orders-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #b5742a;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.orders-header h1 {
  margin: 0;
  color: #172521;
  font-size: 28px;
  font-weight: 900;
}

.status-tabs {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  margin-bottom: 16px;
}

.status-tab {
  flex-shrink: 0;
  min-width: 82px;
  min-height: 38px;
  padding: 0 14px;
  border-radius: 6px;
  border: 1px solid #dce7e1;
  background: #fff;
  color: #586963;
  font-size: 13px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.18s ease;
}

.status-tab:hover,
.status-tab.active {
  background: #1f6a58;
  border-color: #1f6a58;
  color: #fff;
}

.error-alert {
  margin-bottom: 16px;
}

.orders-list {
  min-height: 240px;
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.order-card {
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 8px;
  padding: 18px;
  box-shadow: 0 10px 28px rgba(25, 47, 39, 0.07);
}

.order-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 14px;
  padding-bottom: 14px;
  border-bottom: 1px solid #edf2ef;
}

.order-head strong,
.order-head span {
  display: block;
}

.order-head strong {
  color: #263832;
  font-size: 14px;
}

.order-head span {
  margin-top: 5px;
  color: #81908a;
  font-size: 12px;
}

.order-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 14px 0;
}

.order-item {
  display: grid;
  grid-template-columns: 76px minmax(0, 1fr) auto;
  align-items: center;
  gap: 12px;
}

.item-image {
  width: 76px;
  height: 76px;
  border-radius: 8px;
  background: #edf4ef;
  flex-shrink: 0;
}

.item-visual {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1f6a58;
  font-size: 28px;
}

.item-info {
  min-width: 0;
}

.item-name {
  color: #172521;
  font-size: 15px;
  font-weight: 900;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-price {
  color: #7b8983;
  font-size: 13px;
}

.item-subtotal {
  color: #c7672e;
  font-size: 18px;
  font-weight: 900;
  white-space: nowrap;
}

.order-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding-top: 14px;
  border-top: 1px solid #edf2ef;
}

.order-total {
  color: #263832;
  font-size: 14px;
}

.order-total span {
  color: #c7672e;
  font-size: 22px;
  font-weight: 900;
}

.empty-tip {
  margin-top: 48px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 22px;
}

@media (max-width: 620px) {
  .orders-page {
    width: calc(100% - 20px);
    padding: 12px 0 28px;
  }

  .orders-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .order-item {
    grid-template-columns: 64px minmax(0, 1fr);
  }

  .item-image {
    width: 64px;
    height: 64px;
  }

  .item-subtotal {
    grid-column: 1 / -1;
  }
}
</style>
