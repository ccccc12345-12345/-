<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import html2canvas from 'html2canvas'
import { getShareBoard, type ShareBoardData, type BoardSpot } from '@/api/merchant'

const route = useRoute()
const loading = ref(false)
const boardData = ref<ShareBoardData | null>(null)
const errorMsg = ref('')
const boardRef = ref<HTMLElement | null>(null)

const pondId = computed(() => Number(route.query.pondId))
const slotId = computed(() => Number(route.query.slotId))
const token = computed(() => String(route.query.token || ''))

const pageTitle = computed(() => {
  if (!boardData.value) return '钓位分享看板'
  const { pondName, slotDate, slotName } = boardData.value
  return `${pondName || ''} ${slotDate || ''} ${slotName || ''}`.trim() || '钓位分享看板'
})

const spotClass = (spot: BoardSpot) => {
  if (spot.status === '维修中') return 'spot-disabled'
  if (spot.status === '已核销') return 'spot-checked-in'
  if (spot.status === '已预约') return 'spot-booked'
  return 'spot-free'
}

const loadBoard = async () => {
  if (!pondId.value || !slotId.value || !token.value) {
    errorMsg.value = '分享链接参数不完整'
    return
  }
  loading.value = true
  errorMsg.value = ''
  try {
    const res = await getShareBoard({
      pondId: pondId.value,
      slotId: slotId.value,
      token: token.value
    })
    boardData.value = res.data
    document.title = pageTitle.value
  } catch (e: any) {
    errorMsg.value = e?.response?.data?.message || '分享链接已过期或无效'
    ElMessage.error(errorMsg.value)
  } finally {
    loading.value = false
  }
}

const generatePoster = async () => {
  if (!boardRef.value) return
  try {
    const canvas = await html2canvas(boardRef.value, { scale: 2, backgroundColor: '#ffffff' })
    const link = document.createElement('a')
    link.download = `钓位看板-${Date.now()}.png`
    link.href = canvas.toDataURL('image/png')
    link.click()
    ElMessage.success('海报已生成')
  } catch (e) {
    ElMessage.error('生成海报失败')
  }
}

onMounted(loadBoard)
</script>

<template>
  <div class="share-page">
    <div v-if="loading" class="loading">加载中...</div>
    <div v-else-if="errorMsg" class="error">{{ errorMsg }}</div>
    <template v-else-if="boardData">
      <div ref="boardRef" class="board-card">
        <header class="board-header">
          <h1 class="board-title">{{ pageTitle }}</h1>
          <p class="board-subtitle">钓位状态看板（实时分享）</p>
        </header>

        <div class="legend">
          <span class="legend-item"><i class="dot spot-free"></i> 空闲</span>
          <span class="legend-item"><i class="dot spot-booked"></i> 已预约</span>
          <span class="legend-item"><i class="dot spot-checked-in"></i> 已核销</span>
          <span class="legend-item"><i class="dot spot-disabled"></i> 维修中</span>
        </div>

        <div class="spot-grid">
          <div
            v-for="spot in boardData.spots"
            :key="spot.spotId"
            class="spot-cell"
            :class="spotClass(spot)"
          >
            <div class="spot-code">{{ spot.spotCode }}</div>
            <div class="spot-status">{{ spot.status }}</div>
            <div v-if="spot.userNickname" class="spot-user">
              {{ spot.userNickname }}
              <span v-if="spot.userPhone">({{ spot.userPhone }})</span>
            </div>
          </div>
        </div>
      </div>

      <div class="actions">
        <el-button type="primary" size="large" @click="generatePoster">
          生成海报
        </el-button>
      </div>
    </template>
  </div>
</template>

<style scoped>
.share-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding: 16px;
}

.loading,
.error {
  text-align: center;
  padding: 60px 0;
  color: #606266;
  font-size: 16px;
}

.error {
  color: #f56c6c;
}

.board-card {
  max-width: 960px;
  margin: 0 auto;
  background: #fff;
  border-radius: 12px;
  padding: 24px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.board-header {
  text-align: center;
  margin-bottom: 20px;
}

.board-title {
  font-size: 22px;
  font-weight: 700;
  color: #0f4c75;
  margin: 0 0 8px;
}

.board-subtitle {
  font-size: 14px;
  color: #909399;
  margin: 0;
}

.legend {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-bottom: 20px;
  flex-wrap: wrap;
}

.legend-item {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #606266;
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  display: inline-block;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(100px, 1fr));
  gap: 12px;
}

.spot-cell {
  border-radius: 8px;
  padding: 12px 8px;
  text-align: center;
  color: #fff;
  transition: transform 0.15s;
}

.spot-cell:hover {
  transform: translateY(-2px);
}

.spot-code {
  font-size: 18px;
  font-weight: 700;
  margin-bottom: 4px;
}

.spot-status {
  font-size: 12px;
  opacity: 0.95;
}

.spot-user {
  font-size: 11px;
  margin-top: 6px;
  opacity: 0.95;
  word-break: break-all;
}

.spot-free {
  background: #67c23a;
}

.spot-booked {
  background: #e6a23c;
}

.spot-checked-in {
  background: #409eff;
}

.spot-disabled {
  background: #f56c6c;
}

.actions {
  max-width: 960px;
  margin: 20px auto 0;
  text-align: center;
}

@media (max-width: 768px) {
  .share-page {
    padding: 8px;
  }

  .board-card {
    padding: 16px;
  }

  .board-title {
    font-size: 18px;
  }

  .spot-grid {
    grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
    gap: 8px;
  }

  .spot-cell {
    padding: 10px 4px;
  }

  .spot-code {
    font-size: 15px;
  }

  .spot-status,
  .spot-user {
    font-size: 10px;
  }
}
</style>
