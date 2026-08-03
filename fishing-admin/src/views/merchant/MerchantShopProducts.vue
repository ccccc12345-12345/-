<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import {
  getMerchantShopProducts,
  createMerchantShopProduct,
  updateMerchantShopProduct,
  updateMerchantShopProductStatus,
  deleteMerchantShopProduct,
  categoryLabels,
  type ShopProduct,
  type ProductCategory
} from '@/api/shop'

const list = ref<ShopProduct[]>([])
const loading = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const editId = ref<number | null>(null)
const formRef = ref<any>(null)

const categories: ProductCategory[] = ['equipment', 'bait', 'fish', 'food']

const form = reactive<Partial<ShopProduct>>({
  name: '',
  category: 'equipment',
  price: 0,
  stock: 0,
  imageUrl: '',
  description: '',
  status: 'on'
})

const query = reactive({
  keyword: '',
  category: '' as '' | ProductCategory,
  status: '' as '' | 'on' | 'off',
  pageNum: 1,
  pageSize: 10,
  total: 0
})

const rules = {
  name: [{ required: true, message: '请输入商品名称', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  stock: [{ required: true, message: '请输入库存', trigger: 'blur' }]
}

const load = async () => {
  loading.value = true
  try {
    const res = await getMerchantShopProducts({
      keyword: query.keyword || undefined,
      category: query.category || undefined,
      status: query.status || undefined,
      pageNum: query.pageNum,
      pageSize: query.pageSize
    })
    list.value = res.data?.records || []
    query.total = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

const onSearch = () => {
  query.pageNum = 1
  load()
}

const onPageChange = (page: number) => {
  query.pageNum = page
  load()
}

const openCreate = () => {
  isEdit.value = false
  editId.value = null
  form.name = ''
  form.category = 'equipment'
  form.price = 0
  form.stock = 0
  form.imageUrl = ''
  form.description = ''
  form.status = 'on'
  dialogVisible.value = true
}

const openEdit = (row: ShopProduct) => {
  isEdit.value = true
  editId.value = row.id
  form.name = row.name
  form.category = row.category
  form.price = row.price
  form.stock = row.stock
  form.imageUrl = row.imageUrl || ''
  form.description = row.description || ''
  form.status = row.status
  dialogVisible.value = true
}

const save = async () => {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  const data = {
    name: form.name,
    category: form.category,
    price: form.price,
    stock: form.stock,
    imageUrl: form.imageUrl || null,
    description: form.description || null,
    status: form.status || 'on'
  }
  if (isEdit.value && editId.value) {
    await updateMerchantShopProduct(editId.value, data)
    ElMessage.success('更新成功')
  } else {
    await createMerchantShopProduct(data)
    ElMessage.success('新增成功')
  }
  dialogVisible.value = false
  load()
}

const toggleStatus = async (row: ShopProduct) => {
  const status = row.status === 'on' ? 'off' : 'on'
  await updateMerchantShopProductStatus(row.id, status)
  ElMessage.success(status === 'on' ? '已上架' : '已下架')
  load()
}

const del = async (row: ShopProduct) => {
  try {
    await ElMessageBox.confirm(`确定删除商品“${row.name}”吗？`, '提示', { type: 'warning' })
    await deleteMerchantShopProduct(row.id)
    ElMessage.success('删除成功')
    load()
  } catch {}
}

const formatPrice = (price?: number | null) => {
  if (price == null) return '0.00'
  return (price / 100).toFixed(2)
}

onMounted(load)
</script>

<template>
  <div class="page-container">
    <div class="page-header">
      <h2>商城商品管理</h2>
      <el-button type="primary" :icon="Plus" @click="openCreate">新增商品</el-button>
    </div>

    <el-card shadow="never" class="filter-card">
      <el-form :inline="true" class="filter-form">
        <el-form-item label="关键词">
          <el-input v-model="query.keyword" placeholder="商品名称" clearable @keyup.enter="onSearch" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="query.category" placeholder="全部" clearable style="width: 120px">
            <el-option
              v-for="cat in categories"
              :key="cat"
              :label="categoryLabels[cat]"
              :value="cat"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable style="width: 120px">
            <el-option label="上架" value="on" />
            <el-option label="下架" value="off" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="onSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card v-loading="loading" shadow="never" class="table-card">
      <el-table :data="list" stripe>
        <el-table-column label="图片" width="80">
          <template #default="{ row }">
            <el-image
              :src="row.imageUrl || 'https://placehold.co/80x80/e8f4f8/0f4c75?text=No+Image'"
              fit="cover"
              style="width: 50px; height: 50px; border-radius: 4px"
            />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="160" />
        <el-table-column label="分类" width="100">
          <template #default="{ row }">
            {{ categoryLabels[row.category] || row.category }}
          </template>
        </el-table-column>
        <el-table-column label="价格" width="110">
          <template #default="{ row }">
            <span style="color: #e8a838; font-weight: 700">¥{{ formatPrice(row.price) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="stock" label="库存" width="90" />
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

      <div class="pagination-bar">
        <el-pagination
          v-model:current-page="query.pageNum"
          :page-size="query.pageSize"
          :total="query.total"
          layout="total, prev, pager, next"
          @current-change="onPageChange"
        />
      </div>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑商品' : '新增商品'" width="560px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="80px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="请输入商品名称" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="form.category" placeholder="请选择分类" style="width: 100%">
            <el-option
              v-for="cat in categories"
              :key="cat"
              :label="categoryLabels[cat]"
              :value="cat"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="价格" prop="price">
          <el-input-number v-model="form.price" :min="0" :precision="0" style="width: 100%" />
          <div class="form-tip">单位：分</div>
        </el-form-item>
        <el-form-item label="库存" prop="stock">
          <el-input-number v-model="form.stock" :min="0" :precision="0" style="width: 100%" />
        </el-form-item>
        <el-form-item label="图片URL">
          <el-input v-model="form.imageUrl" placeholder="https://" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="3" placeholder="请输入商品描述" />
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

.filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
}

.filter-form {
  margin-bottom: -18px;
}

.table-card {
  border-radius: 12px;
}

.pagination-bar {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.form-tip {
  color: #909399;
  font-size: 12px;
  line-height: 1.4;
  margin-top: 4px;
}
</style>
