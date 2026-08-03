<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMerchantSpots,
  createMerchantSpot,
  updateMerchantSpot,
  deleteMerchantSpot,
  batchCreateMerchantSpots,
  type FishingSpot,
  type FishingSpotParams
} from '@/api/merchant'
import { usePondStore } from '@/store/pond'

const pondStore = usePondStore()

const list = ref<FishingSpot[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const batchDialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const total = ref(0)
const query = reactive({ pageNum: 1, pageSize: 50 })

const form = reactive<Partial<FishingSpotParams>>({ spotCode: '', status: 1, pondId: pondStore.currentPondId })
const batchForm = reactive({ prefix: '', startNum: 1, endNum: 10 })

const load = async () => {
  loading.value = true
  try {
    const res = await getMerchantSpots({ pageNum: query.pageNum, pageSize: query.pageSize })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
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
  const data = { ...form, pondId: pondStore.currentPondId } as FishingSpotParams
  if (isEdit.value && editId.value) {
    await updateMerchantSpot(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createMerchantSpot(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const del = async (row: FishingSpot) => {
  try {
    await ElMessageBox.confirm('确定删除该钓位吗？', '提示', { type: 'warning' })
    await deleteMerchantSpot(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

const toggleStatus = async (row: FishingSpot) => {
  await updateMerchantSpot(row.id, { spotCode: row.spotCode, status: row.status === 1 ? 0 : 1 })
  ElMessage.success('状态更新成功')
  load()
}

const openBatch = () => {
  batchForm.prefix = ''
  batchForm.startNum = 1
  batchForm.endNum = 10
  batchDialogVisible.value = true
}

const submitBatch = async () => {
  if (!batchForm.prefix || batchForm.startNum > batchForm.endNum) {
    ElMessage.warning('请填写完整且结束编号不小于起始编号')
    return
  }
  await batchCreateMerchantSpots({
    prefix: batchForm.prefix,
    startNum: batchForm.startNum,
    endNum: batchForm.endNum,
    pondId: pondStore.currentPondId ?? undefined
  })
  ElMessage.success('批量新增成功')
  batchDialogVisible.value = false
  load()
}

onMounted(load)
watch(() => pondStore.currentPondId, () => {
  query.pageNum = 1
  load()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>钓位管理</h2>
      <div>
        <el-select v-model="pondStore.currentPondId" placeholder="选择鱼塘" style="width: 160px; margin-right: 12px">
          <el-option
            v-for="pond in pondStore.ponds"
            :key="pond.id"
            :label="pond.name"
            :value="pond.id"
          />
        </el-select>
        <el-button type="success" icon="DocumentCopy" @click="openBatch">批量新增</el-button>
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
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '维修' : '恢复' }}
            </el-button>
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

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑钓位' : '新增钓位'" width="420px">
      <el-form :model="form" label-width="90px">
        <el-form-item label="钓位编号">
          <el-input v-model="form.spotCode" placeholder="如 A01" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">可用</el-radio>
            <el-radio :value="0">维修中</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="batchDialogVisible" title="批量新增钓位" width="420px">
      <el-form :model="batchForm" label-width="100px">
        <el-form-item label="编号前缀">
          <el-input v-model="batchForm.prefix" placeholder="如 A" />
        </el-form-item>
        <el-form-item label="起始编号">
          <el-input-number v-model="batchForm.startNum" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束编号">
          <el-input-number v-model="batchForm.endNum" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="batchDialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitBatch">确定</el-button>
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
  flex-wrap: wrap;
  gap: 12px;
}

.page-header h2 {
  color: #0f4c75;
  margin: 0;
}
</style>
