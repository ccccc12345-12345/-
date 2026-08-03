<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMerchantFishingSpot,
  deleteMerchantFishingSpot,
  getMerchantFishingSpots,
  getMerchantPond,
  spotStatusText,
  updateMerchantFishingSpot,
  type FishingSpot
} from '@/api/merchant'
import type { Pond } from '@/api/pond'

const route = useRoute()
const router = useRouter()
const pondId = Number(route.params.id)
const pond = ref<Pond | null>(null)
const spots = ref<FishingSpot[]>([])
const loading = ref(false)
const editMode = ref(true)
const draggingId = ref<number | null>(null)
const mapRef = ref<HTMLElement | null>(null)
const dialogVisible = ref(false)
const form = reactive({ id: null as number | null, spotCode: '', status: 1 as 0 | 1 | 2, coordinateX: 50, coordinateY: 50 })

// Water ripple effect inside the default pond diagram
const ripples = ref<{ id: number; x: number; y: number }[]>([])
let rippleId = 0
const RIPPLE_CX = 50
const RIPPLE_CY = 50
const RIPPLE_RX = 42
const RIPPLE_RY = 42

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

const isInsidePondEllipse = (x: number, y: number) => {
  const dx = x - RIPPLE_CX
  const dy = y - RIPPLE_CY
  return (dx * dx) / (RIPPLE_RX * RIPPLE_RX) + (dy * dy) / (RIPPLE_RY * RIPPLE_RY) <= 1
}

const getSvgPercent = (event: MouseEvent) => {
  const rect = (event.currentTarget as SVGElement | null)?.getBoundingClientRect()
  if (!rect) return { x: 50, y: 50 }
  return {
    x: Math.min(100, Math.max(0, ((event.clientX - rect.left) / rect.width) * 100)),
    y: Math.min(100, Math.max(0, ((event.clientY - rect.top) / rect.height) * 100))
  }
}

const spawnRipple = (event: MouseEvent) => {
  const pos = getSvgPercent(event)
  if (!isInsidePondEllipse(pos.x, pos.y)) return
  const id = ++rippleId
  ripples.value.push({ id, x: pos.x, y: pos.y })
  window.setTimeout(() => {
    ripples.value = ripples.value.filter((r) => r.id !== id)
  }, 1400)
}

const onPondMove = throttle(spawnRipple, 90)

const sortedSpots = computed(() => [...spots.value].sort((a, b) => String(a.spotCode).localeCompare(String(b.spotCode))))

const toNumber = (value: unknown) => Number(value || 0)

const isDefaultPosition = (spot: FishingSpot) => {
  const x = toNumber(spot.coordinateX)
  const y = toNumber(spot.coordinateY)
  const nearCenter = Math.abs(x - 50) < 6 && Math.abs(y - 50) < 6
  return spot.coordinateX == null || spot.coordinateY == null || nearCenter || (x === 0 && y === 0)
}

/**
 * 将无坐标钓位按容量分布在池塘外围的同心圆环上，
 * 钓位多时自动扩展多层，避免所有默认钓位挤在图中央或相互重叠。
 */
const distributeAlongShore = (missing: FishingSpot[]) => {
  const n = missing.length || 1
  const sorted = [...missing].sort((a, b) => String(a.spotCode).localeCompare(String(b.spotCode)))
  const spacing = 16
  const minR = Math.min(EDITOR_RX, EDITOR_RY) + 2
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
      const x = EDITOR_CX + r * Math.cos(theta)
      const y = EDITOR_CY + r * Math.sin(theta)
      idx++
      return [spot.id, { x: Number(x.toFixed(2)), y: Number(y.toFixed(2)) }]
    })
  )
}

