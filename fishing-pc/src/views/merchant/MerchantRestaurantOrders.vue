<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Phone, RefreshRight, User } from '@element-plus/icons-vue'
import {
  getMerchantRestaurantOrders,
  updateMerchantRestaurantOrderStatus,
  type RestaurantOrder
} from '@/api/merchant'
import { restaurantStatusLabels } from '@/api/restaurant'
import { useMerchantPush } from '@/composables/useMerchantPush'
import { formatDateTime } from '@/utils/date'
import RestaurantOrderItems from '@/components/RestaurantOrderItems.vue'

const list = ref<RestaurantOrder[]>([])
const loading = ref(false)
const statusFilter = ref('')
const expandedIds = ref<number[]>([])

const statusOptions = [
  { label: '待处理', value: 'pending' },
  { label: '已接单', value: 'accepted' },
  { label: '制作中', value: 'cooking' },
  { label: '已配送', value: 'delivered' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const statusFlow: Record<string, { next: string; label: string }> = {
  pending: { next: 'accepted', label: '确认接单' },
  accepted: { next: 'cooking', label: '开始制作' },
  cooking: { next: 'delivered', label: '确认送达' },
  delivered: { next: 'completed', label: '完成订单' }
}

const filteredList = computed(() =>
  statusFilter.value ? list.value.filter((item) => item.status === statusFilter.value) : list.value
)

const stats = computed(() => {
  const total = list.value.length
  const pending = list.value.filter((o) => o.status === 'pending').length
  const cooking = list.value.filter((o) => o.status === 'cooking' || o.status === 'accepted').length
  const today = list.value.filter((o) => {
    if (!o.createTime) return false
    const d = new Date(o.createTime)
    const now = new Date()
    return d.toDateString() === now.toDateString()
  }).length
  return { total, pending, cooking, today }
})

const load = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    const params: { status?: string } = {}
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getMerchantRestaurantOrders(params)
    list.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const advanceStatus = async (row: RestaurantOrder) => {
  const flow = statusFlow[row.status]
  if (!flow) return
  try {
    await updateMerchantRestaurantOrderStatus(row.id, flow.next)
    ElMessage.success('状态更新成功')
    load(false)
  } catch (e: any) {
    ElMessage.error(e.message || '操作失败')
  }
}

const statusClass = (value: string) => {
  const map: Record<string, string> = {
    pending: 'pending',
    accepted: 'accepted',
    cooking: 'cooking',
    delivered: 'delivered',
    completed: 'completed',
    cancelled: 'cancelled'
  }
  return map[value] || ''
}

const toggleExpand = (id: number) => {
  const idx = expandedIds.value.indexOf(id)
  if (idx > -1) {
    expandedIds.value.splice(idx, 1)
  } else {
    expandedIds.value.push(id)
  }
}

const itemCount = (order: RestaurantOrder) =>
  order.items?.reduce((sum, item) => sum + (item.quantity || 0), 0) || 0

const formatPrice = (price?: number) => `¥${((price || 0) / 100).toFixed(2)}`

useMerchantPush({
  events: ['RESTAURANT_ORDER_CREATED', 'RESTAURANT_ORDER_STATUS_CHANGED'],
  onEvent: () => load(false),
  fallback: () => load(false)
})

onMounted(() => {
  load()
})
</script>

<template>
  <section class="page">
    <header class="page-head">
      <div>
        <h1>点餐订单</h1>
        <p class="lead">钓位客户的实时点餐内容与全流程状态。</p>
      </div>
      <el-button text :icon="RefreshRight" :loading="loading" @click="load">
        刷新
      </el-button>
    </header>

    <div class="metrics">
      <div class="metric">
        <span class="metric-value">{{ stats.today }}</span>
        <span class="metric-label">今日订单</span>
      </div>
      <div class="metric">
        <span class="metric-value">{{ stats.pending }}</span>
        <span class="metric-label">待处理</span>
      </div>
      <div class="metric">
        <span class="metric-value">{{ stats.cooking }}</span>
        <span class="metric-label">制作中</span>
      </div>
      <div class="metric">
        <span class="metric-value">{{ stats.total }}</span>
        <span class="metric-label">全部</span>
      </div>
    </div>

    <div class="tabs">
      <button
        class="tab"
        :class="{ active: statusFilter === '' }"
        type="button"
        @click="statusFilter = ''; load()"
      >
        全部
      </button>
      <button
        v-for="s in statusOptions"
        :key="s.value"
        class="tab"
        :class="{ active: statusFilter === s.value }"
        type="button"
        @click="statusFilter = statusFilter === s.value ? '' : s.value; load()"
      >
        {{ s.label }}
      </button>
    </div>

    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="filteredList.length === 0" class="empty">
      <p>暂无订单</p>
    </div>

    <div v-else class="orders">
      <article
        v-for="order in filteredList"
        :key="order.id"
        class="order-row"
        :class="{ expanded: expandedIds.includes(order.id) }"
      >
        <button class="row-trigger" type="button" @click="toggleExpand(order.id)">
          <span class="status-dot" :class="statusClass(order.status)" />
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="order-time">{{ formatDateTime(order.createTime) }}</span>
          <span class="order-customer">
            <el-icon><User /></el-icon>
            <span>{{ order.userNickname || '匿名' }}</span>
          </span>
          <span class="order-phone">
            <el-icon><Phone /></el-icon>
            <span>{{ order.userPhone || '-' }}</span>
          </span>
          <span class="order-count">{{ itemCount(order) }} 件</span>
          <span class="order-total">{{ formatPrice(order.totalAmount) }}</span>
          <span class="status-text" :class="statusClass(order.status)">
            {{ restaurantStatusLabels[order.status] || order.status }}
          </span>
          <el-icon class="chevron" :class="{ up: expandedIds.includes(order.id) }">
            <RefreshRight />
          </el-icon>
        </button>

        <div class="row-detail">
          <RestaurantOrderItems :items="order.items || []" :compact="true" />
          <div v-if="order.remark" class="remark">
            <span>客户备注</span>
            <p>{{ order.remark }}</p>
          </div>
          <div class="actions">
            <button
              v-if="statusFlow[order.status]"
              class="action-btn primary"
              type="button"
              @click="advanceStatus(order)"
            >
              {{ statusFlow[order.status].label }}
            </button>
            <button v-else class="action-btn" type="button" disabled>已结束</button>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
/* Hallmark · macrostructure: Merchant Editorial List · tone: luxury · anchor hue: forest-green · redesign · existing-system */
.page {
  padding: 48px 40px 80px;
  color: var(--fp-text);
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  padding-bottom: 28px;
  border-bottom: 1px solid var(--fp-border);
  margin-bottom: 28px;
}

.page-head h1 {
  margin: 0;
  font-size: 38px;
  font-weight: 400;
  letter-spacing: -0.5px;
  font-family: var(--fp-font-display);
}

.lead {
  margin: 10px 0 0;
  color: var(--fp-text-secondary);
  font-size: 15px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 24px;
  margin-bottom: 32px;
}

.metric {
  padding: 20px 0;
  border-bottom: 1px solid var(--fp-border);
}

.metric-value {
  display: block;
  font-size: 32px;
  font-weight: 400;
  font-family: var(--fp-font-display);
  color: var(--fp-text);
  line-height: 1;
}

.metric-label {
  display: block;
  margin-top: 8px;
  font-size: 12px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--fp-muted);
  font-weight: 700;
}

.tabs {
  display: flex;
  gap: 4px;
  margin-bottom: 32px;
  border-bottom: 1px solid var(--fp-border);
}

.tab {
  position: relative;
  padding: 14px 18px;
  border: 0;
  background: transparent;
  color: var(--fp-muted);
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: color var(--fp-dur-fast) var(--fp-ease-out);
}

.tab::after {
  content: "";
  position: absolute;
  left: 0;
  right: 0;
  bottom: 0;
  height: 2px;
  background: var(--fp-primary);
  transform: scaleX(0);
  transition: transform var(--fp-dur-fast) var(--fp-ease-out);
}

.tab:hover {
  color: var(--fp-text);
}

.tab.active {
  color: var(--fp-primary);
}

.tab.active::after {
  transform: scaleX(1);
}

.empty {
  text-align: center;
  padding: 80px 0;
  color: var(--fp-muted);
}

.empty p {
  margin: 0;
  font-size: 15px;
}

.orders {
  border-top: 1px solid var(--fp-border);
}

.order-row {
  border-bottom: 1px solid var(--fp-border);
}

.row-trigger {
  width: 100%;
  display: grid;
  grid-template-columns: 16px 1.4fr 1fr 120px 130px 70px 100px 90px 20px;
  align-items: center;
  gap: 16px;
  padding: 22px 0;
  border: 0;
  background: transparent;
  color: inherit;
  text-align: left;
  cursor: pointer;
  transition: background-color var(--fp-dur-fast) var(--fp-ease-out);
}

.row-trigger:hover {
  background: oklch(98% 0.01 100 / 0.5);
}

.status-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: var(--fp-muted);
}

