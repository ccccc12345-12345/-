<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelReservation, getMyReservations, type ReservationVO } from '@/api/reservation'

const router = useRouter()
const loading = ref(false)
const error = ref('')
const reservations = ref<ReservationVO[]>([])
const status = ref('')

const filtered = computed(() => (status.value ? reservations.value.filter((item) => item.status === status.value) : reservations.value))

const statusType = (value: string) => {
  if (value.includes('取消') || value.includes('失效')) return 'info'
  if (value.includes('核销')) return 'success'
  if (value.includes('抽') || value.includes('分配')) return 'warning'
  return 'primary'
}

const canCancel = (item: ReservationVO) => item.status.includes('待') || item.status.includes('抽')

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getMyReservations()
    reservations.value = res.data || []
  } catch (e: any) {
    error.value = e?.message || '预约数据加载失败'
  } finally {
    loading.value = false
  }
}

const cancelItem = async (item: ReservationVO) => {
  await ElMessageBox.confirm(`确认取消 ${item.slotDate} ${item.slotName} 的预约吗？`, '取消预约', { type: 'warning' })
  await cancelReservation(item.id)
  ElMessage.success('预约已取消')
  await loadData()
}

onMounted(loadData)
</script>

<template>
  <section class="page">
    <div class="page-head">
      <div>
        <p class="eyebrow">我的预约</p>
        <h1>预约记录与钓位凭证</h1>
      </div>
      <div class="actions">
        <el-select v-model="status" clearable placeholder="全部状态" style="width: 160px">
          <el-option v-for="item in [...new Set(reservations.map((r) => r.status))]" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button @click="loadData">刷新</el-button>
        <el-button type="primary" @click="router.push('/user/booking')">继续预约</el-button>
      </div>
    </div>

    <el-alert v-if="error" :title="error" type="error" show-icon :closable="false">
      <template #default>
        <el-button size="small" @click="loadData">重试</el-button>
      </template>
    </el-alert>

    <el-skeleton v-if="loading" :rows="5" animated />
    <el-empty v-else-if="filtered.length === 0" description="暂无预约记录">
      <el-button type="primary" @click="router.push('/user/booking')">去预约场次</el-button>
    </el-empty>

    <div v-else class="list">
      <article v-for="item in filtered" :key="item.id" class="card">
        <div class="main-info">
          <div>
            <h3>{{ item.pondName || '鱼塘' }} · {{ item.slotName }}</h3>
            <p>{{ item.slotDate }} {{ item.startTime }} - {{ item.endTime }}</p>
          </div>
          <el-tag :type="statusType(item.status)">{{ item.status }}</el-tag>
        </div>
        <div class="voucher">
          <span>钓位：<b>{{ item.spotCode || '待分配' }}</b></span>
          <span>核销码：<b>{{ item.checkinCode || '-' }}</b></span>
          <span>创建：{{ item.createTime }}</span>
        </div>
        <div class="card-actions">
          <el-button type="primary" plain @click="router.push(`/user/reservations/${item.id}`)">查看详情</el-button>
          <el-button :disabled="!item.spotCode" @click="router.push({ path: '/restaurant', query: { pondId: item.pondId, reservationId: item.id, spotId: item.spotId } })">去餐厅点餐</el-button>
          <el-button :disabled="!item.spotCode" @click="router.push({ path: '/user/catches', query: { reservationId: item.id } })">提交渔获</el-button>
          <el-button v-if="canCancel(item)" type="danger" plain @click="cancelItem(item)">取消预约</el-button>
        </div>
      </article>
    </div>
  </section>
</template>

<style scoped>
.page {
  width: min(1180px, calc(100% - 32px));
  margin: 22px auto 48px;
}

.page-head {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: flex-end;
  margin-bottom: 16px;
}

.eyebrow {
  margin: 0 0 6px;
  color: #1f6a58;
  font-weight: 900;
}

h1 {
  margin: 0;
  color: #172521;
}

.actions,
.card-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.list {
  display: grid;
  gap: 14px;
}

.card {
  padding: 18px;
  border: 1px solid #e1ebe5;
  border-radius: 8px;
  background: white;
  box-shadow: 0 14px 30px rgba(21, 60, 53, 0.08);
}

.main-info,
.voucher {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.main-info h3 {
  margin: 0 0 6px;
  color: #172521;
}

.main-info p {
  margin: 0;
  color: #66766f;
}

.voucher {
  margin: 14px 0;
  padding: 12px;
  border-radius: 8px;
  background: #f4f8f5;
}

.voucher b {
  color: #174c41;
}

@media (max-width: 720px) {
  .page-head {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
