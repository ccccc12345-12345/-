<script setup lang="ts">
import { ref, reactive, onMounted, onUnmounted, watch, computed } from 'vue'
import { ElMessage } from 'element-plus'
import { getMerchantBoard, getMerchantTimeSlots, createShareLink, type BoardSpot, type TimeSlot } from '@/api/merchant'
import { useMerchantPush } from '@/composables/useMerchantPush'
import { usePondStore } from '@/store/pond'
import { formatDate } from '@/utils/date'

const pondStore = usePondStore()

const spots = ref<BoardSpot[]>([])
const slots = ref<TimeSlot[]>([])
const loading = ref(false)
const selectedSpot = ref<BoardSpot | null>(null)
const detailVisible = ref(false)
const shareLoading = ref(false)

const query = reactive({
  slotDate: formatDate(new Date()),
  slotId: undefined as number | undefined
})

// 后端返回的状态为字符串，使用字符串 key 匹配颜色
const statusMap: Record<string, { label: string; color: string }> = {
  '维修中': { label: '维修', color: '#f56c6c' },
  '空闲': { label: '空闲', color: '#67c23a' },
  '已预约': { label: '已预约', color: '#e6a23c' },
  '已核销': { label: '已核销', color: '#409eff' }
}

const sortedSpots = computed(() => {
  return [...spots.value].sort((a, b) => a.spotCode.localeCompare(b.spotCode, undefined, { numeric: true }))
})

const loadSlots = async () => {
  if (!pondStore.currentPondId) return
  try {
    const res = await getMerchantTimeSlots({ slotDate: query.slotDate, pageSize: 100 })
    slots.value = (res.data.records || []).filter(s => s.pondId === pondStore.currentPondId)
    if (slots.value.length > 0 && !slots.value.find(s => s.id === query.slotId)) {
      query.slotId = slots.value[0].id
    }
  } catch {}
}

const loadBoard = async () => {
  if (!pondStore.currentPondId || !query.slotId) return
  loading.value = true
  try {
    const res = await getMerchantBoard({
      pondId: pondStore.currentPondId,
      slotDate: query.slotDate,
      slotId: query.slotId
    })
    spots.value = res.data || []
  } catch (e: any) {
    ElMessage.error(e.message || '加载看板失败')
  } finally {
    loading.value = false
  }
}

let timer: ReturnType<typeof setInterval> | null = null
const startAutoRefresh = () => {
  stopAutoRefresh()
  timer = setInterval(loadBoard, 5000)
}
const stopAutoRefresh = () => {
  if (timer) {
    clearInterval(timer)
    timer = null
  }
}

const openDetail = (spot: BoardSpot) => {
  selectedSpot.value = spot
  detailVisible.value = true
}

const shareLink = async () => {
  if (!pondStore.currentPondId || !query.slotId) return
  shareLoading.value = true
  try {
    const res = await createShareLink({ pondId: pondStore.currentPondId, slotId: query.slotId })
    // 后端已返回完整分享 URL（含 token，有效期 2 小时）
    const url = res.data
    await navigator.clipboard.writeText(url)
    ElMessage.success('分享链接已复制')
  } catch (e: any) {
    ElMessage.error(e.message || '生成分享链接失败')
  } finally {
    shareLoading.value = false
  }
}

onMounted(async () => {
  await loadSlots()
  loadBoard()
  startAutoRefresh()
})

onUnmounted(stopAutoRefresh)

watch(() => pondStore.currentPondId, async () => {
  await loadSlots()
  loadBoard()
})
watch(() => query.slotDate, async () => {
  await loadSlots()
  loadBoard()
})
watch(() => query.slotId, loadBoard)

useMerchantPush({
  events: ['SPOT_BOARD_CHANGED', 'RESERVATION_STATUS_CHANGED', 'RESERVATION_CHECKED_IN'],
  onEvent: () => loadBoard(),
  fallback: () => loadBoard()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>钓位看板</h2>
      <el-button type="primary" :loading="shareLoading" icon="Share" @click="shareLink">生成分享链接</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="鱼塘">
          <el-select v-model="pondStore.currentPondId" placeholder="选择鱼塘" style="width: 160px">
            <el-option
              v-for="pond in pondStore.ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="query.slotDate" type="date" value-format="YYYY-MM-DD" />
        </el-form-item>
        <el-form-item label="时段">
          <el-select v-model="query.slotId" placeholder="选择时段" clearable style="width: 160px">
            <el-option
              v-for="slot in slots"
              :key="slot.id"
              :label="`${slot.slotName} ${slot.startTime}-${slot.endTime}`"
              :value="slot.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" class="board-card">
      <div v-if="sortedSpots.length === 0" class="empty-wrap">
        <el-empty description="暂无钓位数据" />
      </div>
      <div v-else class="spot-grid">
        <div
          v-for="spot in sortedSpots"
          :key="spot.spotId"
          class="spot-item"
          :style="{ backgroundColor: statusMap[spot.status]?.color || '#909399' }"
          @click="openDetail(spot)"
        >
          <div class="spot-code">{{ spot.spotCode }}</div>
          <div class="spot-status">{{ statusMap[spot.status]?.label || '未知' }}</div>
        </div>
      </div>
    </el-card>

    <el-dialog v-model="detailVisible" title="钓位详情" width="360px">
      <div v-if="selectedSpot" class="detail-content">
        <div class="detail-row">
          <span>钓位编号</span>
          <strong>{{ selectedSpot.spotCode }}</strong>
        </div>
        <div class="detail-row">
          <span>当前状态</span>
          <el-tag :color="statusMap[selectedSpot.status]?.color" effect="dark">{{ statusMap[selectedSpot.status]?.label }}</el-tag>
        </div>
        <div v-if="selectedSpot.userNickname" class="detail-row">
          <span>预约用户</span>
          <strong>{{ selectedSpot.userNickname }}</strong>
        </div>
        <div v-if="selectedSpot.userPhone" class="detail-row">
          <span>联系电话</span>
          <strong>{{ selectedSpot.userPhone }}</strong>
        </div>
      </div>
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
  color: #0f4c75;
  margin: 0;
}

.filter-card {
  margin-bottom: 16px;
}

.board-card {
  min-height: 300px;
}

.empty-wrap {
  padding: 40px 0;
}

.spot-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(90px, 1fr));
  gap: 12px;
}

.spot-item {
  border-radius: 12px;
  color: white;
  padding: 12px 6px;
  text-align: center;
  cursor: pointer;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  transition: transform 0.15s;
}

.spot-item:hover {
  transform: scale(1.03);
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

.detail-content {
  padding: 8px 0;
}

.detail-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 0;
  border-bottom: 1px solid #ebeef5;
  font-size: 15px;
}

.detail-row:last-child {
  border-bottom: none;
}

@media (max-width: 768px) {
  .page-header {
    flex-wrap: wrap;
    gap: 10px;
  }

  .page-header :deep(.el-button) {
    height: 44px;
    font-size: 15px;
  }

  .spot-grid {
    grid-template-columns: repeat(auto-fill, minmax(72px, 1fr));
    gap: 8px;
  }

  .spot-item {
    padding: 10px 4px;
    border-radius: 10px;
  }

  .spot-code {
    font-size: 16px;
  }

  .spot-status {
    font-size: 11px;
  }
}
</style>
