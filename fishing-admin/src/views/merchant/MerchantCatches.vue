<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPendingCatchRecords, confirmRecycle, type CatchRecord } from '@/api/catch'
import { useMerchantPush } from '@/composables/useMerchantPush'
import { formatDateTime } from '@/utils/date'

const list = ref<CatchRecord[]>([])
const loading = ref(false)
const total = ref(0)
const query = reactive({
  pageNum: 1,
  pageSize: 20
})
const recyclePrices = ref<Record<number, number | undefined>>({})
const confirmingIds = ref<Set<number>>(new Set())

const load = async () => {
  loading.value = true
  try {
    const res = await getPendingCatchRecords({ pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = res.data.records || []
    total.value = res.data.total || 0
    recyclePrices.value = {}
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleConfirm = async (row: CatchRecord) => {
  const price = recyclePrices.value[row.id]
  if (price == null || price <= 0) {
    ElMessage.warning('请输入有效的回收价格')
    return
  }
  try {
    await ElMessageBox.confirm(`确认以 ${price} 分的价格回收该渔获吗？`, '确认回收', { type: 'warning' })
    confirmingIds.value.add(row.id)
    await confirmRecycle(row.id, price)
    ElMessage.success('回收确认成功')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '确认失败')
  } finally {
    confirmingIds.value.delete(row.id)
  }
}

  useMerchantPush({
    events: ['CATCH_CREATED'],
    onEvent: () => load(),
    fallback: () => load()
  })

  onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>渔获回收</h2>
    </div>

    <el-card v-loading="loading">
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
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="回收价格（分）" width="160" fixed="right">
          <template #default="{ row }">
            <el-input-number
              v-model="recyclePrices[row.id]"
              :min="1"
              :precision="0"
              placeholder="价格"
              style="width: 120px"
              size="small"
            />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button
              type="success"
              size="small"
              :loading="confirmingIds.has(row.id)"
              @click="handleConfirm(row)"
            >确认回收</el-button>
          </template>
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

      <el-empty v-if="!loading && list.length === 0" description="暂无待处理渔获" />
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