.status-dot.pending,
.status-dot.cooking {
  background: var(--fp-accent);
}

.status-dot.accepted {
  background: oklch(70% 0.12 85);
}

.status-dot.delivered,
.status-dot.completed {
  background: var(--fp-primary);
}

.status-dot.cancelled {
  background: oklch(60% 0.12 25);
}

.order-no {
  font-weight: 700;
  color: var(--fp-text);
}

.order-time {
  font-size: 13px;
  color: var(--fp-text-secondary);
}

.order-customer,
.order-phone {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: var(--fp-text-secondary);
}

.order-customer .el-icon,
.order-phone .el-icon {
  color: var(--fp-muted);
}

.order-phone {
  font-variant-numeric: tabular-nums;
}

.order-count {
  font-size: 13px;
  color: var(--fp-muted);
  text-align: right;
}

.order-total {
  font-weight: 700;
  text-align: right;
  color: var(--fp-text);
}

.status-text {
  text-align: right;
  font-size: 13px;
  font-weight: 700;
}

.status-text.pending,
.status-text.cooking {
  color: var(--fp-accent-dark);
}

.status-text.accepted {
  color: oklch(55% 0.10 85);
}

.status-text.delivered,
.status-text.completed {
  color: var(--fp-primary);
}

.status-text.cancelled {
  color: oklch(50% 0.16 25);
}

