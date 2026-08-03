<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getMerchantRestaurantMenus,
  createMerchantRestaurantMenu,
  updateMerchantRestaurantMenu,
  updateMerchantRestaurantMenuStatus,
  deleteMerchantRestaurantMenu,
  type RestaurantMenu,
  type RestaurantMenuParams
} from '@/api/restaurant-merchant'
import { usePondStore } from '@/store/pond'

const pondStore = usePondStore()

const list = ref<RestaurantMenu[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<any>(null)

const form = reactive<RestaurantMenuParams>({
  pondId: pondStore.currentPondId ?? 0,
  name: '',
  category: 'fresh_fish',
  price: 0,
  stock: -1,
  imageUrl: '',
  description: '',
  isSpecial: 0,
  status: 'on'
})

const rules = {
  name: [{ required: true, message: '请输入菜品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }]
}

const categoryOptions = [
  { label: '鲜鱼', value: 'fresh_fish' },
  { label: '加工菜品', value: 'cooked' },
  { label: '饮品', value: 'drink' }
]

const load = async () => {
  loading.value = true
  try {
    const res = await getMerchantRestaurantMenus(pondStore.currentPondId ?? undefined)
    list.value = res.data || []
  } finally {
    loading.value = false
  }
}

const resetForm = () => {
  form.pondId = pondStore.currentPondId ?? 0
  form.name = ''
  form.category = 'fresh_fish'
  form.price = 0
  form.stock = -1
  form.imageUrl = ''
  form.description = ''
  form.isSpecial = 0
  form.status = 'on'
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  resetForm()
  dialogVisible.value = true
}

const openEdit = (row: RestaurantMenu) => {
  isEdit.value = true
  editId.value = row.id
  form.pondId = row.pondId
  form.name = row.name
  form.category = row.category
  form.price = row.price
  form.stock = row.stock
  form.imageUrl = row.imageUrl || ''
  form.description = row.description || ''
  form.isSpecial = row.isSpecial
  form.status = row.status
  dialogVisible.value = true
}

const save = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const data: RestaurantMenuParams = {
    pondId: form.pondId,
    name: form.name.trim(),
    category: form.category,
    price: Math.round(form.price),
    stock: form.stock == null ? -1 : form.stock,
    imageUrl: form.imageUrl || null,
    description: form.description || null,
    isSpecial: form.isSpecial ?? 0,
    status: form.status || 'on'
  }
  if (isEdit.value && editId.value) {
    await updateMerchantRestaurantMenu(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createMerchantRestaurantMenu(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const toggleStatus = async (row: RestaurantMenu) => {
  const status = row.status === 'on' ? 'off' : 'on'
  await updateMerchantRestaurantMenuStatus(row.id, status)
  ElMessage.success(status === 'on' ? '已上架' : '已下架')
  load()
}

const del = async (row: RestaurantMenu) => {
  try {
    await ElMessageBox.confirm(`确定删除菜品“${row.name}”吗？`, '提示', { type: 'warning' })
    await deleteMerchantRestaurantMenu(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

const categoryLabel = (value: string) => {
  return categoryOptions.find(c => c.value === value)?.label || value
}

const formatPrice = (price: number) => (price / 100).toFixed(2)

onMounted(load)
watch(() => pondStore.currentPondId, load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>餐厅菜单管理</h2>
      <el-button type="primary" icon="Plus" @click="openCreate">新增菜品</el-button>
    </div>

    <el-card v-loading="loading">
      <el-table :data="list" stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              fit="cover"
              style="width: 60px; height: 60px; border-radius: 6px"
            />
            <div v-else class="image-placeholder">无图</div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="菜品名称" min-width="140" />
        <el-table-column label="分类" width="110">
          <template #default="{ row }">{{ categoryLabel(row.category) }}</template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">¥{{ formatPrice(row.price) }}</template>
        </el-table-column>
        <el-table-column label="库存" width="110">
          <template #default="{ row }">{{ row.stock < 0 ? '无限' : row.stock }}</template>
        </el-table-column>
        <el-table-column label="招牌" width="80">
          <template #default="{ row }">
            <el-tag v-if="row.isSpecial === 1" type="warning">是</el-tag>
            <span v-else>否</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 'on' ? 'success' : 'info'">
              {{ row.status === 'on' ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link :type="row.status === 'on' ? 'danger' : 'success'" @click="toggleStatus(row)">
              {{ row.status === 'on' ? '下架' : '上架' }}
            </el-button>
            <el-button link type="danger" @click="del(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑菜品' : '新增菜品'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属鱼塘" prop="pondId">
          <el-select v-model="form.pondId" placeholder="选择鱼塘" style="width: 100%">
            <el-option
              v-for="pond in pondStore.ponds"
              :key="pond.id"
              :label="pond.name"
              :value="pond.id"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="菜品名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="选择分类" style="width: 100%">
            <el-option
              v-for="opt in categoryOptions"
              :key="opt.value"
              :label="opt.label"
              :value="opt.value"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格（分）" prop="price">
          <el-input-number v-model="form.price" :min="0" :step="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="form.stock" :min="-1" :step="1" style="width: 100%" />
          <span class="form-tip">-1 表示无限库存</span>
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.imageUrl" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="是否招牌">
          <el-radio-group v-model="form.isSpecial">
            <el-radio :value="1">是</el-radio>
            <el-radio :value="0">否</el-radio>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio value="on">上架</el-radio>
            <el-radio value="off">下架</el-radio>
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
  margin: 0;
}

.image-placeholder {
  width: 60px;
  height: 60px;
  border-radius: 6px;
  background: #f0f2f5;
  color: #909399;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
}
</style>
