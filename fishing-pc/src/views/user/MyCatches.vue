<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { RefreshRight } from '@element-plus/icons-vue'
import { getPonds, type Pond } from '@/api/pond'
import { createCatchRecord, getMyCatches, requestRecycle, catchStatusLabels, type CatchRecord } from '@/api/catch'
import { getMyReservations, type ReservationVO } from '@/api/reservation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const ponds = ref<Pond[]>([])
const reservations = ref<ReservationVO[]>([])
const catches = ref<CatchRecord[]>([])

const form = reactive({
  pondId: undefined as number | undefined,
  reservationId: undefined as number | undefined,
  spotId: undefined as number | undefined,
  fishType: '',
  weight: 1,
  quantity: 1,
  imageUrl: '/demo-assets/catches/catch-1.svg'
})

const assignedReservations = computed(() => reservations.value.filter((item) => item.spotId && item.pondId && !String(item.status).includes('取消')))

const selectedReservation = computed(() => assignedReservations.value.find((item) => item.id === form.reservationId))

const selectablePonds = computed(() => {
  const ids = new Set(assignedReservations.value.map((r) => r.pondId).filter(Boolean))
  if (!ids.size) return ponds.value
  return ponds.value.filter((p) => ids.has(p.id))
})

watch(
  selectedReservation,
  (item) => {
    if (!item) return
    form.pondId = item.pondId || undefined
    form.spotId = item.spotId || undefined
  },
  { immediate: true }
)

const loadData = async () => {
  loading.value = true
  try {
    const [pondRes, reservationRes, catchRes] = await Promise.all([getPonds(), getMyReservations(), getMyCatches()])
    ponds.value = pondRes.data || []
    reservations.value = reservationRes.data || []
    catches.value = catchRes.data || []
    const queryReservationId = Number(route.query.reservationId)
    if (queryReservationId && !form.reservationId) {
      form.reservationId = queryReservationId
    }
  } finally {
    loading.value = false
  }
}

const submit = async () => {
  if (!form.pondId) {
    ElMessage.warning('请选择鱼塘')
    return
  }
  if (!form.fishType.trim()) {
    ElMessage.warning('请填写鱼种')
    return
  }
  submitting.value = true
  try {
    const res = await createCatchRecord({
      pondId: form.pondId,
      reservationId: form.reservationId || null,
      spotId: form.spotId || null,
      fishType: form.fishType.trim(),
      weight: form.weight,
      quantity: form.quantity,
      imageUrl: form.imageUrl || null
    })
    const newId = res.data
    ElMessage.success('渔获已提交')
    form.fishType = ''
    form.weight = 1
    form.quantity = 1
    await loadData()
    if (newId) {
      try {
        await ElMessageBox.confirm('渔获提交成功，是否立即申请商家回收？', '申请回收', {
          type: 'success',
          confirmButtonText: '立即申请',
          cancelButtonText: '暂不申请'
        })
        await requestRecycle(newId)
        ElMessage.success('已申请回收')
        await loadData()
      } catch (e: any) {
        if (e !== 'cancel') {
          ElMessage.error(e.message || '申请回收失败')
        }
      }
    }
  } finally {
    submitting.value = false
  }
}

const applyRecycle = async (item: CatchRecord) => {
  await requestRecycle(item.id)
  ElMessage.success('已申请回收')
  await loadData()
}

const pondName = (id?: number | null) => ponds.value.find((pond) => pond.id === id)?.name || '鱼塘'
const money = (value?: number | null) => (value == null ? '-' : `¥${(Number(value) / 100).toFixed(2)}`)

onMounted(loadData)
</script>

