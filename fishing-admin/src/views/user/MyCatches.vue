<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getMyCatchRecords, requestRecycle, type CatchRecord } from '@/api/catch'
import { formatDateTime } from '@/utils/date'

const router = useRouter()
const list = ref<CatchRecord[]>([])
const loading = ref(false)

const sortedList = computed(() => {
  return [...list.value].sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
})

const statusType = (status: string) => {
  const map: Record<string, string> = {
    pending: 'warning',
    sold_recycle: 'success',
    sold_restaurant: 'primary',
    released: 'info'
  }
  return map[status] || 'info'
}

const statusText = (status: string) => {
  const map: Record<string, string> = {
    pending: '待处理',
    sold_recycle: '已回收',
    sold_restaurant: '已售餐厅',
    released: '已放生'
  }
  return map[status] || status
}

const load = async () => {
  loading.value = true
  try {
    const res = await getMyCatchRecords()
    list.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
}

const handleRequestRecycle = async (row: CatchRecord) => {
  try {
    await ElMessageBox.confirm('确定申请回收该渔获吗？', '申请回收', { type: 'info' })
    await requestRecycle(row.id)
    ElMessage.success('申请已提交')
    load()
  } catch (e: any) {
    if (e !== 'cancel') ElMessage.error(e.message || '申请失败')
  }
}

const goAdd = () => {
  router.push('/my-catches/add')
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>我的渔获</h2>
      <el-button type="primary" icon="Plus" @click="goAdd">记录渔获</el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="sortedList" stripe>
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
        <el-table-column prop="fishType" label="鱼种" width="120" />
        <el-table-column label="重量" width="120">
          <template #default="{ row }">{{ row.weight }} kg</template>
        </el-table-column>
        <el-table-column prop="quantity" label="数量" width="100" />
        <el-table-column prop="pondName" label="鱼塘" width="140" />
        <el-table-column label="钓位" width="120">
          <template #default="{ row }">{{ row.spotCode || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="提交时间" width="180">
          <template #default="{ row }">{{ formatDateTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" min-width="160" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'pending'"
              type="warning"
              link
              size="small"
              @click="handleRequestRecycle(row)"
            >申请回收</el-button>
            <span v-else class="action-empty">-</span>
          </template>
        </el-table-column>
      </el-table>

      <el-empty v-if="!loading && sortedList.length === 0" description="暂无渔获记录" />
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

.action-empty {
  color: #c0c4cc;
}
</style>
