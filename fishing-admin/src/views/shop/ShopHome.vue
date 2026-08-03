<script setup lang="ts">
import { ref, reactive, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Search, ShoppingBag } from '@element-plus/icons-vue'
import { getShopProducts, categoryLabels, type ProductCategory, type ShopProduct } from '@/api/shop'

const router = useRouter()

const categories: (ProductCategory | 'all')[] = ['all', 'equipment', 'bait', 'fish', 'food']

const state = reactive({
  keyword: '',
  category: 'all' as ProductCategory | 'all',
  pageNum: 1,
  pageSize: 20,
  total: 0,
  loading: false,
  products: [] as ShopProduct[]
})

const loadProducts = async () => {
  state.loading = true
  try {
    const params: any = {
      pageNum: state.pageNum,
      pageSize: state.pageSize,
      keyword: state.keyword || undefined,
      category: state.category === 'all' ? undefined : state.category
    }
    const res = await getShopProducts(params)
    state.products = res.data?.records || []
    state.total = res.data?.total || 0
  } finally {
    state.loading = false
  }
}

const onSearch = () => {
  state.pageNum = 1
  loadProducts()
}

const onCategoryChange = () => {
  state.pageNum = 1
  loadProducts()
}

const onPageChange = (page: number) => {
  state.pageNum = page
  loadProducts()
}

const goDetail = (product: ShopProduct) => {
  router.push(`/shop/product/${product.id}`)
}

const formatPrice = (price?: number | null) => {
  if (price == null) return '0.00'
  return (price / 100).toFixed(2)
}

watch(() => state.keyword, () => {
  state.pageNum = 1
})

onMounted(loadProducts)
</script>

<template>
  <div class="shop-home">
    <div class="shop-header">
      <div class="shop-title">商城</div>
      <el-input
        v-model="state.keyword"
        placeholder="搜索商品"
        class="search-input"
        clearable
        @keyup.enter="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
      </el-input>
      <el-button type="primary" :icon="ShoppingBag" text @click="router.push('/shop/orders')">
        我的订单
      </el-button>
    </div>

    <div class="category-bar">
      <div
        v-for="cat in categories"
        :key="cat"
        class="category-item"
        :class="{ active: state.category === cat }"
        @click="state.category = cat; onCategoryChange()"
      >
        {{ categoryLabels[cat] }}
      </div>
    </div>

    <div v-loading="state.loading" class="product-grid">
      <div
        v-for="product in state.products"
        :key="product.id"
        class="product-card"
        @click="goDetail(product)"
      >
        <div class="product-image">
          <el-image
            :src="product.imageUrl || 'https://placehold.co/400x400/e8f4f8/0f4c75?text=No+Image'"
            fit="cover"
            style="width: 100%; height: 100%"
          />
        </div>
        <div class="product-info">
          <div class="product-name">{{ product.name }}</div>
          <div class="product-meta">
            <span class="product-price">¥{{ formatPrice(product.price) }}</span>
            <span class="product-stock">库存 {{ product.stock }}</span>
          </div>
        </div>
      </div>
    </div>

    <div v-if="!state.loading && state.products.length === 0" class="empty-tip">
      <el-empty description="暂无商品" />
    </div>

    <div v-if="state.total > state.pageSize" class="pagination-bar">
      <el-pagination
        v-model:current-page="state.pageNum"
        :page-size="state.pageSize"
        :total="state.total"
        layout="prev, pager, next"
        @current-change="onPageChange"
      />
    </div>
  </div>
</template>

<style scoped>
.shop-home {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
}

.shop-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.shop-title {
  font-size: 20px;
  font-weight: 700;
  color: #0f4c75;
  flex-shrink: 0;
}

.search-input {
  flex: 1;
}

.category-bar {
  display: flex;
  gap: 10px;
  overflow-x: auto;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.category-item {
  flex-shrink: 0;
  padding: 6px 16px;
  border-radius: 16px;
  background: #f5f7fa;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.category-item.active {
  background: #0f4c75;
  color: #fff;
}

.product-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 12px;
}

.product-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.product-card:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.1);
}

.product-image {
  width: 100%;
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
}

.product-info {
  padding: 12px;
}

.product-name {
  font-size: 14px;
  color: #303133;
  line-height: 1.4;
  height: 40px;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  margin-bottom: 8px;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.product-price {
  color: #e8a838;
  font-size: 18px;
  font-weight: 700;
}

.product-stock {
  color: #909399;
  font-size: 12px;
}

.empty-tip {
  margin-top: 40px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 20px;
}

@media (max-width: 600px) {
  .shop-home {
    padding: 12px;
  }

  .product-grid {
    gap: 10px;
  }
}
</style>
