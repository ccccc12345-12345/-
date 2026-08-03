<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getUsers, updateAdminBinding, updateUserStatus, type SysUser } from '@/api/users'
import { getPonds, type Pond } from '@/api/ponds'

const list = ref<SysUser[]>([])
const ponds = ref<Pond[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentUser = ref<SysUser | null>(null)
const bindingForm = reactive({ adminType: 0, pondId: null as number | null })
const query = reactive({
  keyword: '',
  pageNum: 1,
  pageSize: 10,
  sortField: 'create_time',
  sortOrder: 'descending' as 'ascending' | 'descending'
})
const total = ref(0)

const load = async () => {
  loading.value = true
  try {
    const res = await getUsers({
      keyword: query.keyword,
      pageNum: query.pageNum,
      pageSize: query.pageSize,
      sortField: query.sortField,
      sortOrder: query.sortOrder === 'descending' ? 'desc' : 'asc'
    })
    list.value = res.data.records || []
    total.value = res.data.total || 0
  } finally {
    loading.value = false
  }
}

const loadPonds = async () => {
  const res = await getPonds()
  ponds.value = res.data || []
}

const openBinding = (row: SysUser) => {
  currentUser.value = row
  bindingForm.adminType = row.adminType ?? 0
  bindingForm.pondId = row.pondId ?? null
  dialogVisible.value = true
}

const saveBinding = async () => {
  if (!currentUser.value) return
  await updateAdminBinding(currentUser.value.id, {
    adminType: bindingForm.adminType,
    pondId: bindingForm.adminType === 1 ? bindingForm.pondId : null
  })
  ElMessage.success('权限配置已更新')
  dialogVisible.value = false
  load()
}

const toggleStatus = async (row: SysUser) => {
  const newStatus = row.status === 1 ? 0 : 1
  const action = newStatus === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(`确定${action}该用户吗？`, '提示', { type: 'warning' })
    await updateUserStatus(row.id, newStatus)
    ElMessage.success(`${action}成功`)
    load()
  } catch {}
}

const roleText = (row: SysUser) => {
  if (row.role === 0) return '普通用户'
  if (row.role === 1) return '商家'
  if (row.role === 2) return row.adminType === 1 ? '普通管理员' : '超级管理员'
  return '未知'
}

const roleType = (row: SysUser) => {
  if (row.role === 0) return 'info'
  if (row.role === 1) return 'warning'
  if (row.adminType === 0) return 'danger'
  return 'success'
}

const handleSortChange = (params: { prop?: string; order?: 'ascending' | 'descending' | null }) => {
  query.sortField = params.prop || 'create_time'
  query.sortOrder = params.order || 'descending'
  load()
}

const formatTime = (time?: string | null) => {
  return time ? time.replace('T', ' ').substring(0, 19) : '-'
}

onMounted(() => {
  load()
  loadPonds()
})
</script>

<template>
  <div class="page-container">
    <div class="page-header admin-area">
      <div class="admin-badge">
        <el-icon :size="18"><UserFilled /></el-icon>
        <span>超级管理员专区</span>
      </div>
      <h2>普通用户管理</h2>
    </div>

    <el-card class="filter-card admin-area">
      <el-form :model="query" inline>
        <el-form-item label="搜索">
          <el-input
            v-model="query.keyword"
            placeholder="手机号 / 昵称 / 邮箱"
            clearable
            style="width: 260px"
            @keyup.enter="load"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="query.pageNum = 1; load()">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" class="admin-area">
      <el-table :data="list" stripe @sort-change="handleSortChange">
        <el-table-column prop="id" label="用户ID" width="90" sortable="custom" />
        <el-table-column prop="phone" label="手机号" width="130" />
        <el-table-column prop="nickname" label="用户名/昵称" width="130" />
        <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
        <el-table-column prop="createTime" label="创建时间" width="170" sortable="custom">
          <template #default="{ row }">{{ formatTime(row.createTime) }}</template>
        </el-table-column>
        <el-table-column prop="lastLoginTime" label="最后登录时间" width="170" sortable="custom">
          <template #default="{ row }">{{ formatTime(row.lastLoginTime) }}</template>
        </el-table-column>
        <el-table-column label="角色" width="120">
          <template #default="{ row }">
            <el-tag :type="roleType(row)">{{ roleText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="绑定鱼塘" min-width="150">
          <template #default="{ row }">
            {{ row.pondId ? (ponds.find(p => p.id === row.pondId)?.name || row.pondId) : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="账户状态" width="100" sortable="custom" prop="status">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '启用' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openBinding(row)">权限配置</el-button>
            <el-button link :type="row.status === 1 ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 1 ? '禁用' : '启用' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination
        v-model:current-page="query.pageNum"
        v-model:page-size="query.pageSize"
        :total="total"
        layout="total, prev, pager, next"
        class="pagination"
        @current-change="load"
      />
    </el-card>

    <el-dialog v-model="dialogVisible" title="管理员权限配置" width="420px">
      <el-form label-width="100px">
        <el-form-item label="管理员类型">
          <el-radio-group v-model="bindingForm.adminType">
            <el-radio :value="0">超级管理员</el-radio>
            <el-radio :value="1">普通管理员</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item v-if="bindingForm.adminType === 1" label="绑定鱼塘">
          <el-select v-model="bindingForm.pondId" placeholder="选择鱼塘" clearable style="width: 100%">
            <el-option
              v-for="pond in ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="saveBinding">保存</el-button>
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
  flex-direction: column;
  gap: 8px;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  color: #0f4c75;
}

.admin-area {
  border: 1px solid #e6a23c;
  background: linear-gradient(135deg, #fffaf0 0%, #ffffff 100%);
}

.admin-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  align-self: flex-start;
  background: #e6a23c;
  color: white;
  padding: 4px 12px;
  border-radius: 20px;
  font-size: 13px;
  font-weight: 600;
}

.filter-card {
  margin-bottom: 16px;
}

.pagination {
  margin-top: 16px;
  justify-content: flex-end;
}
</style>