.chevron {
  color: var(--fp-muted);
  transition: transform var(--fp-dur-normal) var(--fp-ease-out);
  transform: rotate(90deg);
  justify-self: end;
}

.chevron.up {
  transform: rotate(-90deg);
}

.row-detail {
  max-height: 0;
  overflow: hidden;
  transition: max-height var(--fp-dur-slow) var(--fp-ease-out),
    padding var(--fp-dur-slow) var(--fp-ease-out);
  padding: 0 0 0 36px;
}

.order-row.expanded .row-detail {
  max-height: 1200px;
  padding: 0 0 40px 36px;
}

.remark {
  margin: 24px 0;
  padding: 20px 0;
  border-top: 1px dashed var(--fp-border);
  border-bottom: 1px dashed var(--fp-border);
}

.remark span {
  font-size: 11px;
  letter-spacing: 1px;
  text-transform: uppercase;
  color: var(--fp-muted);
  font-weight: 700;
}

.remark p {
  margin: 6px 0 0;
  color: var(--fp-text-secondary);
  font-size: 14px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  padding-top: 24px;
  border-top: 1px solid var(--fp-border);
}

.action-btn {
  padding: 10px 28px;
  border: 1px solid var(--fp-border);
  background: transparent;
  color: var(--fp-text-secondary);
  font-size: 13px;
  font-weight: 700;
  letter-spacing: 0.5px;
  cursor: pointer;
  transition: border-color var(--fp-dur-fast) var(--fp-ease-out),
    color var(--fp-dur-fast) var(--fp-ease-out),
    background-color var(--fp-dur-fast) var(--fp-ease-out);
}

.action-btn:hover:not(:disabled) {
  border-color: var(--fp-primary);
  color: var(--fp-primary);
}

.action-btn.primary {
  border-color: var(--fp-primary);
  color: var(--fp-primary);
}

.action-btn.primary:hover {
  background: var(--fp-primary);
  color: #fff;
}

.action-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

@media (max-width: 1100px) {
  .row-trigger {
    grid-template-columns: 16px 1.4fr 1fr 120px 70px 100px 90px 20px;
  }

  .order-phone {
    display: none;
  }
}

@media (max-width: 860px) {
  .metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .row-trigger {
    grid-template-columns: 16px 1.5fr 1fr 70px 90px 20px;
    gap: 14px;
  }

  .order-customer,
  .order-count {
    display: none;
  }
}

@media (max-width: 560px) {
  .page {
    padding: 32px 20px 60px;
  }

  .page-head h1 {
    font-size: 28px;
  }

  .tabs {
    overflow-x: auto;
    scrollbar-width: none;
  }

  .tabs::-webkit-scrollbar {
    display: none;
  }

  .tab {
    white-space: nowrap;
  }

  .row-trigger {
    grid-template-columns: 10px 1fr auto auto 20px;
    gap: 10px;
    padding: 18px 0;
  }

  .order-time {
    display: none;
  }

  .row-detail {
    padding-left: 20px;
  }

  .order-row.expanded .row-detail {
    padding-left: 20px;
  }
}
</style>
