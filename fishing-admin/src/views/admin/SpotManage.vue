<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSpots, createSpot, updateSpot, deleteSpot, type FishingSpot, type FishingSpotParams } from '@/api/spots'
import { usePondStore } from '@/store/pond'

const pondStore = usePondStore()

const list = ref<FishingSpot[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const form = reactive<Partial<FishingSpotParams>>({ spotCode: '', status: 1, pondId: pondStore.currentPondId })

const pondName = (pondId: number | null) => {
  return pondStore.ponds.find(p => p.id === pondId)?.name || '-'
}

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
  form.pondId = pondStore.currentPondId
  dialogVisible.value = true
}

const openEdit = (row: FishingSpot) => {
  isEdit.value = true
  editId.value = row.id
  form.spotCode = row.spotCode
  form.status = row.status
  form.pondId = row.pondId ?? pondStore.currentPondId
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

const batchImport = () => {
  ElMessage.info('批量导入功能待扩展，可上传 Excel 文件')
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>钓位管理</h2>
      <div>
        <el-button type="success" icon="Upload" @click="batchImport">批量导入</el-button>
        <el-button type="primary" icon="Plus" @click="openCreate">新增钓位</el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="spotCode" label="钓位编号" />
        <el-table-column label="鱼塘" width="140">
          <template #default="{ row }">{{ pondName(row.pondId) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" effect="dark">
              {{ row.status === 1 ? '可用' : '维修/禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑钓位' : '新增钓位'" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="钓位编号">
          <el-input v-model="form.spotCode" placeholder="如 A01" />
        </el-form-item>
        <el-form-item label="所属鱼塘">
          <el-select v-model="form.pondId" disabled style="width: 100%">
            <el-option
              v-for="pond in pondStore.ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">可用</el-radio>
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