const resolveOverlaps = <T extends { x: number; y: number }>(items: T[], minDistance = 16) => {
  const result = items.map((i) => ({ ...i }))
  const clampToShore = (x: number, y: number) => {
    const dx = x - EDITOR_CX
    const dy = y - EDITOR_CY
    const dist = Math.sqrt(dx * dx + dy * dy) || 1
    const minR = Math.min(EDITOR_RX, EDITOR_RY) + 1
    const maxR = Math.min(EDITOR_RX, EDITOR_RY) + 40
    if (dist < minR) {
      const ratio = minR / dist
      return { x: EDITOR_CX + dx * ratio, y: EDITOR_CY + dy * ratio }
    }
    if (dist > maxR) {
      const ratio = maxR / dist
      return { x: EDITOR_CX + dx * ratio, y: EDITOR_CY + dy * ratio }
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
  const missing = spots.value.filter(isDefaultPosition)
  const shoreMap = distributeAlongShore(missing)
  const positioned = spots.value.map((spot) => {
    let x = toNumber(spot.coordinateX)
    let y = toNumber(spot.coordinateY)
    if (isDefaultPosition(spot)) {
      const fallback = shoreMap.get(spot.id)
      if (fallback) {
        x = fallback.x
        y = fallback.y
      }
    }
    return { ...spot, x, y }
  })
  return resolveOverlaps(positioned)
})

const pointStyle = (spot: FishingSpot & { x?: number; y?: number }) => ({
  left: `${spot.x ?? toNumber(spot.coordinateX)}%`,
  top: `${spot.y ?? toNumber(spot.coordinateY)}%`
})

const loadData = async () => {
  loading.value = true
  try {
    const [pondRes, spotRes] = await Promise.all([getMerchantPond(pondId), getMerchantFishingSpots(pondId)])
    pond.value = pondRes.data
    spots.value = spotRes.data?.records || []
  } finally {
    loading.value = false
  }
}

const getPercent = (event: MouseEvent) => {
  const rect = mapRef.value?.getBoundingClientRect()
  if (!rect) return { x: 50, y: 50 }
  return {
    x: Math.min(100, Math.max(0, ((event.clientX - rect.left) / rect.width) * 100)),
    y: Math.min(100, Math.max(0, ((event.clientY - rect.top) / rect.height) * 100))
  }
}

const EDITOR_CX = 50
const EDITOR_CY = 50
const EDITOR_RX = 40
const EDITOR_RY = 40

const nextShorePosition = () => {
  const n = Math.max(1, spots.value.length + 1)
  const step = (2 * Math.PI) / n
  const startAngle = -Math.PI / 2
  const idx = spots.value.length
  const theta = startAngle + idx * step
  return {
    x: Number((EDITOR_CX + EDITOR_RX * Math.cos(theta)).toFixed(2)),
    y: Number((EDITOR_CY + EDITOR_RY * Math.sin(theta)).toFixed(2))
  }
}

const resetSpotForm = (patch: Partial<typeof form> = {}) => {
  const fallback = patch.coordinateX == null || patch.coordinateY == null ? nextShorePosition() : null
  form.id = null
  form.spotCode = patch.spotCode || `A${String(spots.value.length + 1).padStart(2, '0')}`
  form.status = patch.status ?? 1
  form.coordinateX = patch.coordinateX ?? fallback?.x ?? 50
  form.coordinateY = patch.coordinateY ?? fallback?.y ?? 50
}

const openCreateAt = (event: MouseEvent) => {
  if (!editMode.value || (event.target as HTMLElement).closest('.spot-point')) return
  const pos = getPercent(event)
  resetSpotForm({
    spotCode: `A${String(spots.value.length + 1).padStart(2, '0')}`,
    coordinateX: Number(pos.x.toFixed(2)),
    coordinateY: Number(pos.y.toFixed(2))
  })
  dialogVisible.value = true
}

const openCreateDialog = () => {
  resetSpotForm({ spotCode: `A${String(spots.value.length + 1).padStart(2, '0')}` })
  dialogVisible.value = true
}

const save = async () => {
  if (!form.spotCode.trim()) {
    ElMessage.warning('请填写钓位编号')
    return
  }
  const payload = {
    pondId,
    spotCode: form.spotCode.trim(),
    status: form.status,
    coordinateX: Number(form.coordinateX),
    coordinateY: Number(form.coordinateY)
  }
  try {
    if (form.id) {
      await updateMerchantFishingSpot(form.id, payload)
      ElMessage.success('钓位已更新')
    } else {
      await createMerchantFishingSpot(payload)
      ElMessage.success('钓位已新增')
    }
    dialogVisible.value = false
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '保存失败')
  }
}

const remove = async (spot: FishingSpot) => {
  try {
    await ElMessageBox.confirm(`确认删除钓位 ${spot.spotCode}？`, '删除钓位', { type: 'warning' })
    await deleteMerchantFishingSpot(spot.id)
    ElMessage.success('已删除')
    await loadData()
  } catch (e: any) {
    if (e !== 'cancel') console.error(e)
  }
}

const setStatus = async (spot: FishingSpot, status: 0 | 1 | 2) => {
  try {
    await updateMerchantFishingSpot(spot.id, {
      pondId,
      spotCode: spot.spotCode,
      status,
      coordinateX: toNumber(spot.coordinateX),
      coordinateY: toNumber(spot.coordinateY)
    })
    await loadData()
  } catch (e: any) {
    ElMessage.error(e?.message || '状态更新失败')
  }
}

