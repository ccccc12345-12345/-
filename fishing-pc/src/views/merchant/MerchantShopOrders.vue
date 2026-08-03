<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { View } from '@element-plus/icons-vue'
import { getMerchantShopOrders, updateMerchantShopOrderStatus, type ShopOrder, statusLabels } from '@/api/shop'
import { useMerchantPush } from '@/composables/useMerchantPush'
import { formatDateTime } from '@/utils/date'

const list = ref<ShopOrder[]>([])
const loading = ref(false)
const total = ref(0)
const statusFilter = ref('')
const detailVisible = ref(false)
const currentOrder = ref<ShopOrder | null>(null)

const statusOptions = [
  { label: '待支付', value: 'pending_pay' },
  { label: '已支付', value: 'paid' },
  { label: '已完成', value: 'completed' },
  { label: '已取消', value: 'cancelled' }
]

const statusType = (value: string) => {
  const map: Record<string, any> = {
    pending_pay: 'info',
    paid: 'warning',
    completed: 'success',
    cancelled: 'danger'
  }
  return map[value] || 'info'
}

const query = ref({
  pageNum: 1,
  pageSize: 20
})

const load = async (showLoading = true) => {
  if (showLoading) loading.value = true
  try {
    const params: any = { pageNum: query.value.pageNum, pageSize: query.value.pageSize }
    if (statusFilter.value) params.status = statusFilter.value
    const res = await getMerchantShopOrders(params)
    list.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    total.value = Array.isArray(res.data) ? res.data.length : (res.data?.total || 0)
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const openDetail = (row: ShopOrder) => {
  currentOrder.value = row
  detailVisible.value = true
}

const closeDetail = () => {
  detailVisible.value = false
  currentOrder.value = null
}

const handleStatusChange = async (row: ShopOrder, status: string) => {
  const label = statusLabels[status as ShopOrder['status']] || status
  try {
    await ElMessageBox.confirm(`确认将订单状态更新为「${label}」吗？`, '确认操作', { type: 'warning' })
    await updateMerchantShopOrderStatus(row.id, status)
    ElMessage.success('状态更新成功')
    load(false)
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  }
}

const formatPrice = (price?: number | null) => {
  if (price == null) return '0.00'
  return (price / 100).toFixed(2)
}

const handleRefresh = () => {
  load(true)
  ElMessage.success('已刷新')
}

useMerchantPush({
  events: ['SHOP_ORDER_CREATED', 'SHOP_ORDER_STATUS_CHANGED'],
  onEvent: (type) => {
    ElMessage.info(type === 'SHOP_ORDER_CREATED' ? '收到新商城订单' : '商城订单状态已更新')
    load(false)
  },
  fallback: () => load(false)
})

onMounted(() => {
  load()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>商城订单</h2>
      <div class="header-actions">
        <el-button plain @click="handleRefresh" :loading="loading">
          <el-icon><Refresh /></el-icon>
          刷新
        </el-button>
        <el-select v-model="statusFilter" placeholder="全部状态" clearable style="width: 160px" @change="query.pageNum = 1; load()">
          <el-option
            v-for="opt in statusOptions"
            :key="opt.value"
            :label="opt.label"
            :value="opt.value"
          />
        </el-select>
      </div>
    </div>

    <el-card v-loading="loading" shadow="never" class="table-card">
      <el-table :data="list" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="170" />
        <el-table-column label="用户信息" min-width="140">
          <template #default="{ row }">
            <div class="user-info">
              <div class="nickname">{{ row.userNickname || `用户${row.userId}` }}</div>
              <div v-if="row.userPhone" class="phone">{{ row.userPhone }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="pondName" label="鱼塘" width="120">
          <template #default="{ row }">{{ row.pondName || '-' }}</template>
        </el-table-column>
        <el-table-column label="商品" min-width="200">
          <template #default="{ row }">
            <div v-for="item in row.items" :key="item.id" class="item-line">
              {{ item.productName }} x{{ item.quantity }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="110">
          <template #default="{ row }">¥{{ formatPrice(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusLabels[row.status as ShopOrder['status']] || row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="下单时间" width="160">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button size="small" :icon="View" @click="openDetail(row)">详情</el-button>
            <el-button
              v-if="row.status === 'pending_pay'"
              type="danger"
              size="small"
              @click="handleStatusChange(row, 'cancelled')"
            >取消订单</el-button>
            <el-button
              v-if="row.status === 'paid'"
              type="success"
              size="small"
              @click="handleStatusChange(row, 'completed')"
            >确认完成</el-button>
            <el-button
              v-if="row.status === 'paid'"
              type="danger"
              size="small"
              @click="handleStatusChange(row, 'cancelled')"
            >取消订单</el-button>
            <span v-if="row.status === 'completed' || row.status === 'cancelled'">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div v-if="!loading && list.length === 0" class="empty-wrap">
        <el-empty description="暂无商城订单">
          <p class="empty-tip">用户端提交并支付后，订单将实时出现在此处。</p>
        </el-empty>
      </div>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="load"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="detailVisible"
      title="订单详情"
      width="600px"
      align-center
      destroy-on-close
      @close="closeDetail"
    >
      <template v-if="currentOrder">
        <div class="detail-section">
          <div class="detail-row">
            <span>订单号</span>
            <strong>{{ currentOrder.orderNo }}</strong>
          </div>
          <div class="detail-row">
            <span>下单用户</span>
            <strong>{{ currentOrder.userNickname || `用户${currentOrder.userId}` }}</strong>
          </div>
          <div class="detail-row">
            <span>联系电话</span>
            <strong>{{ currentOrder.userPhone || '-' }}</strong>
          </div>
          <div class="detail-row">
            <span>所属鱼塘</span>
            <strong>{{ currentOrder.pondName || '-' }}</strong>
          </div>
          <div class="detail-row">
            <span>下单时间</span>
            <strong>{{ formatDateTime(currentOrder.createTime) }}</strong>
          </div>
          <div class="detail-row">
            <span>订单状态</span>
            <el-tag :type="statusType(currentOrder.status)">
              {{ statusLabels[currentOrder.status as ShopOrder['status']] || currentOrder.status }}
            </el-tag>
          </div>
        </div>

        <div class="detail-section">
          <h4>商品明细</h4>
          <div v-for="item in currentOrder.items" :key="item.id" class="detail-item">
            <el-image v-if="item.productImageUrl" :src="item.productImageUrl" fit="cover" class="detail-item-img" />
            <div v-else class="detail-item-img placeholder">{{ item.productName?.slice(0, 1) }}</div>
            <div class="detail-item-info">
              <div class="detail-item-name">{{ item.productName || '商城商品' }}</div>
              <div class="detail-item-meta">¥{{ formatPrice(item.unitPrice) }} x {{ item.quantity }}</div>
            </div>
            <div class="detail-item-subtotal">¥{{ formatPrice(item.subtotal) }}</div>
          </div>
        </div>

        <div class="detail-total">
          <span>合计金额</span>
          <strong>¥{{ formatPrice(currentOrder.totalAmount) }}</strong>
        </div>
      </template>

      <template #footer>
        <div class="dialog-footer">
          <el-button @click="closeDetail">关闭</el-button>
          <el-button
            v-if="currentOrder?.status === 'pending_pay'"
            type="danger"
            @click="handleStatusChange(currentOrder, 'cancelled'); closeDetail()"
          >取消订单</el-button>
          <el-button
            v-if="currentOrder?.status === 'paid'"
            type="success"
            @click="handleStatusChange(currentOrder, 'completed'); closeDetail()"
          >确认完成</el-button>
          <el-button
            v-if="currentOrder?.status === 'paid'"
            type="danger"
            @click="handleStatusChange(currentOrder, 'cancelled'); closeDetail()"
          >取消订单</el-button>
        </div>
      </template>
    </el-dialog>
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
  color: #1f6a58;
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.table-card {
  border-radius: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.item-line {
  font-size: 13px;
  color: #606266;
  line-height: 1.6;
}

.user-info {
  line-height: 1.6;
}

.nickname {
  font-weight: 600;
  color: #303133;
}

.phone {
  font-size: 13px;
  color: #606266;
}

.empty-wrap {
  padding: 40px 0;
}

.empty-tip {
  margin: 8px 0 0;
  color: #909399;
  font-size: 13px;
}

.detail-section {
  margin-bottom: 20px;
}

.detail-section h4 {
  margin: 0 0 12px;
  color: #1f6a58;
  font-size: 15px;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 0;
  border-bottom: 1px solid #f0f0f0;
  font-size: 14px;
}

.detail-row span {
  color: #606266;
}

.detail-row strong {
  color: #303133;
  font-weight: 600;
}

.detail-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #f5f5f5;
}

.detail-item-img {
  width: 56px;
  height: 56px;
  border-radius: 6px;
  flex-shrink: 0;
}

.detail-item-img.placeholder {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  background: #edf4ef;
  color: #1f6a58;
  font-weight: 700;
}

.detail-item-info {
  flex: 1;
  min-width: 0;
}

.detail-item-name {
  color: #303133;
  font-weight: 600;
  margin-bottom: 4px;
}

.detail-item-meta {
  color: #909399;
  font-size: 13px;
}

.detail-item-subtotal {
  color: #c7672e;
  font-weight: 700;
  font-size: 15px;
}

.detail-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px;
  border-radius: 8px;
  background: #172521;
  color: #fff;
}

.detail-total strong {
  color: #ffd36f;
  font-size: 22px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
}
</style>
