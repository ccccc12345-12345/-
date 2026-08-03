<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import QRCode from 'qrcode'
import { getMyReservations, type Reservation } from '@/api/reservation'
import { formatDate, formatTime } from '@/utils/date'

const reservations = ref<Reservation[]>([])
const loading = ref(false)
const qrCodeMap = ref<Record<number, string>>({})

const drawnList = computed(() => {
  return reservations.value
    .filter(r => r.status === '已抽号' && r.spotCode)
    .sort((a, b) => new Date(b.createTime).getTime() - new Date(a.createTime).getTime())
})

const generateQrCodes = async () => {
  const map: Record<number, string> = {}
  await Promise.all(
    drawnList.value.map(async (r) => {
      if (!r.checkinCode) return
      try {
        const dataUrl = await QRCode.toDataURL(r.checkinCode, {
          width: 180,
          margin: 2,
          errorCorrectionLevel: 'M'
        })
        map[r.id] = dataUrl
      } catch (e) {
        console.error('二维码生成失败', e)
      }
    })
  )
  qrCodeMap.value = map
}

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMyReservations()
    reservations.value = res.data.records
  } catch (e: any) {
    ElMessage.error(e.message || '加载失败')
  } finally {
    loading.value = false
  }
  await generateQrCodes()
}

const printCard = (id: string) => {
  const el = document.getElementById(id)
  if (!el) return
  const printWindow = window.open('', '_blank')
  if (printWindow) {
    printWindow.document.write(`<html><head><title>钓位凭证</title></head><body>${el.innerHTML}</body></html>`)
    printWindow.document.close()
    printWindow.print()
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container">
    <div class="page-header"><h2>我的钓位</h2></div>

    <el-row v-loading="loading" :gutter="16">
      <el-col v-for="r in drawnList" :key="r.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card class="spot-card" :id="`spot-card-${r.id}`">
          <div class="spot-header">
            <span>到场凭证</span>
            <el-tag type="success">已抽号</el-tag>
          </div>
          <div class="spot-body">
            <div class="spot-code">{{ r.spotCode }}</div>
            <div class="spot-info">
              <p><strong>日期：</strong>{{ r.slotDate ? formatDate(r.slotDate) : '-' }}</p>
              <p><strong>时段：</strong>{{ r.slotName || '-' }}</p>
              <p><strong>时间：</strong>{{ r.startTime && r.endTime ? formatTime(r.startTime) + ' - ' + formatTime(r.endTime) : '-' }}</p>
              <p><strong>鱼塘：</strong>{{ r.pondName || '-' }}</p>
            </div>
            <div class="checkin-section">
              <div class="checkin-label">核销码</div>
              <div v-if="r.checkinCode" class="checkin-code">{{ r.checkinCode }}</div>
              <div v-else class="checkin-empty">暂无核销码</div>
              <div class="checkin-tip">请出示此码给工作人员核销</div>
              <img
                v-if="qrCodeMap[r.id]"
                :src="qrCodeMap[r.id]"
                alt="核销二维码"
                class="qr-code"
              />
            </div>
          </div>
          <div class="spot-footer">
            <el-button type="primary" icon="Printer" size="small" @click="printCard(`spot-card-${r.id}`)">打印凭证</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <el-empty v-if="!loading && drawnList.length === 0" description="暂无钓位凭证，请先预约并抽号" />
  </div>
</template>

<style scoped>
.page-container {
  padding-bottom: 20px;
}

.page-header {
  margin-bottom: 20px;
}

.page-header h2 {
  color: #0f4c75;
}

.spot-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.spot-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
  font-weight: 600;
  color: #0f4c75;
}

.spot-body {
  text-align: center;
  margin-bottom: 16px;
}

.spot-code {
  font-size: 48px;
  font-weight: 800;
  color: #f9a825;
  margin-bottom: 12px;
}

.spot-info {
  text-align: left;
  color: #606266;
  line-height: 1.8;
  margin-bottom: 16px;
}

.checkin-section {
  background: #f5f7fa;
  border-radius: 12px;
  padding: 16px;
  text-align: center;
}

.checkin-label {
  font-size: 14px;
  color: #606266;
  margin-bottom: 6px;
}

.checkin-code {
  font-size: 28px;
  font-weight: 700;
  color: #0f4c75;
  letter-spacing: 4px;
  margin-bottom: 6px;
}

.checkin-empty {
  font-size: 16px;
  color: #909399;
  margin-bottom: 6px;
}

.checkin-tip {
  font-size: 13px;
  color: #909399;
  margin-bottom: 12px;
}

.qr-code {
  width: 180px;
  height: 180px;
  border-radius: 8px;
}

.spot-footer {
  text-align: center;
}
</style>
