<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getSpots, createSpot, updateSpot, deleteSpot, type FishingSpot, type FishingSpotParams } from '@/api/spots'

const list = ref<FishingSpot[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = ref<Partial<FishingSpotParams>>({ spotCode: '', status: 1 })
const editId = ref<number | null>(null)

const load = async () => {
  loading.value = true
  const res = await getSpots({ pageSize: 100 })
  list.value = res.data.records
  loading.value = false
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.value = { spotCode: '', status: 1 }
  dialogVisible.value = true
}

const openEdit = (row: FishingSpot) => {
  isEdit.value = true
  editId.value = row.id
  form.value = { ...row }
  dialogVisible.value = true
}

const save = async () => {
  const data = form.value as FishingSpotParams
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

onMounted(load)
</script>

<template>
  <div class="admin-page">
    <div class="admin-header">
      <el-button icon="Back" circle @click="$router.push('/admin')" />
      <h3>钓位管理</h3>
      <el-button type="primary" icon="Plus" circle @click="openCreate" />
    </div>

    <el-table :data="list" v-loading="loading" size="small" stripe>
      <el-table-column prop="spotCode" label="钓位编号" />
      <el-table-column prop="status" label="状态">
        <template #default="{ row }">
          <el-tag size="small" :type="row.status === 1 ? 'success' : 'danger'">
            {{ row.status === 1 ? '可用' : '禁用' }}
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑钓位' : '新增钓位'" width="90%">
      <el-form :model="form" label-width="80px" size="small">
        <el-form-item label="编号">
          <el-input v-model="form.spotCode" placeholder="如 A01" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :label="1">可用</el-radio>
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
