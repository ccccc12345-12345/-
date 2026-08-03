<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  boardStatusText,
  createMerchantFishingSpot,
  deleteMerchantFishingSpot,
  getMerchantPond,
  getMerchantPondBoard,
  getMerchantPonds,
  getMerchantTimeSlots,
  updateMerchantFishingSpot,
  type SpotBoardItem
} from '@/api/merchant'
import { useMerchantPush } from '@/composables/useMerchantPush'
import type { Pond } from '@/api/pond'
import type { TimeSlot } from '@/api/timeslot'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const ponds = ref<Pond[]>([])
const pond = ref<Pond | null>(null)
const slots = ref<TimeSlot[]>([])
const board = ref<SpotBoardItem[]>([])
const pondId = ref<number | undefined>(Number(route.query.pondId) || undefined)
const slotDate = ref('')
const slotId = ref<number | undefined>(undefined)
const autoRefresh = ref(true)
const lastUpdatedAt = ref<number | null>(null)
const lastEventAt = ref<number | null>(null)
let timer: number | undefined

const selectedSpot = ref<SpotBoardItem | null>(null)
const dialogVisible = ref(false)
const showGuide = ref(false)
const form = reactive({
  id: null as number | null,
  spotCode: '',
  status: 1 as 0 | 1 | 2,
  coordinateX: 50,
  coordinateY: 50
})

const currentSlot = computed(() => slots.value.find((item) => item.id === slotId.value))
const hasFloorPlan = computed(() => !!pond.value?.floorPlanUrl)
const drawerVisible = computed({
  get: () => !!selectedSpot.value,
  set: (value: boolean) => {
    if (!value) selectedSpot.value = null
  }
})

const isLive = computed(() => {
  const t = lastEventAt.value || lastUpdatedAt.value
  if (!t) return false
  return Date.now() - t < 12000
})

const colorClass = (status: string) => {
  if (status === 'reserved' || status.includes('预约')) return 'reserved'
  if (status === 'using' || status.includes('核销')) return 'using'
  if (status === 'maintenance' || status.includes('维修')) return 'maintenance'
  if (status === 'disabled' || status.includes('禁用')) return 'disabled'
  return 'free'
}

const statusLabel = (status: string) => boardStatusText[status] || status
const toNumber = (value: unknown) => Number(value || 0)

const boardStatusToNumber = (status: string): 0 | 1 | 2 => {
  if (status === 'maintenance') return 2
  if (status === 'disabled') return 0
  return 1
}

const loadPonds = async () => {
  const res = await getMerchantPonds()
  ponds.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
  if (!pondId.value && ponds.value.length > 0) {
    // 默认选中最早创建的鱼塘（ID 最小），而不是按创建时间倒序的最新鱼塘
    const defaultPond = ponds.value.slice().sort((a, b) => a.id - b.id)[0]
    pondId.value = defaultPond.id
  }
}

const loadPond = async () => {
  if (!pondId.value) return
  const res = await getMerchantPond(pondId.value)
  pond.value = res.data
}

const loadSlots = async () => {
  if (!pondId.value) return
  const res = await getMerchantTimeSlots({ pondId: pondId.value, slotDate: slotDate.value || undefined, pageNum: 1, pageSize: 50 })
  slots.value = res.data?.records || []
  if (!slotId.value || !slots.value.some((item) => item.id === slotId.value)) {
    slotId.value = slots.value[0]?.id
  }
}

const loadBoard = async () => {
  if (!pondId.value || !slotId.value) {
    board.value = []
    return
  }
  loading.value = true
  try {
    const res = await getMerchantPondBoard({ pondId: pondId.value, slotId: slotId.value, date: slotDate.value || undefined })
    board.value = res.data || []
    lastUpdatedAt.value = Date.now()
    console.log('[PondBoard] loaded board:', board.value.length, 'spots', board.value.map(s => ({ id: s.spotId, code: s.spotCode, x: s.coordinateX, y: s.coordinateY, status: s.status })))
  } finally {
    loading.value = false
  }
}

const reloadAll = async () => {
  await loadPond()
  await loadSlots()
  await loadBoard()
}

const onEvent = () => {
  lastEventAt.value = Date.now()
  loadBoard()
}

watch([pondId, slotDate], async () => {
  slotId.value = undefined
  await reloadAll()
})

watch(slotId, loadBoard)

watch(autoRefresh, (enabled) => {
  if (timer) window.clearInterval(timer)
  if (enabled) timer = window.setInterval(loadBoard, 3000)
})

useMerchantPush({
  events: ['SPOT_BOARD_CHANGED', 'RESERVATION_STATUS_CHANGED', 'RESERVATION_CHECKED_IN'],
  onEvent: onEvent,
  fallback: () => loadBoard()
})

onMounted(async () => {
  await loadPonds()
  await reloadAll()
  timer = window.setInterval(loadBoard, 3000)
})

onBeforeUnmount(() => {
  if (timer) window.clearInterval(timer)
})

const POND_CX = 49
const POND_CY = 50
const POND_RX = 42
const POND_RY = 42
const SHORE_OFFSET = 5