const dragStartPos = ref<{ x: number; y: number } | null>(null)
const dragMoved = ref(false)

const startDrag = (event: MouseEvent, spot: FishingSpot) => {
  if (!editMode.value) return
  event.preventDefault()
  draggingId.value = spot.id
  dragStartPos.value = { x: event.clientX, y: event.clientY }
  dragMoved.value = false
  window.addEventListener('mousemove', moveDrag)
  window.addEventListener('mouseup', endDrag)
}

const moveDrag = (event: MouseEvent) => {
  if (!draggingId.value || !dragStartPos.value) return
  const dx = event.clientX - dragStartPos.value.x
  const dy = event.clientY - dragStartPos.value.y
  if (Math.sqrt(dx * dx + dy * dy) > 3) dragMoved.value = true
  const pos = getPercent(event)
  const spot = spots.value.find((item) => item.id === draggingId.value)
  if (spot) {
    spot.coordinateX = Number(pos.x.toFixed(2))
    spot.coordinateY = Number(pos.y.toFixed(2))
  }
}

const endDrag = async () => {
  const spot = spots.value.find((item) => item.id === draggingId.value)
  const moved = dragMoved.value
  draggingId.value = null
  dragStartPos.value = null
  dragMoved.value = false
  window.removeEventListener('mousemove', moveDrag)
  window.removeEventListener('mouseup', endDrag)
  if (spot && moved) {
    try {
      await updateMerchantFishingSpot(spot.id, {
        pondId,
        spotCode: spot.spotCode,
        status: spot.status,
        coordinateX: toNumber(spot.coordinateX),
        coordinateY: toNumber(spot.coordinateY)
      })
      ElMessage.success('坐标已保存')
    } catch (e: any) {
      ElMessage.error(e?.message || '保存失败')
      await loadData()
    }
  }
}

const openEdit = (spot: FishingSpot) => {
  if (dragMoved.value) return
  Object.assign(form, {
    id: spot.id,
    spotCode: spot.spotCode,
    status: spot.status,
    coordinateX: toNumber(spot.coordinateX),
    coordinateY: toNumber(spot.coordinateY)
  })
  dialogVisible.value = true
}

onMounted(loadData)
</script>

