<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getPonds, createPond, updatePond, deletePond, type Pond } from '@/api/ponds'
import { useUserStore } from '@/store/user'

const userStore = useUserStore()
const list = ref<Pond[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<any>(null)

const form = reactive<Pond>({
  id: 0,
  name: '',
  address: '',
  phone: '',
  status: 1
})

const rules = {
  name: [{ required: true, message: '请输入鱼塘名称', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await getPonds()
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.address = ''
  form.phone = ''
  form.status = 1
  dialogVisible.value = true
}

const openEdit = (row: Pond) => {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.address = row.address || ''
  form.phone = row.phone || ''
  form.status = row.status
  dialogVisible.value = true
}

const save = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const data = { name: form.name, address: form.address, phone: form.phone, status: form.status }
  if (isEdit.value && editId.value) {
    await updatePond(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createPond(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const del = async (row: Pond) => {
  try {
    await ElMessageBox.confirm(`确定删除鱼塘“${row.name}”吗？`, '提示', { type: 'warning' })
    await deletePond(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>鱼塘管理</h2>
      <el-button v-if="userStore.isSuperAdmin" type="primary" icon="Plus" @click="openCreate">新增鱼塘</el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="name" label="鱼塘名称" min-width="150" />
        <el-table-column prop="address" label="地址" min-width="200" />
        <el-table-column prop="phone" label="联系电话" width="140" />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column v-if="userStore.isSuperAdmin" label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑鱼塘' : '新增鱼塘'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="鱼塘名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model="form.address" />
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="form.phone" />
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