/**
 * 将无坐标钓位按容量分布在池塘外围的同心圆环上，
 * 钓位多时自动扩展多层，避免所有默认钓位挤在图中央或相互重叠。
 */
const distributeAlongShore = (missing: SpotBoardItem[]) => {
  const n = missing.length || 1
  const sorted = [...missing].sort((a, b) => String(a.spotCode).localeCompare(String(b.spotCode)))
  const spacing = 14
  const minR = Math.min(POND_RX, POND_RY) + 2
  const ringStep = 10

  const ringCounts: number[] = []
  let remaining = n
  let ringIdx = 0
  while (remaining > 0) {
    const r = minR + ringIdx * ringStep
    const capacity = Math.max(1, Math.floor((2 * Math.PI * r) / spacing))
    const count = Math.min(remaining, capacity)
    ringCounts.push(count)
    remaining -= count
    ringIdx++
  }

  let idx = 0
  return new Map(
    sorted.map((spot) => {
      let ring = 0
      let idxInRing = idx
      for (let i = 0; i < ringCounts.length; i++) {
        if (idxInRing < ringCounts[i]) {
          ring = i
          break
        }
        idxInRing -= ringCounts[i]
      }
      const r = minR + ring * ringStep
      const step = (2 * Math.PI) / ringCounts[ring]
      const theta = -Math.PI / 2 + idxInRing * step
      const x = POND_CX + r * Math.cos(theta)
      const y = POND_CY + r * Math.sin(theta)
      idx++
      return [spot.spotId, { x: Number(x.toFixed(2)), y: Number(y.toFixed(2)) }]
    })
  )
}

const isDefaultPosition = (spot: SpotBoardItem) => {
  const x = toNumber(spot.coordinateX)
  const y = toNumber(spot.coordinateY)
  // 只有真正未设置坐标、原点或挤在中心小方块内的点才视为默认位置
  const nearCenter = Math.abs(x - 50) < 6 && Math.abs(y - 50) < 6
  return spot.coordinateX == null || spot.coordinateY == null || nearCenter || (x === 0 && y === 0)
}

/**
 * 检测是否所有钓位都挤在同一个坐标上（数据异常/全未设置坐标时触发兜底重新分布）
 */
const hasAllSamePosition = (spots: SpotBoardItem[]) => {
  if (spots.length <= 1) return false
  const firstX = toNumber(spots[0].coordinateX)
  const firstY = toNumber(spots[0].coordinateY)
  return spots.every((s) => {
    const dx = toNumber(s.coordinateX) - firstX
    const dy = toNumber(s.coordinateY) - firstY
    return Math.abs(dx) < 0.01 && Math.abs(dy) < 0.01
  })
}

const resolveOverlaps = <T extends { x: number; y: number }>(items: T[], minDistance = 14) => {
  const result = items.map((i) => ({ ...i }))
  const clampToShore = (x: number, y: number) => {
    const dx = x - POND_CX
    const dy = y - POND_CY
    const dist = Math.sqrt(dx * dx + dy * dy) || 1
    const minR = Math.min(POND_RX, POND_RY) + 1
    const maxR = Math.min(POND_RX, POND_RY) + 40
    if (dist < minR) {
      const ratio = minR / dist
      return { x: POND_CX + dx * ratio, y: POND_CY + dy * ratio }
    }
    if (dist > maxR) {
      const ratio = maxR / dist
      return { x: POND_CX + dx * ratio, y: POND_CY + dy * ratio }
    }
    return { x, y }
  }

  for (let iter = 0; iter < 120; iter++) {
    let moved = false
    for (let i = 0; i < result.length; i++) {
      for (let j = i + 1; j < result.length; j++) {
        const dx = result[j].x - result[i].x
        const dy = result[j].y - result[i].y
        const dist = Math.sqrt(dx * dx + dy * dy) || 0.001
        if (dist < minDistance) {
          const overlap = (minDistance - dist) / 2
          const nx = dx / dist
          const ny = dy / dist
          const ni = clampToShore(result[i].x - nx * overlap, result[i].y - ny * overlap)
          const nj = clampToShore(result[j].x + nx * overlap, result[j].y + ny * overlap)
          result[i].x = ni.x
          result[i].y = ni.y
          result[j].x = nj.x
          result[j].y = nj.y
          moved = true
        }
      }
    }
    if (!moved) break
  }
  return result
}

const spotsWithPosition = computed(() => {
  // 如果后端返回的坐标全部相同（数据异常），则把所有钓位都按默认位置重新分布
  const allSame = hasAllSamePosition(board.value)
  const missing = allSame ? board.value : board.value.filter(isDefaultPosition)
  const shoreMap = distributeAlongShore(missing)

  const positioned = board.value.map((spot) => {
    let x = toNumber(spot.coordinateX)
    let y = toNumber(spot.coordinateY)
    if (allSame || isDefaultPosition(spot)) {
      const fallback = shoreMap.get(spot.spotId)
      if (fallback) {
        x = fallback.x
        y = fallback.y
      }
    }
    return { ...spot, x, y }
  })

  const resolved = resolveOverlaps(positioned)
  console.log('[PondBoard] spotsWithPosition:', resolved.map(s => ({ id: s.spotId, code: s.spotCode, x: s.x, y: s.y })))
  return resolved
})