<template>
  <section v-loading="loading" class="editor-page">
    <div class="page-hero">
      <div>
        <p class="subtitle">钓位可视化编排</p>
        <h1>{{ pond?.name || '鱼塘' }}</h1>
      </div>
      <div class="hero-actions">
        <el-button text :icon="Back" @click="router.push('/merchant/ponds')">返回鱼塘列表</el-button>
        <el-switch v-model="editMode" active-text="编辑模式" inactive-text="查看模式" />
        <el-button v-fp-ripple @click="loadData">刷新</el-button>
      </div>
    </div>

    <div class="workspace">
      <div ref="mapRef" class="map" :class="{ 'edit-enabled': editMode }" @click="openCreateAt">
        <img v-if="pond?.floorPlanUrl" :src="pond.floorPlanUrl" alt="" />
        <div v-else class="blank-map">
          <svg class="blank-pond" viewBox="0 0 100 100" preserveAspectRatio="xMidYMid meet" @mousemove="onPondMove">
            <defs>
              <radialGradient id="editor-water" cx="50%" cy="45%" r="70%">
                <stop offset="0%" stop-color="oklch(82% 0.09 165)" />
                <stop offset="45%" stop-color="oklch(68% 0.12 165)" />
                <stop offset="100%" stop-color="oklch(54% 0.11 165)" />
              </radialGradient>
              <radialGradient id="editor-shore" cx="50%" cy="50%" r="75%">
                <stop offset="55%" stop-color="rgba(255,255,255,0)" />
                <stop offset="100%" stop-color="rgba(33, 82, 68, 0.28)" />
              </radialGradient>
              <filter id="editor-shadow" x="-50%" y="-50%" width="200%" height="200%">
                <feDropShadow dx="0" dy="1.2" stdDeviation="1.2" flood-color="#153c35" flood-opacity="0.28" />
              </filter>
            </defs>
            <path
              class="pond-shape"
              d="M 50 8 C 74 8, 91 26, 91 50 C 91 72, 74 92, 50 92 C 24 92, 7 72, 7 50 C 7 26, 26 8, 50 8 Z"
              fill="url(#editor-water)"
              stroke="var(--fp-primary-light)"
              stroke-width="0.6"
            />
            <path
              d="M 50 8 C 74 8, 91 26, 91 50 C 91 72, 74 92, 50 92 C 24 92, 7 72, 7 50 C 7 26, 26 8, 50 8 Z"
              fill="url(#editor-shore)"
            />
            <ellipse cx="49" cy="50" rx="28" ry="28" fill="none" stroke="oklch(100% 0 0 / 0.22)" stroke-width="0.4" stroke-dasharray="2 2" />
            <ellipse cx="49" cy="50" rx="16" ry="16" fill="none" stroke="oklch(100% 0 0 / 0.18)" stroke-width="0.4" stroke-dasharray="1.5 2" />
            <text x="49" y="50" font-size="5" text-anchor="middle" fill="oklch(100% 0 0 / 0.55)" font-weight="800" font-family="var(--fp-font-display)">
              {{ pond?.name || '鱼塘' }}
            </text>
            <g transform="translate(86, 86)">
              <circle r="5" fill="#fff" stroke="var(--fp-primary-dark)" stroke-width="0.5" />
              <path d="M0 -4 L1.2 1.2 L0 0 L-1.2 1.2 Z" fill="var(--fp-primary-dark)" />
              <text y="-0.5" font-size="2.6" text-anchor="middle" fill="var(--fp-primary-dark)" font-weight="800">N</text>
            </g>
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
          </svg>
          <div class="blank-content">
            <p>暂无鱼塘平面图</p>
            <span>已启用默认示意图，点击任意位置即可添加钓位</span>
          </div>
        </div>
        <button
          v-for="(spot, index) in spotsWithPosition"
          :key="spot.id"
          class="spot-point"
          :class="[`status-${spot.status}`, { dragging: draggingId === spot.id }]"
          :style="[pointStyle(spot), { animationDelay: `${index * 50}ms` }]"
          type="button"
          @mousedown="startDrag($event, spot)"
          @click.stop="openEdit(spot)"
        >
          <svg class="spot-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round">
            <path d="M12 2C8.5 2 6 4.5 6 8c0 3.5 3.5 7.5 6 9.5 2.5-2 6-6 6-9.5 0-3.5-2.5-6-6-6z" />
            <circle cx="12" cy="8" r="2.5" fill="currentColor" stroke="none" />
          </svg>
          <span class="spot-label">{{ spot.spotCode }}</span>
        </button>
      </div>

      <aside class="side">
        <div class="side-head">
          <div>
            <h2>钓位列表</h2>
            <p class="side-desc">共 {{ spots.length }} 个钓位</p>
          </div>
          <el-button type="primary" v-fp-ripple @click="openCreateDialog">新增</el-button>
        </div>
        <el-empty v-if="spots.length === 0" description="暂无钓位，点击画布添加">
          <el-button type="primary" v-fp-ripple @click="openCreateDialog">添加首个钓位</el-button>
        </el-empty>
        <div v-else class="spot-rows">
          <div v-for="spot in sortedSpots" :key="spot.id" class="spot-row fp-lift">
            <div class="spot-info">
              <strong>{{ spot.spotCode }}</strong>
              <small>X {{ spot.coordinateX }}% · Y {{ spot.coordinateY }}%</small>
            </div>
            <el-tag :type="spot.status === 1 ? 'success' : spot.status === 2 ? 'warning' : 'info'">{{ spotStatusText[spot.status] }}</el-tag>
            <div class="row-actions">
              <el-button link type="primary" v-fp-ripple @click="openEdit(spot)">编辑</el-button>
              <el-dropdown>
                <el-button link v-fp-ripple>状态</el-button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item @click="setStatus(spot, 1)">可用</el-dropdown-item>
                    <el-dropdown-item @click="setStatus(spot, 2)">维修</el-dropdown-item>
                    <el-dropdown-item @click="setStatus(spot, 0)">禁用</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
              <el-button link type="danger" v-fp-ripple @click="remove(spot)">删除</el-button>
            </div>
          </div>
        </div>
      </aside>
    </div>

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
        <el-button type="primary" v-fp-ripple @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<script lang="ts">
import { Back } from '@element-plus/icons-vue'
export { Back }
</script>

<style scoped>
.editor-page {
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
  gap: 14px;
  flex-wrap: wrap;
  position: relative;
}

.workspace {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: var(--fp-space-5);
}

.map,
.side {
  border-radius: var(--fp-radius);
  background: var(--fp-surface);
  box-shadow: var(--fp-shadow-md);
  overflow: hidden;
  transform-style: preserve-3d;
}

.map {
  position: relative;
  min-height: 640px;
  cursor: crosshair;
}

.map.edit-enabled {
  cursor: crosshair;
}

.map:not(.edit-enabled) {
  cursor: default;
}

.map img {
  width: 100%;
  height: 100%;
  min-height: 640px;
  object-fit: cover;
  display: block;
}