<template>
  <section class="page catch-page">
    <div class="page-header catch-header">
      <div>
        <p class="eyebrow">渔获回收</p>
        <h1>提交渔获，申请商家回收</h1>
      </div>
      <el-button @click="loadData">刷新数据</el-button>
    </div>

    <div class="workspace">
      <el-form class="form-card" label-position="top">
        <h2>提交渔获</h2>
        <el-form-item label="关联预约">
          <el-select v-model="form.reservationId" clearable placeholder="可选：从已分配钓位的预约带入">
            <el-option
              v-for="item in assignedReservations"
              :key="item.id"
              :label="`${item.pondName || pondName(item.pondId)} · ${item.slotDate} · 钓位 ${item.spotCode}`"
              :value="item.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="鱼塘">
          <el-select v-model="form.pondId" placeholder="选择鱼塘" :disabled="!!selectedReservation">
            <el-option v-for="pond in selectablePonds" :key="pond.id" :label="pond.name" :value="pond.id" />
          </el-select>
          <small v-if="selectedReservation" class="field-hint">已由关联预约自动带入</small>
        </el-form-item>
        <el-form-item label="钓位">
          <el-input :model-value="selectedReservation?.spotCode || form.spotId || '未绑定'" disabled />
        </el-form-item>
        <el-form-item label="鱼种">
          <el-input v-model="form.fishType" placeholder="例如：鲈鱼、草鱼、鲫鱼" />
        </el-form-item>
        <div class="inline">
          <el-form-item label="重量（kg）">
            <el-input-number v-model="form.weight" :min="0.1" :step="0.1" :precision="1" />
          </el-form-item>
          <el-form-item label="数量">
            <el-input-number v-model="form.quantity" :min="1" :step="1" />
          </el-form-item>
        </div>
        <el-form-item label="图片 URL">
          <el-input v-model="form.imageUrl" placeholder="/demo-assets/catches/catch-1.svg" />
        </el-form-item>
        <el-button type="primary" :loading="submitting" @click="submit">提交渔获</el-button>
      </el-form>

      <div class="list-card catch-list" v-loading="loading">
        <h2>我的渔获</h2>
        <el-empty v-if="catches.length === 0 && !loading" description="暂无渔获记录" />
        <article v-for="item in catches" :key="item.id" class="catch-item">
          <img :src="item.imageUrl || '/demo-assets/catches/catch-1.svg'" alt="" />
          <div class="catch-main">
            <div class="catch-title">
              <strong>{{ item.fishType }}</strong>
              <el-tag>{{ catchStatusLabels[item.status] || item.status }}</el-tag>
            </div>
            <p>{{ pondName(item.pondId) }} · 钓位 {{ item.spotCode || item.spotId || '-' }}</p>
            <p>{{ item.weight }}kg · {{ item.quantity }} 条 · 回收价 {{ money(item.recyclePrice) }}</p>
            <small>{{ item.createTime }}</small>
          </div>
          <el-button v-if="item.status === 'pending'" type="success" plain :icon="RefreshRight" @click="applyRecycle(item)">申请回收</el-button>
        </article>
      </div>
    </div>
  </section>
</template>

<style scoped>
.page {
  width: min(1220px, calc(100% - 32px));
  margin: 22px auto 48px;
}

.page-header,
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

h1,
h2 {
  margin: 0;
  color: #172521;
}

.workspace {
  display: grid;
  grid-template-columns: 380px 1fr;
  gap: 16px;
}

.form-card,
.list-card {
  padding: 18px;
  border: 1px solid #e1ebe5;
  border-radius: 8px;
  background: white;
  box-shadow: 0 14px 30px rgba(21, 60, 53, 0.08);
}

.form-card h2,
.list-card h2 {
  margin-bottom: 16px;
}

.inline {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.catch-item {
  display: grid;
  grid-template-columns: 108px 1fr auto;
  gap: 14px;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #edf3ef;
}

.catch-item img {
  width: 108px;
  height: 78px;
  border-radius: 8px;
  object-fit: cover;
  background: #f4f8f5;
}

.catch-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 6px;
}

.catch-main p,
.catch-main small {
  margin: 4px 0 0;
  color: #66766f;
}

.field-hint {
  display: block;
  margin-top: 6px;
  color: #8aa89b;
  font-size: 12px;
}

@media (max-width: 900px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .catch-item {
    grid-template-columns: 90px 1fr;
  }

  .catch-item .el-button {
    grid-column: 1 / -1;
  }
}
</style>
