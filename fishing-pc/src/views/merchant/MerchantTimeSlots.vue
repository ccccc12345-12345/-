<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMerchantTimeSlots,
  createMerchantTimeSlot,
  updateMerchantTimeSlot,
  deleteMerchantTimeSlot,
  getMerchantPonds,
  type TimeSlot,
  type TimeSlotParams
} from '@/api/merchant'
import type { Pond } from '@/api/pond'
import { formatDateTime } from '@/utils/date'

const list = ref<TimeSlot[]>([])
const ponds = ref<Pond[]>([])
const loading = ref(false)
const pondsLoading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<any>(null)
const total = ref(0)
const query = reactive({
  pondId: undefined as number | undefined,
  slotDate: '',
  pageNum: 1,
  pageSize: 20
})

const form = reactive<Partial<TimeSlotParams>>({
  pondId: undefined,
  slotDate: '', slotName: '早场', startTime: '06:00:00', endTime: '12:00:00',
  maxBookings: 10, advanceDays: 7, drawStartTime: '', drawEndTime: '', status: 1,
  defaultPrice: undefined
})

const rules = {
  pondId: [{ required: true, message: '请选择鱼塘', trigger: 'change' }],
  slotDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  slotName: [{ required: true, message: '请选择场次', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  drawStartTime: [{ required: true, message: '请选择抽号开始时间', trigger: 'change' }],
  drawEndTime: [{ required: true, message: '请选择抽号结束时间', trigger: 'change' }]
}

const currentPondName = computed(() => ponds.value.find((p) => p.id === query.pondId)?.name || '')

const loadPonds = async () => {
  pondsLoading.value = true
  try {
    const res = await getMerchantPonds()
    ponds.value = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    if (ponds.value.length && !query.pondId) {
      query.pondId = ponds.value[0].id
    }
  } finally {
    pondsLoading.value = false
  }
}

const load = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: query.pageNum, pageSize: query.pageSize }
    if (query.pondId) params.pondId = query.pondId
    if (query.slotDate) params.slotDate = query.slotDate
    const res = await getMerchantTimeSlots(params)
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  Object.assign(form, {
    pondId: query.pondId,
    slotDate: '', slotName: '早场', startTime: '06:00:00', endTime: '12:00:00',
    maxBookings: 10, advanceDays: 7, drawStartTime: '', drawEndTime: '', status: 1,
    defaultPrice: undefined
  })
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row: TimeSlot) => {
  isEdit.value = true
  editId.value = row.id
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const save = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const data = { ...form } as TimeSlotParams
  if (isEdit.value && editId.value) {
    await updateMerchantTimeSlot(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createMerchantTimeSlot(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const del = async (row: TimeSlot) => {
  try {
    await ElMessageBox.confirm('确定删除该时段吗？', '提示', { type: 'warning' })
    await deleteMerchantTimeSlot(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

onMounted(async () => {
  await loadPonds()
  load()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>时段配置</h2>
      <el-button type="primary" icon="Plus" @click="openCreate">新增时段</el-button>
    </div>

    <el-card class="filter-card">
      <el-form :model="query" inline>
        <el-form-item label="鱼塘">
          <el-select v-model="query.pondId" :loading="pondsLoading" placeholder="选择鱼塘" style="width: 180px" @change="query.pageNum = 1; load()">
            <el-option v-for="pond in ponds" :key="pond.id" :label="pond.name" :value="pond.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期">
          <el-date-picker v-model="query.slotDate" type="date" value-format="YYYY-MM-DD" placeholder="选择日期" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="query.pageNum = 1; load()">查询</el-button>
          <el-button icon="Refresh" @click="query.slotDate = ''; query.pageNum = 1; load()">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="slotDate" label="日期" width="120" />
        <el-table-column prop="slotName" label="场次" width="100" />
        <el-table-column prop="startTime" label="开始时间" width="110" />
        <el-table-column prop="endTime" label="结束时间" width="110" />
        <el-table-column prop="maxBookings" label="最大预约数" width="110" />
        <el-table-column prop="advanceDays" label="提前天数" width="100" />
        <el-table-column label="默认票价" width="110">
          <template #default="{ row }">{{ row.defaultPrice != null ? `¥${row.defaultPrice}` : '-' }}</template>
        </el-table-column>
        <el-table-column label="抽号开始" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.drawStartTime) }}</template>
        </el-table-column>
        <el-table-column label="抽号结束" min-width="150">
          <template #default="{ row }">{{ formatDateTime(row.drawEndTime) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination
        v-model:current-page="query.pageNum"
        :page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        style="margin-top: 16px; justify-content: flex-end"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑时段' : '新增时段'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="鱼塘" prop="pondId">
          <el-select v-model="form.pondId" :loading="pondsLoading" placeholder="选择鱼塘" style="width: 100%" :disabled="isEdit">
            <el-option v-for="pond in ponds" :key="pond.id" :label="pond.name" :value="pond.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="日期" prop="slotDate">
          <el-date-picker v-model="form.slotDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="场次" prop="slotName">
          <el-select v-model="form.slotName" style="width: 100%">
            <el-option label="早场" value="早场" />
            <el-option label="午场" value="午场" />
            <el-option label="晚场" value="晚场" />
            <el-option label="全天场" value="全天场" />
          </el-select>
        </el-form-item>
        <el-form-item label="时间范围">
          <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" placeholder="开始" style="width: 48%" />
          <span style="margin: 0 2%">-</span>
          <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" placeholder="结束" style="width: 48%" />
        </el-form-item>
        <el-form-item label="最大预约人数">
          <el-input-number v-model="form.maxBookings" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="提前预约天数">
          <el-input-number v-model="form.advanceDays" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="抽号开始时间">
          <el-date-picker v-model="form.drawStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="抽号结束时间">
          <el-date-picker v-model="form.drawEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="默认票价">
          <el-input-number v-model="form.defaultPrice" :min="0" :precision="2" placeholder="留空表示无默认票价" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.page-container {
  padding-bottom: 28px;
}
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  padding: 26px 30px;
  border-radius: 16px;
  background: linear-gradient(135deg, #153c35 0%, #1d5244 100%);
  color: #fff;
  box-shadow: 0 20px 50px rgba(21, 60, 53, 0.22);
}
.page-header h2 {
  margin: 0;
  font-family: 'Noto Serif SC', 'Source Han Serif SC', serif;
  font-size: 26px;
  font-weight: 700;
  letter-spacing: 0.5px;
}
.filter-card {
  margin-bottom: 20px;
  border-radius: 14px;
  border: none;
  background: rgba(255, 255, 255, 0.92);
  box-shadow: 0 10px 34px rgba(21, 60, 53, 0.08);
  backdrop-filter: blur(10px);
}
:deep(.el-card) {
  border-radius: 14px;
  border: none;
  background: rgba(255, 255, 255, 0.95);
  box-shadow: 0 14px 40px rgba(21, 60, 53, 0.09);
}
:deep(.el-button--primary) {
  background: linear-gradient(135deg, #1d6a56 0%, #268f73 100%);
  border: none;
  box-shadow: 0 6px 18px rgba(29, 106, 86, 0.28);
}
:deep(.el-button--primary:hover) {
  background: linear-gradient(135deg, #164f40 0%, #1d6a56 100%);
}
</style>
