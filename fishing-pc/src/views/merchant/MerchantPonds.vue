<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createMerchantPond,
  deleteMerchantPond,
  getMerchantFishingSpots,
  getMerchantPonds,
  updateMerchantPond
} from '@/api/merchant'
import type { Pond } from '@/api/pond'

const router = useRouter()
const loading = ref(false)
const ponds = ref<Array<Pond & { spotCount?: number }>>([])
const dialogVisible = ref(false)
const editingId = ref<number | null>(null)
const form = reactive<Partial<Pond>>({
  name: '',
  category: 'pond',
  address: '',
  phone: '',
  coverImage: '/demo-assets/ponds/pond-1.svg',
  floorPlanUrl: '/demo-assets/ponds/map-1.svg',
  bookingNotice: '请按预约时间到场，听从现场工作人员安排。',
  cancelRule: '开场前可取消，临近开场请联系商家。',
  refundRule: '符合取消规则的订单按原路退回。',
  status: 1
})

const loadData = async () => {
  loading.value = true
  try {
    const res = await getMerchantPonds()
    const records = Array.isArray(res.data) ? res.data : (res.data?.records || [])
    const withCounts = await Promise.all(
      records.map(async (pond) => {
        try {
          const spotRes = await getMerchantFishingSpots(pond.id)
          return { ...pond, spotCount: spotRes.data?.total || spotRes.data?.records?.length || 0 }
        } catch {
          return { ...pond, spotCount: 0 }
        }
      })
    )
    ponds.value = withCounts
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  editingId.value = null
  Object.assign(form, {
    name: '',
    category: 'pond',
    address: '',
    phone: '',
    coverImage: '/demo-assets/ponds/pond-1.svg',
    floorPlanUrl: '/demo-assets/ponds/map-1.svg',
    bookingNotice: '请按预约时间到场，听从现场工作人员安排。',
    cancelRule: '开场前可取消，临近开场请联系商家。',
    refundRule: '符合取消规则的订单按原路退回。',
    status: 1
  })
}

const openCreate = () => {
  resetForm()
  dialogVisible.value = true
}

const openEdit = (pond: Pond) => {
  editingId.value = pond.id
  Object.assign(form, pond)
  dialogVisible.value = true
}

const save = async () => {
  if (!form.name) {
    ElMessage.warning('请填写鱼塘名称')
    return
  }
  if (editingId.value) {
    await updateMerchantPond(editingId.value, form)
    ElMessage.success('鱼塘已更新')
  } else {
    await createMerchantPond(form)
    ElMessage.success('鱼塘已创建')
  }
  dialogVisible.value = false
  await loadData()
}

const remove = async (pond: Pond) => {
  await ElMessageBox.confirm(`确认删除鱼塘“${pond.name}”？`, '删除鱼塘', { type: 'warning' })
  await deleteMerchantPond(pond.id)
  ElMessage.success('已删除')
  await loadData()
}

const categoryText: Record<string, string> = {
  lake: '湖库',
  river: '江河',
  pond: '鱼塘',
  sea: '海区'
}

onMounted(loadData)
</script>

<template>
  <section class="fp-spotlight" v-fp-spotlight>
    <div class="head">
      <div>
        <p>商家鱼塘</p>
        <h1>鱼塘管理</h1>
      </div>
      <el-button type="primary" v-fp-ripple @click="openCreate">新增鱼塘</el-button>
    </div>

    <el-table v-loading="loading" :data="ponds" row-key="id" class="table">
      <el-table-column label="封面" width="120">
        <template #default="{ row }">
          <img class="cover" :src="row.coverImage || '/demo-assets/ponds/pond-1.svg'" alt="" />
        </template>
      </el-table-column>
      <el-table-column prop="name" label="名称" min-width="160" />
      <el-table-column label="分类" width="100">
        <template #default="{ row }">{{ categoryText[row.category] || row.category || '-' }}</template>
      </el-table-column>
      <el-table-column prop="address" label="地址" min-width="220" />
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column label="钓位数" width="90">
        <template #default="{ row }">{{ row.spotCount || 0 }}</template>
      </el-table-column>
      <el-table-column label="状态" width="90">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="300" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="router.push(`/merchant/ponds/${row.id}/spots`)">钓位编辑</el-button>
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-button link @click="router.push({ path: '/merchant/pond-board', query: { pondId: row.id } })">看板</el-button>
          <el-button link type="danger" @click="remove(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-dialog v-model="dialogVisible" :title="editingId ? '编辑鱼塘' : '新增鱼塘'" width="720px">
      <el-form label-position="top">
        <div class="form-grid">
          <el-form-item label="名称"><el-input v-model="form.name" /></el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.category">
              <el-option label="湖库" value="lake" />
              <el-option label="江河" value="river" />
              <el-option label="鱼塘" value="pond" />
              <el-option label="海区" value="sea" />
            </el-select>
          </el-form-item>
          <el-form-item label="电话"><el-input v-model="form.phone" /></el-form-item>
          <el-form-item label="状态">
            <el-select v-model="form.status">
              <el-option label="启用" :value="1" />
              <el-option label="停用" :value="0" />
            </el-select>
          </el-form-item>
        </div>
        <el-form-item label="地址"><el-input v-model="form.address" /></el-form-item>
        <el-form-item label="封面图"><el-input v-model="form.coverImage" /></el-form-item>
        <el-form-item label="平面图"><el-input v-model="form.floorPlanUrl" /></el-form-item>
        <el-form-item label="预约须知"><el-input v-model="form.bookingNotice" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="取消规则"><el-input v-model="form.cancelRule" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="退款规则"><el-input v-model="form.refundRule" type="textarea" :rows="2" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </section>
</template>

<style scoped>
.head,
.table,
.form-grid {
  position: relative;
  z-index: 1;
}

.head {
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  margin-bottom: 16px;
}

.head p {
  margin: 0 0 6px;
  color: #1f6a58;
  font-weight: 900;
}

.head h1 {
  margin: 0;
  color: #172521;
}

.table {
  border-radius: 8px;
  overflow: hidden;
}

.cover {
  width: 88px;
  height: 58px;
  border-radius: 8px;
  object-fit: cover;
  background: #f4f8f5;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    box-shadow var(--fp-dur-fast) var(--fp-ease-out);
}

tr:hover .cover {
  transform: scale(1.08);
  box-shadow: var(--fp-shadow-sm);
}

:deep(.el-table__row) {
  animation: fadeUp 0.45s var(--fp-ease-out) both;
}

:deep(.el-table__row:nth-child(1)) { animation-delay: 0ms; }
:deep(.el-table__row:nth-child(2)) { animation-delay: 40ms; }
:deep(.el-table__row:nth-child(3)) { animation-delay: 80ms; }
:deep(.el-table__row:nth-child(4)) { animation-delay: 120ms; }
:deep(.el-table__row:nth-child(5)) { animation-delay: 160ms; }
:deep(.el-table__row:nth-child(6)) { animation-delay: 200ms; }
:deep(.el-table__row:nth-child(7)) { animation-delay: 240ms; }
:deep(.el-table__row:nth-child(8)) { animation-delay: 280ms; }

:deep(.el-button.is-link) {
  position: relative;
  transition: transform var(--fp-dur-fast) var(--fp-ease-out),
    color var(--fp-dur-fast) var(--fp-ease-out);
}

:deep(.el-button.is-link:hover) {
  transform: translateY(-1px);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}
</style>
