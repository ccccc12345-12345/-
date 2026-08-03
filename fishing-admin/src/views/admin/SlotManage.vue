<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSlots, createSlot, updateSlot, deleteSlot, type TimeSlot, type TimeSlotParams } from '@/api/slots'
import { formatDateTime } from '@/utils/date'

const list = ref<TimeSlot[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<any>(null)
const form = reactive<Partial<TimeSlotParams>>({
  slotDate: '', slotName: '早场', startTime: '', endTime: '',
  maxBookings: 10, advanceDays: 7, drawStartTime: '', drawEndTime: '', status: 1,
  defaultPrice: undefined
})

const rules = {
  slotDate: [{ required: true, message: '请选择日期', trigger: 'change' }],
  slotName: [{ required: true, message: '请选择场次', trigger: 'change' }],
  startTime: [{ required: true, message: '请选择开始时间', trigger: 'change' }],
  endTime: [{ required: true, message: '请选择结束时间', trigger: 'change' }],
  drawStartTime: [{ required: true, message: '请选择抽号开始时间', trigger: 'change' }],
  drawEndTime: [{ required: true, message: '请选择抽号结束时间', trigger: 'change' }]
}

const load = async () => {
  loading.value = true
  const res = await getSlots({ pageSize: 100 })
  list.value = res.data.records
  loading.value = false
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  Object.assign(form, {
    slotDate: '', slotName: '早场', startTime: '06:00:00', endTime: '12:00:00',
    maxBookings: 10, advanceDays: 7, drawStartTime: '', drawEndTime: '', status: 1,
    defaultPrice: undefined
  })
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

  const data = form as TimeSlotParams
  if (isEdit.value && editId.value) {
    await updateSlot(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createSlot(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const del = async (row: TimeSlot) => {
  try {
    await ElMessageBox.confirm('确定删除该时段吗？', '提示', { type: 'warning' })
    await deleteSlot(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>时段配置</h2>
      <el-button type="primary" icon="Plus" @click="openCreate">新增时段</el-button>
    </div>

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
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑时段' : '新增时段'" width="600px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-form-item label="日期">
          <el-date-picker v-model="form.slotDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
        <el-form-item label="场次">
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
}
</style>
