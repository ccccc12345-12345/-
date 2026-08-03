<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Aim, Bowl, Printer, Tickets, View } from '@element-plus/icons-vue'
import { getMyReservations, type ReservationVO } from '@/api/reservation'
import { formatDate, formatTime } from '@/utils/date'

const router = useRouter()

const reservations = ref<ReservationVO[]>([])
const loading = ref(false)
const error = ref('')

const assignedList = computed(() => {
  return reservations.value
    .filter((item) => item.spotCode)
    .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
})

const loadData = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getMyReservations()
    reservations.value = Array.isArray(res.data) ? res.data : (res.data as any)?.records || []
  } catch (err: any) {
    reservations.value = []
    error.value = err?.message || '钓位凭证加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const printCard = (id: string) => {
  const el = document.getElementById(id)
  if (!el) return
  const printWindow = window.open('', '_blank')
  if (!printWindow) return
  printWindow.document.write(`
    <html>
      <head>
        <title>钓位凭证</title>
        <style>
          body { font-family: 'Microsoft YaHei', sans-serif; padding: 40px; color: #172521; }
          .card { border: 2px solid #1f6a58; border-radius: 8px; padding: 32px; width: 420px; margin: 0 auto; text-align: center; }
          .spot-code { font-size: 64px; font-weight: 900; color: #d3983f; margin: 16px 0; }
          .spot-info { text-align: left; color: #4e6159; line-height: 2; font-size: 16px; }
          .spot-header, .spot-footer { display: none; }
        </style>
      </head>
      <body><div class="card">${el.innerHTML}</div></body>
    </html>
  `)
  printWindow.document.close()
  printWindow.print()
}

onMounted(loadData)
</script>

<template>
  <div class="spots-page">
    <section class="page-heading">
      <div>
        <p class="eyebrow">Spot Pass</p>
        <h1>我的钓位</h1>
        <p class="summary">预约成功后系统会直接分配钓位并生成到场凭证，刷新页面后仍从后端读取。</p>
      </div>
      <el-button type="primary" :icon="Tickets" @click="router.push('/user/reservations')">查看预约</el-button>
    </section>

    <el-alert v-if="error" class="error-alert" type="error" :title="error" show-icon :closable="false">
      <template #default>
        <el-button size="small" type="danger" plain @click="loadData">重试</el-button>
      </template>
    </el-alert>

    <section v-loading="loading" class="spot-grid">
      <article v-for="item in assignedList" :id="`spot-card-${item.id}`" :key="item.id" class="spot-card">
        <div class="spot-header">
          <span>{{ item.pondName || '钓位凭证' }}</span>
          <el-tag type="success" effect="light">已分配</el-tag>
        </div>

        <div class="spot-code">{{ item.spotCode }}</div>

        <div class="spot-info">
          <p><strong>日期</strong><span>{{ formatDate(item.slotDate) }}</span></p>
          <p><strong>场次</strong><span>{{ item.slotName }}</span></p>
          <p><strong>时间</strong><span>{{ formatTime(item.startTime) }} - {{ formatTime(item.endTime) }}</span></p>
          <p><strong>核销码</strong><span>{{ item.checkinCode || '-' }}</span></p>
        </div>

        <div class="spot-footer">
          <el-button type="primary" plain :icon="View" @click="router.push(`/user/reservations/${item.id}`)">查看详情</el-button>
          <el-button type="success" plain :icon="Bowl" @click="router.push({ path: '/restaurant', query: { pondId: item.pondId, reservationId: item.id } })">去点餐</el-button>
          <el-button plain :icon="Aim" @click="router.push({ path: '/user/catches', query: { reservationId: item.id } })">提交渔获</el-button>
          <el-button plain :icon="Printer" @click="printCard(`spot-card-${item.id}`)">打印</el-button>
        </div>
      </article>

      <div v-if="!loading && !error && assignedList.length === 0" class="empty-panel">
        <el-empty description="暂无钓位凭证">
          <el-button type="primary" plain :icon="Tickets" @click="router.push('/user/booking')">去预约钓位</el-button>
        </el-empty>
      </div>
    </section>
  </div>
</template>

<style scoped>
.spots-page {
  width: min(1160px, calc(100% - 32px));
  margin: 0 auto;
  padding: 28px 0 42px;
}

.page-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 22px;
  margin-bottom: 18px;
}

.eyebrow {
  margin: 0 0 7px;
  color: #b5742a;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.page-heading h1 {
  margin: 0;
  color: #172521;
  font-size: 32px;
  line-height: 1.18;
  font-weight: 900;
}

.summary {
  margin: 10px 0 0;
  color: #687a73;
  font-size: 15px;
  line-height: 1.7;
}

.error-alert {
  margin-bottom: 16px;
}

.spot-grid {
  min-height: 360px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.spot-card,
.empty-panel {
  border-radius: 8px;
  background: #ffffff;
  border: 1px solid #e1eae5;
  box-shadow: 0 12px 30px rgba(25, 47, 39, 0.07);
}

.spot-card {
  min-height: 320px;
  display: flex;
  flex-direction: column;
  padding: 18px;
}

.spot-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  color: #172521;
  font-weight: 900;
}

.spot-code {
  margin: 24px 0 20px;
  padding: 18px;
  border-radius: 8px;
  background: #fff8e8;
  color: #d3983f;
  font-size: 56px;
  line-height: 1;
  font-weight: 900;
  text-align: center;
}

.spot-info {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.spot-info p {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 0;
  color: #5f7069;
  font-size: 14px;
}

.spot-info strong {
  flex-shrink: 0;
  color: #172521;
  font-weight: 900;
}

.spot-info span {
  min-width: 0;
  text-align: right;
}

.spot-footer {
  margin-top: auto;
  padding-top: 18px;
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.empty-panel {
  grid-column: 1 / -1;
  min-height: 300px;
  display: flex;
  align-items: center;
  justify-content: center;
}

@media (max-width: 980px) {
  .page-heading {
    flex-direction: column;
    align-items: flex-start;
  }

  .spot-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .spots-page {
    width: calc(100% - 20px);
    padding: 16px 0 30px;
  }

  .spot-grid {
    grid-template-columns: 1fr;
  }
}
</style>
