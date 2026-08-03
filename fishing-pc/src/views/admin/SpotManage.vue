<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSpots, createSpot, updateSpot, deleteSpot, type FishingSpot, type FishingSpotParams } from '@/api/admin'

const list = ref<FishingSpot[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = reactive<Partial<FishingSpotParams>>({ spotCode: '', status: 1 })
const batchCodes = ref('')

const load = async () => {
  loading.value = true
  const res = await getSpots({ pageSize: 100 })
  list.value = res.data.records
  loading.value = false
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.spotCode = ''
  form.status = 1
  dialogVisible.value = true
}

const openBatch = () => {
  batchCodes.value = ''
  batchDialogVisible.value = true
}

const openEdit = (row: FishingSpot) => {
  isEdit.value = true
  editId.value = row.id
  form.spotCode = row.spotCode
  form.status = row.status
  dialogVisible.value = true
}

const save = async () => {
  const data = form as FishingSpotParams
  if (isEdit.value && editId.value) {
    await updateSpot(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createSpot(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const saveBatch = async () => {
  if (!batchCodes.value.trim()) {
    ElMessage.warning('请输入钓位编号')
    return
  }
  const codes = batchCodes.value.split(/[,，\n]/).map(s => s.trim()).filter(Boolean)
  if (codes.length === 0) {
    ElMessage.warning('请输入有效的钓位编号')
    return
  }
  let success = 0
  let fail = 0
  for (const code of codes) {
    try {
      await createSpot({ spotCode: code, status: 1 })
      success++
    } catch {
      fail++
    }
  }
  ElMessage.success(`批量新增完成：成功 ${success} 个，失败 ${fail} 个`)
  batchDialogVisible.value = false
  load()
}

const del = async (row: FishingSpot) => {
  try {
    await ElMessageBox.confirm('确定删除该钓位吗？', '提示', { type: 'warning' })
    await deleteSpot(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

const toggleStatus = async (row: FishingSpot) => {
  await updateSpot(row.id, { spotCode: row.spotCode, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('状态更新成功')
  load()
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>钓位管理</h2>
      <div>
        <el-button type="success" icon="Plus" @click="openBatch">批量新增</el-button>
        <el-button type="primary" icon="Plus" @click="openCreate">新增钓位</el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="spotCode" label="钓位编号" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark">
              {{ row.status === 1 ? '可用' : '维修中' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="240">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '设为维修' : '设为可用' }}
            </el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑钓位' : '新增钓位'" width="400px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="钓位编号">
          <el-input v-model="form.spotCode" placeholder="如 A01" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">可用</el-radio>
            <el-radio :label="0">维修中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量新增钓位" width="500px">
      <el-form label-width="100px">
        <el-form-item label="钓位编号">
          <el-input
            v-model="batchCodes"
            type="textarea"
            :rows="6"
            placeholder="多个编号可用逗号或换行分隔，例如：A01,A02,A03"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBatch">确认新增</el-button>
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
