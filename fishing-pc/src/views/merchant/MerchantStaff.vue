<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMerchantStaffs,
  createMerchantStaff,
  updateMerchantStaff,
  updateMerchantStaffStatus,
  resetMerchantStaffPassword,
  deleteMerchantStaff,
  type MerchantStaff
} from '@/api/merchant'
import { formatDateTime } from '@/utils/date'

const list = ref<MerchantStaff[]>([])
const loading = ref(false)
const keyword = ref('')

const pageNum = ref(1)
const pageSize = ref(20)
const total = ref(0)

const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<any>(null)

const form = reactive({
  staffName: '',
  phone: '',
  role: '',
  password: ''
})

const roleOptions = [
  { value: 'checker', label: '核销员' },
  { value: 'operator', label: '运营' },
  { value: 'finance', label: '财务' },
  { value: 'manager', label: '店长' }
]

const roleName = (role: string) => roleOptions.find(r => r.value === role)?.label || role

const phonePattern = /^1[3-9]\d{9}$/

const rules = {
  staffName: [{ required: true, message: '请输入员工姓名', trigger: 'blur' }],
  phone: [
    { required: true, message: '请输入手机号', trigger: 'blur' },
    { pattern: phonePattern, message: '手机号格式不正确', trigger: 'blur' }
  ],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }],
  password: [
    { required: !isEdit.value, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码长度不能少于6位', trigger: 'blur' }
  ]
}

const load = async () => {
  loading.value = true
  try {
    const params: any = { pageNum: pageNum.value, pageSize: pageSize.value }
    if (keyword.value) params.keyword = keyword.value
    const res = await getMerchantStaffs(params)
    const data = res.data
    list.value = Array.isArray(data) ? data : (data?.records || [])
    total.value = Array.isArray(data) ? data.length : (data?.total || 0)
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  pageNum.value = 1
  load()
}

const onPageChange = (p: number) => {
  pageNum.value = p
  load()
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.staffName = ''
  form.phone = ''
  form.role = ''
  form.password = ''
  dialogVisible.value = true
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

const openEdit = (row: MerchantStaff) => {
  isEdit.value = true
  editId.value = row.id
  form.staffName = row.staffName
  form.phone = row.phone
  form.role = row.role
  form.password = ''
  dialogVisible.value = true
  setTimeout(() => formRef.value?.clearValidate(), 0)
}

const save = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  const data = {
    staffName: form.staffName.trim(),
    phone: form.phone.trim(),
    role: form.role
  }

  if (isEdit.value && editId.value) {
    await updateMerchantStaff(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createMerchantStaff({ ...data, password: form.password } as any)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const toggleStatus = async (row: MerchantStaff) => {
  const nextStatus = row.status === 1 ? 0 : 1
  const actionText = nextStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定${actionText}员工“${row.staffName}”吗？`, '提示', { type: 'warning' })
    await updateMerchantStaffStatus(row.id, nextStatus)
    ElMessage.success(`${actionText}成功`)
    load()
  } catch {}
}

const confirmResetPassword = async (row: MerchantStaff) => {
  try {
    await ElMessageBox.confirm(`确定重置员工“${row.staffName}”的密码吗？`, '提示', { type: 'warning' })
    const res = await resetMerchantStaffPassword(row.id)
    ElMessageBox.alert(`员工“${row.staffName}”的新密码为：${res.data}`, '重置成功', { type: 'success' })
  } catch {}
}

const del = async (row: MerchantStaff) => {
  try {
    await ElMessageBox.confirm(`确定删除员工“${row.staffName}”吗？删除后该账号将不可用。`, '提示', { type: 'warning' })
    await deleteMerchantStaff(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>员工账号管理</h2>
      <div class="header-actions">
        <el-input
          v-model="keyword"
          placeholder="搜索姓名或手机号"
          style="width: 240px; margin-right: 12px"
          clearable
          @keyup.enter="onSearch"
        >
          <template #append>
            <el-button icon="Search" @click="onSearch" />
          </template>
        </el-input>
        <el-button type="primary" icon="Plus" class="btn-add" @click="openCreate">添加员工</el-button>
      </div>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column type="index" label="序号" width="60" />
        <el-table-column prop="staffName" label="姓名" min-width="120" />
        <el-table-column prop="phone" label="手机号" min-width="140" />
        <el-table-column label="角色" min-width="120">
          <template #default="{ row }">
            {{ roleName(row.role) }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" min-width="160">
          <template #default="{ row }">
            {{ formatDateTime(row.createTime) }}
          </template>
        </el-table-column>
        <el-table-column label="操作" width="260" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="warning" @click="confirmResetPassword(row)">重置密码</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="pageNum"
          v-model:page-size="pageSize"
          :total="total"
          layout="total, prev, pager, next, jumper"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑员工' : '添加员工'" width="500px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="员工姓名" prop="staffName">
          <el-input v-model="form.staffName" maxlength="64" show-word-limit />
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="role">
          <el-select v-model="form.role" placeholder="请选择角色" style="width: 100%">
            <el-option v-for="opt in roleOptions" :key="opt.value" :label="opt.label" :value="opt.value" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="!isEdit" label="初始密码" prop="password">
          <el-input v-model="form.password" type="password" show-password />
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
  margin: 0;
}

.header-actions {
  display: flex;
  align-items: center;
}

.pagination-wrap {
  display: flex;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn-add {
  background-color: #0d5c4e;
  border-color: #0d5c4e;
}

.btn-add:hover,
.btn-add:focus {
  background-color: #117a66;
  border-color: #117a66;
}
</style>