const timeAgo = (ts: number | null) => {
  if (!ts) return '-'
  const sec = Math.floor((Date.now() - ts) / 1000)
  if (sec < 5) return '刚刚'
  if (sec < 60) return `${sec} 秒前`
  return `${Math.floor(sec / 60)} 分钟前`
}

const resetForm = (patch: Partial<typeof form> = {}) => {
  form.id = null
  form.spotCode = patch.spotCode || `A${String(board.value.length + 1).padStart(2, '0')}`
  form.status = patch.status ?? 1
  form.coordinateX = patch.coordinateX ?? 50
  form.coordinateY = patch.coordinateY ?? 50
}

const getSvgPercent = (event: MouseEvent) => {
  const svg = (event.currentTarget as SVGElement).closest('svg')
  if (!svg) return { x: 50, y: 50 }
  const pt = svg.createSVGPoint()
  pt.x = event.clientX
  pt.y = event.clientY
  const ctm = svg.getScreenCTM()
  if (!ctm) return { x: 50, y: 50 }
  const svgP = pt.matrixTransform(ctm.inverse())
  return {
    x: Math.min(100, Math.max(0, Number(svgP.x.toFixed(2)))),
    y: Math.min(100, Math.max(0, Number(svgP.y.toFixed(2))))
  }
}

const ripples = ref<{ id: number; x: number; y: number }[]>([])
let rippleId = 0

const throttle = <T extends (...args: any[]) => void>(fn: T, wait: number) => {
  let last = 0
  return (...args: Parameters<T>) => {
    const now = Date.now()
    if (now - last >= wait) {
      last = now
      fn(...args)
    }
  }
}

const isInsidePond = (x: number, y: number) => {
  const dx = x - POND_CX
  const dy = y - POND_CY
  return (dx * dx) / (POND_RX * POND_RX) + (dy * dy) / (POND_RY * POND_RY) <= 1
}

const spawnRipple = (event: MouseEvent) => {
  const pos = getSvgPercent(event)
  if (!isInsidePond(pos.x, pos.y)) return
  const id = ++rippleId
  ripples.value.push({ id, x: pos.x, y: pos.y })
  window.setTimeout(() => {
    ripples.value = ripples.value.filter((r) => r.id !== id)
  }, 1400)
}

const onPondMove = throttle(spawnRipple, 90)

const openCreateAt = (event: MouseEvent) => {
  if ((event.target as HTMLElement).closest('.spot-marker')) return
  if (!pondId.value) {
    if (ponds.value.length > 0) {
      pondId.value = ponds.value[0].id
    } else {
      ElMessage.warning('暂无鱼塘数据，请先创建鱼塘')
      return
    }
  }
  const pos = getSvgPercent(event)
  resetForm({
    spotCode: `A${String(board.value.length + 1).padStart(2, '0')}`,
    coordinateX: pos.x,
    coordinateY: pos.y
  })
  dialogVisible.value = true
}

const openEdit = (spot: SpotBoardItem) => {
  selectedSpot.value = spot
  Object.assign(form, {
    id: spot.spotId,
    spotCode: spot.spotCode,
    status: boardStatusToNumber(spot.status),
    coordinateX: toNumber(spot.coordinateX),
    coordinateY: toNumber(spot.coordinateY)
  })
  dialogVisible.value = true
}

const saveSpot = async () => {
  if (!form.spotCode.trim()) {
    ElMessage.warning('请填写钓位编号')
    return
  }
  if (!pondId.value) {
    if (ponds.value.length > 0) {
      pondId.value = ponds.value[0].id
    } else {
      ElMessage.warning('请先选择鱼塘')
      return
    }
  }
  const payload = {
    pondId: pondId.value,
    spotCode: form.spotCode.trim(),
    status: form.status,
    coordinateX: Number(form.coordinateX),
    coordinateY: Number(form.coordinateY)
  }
  console.log('[saveSpot] payload', payload, 'form.id', form.id)
  try {
    if (form.id) {
      await updateMerchantFishingSpot(form.id, payload)
      ElMessage.success('钓位已更新')
    } else {
      await createMerchantFishingSpot(payload)
      ElMessage.success('钓位已新增')
    }
    dialogVisible.value = false
    selectedSpot.value = null
    await loadBoard()
  } catch (e: any) {
    console.error('[saveSpot] error', e)
    ElMessage.error(e?.message || e?.response?.data?.message || '保存失败')
  }
}

