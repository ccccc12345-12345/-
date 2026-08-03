<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Money, Food, CircleCheck, Refresh } from '@element-plus/icons-vue'
import {
  getMerchantCatches,
  updateMerchantCatchStatus,
  getMerchantPonds,
  type CatchRecord
} from '@/api/merchant'
import type { Pond } from '@/api/pond'
import { formatDateTime } from '@/utils/date'
import { useMerchantPush } from '@/composables/useMerchantPush'

const catchStatusLabels: Record<string, string> = {
  pending: '待处理',
  recycle_requested: '已申请回收',
  sold_recycle: '已回收',
  sold_restaurant: '已入餐厅',
  released: '已放生'
}

const catchStatusTagType: Record<string, string> = {
  pending: 'danger',
  recycle_requested: 'warning',
  sold_recycle: 'success',
  sold_restaurant: '',
  released: 'info'
}

const list = ref<CatchRecord[]>([])
const loading = ref(false)
const total = ref(0)
const ponds = ref<Pond[]>([])

const query = reactive({
  pondId: undefined as number | undefined,
  status: 'recycle_requested' as string,
  pageNum: 1,
  pageSize: 20
})

const recyclePrices = ref<Record<number, number | undefined>>({})
const confirmingIds = ref<Set<number>>(new Set())
const pendingCount = computed(() => list.value.filter((item) => item.status === 'recycle_requested').length)

const loadPonds = async () => {
  try {
    const res = await getMerchantPonds()
    ponds.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
  } catch {
    // ignore
  }
}

const load = async () => {
  loading.value = true
  try {
    const res = await getMerchantCatches({
      pondId: query.pondId,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    total.value = res.data?.total || 0
    recyclePrices.value = {}
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  query.pageNum = 1
  load()
}

const onPageChange = (page: number) => {
  query.pageNum = page
  load()
}

const handleRefresh = () => {
  load()
  ElMessage.success('已刷新')
}

const handleConfirm = async (row: CatchRecord) => {
  const price = recyclePrices.value[row.id]
  if (price == null || price <= 0) {
    ElMessage.warning('请输入有效的回收价格')
    return
  }
  try {
    await ElMessageBox.confirm(`确认以 ¥${price} 的价格回收该渔获吗？`, '确认回收', { type: 'warning' })
    confirmingIds.value.add(row.id)
    await updateMerchantCatchStatus(row.id, { status: 'sold_recycle', recyclePrice: Math.round(price * 100) })
    ElMessage.success('回收确认成功')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '确认失败')
  } finally {
    confirmingIds.value.delete(row.id)
  }
}

const setReleased = async (row: CatchRecord) => {
  try {
    await ElMessageBox.confirm(`确认将 ${row.fishType} 标记为已放生？`, '确认放生', { type: 'warning' })
    confirmingIds.value.add(row.id)
    await updateMerchantCatchStatus(row.id, { status: 'released' })
    ElMessage.success('已标记为放生')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  } finally {
    confirmingIds.value.delete(row.id)
  }
}

const setRestaurant = async (row: CatchRecord) => {
  try {
    await ElMessageBox.confirm(`确认将 ${row.fishType} 入餐厅？`, '确认入餐厅', { type: 'warning' })
    confirmingIds.value.add(row.id)
    await updateMerchantCatchStatus(row.id, { status: 'sold_restaurant' })
    ElMessage.success('已入餐厅')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '操作失败')
  } finally {
    confirmingIds.value.delete(row.id)
  }
}

useMerchantPush({
  events: ['CATCH_CREATED'],
  onEvent: () => load(),
  fallback: () => load()
})

onMounted(async () => {
  await loadPonds()
  if (!query.pondId && ponds.value.length > 0) {
    query.pondId = ponds.value[0].id
  }
  load()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>渔获管理</h2>
      <el-button plain :icon="Refresh" @click="handleRefresh" :loading="loading">刷新</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="鱼塘">
          <el-select v-model="query.pondId" placeholder="全部鱼塘" clearable style="width: 160px">
            <el-option
              v-for="pond in ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部状态" clearable style="width: 140px">
            <el-option
              v-for="(label, key) in catchStatusLabels"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-alert
      v-if="pendingCount > 0"
      :title="`当前有 ${pendingCount} 条待回收的渔获订单`"
      type="success"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />
    <el-alert
      v-else-if="!loading && query.status === 'recycle_requested'"
      title="暂无待回收的渔获订单，用户提交渔获后需先点击“申请回收”才会出现在此处"
      type="info"
      :closable="false"
      show-icon
      style="margin-bottom: 16px"
    />

    <el-card v-loading="loading" shadow="never" class="table-card">
      <el-table :data="list" stripe>
        <el-table-column type="index" label="序号" width="70" />
        <el-table-column label="照片" width="120">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              :preview-src-list="[row.imageUrl]"
              fit="cover"
              style="width: 80px; height: 60px; border-radius: 8px"
            />
            <span v-else class="no-image">暂无照片</span>
          </template>
        </el-table-column>
        <el-table-column label="用户信息" min-width="160">
          <template #default="{ row }">
            <div class="user-info">
              <div class="nickname">{{ row.userNickname || '-' }}</div>
              <div class="phone">{{ row.userPhone || '-' }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="pondName" label="鱼塘" width="140" />
        <el-table-column label="钓位" width="100">
          <template #default="{ row }">{{ row.spotCode || '-' }}</template>
        </el-table-column>
        <el-table-column prop="fishType" label="鱼种" width="120" />
        <el-table-column label="重量" width="100">
          <template #default="{ row }">{{ row.weight }} kg</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="80" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="catchStatusTagType[row.status] || 'info'">
              {{ catchStatusLabels[row.status] || row.status }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="回收价格（元）" width="160" fixed="right">
          <template #default="{ row }">
            <el-input-number
              v-if="row.status === 'pending' || row.status === 'recycle_requested'"
              v-model="recyclePrices[row.id]"
              :min="0.01"
              :step="0.5"
              :precision="2"
              placeholder="价格"
              style="width: 130px"
              size="small"
            />
            <span v-else>{{ row.recyclePrice != null ? `¥${row.recyclePrice}` : '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending' || row.status === 'recycle_requested'"
              type="success"
              size="small"
              :icon="Money"
              :loading="confirmingIds.has(row.id)"
              @click="handleConfirm(row)"
            >回收</el-button>
            <el-button
              v-if="row.status === 'pending' || row.status === 'recycle_requested'"
              type="warning"
              size="small"
              :icon="Food"
              :loading="confirmingIds.has(row.id)"
              @click="setRestaurant(row)"
            >入餐厅</el-button>
            <el-button
              v-if="row.status === 'pending' || row.status === 'recycle_requested'"
              type="info"
              size="small"
              :icon="CircleCheck"
              :loading="confirmingIds.has(row.id)"
              @click="setReleased(row)"
            >放生</el-button>
            <span v-else>-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
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

.header-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.filter-form {
  margin-bottom: -18px;
}

.table-card {
  border-radius: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.no-image {
  color: #909399;
  font-size: 13px;
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
</style>
