<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSlots, createSlot, updateSlot, deleteSlot, type TimeSlot, type TimeSlotParams } from '@/api/slots'
import { formatDateTime } from '@/utils/date'

const list = ref<TimeSlot[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<Partial<TimeSlotParams>>({
  slotDate: '',
  slotName: '',
  startTime: '',
  endTime: '',
  maxBookings: 10,
  advanceDays: 7,
  drawStartTime: '',
  drawEndTime: '',
  status: 1
})
const editId = ref<number | null>(null)

const load = async () => {
  loading.value = true
  const res = await getSlots({ pageSize: 100 })
  list.value = res.data.records
  loading.value = false
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.value = {
    slotDate: '',
    slotName: '早场',
    startTime: '06:00:00',
    endTime: '12:00:00',
    maxBookings: 10,
    advanceDays: 7,
    drawStartTime: '',
    drawEndTime: '',
    status: 1
  }
  dialogVisible.value = true
}

const openEdit = (row: TimeSlot) => {
  isEdit.value = true
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

const save = async () => {
  const data = form.value as TimeSlotParams
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
  <div class="admin-page">
    <div class="admin-header">
      <el-button icon="Back" circle @click="$router.push('/admin')" />
      <h3>时段管理</h3>
      <el-button type="primary" icon="Plus" circle @click="openCreate" />
    </div>

    <el-table :data="list" v-loading="loading" size="small" stripe>
      <el-table-column prop="slotDate" label="日期" width="110" />
      <el-table-column prop="slotName" label="场次" width="80" />
      <el-table-column prop="startTime" label="开始" width="80" />
      <el-table-column prop="endTime" label="结束" width="80" />
      <el-table-column prop="maxBookings" label="名额" width="70" />
      <el-table-column prop="status" label="状态" width="70">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="danger" size="small" @click="del(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑时段' : '新增时段'" width="90%">
      <el-form :model="form" label-width="90px" size="small">
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
        <el-form-item label="时间">
          <el-time-picker v-model="form.startTime" value-format="HH:mm:ss" placeholder="开始" style="width: 48%" />
          <span style="margin: 0 4%">-</span>
          <el-time-picker v-model="form.endTime" value-format="HH:mm:ss" placeholder="结束" style="width: 48%" />
        </el-form-item>
        <el-form-item label="最大预约数">
          <el-input-number v-model="form.maxBookings" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="提前天数">
          <el-input-number v-model="form.advanceDays" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="抽号开始">
          <el-date-picker v-model="form.drawStartTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="抽号结束">
          <el-date-picker v-model="form.drawEndTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">启用</el-radio>
            <el-radio :label="0">禁用</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button size="small" @click="dialogVisible = false">取消</el-button>
        <el-button size="small" type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.admin-page {
  min-height: 100vh;
  padding: 16px;
  background: var(--bg);
}

.admin-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}

.admin-header h3 {
  font-size: 18px;
  color: var(--primary);
}
</style>
