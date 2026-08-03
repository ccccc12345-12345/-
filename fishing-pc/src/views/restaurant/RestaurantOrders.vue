<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { getMyRestaurantOrders, restaurantStatusLabels, type RestaurantOrder } from '@/api/restaurant'
import RestaurantOrderItems from '@/components/RestaurantOrderItems.vue'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const orders = ref<RestaurantOrder[]>([])
const status = ref('')
const expandedIds = ref<number[]>([])

const statuses = Object.entries(restaurantStatusLabels).map(([value, label]) => ({ value, label }))

const filtered = computed(() =>
  status.value ? orders.value.filter((item) => item.status === status.value) : orders.value
)

const money = (value?: number) => `¥${((value || 0) / 100).toFixed(2)}`
const itemCount = (order: RestaurantOrder) =>
  order.items?.reduce((sum, item) => sum + (item.quantity || 0), 0) || 0

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getMyRestaurantOrders()
    orders.value = res.data || []
  } catch (e: any) {
    error.value = e?.message || '餐厅订单加载失败'
    ElMessage.error(error.value)
  } finally {
    loading.value = false
  }
}

const toggleExpand = (id: number) => {
  const idx = expandedIds.value.indexOf(id)
  if (idx > -1) {
    expandedIds.value.splice(idx, 1)
  } else {
    expandedIds.value.push(id)
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

onMounted(loadData)
</script>

<template>
  <section class="page">
    <header class="page-head">
      <div>
        <h1>餐厅订单</h1>
        <p class="lead">每一次点餐的完整明细，菜品、价格、状态清晰可见。</p>
      </div>
      <el-button text :icon="RefreshRight" :loading="loading" @click="loadData">
        刷新
      </el-button>
    </header>

    <div class="tabs">
      <button
        class="tab"
        :class="{ active: status === '' }"
        type="button"
        @click="status = ''; loadData()"
      >
        全部
      </button>
      <button
        v-for="s in statuses"
        :key="s.value"
        class="tab"
        :class="{ active: status === s.value }"
        type="button"
        @click="status = status === s.value ? '' : s.value; loadData()"
      >
        {{ s.label }}
      </button>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />

    <el-skeleton v-if="loading" :rows="6" animated />

    <div v-else-if="filtered.length === 0" class="empty">
      <p>暂无餐厅订单</p>
      <button class="text-btn" type="button" @click="router.push('/restaurant')">
        去餐厅点餐
      </button>
    </div>

    <div v-else class="orders">
      <article
        v-for="order in filtered"
        :key="order.id"
        class="order-row"
        :class="{ expanded: expandedIds.includes(order.id) }"
      >
        <button class="row-trigger" type="button" @click="toggleExpand(order.id)">
          <span class="status-dot" :class="statusClass(order.status)" />
          <span class="order-no">{{ order.orderNo }}</span>
          <span class="order-time">{{ order.createTime }}</span>
          <span class="order-place">{{ order.pondName || '鱼塘餐厅' }}</span>
          <span class="order-count">{{ itemCount(order) }} 件</span>
          <span class="order-total">{{ money(order.totalAmount) }}</span>
          <span class="status-text" :class="statusClass(order.status)">
            {{ restaurantStatusLabels[order.status] || order.status }}
          </span>
          <el-icon class="chevron" :class="{ up: expandedIds.includes(order.id) }">
            <RefreshRight />
          </el-icon>
        </button>

        <div class="row-detail">
          <RestaurantOrderItems :items="order.items || []" />
          <div v-if="order.remark" class="remark">
            <span>备注</span>
            <p>{{ order.remark }}</p>
          </div>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
/* Hallmark · macrostructure: Editorial List · tone: luxury · anchor hue: forest-green · redesign · existing-system */
.page {
  width: min(900px, calc(100% - 48px));
  margin: 48px auto 80px;
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
  color: var(--fp-text);
}

.lead {
  margin: 10px 0 0;
  color: var(--fp-text-secondary);
  font-size: 15px;
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
  margin: 0 0 12px;
  font-size: 15px;
}

.text-btn {
  border: 0;
  background: transparent;
  color: var(--fp-primary);
  font-size: 14px;
  font-weight: 700;
  cursor: pointer;
  padding: 0;
  border-bottom: 1px solid transparent;
  transition: border-color var(--fp-dur-fast) var(--fp-ease-out);
}

.text-btn:hover {
  border-color: var(--fp-primary);
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
  grid-template-columns: 16px 1.4fr 1fr 1fr 80px 100px 90px 20px;
  align-items: center;
  gap: 20px;
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

.order-time,
.order-place {
  font-size: 13px;
  color: var(--fp-text-secondary);
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
  max-height: 1000px;
  padding: 0 0 32px 36px;
}

.remark {
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px dashed var(--fp-border);
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

@media (max-width: 860px) {
  .row-trigger {
    grid-template-columns: 16px 1.5fr 1fr 70px 90px 20px;
    gap: 14px;
  }

  .order-place,
  .order-count {
    display: none;
  }
}

@media (max-width: 560px) {
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