.blank-map {
  position: relative;
  min-height: 640px;
  display: grid;
  place-items: center;
  background:
    linear-gradient(90deg, oklch(38% 0.09 165 / 0.08) 1px, transparent 1px),
    linear-gradient(0deg, oklch(38% 0.09 165 / 0.08) 1px, transparent 1px),
    linear-gradient(180deg, #f1f7f3 0%, #dfeede 100%);
  background-size: 36px 36px, 36px 36px, 100% 100%;
  overflow: hidden;
}

.blank-pond {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 100%;
  opacity: 0.9;
}

.blank-pond .pond-shape {
  filter: drop-shadow(0 4px 10px oklch(28% 0.06 165 / 0.18));
}

.blank-content {
  position: absolute;
  z-index: 2;
  inset: 50% auto auto 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  padding: var(--fp-space-5) var(--fp-space-6);
  border-radius: var(--fp-radius);
  background: oklch(100% 0 0 / 0.22);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  border: 1px solid oklch(100% 0 0 / 0.35);
  box-shadow: 0 12px 32px oklch(28% 0.06 165 / 0.18);
  color: var(--fp-text);
  pointer-events: none;
  max-width: 320px;
}

.blank-content p {
  margin: 0 0 6px;
  font-size: 18px;
  font-weight: 700;
  color: var(--fp-primary-dark);
  font-family: var(--fp-font-display);
  text-shadow: 0 1px 2px oklch(100% 0 0 / 0.8);
}

.blank-content span {
  color: var(--fp-primary);
  text-shadow: 0 1px 2px oklch(100% 0 0 / 0.8);
}

.spot-point {
  position: absolute;
  transform: translate(-50%, -50%);
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 2px;
  min-width: 56px;
  height: 56px;
  padding: 4px 8px;
  border: 3px solid white;
  border-radius: 999px;
  color: white;
  font-size: 12px;
  font-weight: 800;
  cursor: grab;
  box-shadow: 0 12px 24px oklch(28% 0.06 165 / 0.28);
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out);
  user-select: none;
  touch-action: none;
  opacity: 0;
  animation: popIn 0.45s var(--fp-ease-out) forwards;
}

.spot-icon {
  width: 16px;
  height: 16px;
  flex-shrink: 0;
  filter: drop-shadow(0 1px 1px oklch(0% 0 0 / 0.2));
}

.spot-label {
  line-height: 1;
  text-shadow: 0 1px 2px oklch(0% 0 0 / 0.25);
}

.spot-point:hover {
  transform: translate(-50%, -50%) scale(1.14);
  box-shadow: 0 16px 32px oklch(28% 0.06 165 / 0.34);
  z-index: 10;
}

.spot-point:active,
.spot-point.dragging {
  cursor: grabbing;
  transform: translate(-50%, -50%) scale(1.2);
}

.status-1 {
  background: linear-gradient(135deg, var(--fp-primary-light) 0%, var(--fp-primary) 100%);
}

.status-2 {
  background: linear-gradient(135deg, oklch(60% 0.12 85) 0%, var(--fp-accent-dark) 100%);
}

.status-0 {
  background: linear-gradient(135deg, var(--fp-muted) 0%, oklch(75% 0.03 165) 100%);
}

.side {
  align-self: start;
  padding: var(--fp-space-5);
}

.side-head {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: var(--fp-space-4);
}

.side-head h2 {
  margin: 0;
  color: var(--fp-text);
  font-family: var(--fp-font-display);
  font-size: 20px;
}

.side-desc {
  margin: 4px 0 0;
  color: var(--fp-muted);
  font-size: 13px;
}

.spot-rows {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.spot-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border-radius: var(--fp-radius-sm);
  border: 1px solid var(--fp-border);
  background: var(--fp-surface);
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out),
    border-color var(--fp-dur-fast) var(--fp-ease-out);
}

.spot-row:hover {
  border-color: oklch(38% 0.09 165 / 0.16);
}

.spot-info strong,
.spot-info small {
  display: block;
}

.spot-info strong {
  color: var(--fp-text);
  font-size: 15px;
}

.spot-info small {
  margin-top: 4px;
  color: var(--fp-muted);
  font-size: 12px;
}

.row-actions {
  display: flex;
  gap: 8px;
}

.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
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

@keyframes popIn {
  from {
    opacity: 0;
    transform: translate(-50%, -50%) scale(0.4);
  }
  to {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
  }
}

@media (max-width: 1000px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .page-hero {
    flex-direction: column;
    align-items: flex-start;
  }
}
</style>