const removeSpot = async (spot: SpotBoardItem) => {
  try {
    await ElMessageBox.confirm(`确认删除钓位 ${spot.spotCode}？`, '删除钓位', { type: 'warning' })
    await deleteMerchantFishingSpot(spot.spotId)
    ElMessage.success('已删除')
    selectedSpot.value = null
    await loadBoard()
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

const toggleSpotStatus = async (spot: SpotBoardItem, status: 0 | 1 | 2) => {
  const labels: Record<number, string> = { 0: '禁用', 1: '可用', 2: '维修' }
  try {
    await ElMessageBox.confirm(`确定将钓位 ${spot.spotCode} 标记为“${labels[status]}”吗？`, '提示', { type: 'warning' })
    if (!pondId.value) return
    await updateMerchantFishingSpot(spot.spotId, {
      pondId: pondId.value,
      spotCode: spot.spotCode,
      status
    })
    ElMessage.success('状态已更新')
    await loadBoard()
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

const stats = computed(() => ({
  total: board.value.length,
  free: board.value.filter((s) => s.status === 'free').length,
  reserved: board.value.filter((s) => s.status === 'reserved').length,
  using: board.value.filter((s) => s.status === 'using').length,
  maintenance: board.value.filter((s) => s.status === 'maintenance').length,
  disabled: board.value.filter((s) => s.status === 'disabled').length
}))
</script>

<template>
  <section class="board-page">
    <div class="page-hero">
      <div>
        <p class="subtitle">实时钓位看板</p>
        <h1>{{ pond?.name || '鱼塘看板' }}</h1>
      </div>
      <div class="hero-actions">
        <div class="live-badge" :class="{ offline: !isLive }">
          <span class="pulse" />
          <span>{{ isLive ? '实时连接中' : '已断开' }}</span>
        </div>
        <el-switch v-model="autoRefresh" active-text="3 秒刷新" inline-prompt />
        <el-button type="primary" :icon="Refresh" :loading="loading" v-fp-ripple @click="loadBoard">手动刷新</el-button>
      </div>
    </div>

    <div class="glass-filter fp-lift">
      <el-select v-model="pondId" placeholder="选择鱼塘" style="width: 220px">
        <el-option v-for="item in ponds" :key="item.id" :label="item.name" :value="item.id" />
      </el-select>
      <el-date-picker v-model="slotDate" value-format="YYYY-MM-DD" type="date" placeholder="选择日期" />
      <el-select v-model="slotId" placeholder="选择场次" style="width: 280px">
        <el-option v-for="slot in slots" :key="slot.id" :label="`${slot.slotDate} ${slot.slotName} ${slot.startTime}-${slot.endTime}`" :value="slot.id" />
      </el-select>
      <span v-if="currentSlot" class="slot-tip">{{ currentSlot.slotName }} · 剩余 {{ currentSlot.remain ?? '-' }}</span>
      <span class="update-tip">更新于 {{ timeAgo(lastUpdatedAt) }}</span>
    </div>

    <div class="legend-bar">
      <span class="chip free"><i /> 空闲 {{ stats.free }}</span>
      <span class="chip reserved"><i /> 已预约 {{ stats.reserved }}</span>
      <span class="chip using"><i /> 使用中 {{ stats.using }}</span>
      <span class="chip maintenance"><i /> 维修 {{ stats.maintenance }}</span>
      <span class="chip disabled"><i /> 禁用 {{ stats.disabled }}</span>
      <span class="chip total"><i /> 总钓位 {{ stats.total }}</span>
      <el-button :type="showGuide ? 'primary' : 'default'" link @click="showGuide = !showGuide">
        {{ showGuide ? '隐藏示意图' : '显示示意图' }}
      </el-button>
      <el-button class="editor-link" link type="primary" @click="router.push(`/merchant/ponds/${pondId}/spots`)">
        进入钓位编辑器
      </el-button>
    </div>

    <div class="map-stage fp-magnetic" v-loading="loading" v-fp-magnetic>
      <svg class="pond-map" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid meet" @click="openCreateAt" @mousemove="onPondMove">
        <defs>
          <radialGradient id="water" cx="50%" cy="45%" r="70%">
            <stop offset="0%" stop-color="oklch(82% 0.09 165)" />
            <stop offset="45%" stop-color="oklch(68% 0.12 165)" />
            <stop offset="100%" stop-color="oklch(54% 0.11 165)" />
          </radialGradient>
          <radialGradient id="shore" cx="50%" cy="50%" r="75%">
            <stop offset="55%" stop-color="rgba(255,255,255,0)" />
            <stop offset="100%" stop-color="rgba(33, 82, 68, 0.28)" />
          </radialGradient>
          <linearGradient id="grass" x1="0" y1="0" x2="0" y2="1">
            <stop offset="0%" stop-color="#f1f7f3" />
            <stop offset="100%" stop-color="#dfeede" />
          </linearGradient>
          <filter id="softShadow" x="-50%" y="-50%" width="200%" height="200%">
            <feDropShadow dx="0" dy="1.2" stdDeviation="1.2" flood-color="#153c35" flood-opacity="0.28" />
          </filter>
          <filter id="waterRipple" x="-20%" y="-20%" width="140%" height="140%">
            <feTurbulence type="fractalNoise" baseFrequency="0.06" numOctaves="2" result="noise" />
            <feDisplacementMap in="SourceGraphic" in2="noise" scale="1.2" />
          </filter>
          <marker id="arrow" markerWidth="4" markerHeight="4" refX="2" refY="2" orient="auto">
            <path d="M0,0 L4,2 L0,4 Z" fill="var(--fp-primary-dark)" />
          </marker>
        </defs>

        <!-- 自定义鱼塘平面图 -->
        <image
          v-if="hasFloorPlan"
          x="0"
          y="0"
          width="100"
          height="100"
          :href="pond?.floorPlanUrl"
          preserveAspectRatio="xMidYMid meet"
        />

        <!-- 背景与草地（无平面图时显示） -->
        <template v-if="!hasFloorPlan">
          <rect width="100" height="100" fill="url(#grass)" rx="14" />
          <path d="M0 78 Q 25 64, 50 74 T 100 70 V 100 H 0 Z" fill="#d9ead7" opacity="0.6" />
          <path d="M0 86 Q 30 76, 60 84 T 100 82 V 100 H 0 Z" fill="#c5dec2" opacity="0.7" />

          <!-- 池塘主体 -->
          <path
            class="pond-shape"
            d="M 50 8 C 74 8, 91 26, 91 50 C 91 72, 74 92, 50 92 C 24 92, 7 72, 7 50 C 7 26, 26 8, 50 8 Z"
            fill="url(#water)"
            stroke="var(--fp-primary-light)"
            stroke-width="0.6"
          />
          <path
            d="M 50 8 C 74 8, 91 26, 91 50 C 91 72, 74 92, 50 92 C 24 92, 7 72, 7 50 C 7 26, 26 8, 50 8 Z"
            fill="url(#shore)"
            pointer-events="none"
          />

          <!-- 水深示意环 -->
          <ellipse cx="49" cy="50" rx="28" ry="28" fill="none" stroke="oklch(100% 0 0 / 0.22)" stroke-width="0.4" stroke-dasharray="2 2" />
          <ellipse cx="49" cy="50" rx="16" ry="16" fill="none" stroke="oklch(100% 0 0 / 0.18)" stroke-width="0.4" stroke-dasharray="1.5 2" />

          <!-- 水面光影 -->
          <ellipse cx="34" cy="38" rx="6" ry="3.5" fill="#6ab38f" opacity="0.55" />
          <ellipse cx="68" cy="62" rx="5" ry="3" fill="#6ab38f" opacity="0.5" />

          <!-- 池塘名称 -->
          <text x="49" y="50" font-size="5" text-anchor="middle" fill="oklch(100% 0 0 / 0.55)" font-weight="800" font-family="var(--fp-font-display)">
            {{ pond?.name || '鱼塘' }}
          </text>
        </template>

        <!-- 休息平台 -->
        <g class="map-marker">
          <rect x="78" y="14" width="12" height="7" rx="1.5" fill="oklch(70% 0.08 85 / 0.85)" filter="url(#softShadow)" />
          <text x="84" y="19" font-size="2.2" text-anchor="middle" fill="var(--fp-primary-dark)" font-weight="700">休息亭</text>
        </g>

        <!-- 入口 -->
        <g class="map-marker">
          <path d="M 10 88 L 18 88 L 14 82 Z" fill="var(--fp-primary-dark)" opacity="0.85" />
          <text x="14" y="95" font-size="2.2" text-anchor="middle" fill="var(--fp-primary-dark)" font-weight="700">入口</text>
          <line x1="14" y1="88" x2="14" y2="76" stroke="var(--fp-primary-dark)" stroke-width="0.4" stroke-dasharray="1 1" marker-end="url(#arrow)" />
        </g>

        <!-- 方向标 -->
        <g class="map-marker compass" transform="translate(86, 86)">
          <circle r="5" fill="#fff" stroke="var(--fp-primary-dark)" stroke-width="0.5" />
          <path d="M0 -4 L1.2 1.2 L0 0 L-1.2 1.2 Z" fill="var(--fp-primary-dark)" />
          <text y="-0.5" font-size="2.6" text-anchor="middle" fill="var(--fp-primary-dark)" font-weight="800">N</text>
        </g>

        <!-- 比例尺 -->
        <g class="map-marker scale" transform="translate(6, 6)">
          <rect x="0" y="0" width="14" height="5" rx="1" fill="#fff" stroke="var(--fp-border)" />
          <line x1="2" y1="3" x2="12" y2="3" stroke="var(--fp-primary-dark)" stroke-width="0.5" />
          <text x="7" y="8.5" font-size="2" text-anchor="middle" fill="var(--fp-text-secondary)">≈ 20 m</text>
        </g>

        <!-- 水面波纹（鼠标划过水面的涟漪） -->
        <g class="ripples" pointer-events="none">
          <circle
            v-for="ripple in ripples"
            :key="ripple.id"
            class="ripple-ring"
            :cx="ripple.x"
            :cy="ripple.y"
            r="0"
            fill="none"
            stroke="oklch(100% 0 0 / 0.55)"
            stroke-width="0.6"
          />
        </g>

        <!-- 钓位 -->
        <g
          v-for="(spot, index) in spotsWithPosition"
          :key="spot.spotId"
          class="spot-marker"
          :class="[colorClass(spot.status), { active: selectedSpot?.spotId === spot.spotId, 'has-coords': spot.coordinateX != null && spot.coordinateY != null }]"
          :transform="`translate(${spot.x}, ${spot.y})`"
          @click.stop="openEdit(spot)"
        >
          <g class="spot-marker-inner" :style="{ animationDelay: `${index * 40}ms` }">
            <circle r="8.5" fill="rgba(0,0,0,0.01)" class="hit-area" />
            <circle r="5.6" filter="url(#softShadow)" />
            <circle r="3.2" fill="#fff" opacity="0.35" />
            <text y="1.6" font-size="3" text-anchor="middle" fill="#fff" font-weight="800">{{ spot.spotCode }}</text>
            <title>{{ spot.spotCode }} · {{ statusLabel(spot.status) }}</title>
          </g>
        </g>

        <!-- 示意图标注层 -->
        <g v-if="showGuide" class="guide-overlay" pointer-events="none">
          <path d="M 49 8 L 49 16" stroke="var(--fp-accent-dark)" stroke-width="0.4" stroke-dasharray="1 1" />
          <text x="49" y="5" font-size="2.4" text-anchor="middle" fill="var(--fp-accent-dark)" font-weight="700">北岸钓位区</text>
          <path d="M 91 50 L 83 50" stroke="var(--fp-accent-dark)" stroke-width="0.4" stroke-dasharray="1 1" />
          <text x="90" y="48" font-size="2.4" text-anchor="end" fill="var(--fp-accent-dark)" font-weight="700">东岸</text>
          <path d="M 7 50 L 15 50" stroke="var(--fp-accent-dark)" stroke-width="0.4" stroke-dasharray="1 1" />
          <text x="10" y="48" font-size="2.4" text-anchor="start" fill="var(--fp-accent-dark)" font-weight="700">西岸</text>
          <circle cx="49" cy="50" r="46" fill="none" stroke="var(--fp-accent-dark)" stroke-width="0.3" stroke-dasharray="2 2" opacity="0.5" />
          <text x="49" y="97" font-size="2.2" text-anchor="middle" fill="var(--fp-accent-dark)" font-weight="700">无坐标钓位默认沿池塘边缘均匀分布</text>
        </g>
      </svg>

      <div v-if="board.length === 0 && !loading" class="empty-map">
        <div class="empty-content">
          <p class="empty-title">当前鱼塘暂无钓位</p>
          <p class="empty-desc">点击鱼塘任意位置即可新增钓位，或进入钓位编辑器批量管理。</p>
          <div class="empty-actions">
            <el-button type="primary" v-fp-ripple @click="resetForm(); dialogVisible = true">新增首个钓位</el-button>
            <el-button v-fp-ripple @click="router.push(`/merchant/ponds/${pondId}/spots`)">进入编辑器</el-button>
          </div>
        </div>
      </div>
    </div>

    <el-drawer v-model="drawerVisible" title="钓位详情" direction="rtl" size="380" @close="selectedSpot = null">
      <div v-if="selectedSpot" class="spot-detail">
        <div class="spot-head">
          <div class="spot-code">{{ selectedSpot.spotCode }}</div>
          <span class="spot-status" :class="colorClass(selectedSpot.status)">{{ statusLabel(selectedSpot.status) }}</span>
        </div>

        <div class="detail-grid">
          <div class="detail-card" v-if="selectedSpot.reservationId">
            <h4>预约信息</h4>
            <p><label>预约编号</label><span>#{{ selectedSpot.reservationId }}</span></p>
            <p><label>预约状态</label><span>{{ selectedSpot.reservationStatus || '-' }}</span></p>
          </div>
          <div class="detail-card" v-if="selectedSpot.userNickname">
            <h4>用户信息</h4>
            <p><label>用户昵称</label><span>{{ selectedSpot.userNickname }}</span></p>
            <p v-if="selectedSpot.userPhone"><label>手机尾号</label><span>**** {{ selectedSpot.userPhone }}</span></p>
          </div>
          <div class="detail-card" v-else>
            <h4>当前空闲</h4>
            <p class="muted">该钓位当前无人预约，可正常使用。</p>
          </div>
        </div>

        <div class="drawer-actions">
          <el-button type="primary" v-fp-ripple @click="openEdit(selectedSpot)">编辑钓位</el-button>
          <el-button v-if="selectedSpot.status !== 'maintenance'" type="warning" plain v-fp-ripple @click="toggleSpotStatus(selectedSpot, 2)">标记维修</el-button>
          <el-button v-if="selectedSpot.status === 'maintenance' || selectedSpot.status === 'disabled'" type="success" plain v-fp-ripple @click="toggleSpotStatus(selectedSpot, 1)">恢复可用</el-button>
          <el-button v-if="selectedSpot.status === 'free'" type="danger" plain v-fp-ripple @click="toggleSpotStatus(selectedSpot, 0)">禁用钓位</el-button>
          <el-button type="danger" plain v-fp-ripple @click="removeSpot(selectedSpot)">删除钓位</el-button>
        </div>
      </div>
    </el-drawer>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑钓位' : '新增钓位'" width="460px">
      <el-form label-position="top">
        <el-form-item label="编号"><el-input v-model="form.spotCode" placeholder="如 A01" /></el-form-item>
        <el-form-item label="状态">
          <el-select v-model="form.status">
            <el-option label="可用" :value="1" />
            <el-option label="维修" :value="2" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <div class="form-grid">
          <el-form-item label="X 坐标（%）"><el-input-number v-model="form.coordinateX" :min="0" :max="100" :precision="2" /></el-form-item>
          <el-form-item label="Y 坐标（%）"><el-input-number v-model="form.coordinateY" :min="0" :max="100" :precision="2" /></el-form-item>
        </div>
      </el-form>
      <template #footer>
        <el-button v-fp-ripple @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" v-fp-ripple @click="saveSpot">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script lang="ts">
import { Refresh } from '@element-plus/icons-vue'
export { Refresh }
</script>

<style scoped>
.board-page {
  padding-bottom: var(--fp-space-8);
}

.page-hero {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: var(--fp-space-5);
  padding: var(--fp-space-6) var(--fp-space-8);
  margin-bottom: var(--fp-space-6);
  border-radius: var(--fp-radius);
  background: linear-gradient(135deg, var(--fp-primary-dark) 0%, var(--fp-primary) 100%);
  color: #fff;
  box-shadow: var(--fp-shadow-lg);
  position: relative;
  overflow: hidden;
}

.page-hero h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 700;
  letter-spacing: 0.5px;
}

.subtitle {
  margin: 0 0 6px;
  color: var(--fp-accent-light);
  font-size: 13px;
  letter-spacing: 1px;
  text-transform: uppercase;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
  position: relative;
}

.live-badge {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.15);
  font-size: 13px;
  color: #d4f5e7;
  backdrop-filter: blur(4px);
}

.live-badge.offline {
  color: #f5d4d4;
  background: rgba(214, 77, 67, 0.22);
}

.pulse {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #5ee9a8;
  box-shadow: 0 0 0 0 rgba(94, 233, 168, 0.7);
  animation: pulse 1.6s infinite;
}

.offline .pulse {
  background: #ff9e9e;
  animation: none;
}

@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(94, 233, 168, 0.7); }
  70% { box-shadow: 0 0 0 8px rgba(94, 233, 168, 0); }
  100% { box-shadow: 0 0 0 0 rgba(94, 233, 168, 0); }
}

