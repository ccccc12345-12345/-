<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { ArrowRight } from '@element-plus/icons-vue'
import request from '@/utils/request'

interface Pond {
  id: number
  name: string
  address?: string
  status?: number
}

const router = useRouter()
const ponds = ref<Pond[]>([])
const loading = ref(false)

const loadPonds = async () => {
  loading.value = true
  try {
    const res = await request.get<any, { data: Pond[] }>('/api/ponds')
    ponds.value = (res.data || []).filter((p: Pond) => p.status !== 0)
  } catch (e: any) {
    ElMessage.error(e.message || '加载鱼塘失败')
  } finally {
    loading.value = false
  }
}

const goRestaurant = (pondId: number) => {
  router.push(`/restaurant/${pondId}`)
}

onMounted(loadPonds)
</script>

<template>
  <div class="restaurant-list">
    <div class="page-header">
      <h2>选择鱼塘餐厅</h2>
    </div>

    <div v-loading="loading" class="pond-list">
      <div
        v-for="pond in ponds"
        :key="pond.id"
        class="pond-card"
        @click="goRestaurant(pond.id)"
      >
        <div class="pond-info">
          <h3>{{ pond.name }}</h3>
          <p v-if="pond.address" class="address">{{ pond.address }}</p>
        </div>
        <el-icon><ArrowRight /></el-icon>
      </div>
      <el-empty v-if="!loading && ponds.length === 0" description="暂无营业中的鱼塘" />
    </div>
  </div>
</template>

<style scoped>
.restaurant-list {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
}

.page-header {
  margin-bottom: 16px;
}

.page-header h2 {
  margin: 0;
  color: #0f4c75;
  font-size: 20px;
}

.pond-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.pond-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  border-radius: 12px;
  padding: 16px 20px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.pond-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.pond-info h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #303133;
}

.address {
  margin: 0;
  font-size: 13px;
  color: #909399;
}
</style>
