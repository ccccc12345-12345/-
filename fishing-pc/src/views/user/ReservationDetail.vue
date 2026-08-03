<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { cancelReservation, getMyReservations, getReservationDetail, type ReservationVO } from '@/api/reservation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const error = ref('')
const detail = ref<ReservationVO | null>(null)

const id = Number(route.params.id)

const loadDetail = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getReservationDetail(id)
    detail.value = res.data
  } catch {
    const res = await getMyReservations()
    detail.value = (res.data || []).find((item) => item.id === id) || null
    if (!detail.value) error.value = '预约详情加载失败，请返回列表重试'
  } finally {
    loading.value = false
  }
}

const doCancel = async () => {
  if (!detail.value) return
  await ElMessageBox.confirm('确认取消该预约吗？取消后已分配钓位会释放。', '取消预约', { type: 'warning' })
  await cancelReservation(detail.value.id)
  ElMessage.success('已取消预约')
  await loadDetail()
}

const goRestaurant = () => {
  if (!detail.value) return
  router.push({ path: '/restaurant', query: { pondId: detail.value.pondId, reservationId: detail.value.id, spotId: detail.value.spotId } })
}

const goCatch = () => {
  if (!detail.value) return
  router.push({ path: '/user/catches', query: { reservationId: detail.value.id } })
}

onMounted(loadDetail)
</script>

<template>
  <section class="page">
    <el-skeleton v-if="loading" :rows="8" animated />
    <el-alert v-else-if="error" :title="error" type="error" show-icon :closable="false">
      <template #default>
        <el-button size="small" @click="loadDetail">重试</el-button>
      </template>
    </el-alert>

    <template v-else-if="detail">
      <div class="hero">
        <div>
          <p class="eyebrow">预约详情</p>
          <h1>{{ detail.pondName || '鱼塘' }} · {{ detail.slotName }}</h1>
          <p>{{ detail.slotDate }} {{ detail.startTime }} - {{ detail.endTime }}</p>
        </div>
        <el-tag size="large">{{ detail.status }}</el-tag>
      </div>

      <div class="grid">
        <div class="voucher">
          <span>钓位号</span>
          <strong>{{ detail.spotCode || '待分配' }}</strong>
          <small>到场后向工作人员出示该凭证核销</small>
        </div>
        <div class="voucher">
          <span>核销码</span>
          <strong>{{ detail.checkinCode || '-' }}</strong>
          <small>餐厅点单和渔获回收可绑定此预约</small>
        </div>
      </div>

      <el-descriptions class="detail-card" :column="2" border>
        <el-descriptions-item label="预约编号">{{ detail.id }}</el-descriptions-item>
        <el-descriptions-item label="鱼塘">{{ detail.pondName || detail.pondId }}</el-descriptions-item>
        <el-descriptions-item label="状态">{{ detail.status }}</el-descriptions-item>
        <el-descriptions-item label="创建时间">{{ detail.createTime }}</el-descriptions-item>
        <el-descriptions-item label="抽号开放">{{ detail.drawStartTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="抽号截止">{{ detail.drawEndTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取消时间">{{ detail.cancelTime || '-' }}</el-descriptions-item>
        <el-descriptions-item label="取消原因">{{ detail.cancelReason || '-' }}</el-descriptions-item>
      </el-descriptions>

      <div class="actions">
        <el-button @click="router.push('/user/reservations')">返回列表</el-button>
        <el-button type="primary" :disabled="!detail.spotCode" @click="goRestaurant">去餐厅点餐</el-button>
        <el-button type="success" :disabled="!detail.spotCode" @click="goCatch">提交渔获</el-button>
        <el-button type="danger" plain :disabled="!(detail.status.includes('待') || detail.status.includes('抽'))" @click="doCancel">取消预约</el-button>
      </div>
    </template>
  </section>
</template>

<style scoped>
.page {
  width: min(980px, calc(100% - 32px));
  margin: 22px auto 48px;
}

.hero {
  display: flex;
  justify-content: space-between;
  gap: 18px;
  align-items: center;
  padding: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, #153c35, #24745f);
  color: white;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f8c966;
  font-weight: 900;
}

.hero h1 {
  margin: 0;
  font-size: 30px;
}

.hero p {
  margin: 8px 0 0;
  color: #dbe9e3;
}

.grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
  margin: 18px 0;
}

.voucher {
  display: grid;
  gap: 8px;
  padding: 22px;
  border: 1px solid #e1ebe5;
  border-radius: 8px;
  background: white;
  box-shadow: 0 14px 30px rgba(21, 60, 53, 0.08);
}

.voucher span {
  color: #66766f;
  font-weight: 800;
}

.voucher strong {
  font-size: 34px;
  color: #174c41;
}

.voucher small {
  color: #7b8983;
}

.detail-card {
  background: white;
}

.actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: 18px;
}

@media (max-width: 720px) {
  .hero,
  .grid {
    grid-template-columns: 1fr;
  }

  .hero {
    align-items: flex-start;
    flex-direction: column;
  }
}
</style>