.glass-filter {
  display: flex;
  align-items: center;
  gap: var(--fp-space-4);
  flex-wrap: wrap;
  padding: var(--fp-space-4) var(--fp-space-5);
  margin-bottom: var(--fp-space-5);
  border-radius: var(--fp-radius);
  background: var(--fp-surface);
  box-shadow: var(--fp-shadow-sm);
  backdrop-filter: blur(10px);
}

.slot-tip {
  margin-left: auto;
  color: var(--fp-primary);
  font-weight: 700;
  font-size: 14px;
}

.update-tip {
  color: var(--fp-muted);
  font-size: 12px;
}

.legend-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
  margin-bottom: 16px;
}

.chip {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 8px 14px;
  border-radius: 999px;
  background: var(--fp-surface);
  box-shadow: 0 4px 12px oklch(28% 0.06 165 / 0.06);
  font-size: 13px;
  font-weight: 600;
  color: var(--fp-text-secondary);
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out);
}

.chip:hover {
  transform: translateY(-2px);
  box-shadow: var(--fp-shadow-sm);
}

.chip i {
  width: 10px;
  height: 10px;
  border-radius: 50%;
}

.chip.free i { background: var(--fp-primary-light); }
.chip.reserved i { background: var(--fp-accent-dark); }
.chip.using i { background: #2d75c8; }
.chip.maintenance i { background: #d64d43; }
.chip.disabled i { background: var(--fp-muted); }
.chip.total i { background: var(--fp-primary-dark); }

.editor-link {
  margin-left: auto;
  font-weight: 700;
}

.map-stage {
  position: relative;
  border-radius: var(--fp-radius);
  background: var(--fp-surface);
  box-shadow: var(--fp-shadow-md);
  overflow: hidden;
  transform-style: preserve-3d;
}

.pond-map {
  display: block;
  width: 100%;
  height: auto;
  max-height: 78vh;
  cursor: crosshair;
}

.pond-shape {
  filter: drop-shadow(0 4px 10px oklch(28% 0.06 165 / 0.18));
}

.map-marker {
  opacity: 0.9;
  transition: opacity 0.25s var(--fp-ease-out);
}

.map-marker text {
  font-family: var(--fp-font-body);
  pointer-events: none;
}

.compass path {
  pointer-events: none;
}

.guide-overlay text,
.guide-overlay path,
.guide-overlay circle {
  pointer-events: none;
}

.ripple-ring {
  animation: waterRipple 1.3s var(--fp-ease-out) forwards;
  pointer-events: none;
}

@keyframes waterRipple {
  0% {
    r: 0;
    opacity: 0.7;
    stroke-width: 0.8;
  }
  100% {
    r: 7;
    opacity: 0;
    stroke-width: 0.15;
  }
}

.spot-marker {
  cursor: pointer;
}

.spot-marker-inner {
  opacity: 0;
  animation: popIn 0.45s var(--fp-ease-out) forwards;
  transition: transform 0.2s var(--fp-ease-out);
}

.spot-marker .hit-area {
  pointer-events: all;
}

.spot-marker circle:not(.hit-area) {
  stroke: #fff;
  stroke-width: 0.6;
  pointer-events: none;
  transition: r 0.2s var(--fp-ease-out), stroke-width 0.2s var(--fp-ease-out);
}

.spot-marker.free .spot-marker-inner circle:nth-of-type(2) { fill: var(--fp-primary-light); }
.spot-marker.reserved .spot-marker-inner circle:nth-of-type(2) { fill: var(--fp-accent-dark); }
.spot-marker.using .spot-marker-inner circle:nth-of-type(2) { fill: #2d75c8; }
.spot-marker.maintenance .spot-marker-inner circle:nth-of-type(2) { fill: #d64d43; }
.spot-marker.disabled .spot-marker-inner circle:nth-of-type(2) { fill: var(--fp-muted); }

/* 无坐标默认布局的钓位，加一层虚线外环作为提示 */
.spot-marker:not(.has-coords) .spot-marker-inner circle:nth-of-type(3) {
  fill: none;
  stroke: #fff;
  stroke-width: 0.5;
  stroke-dasharray: 1 1;
  opacity: 0.8;
}

.spot-marker.has-coords .spot-marker-inner circle:nth-of-type(3) {
  opacity: 0.2;
}

.spot-marker:hover .spot-marker-inner {
  transform: scale(1.38);
}

.spot-marker:hover .spot-marker-inner circle:nth-of-type(2) {
  r: 6.2;
}

.spot-marker.active .spot-marker-inner {
  transform: scale(1.6);
}

.spot-marker.active .spot-marker-inner circle:nth-of-type(2) {
  r: 6.6;
  stroke: var(--fp-accent-light);
  stroke-width: 1;
}

.spot-marker text {
  pointer-events: none;
  font-family: var(--fp-font-body);
}

@keyframes popIn {
  from {
    opacity: 0;
    transform: scale(0.4);
  }
  to {
    opacity: 1;
    transform: scale(1);
  }
}

.empty-map {
  position: absolute;
  top: 18px;
  left: 18px;
  z-index: 5;
  pointer-events: none;
}

.empty-content {
  pointer-events: auto;
  max-width: 320px;
  text-align: left;
  padding: var(--fp-space-5);
  border-radius: var(--fp-radius);
  background: var(--fp-surface);
  box-shadow: var(--fp-shadow-lg);
  border: 1px solid var(--fp-border);
}

.empty-title {
  margin: 0 0 6px;
  font-size: 16px;
  font-weight: 800;
  color: var(--fp-text);
  font-family: var(--fp-font-display);
}

.empty-desc {
  margin: 0 0 14px;
  color: var(--fp-text-secondary);
  font-size: 13px;
  line-height: 1.5;
}

.empty-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.spot-detail {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.spot-head {
  display: flex;
  align-items: center;
  gap: 14px;
}

.spot-code {
  width: 56px;
  height: 56px;
  display: grid;
  place-items: center;
  border-radius: var(--fp-radius-sm);
  background: linear-gradient(135deg, var(--fp-primary) 0%, var(--fp-primary-light) 100%);
  color: #fff;
  font-size: 22px;
  font-weight: 800;
}

.spot-status {
  padding: 6px 14px;
  border-radius: 999px;
  color: #fff;
  font-size: 13px;
  font-weight: 700;
}

.spot-status.free { background: var(--fp-primary-light); }
.spot-status.reserved { background: var(--fp-accent-dark); }
.spot-status.using { background: #2d75c8; }
.spot-status.maintenance { background: #d64d43; }
.spot-status.disabled { background: var(--fp-muted); }

.detail-grid {
  display: grid;
  gap: 14px;
}

.detail-card {
  padding: 16px;
  border-radius: var(--fp-radius-sm);
  background: var(--fp-primary-soft);
}

.detail-card h4 {
  margin: 0 0 12px;
  color: var(--fp-primary-dark);
  font-size: 15px;
}

.detail-card p {
  display: flex;
  justify-content: space-between;
  margin: 8px 0 0;
  font-size: 14px;
}

.detail-card label {
  color: var(--fp-text-secondary);
}

.detail-card span {
  color: var(--fp-text);
  font-weight: 600;
}

.muted {
  color: var(--fp-muted);
}

.drawer-actions {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
  margin-top: auto;
  padding-top: 10px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

@media (max-width: 860px) {
  .page-hero {
    flex-direction: column;
    align-items: flex-start;
  }

  .slot-tip,
  .editor-link {
    margin-left: 0;
  }
}
</style>
