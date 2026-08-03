<script setup lang="ts">
import { computed, onMounted, reactive, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Refresh, Search, ShoppingBag, ShoppingCart } from '@element-plus/icons-vue'
import { categoryLabels, getShopProducts, type ProductCategory, type ShopProduct } from '@/api/shop'

const router = useRouter()

const categories: (ProductCategory | 'all')[] = ['all', 'equipment', 'bait', 'fish', 'food']
const categoryIcons: Record<ProductCategory, string> = {
  equipment: 'Goods',
  bait: 'Coin',
  fish: 'Aim',
  food: 'Bowl'
}

const categoryVisualText: Record<ProductCategory, string> = {
  equipment: '钓具',
  bait: '饵料',
  fish: '鱼获',
  food: '餐饮'
}

const state = reactive({
  keyword: '',
  category: 'all' as ProductCategory | 'all',
  pageNum: 1,
  pageSize: 20,
  total: 0,
  loading: false,
  error: '',
  products: [] as ShopProduct[]
})

const loadProducts = async () => {
  state.loading = true
  state.error = ''
  try {
    const params = {
      pageNum: state.pageNum,
      pageSize: state.pageSize,
      keyword: state.keyword || undefined,
      category: state.category === 'all' ? undefined : state.category
    }
    const res = await getShopProducts(params)
    state.products = res.data?.records || []
    state.total = res.data?.total || state.products.length
  } catch (err: any) {
    state.products = []
    state.total = 0
    state.error = err?.message || '商品加载失败，请确认后端服务已启动后重试'
  } finally {
    state.loading = false
  }
}

const onSearch = () => {
  state.pageNum = 1
  loadProducts()
}

const onCategoryChange = (category: ProductCategory | 'all') => {
  state.category = category
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

const buyNow = (product: ShopProduct) => {
  router.push({
    path: '/shop/checkout',
    query: {
      productId: product.id,
      quantity: 1
    }
  })
}

const formatPrice = (price?: number | null) => {
  if (price == null) return '0.00'
  return (price / 100).toFixed(2)
}

const productStats = computed(() => {
  return {
    total: state.products.length,
    equipment: state.products.filter((item) => item.category === 'equipment').length,
    bait: state.products.filter((item) => item.category === 'bait').length
  }
})

watch(
  () => state.keyword,
  () => {
    state.pageNum = 1
  }
)

onMounted(loadProducts)
</script>

<template>
  <div class="shop-home">
    <section class="shop-hero">
      <div class="shop-hero-copy">
        <p class="eyebrow">Fishing Mall</p>
        <h1>钓具、饵料、鱼获和钓场餐饮补给</h1>
        <p>商品列表、下单、支付和订单记录全部来自后端数据库，刷新页面或重新登录后仍然保持同步。</p>
        <div class="hero-actions">
          <el-button type="primary" size="large" :icon="ShoppingBag" @click="router.push('/shop/orders')">
            我的订单
          </el-button>
          <el-button size="large" :icon="Refresh" @click="loadProducts">刷新商品</el-button>
        </div>
      </div>
      <div class="hero-panel">
        <div class="hero-stat">
          <strong>{{ productStats.total }}</strong>
          <span>在售商品</span>
        </div>
        <div class="hero-stat">
          <strong>{{ productStats.equipment }}</strong>
          <span>钓具装备</span>
        </div>
        <div class="hero-stat accent">
          <strong>{{ productStats.bait }}</strong>
          <span>饵料补给</span>
        </div>
      </div>
    </section>

    <section class="shop-toolbar">
      <div class="category-bar">
        <button
          v-for="cat in categories"
          :key="cat"
          class="category-item"
          :class="{ active: state.category === cat }"
          type="button"
          @click="onCategoryChange(cat)"
        >
          {{ categoryLabels[cat] }}
        </button>
      </div>

      <el-input
        v-model="state.keyword"
        placeholder="搜索鱼竿、饵料、鱼获"
        class="search-input"
        clearable
        @clear="onSearch"
        @keyup.enter="onSearch"
      >
        <template #prefix>
          <el-icon><Search /></el-icon>
        </template>
        <template #append>
          <el-button :icon="Search" @click="onSearch" />
        </template>
      </el-input>
    </section>

    <el-alert v-if="state.error" class="error-alert" type="error" :title="state.error" show-icon :closable="false">
      <template #default>
        <el-button size="small" type="danger" plain @click="loadProducts">重试</el-button>
      </template>
    </el-alert>

    <section v-loading="state.loading" class="product-grid">
      <article v-for="product in state.products" :key="product.id" class="product-card">
        <button class="product-media" type="button" @click="goDetail(product)">
          <el-image v-if="product.imageUrl" :src="product.imageUrl" fit="cover" />
          <span v-else class="product-visual" :class="product.category">
            <el-icon><component :is="categoryIcons[product.category]" /></el-icon>
            <strong>{{ categoryVisualText[product.category] }}</strong>
          </span>
          <span class="category-badge">{{ categoryLabels[product.category] }}</span>
        </button>
        <div class="product-info">
          <button class="product-name" type="button" @click="goDetail(product)">
            {{ product.name }}
          </button>
          <p class="product-desc">{{ product.description || '钓场精选商品，到场可用。' }}</p>
          <div class="product-meta">
            <span class="product-price">¥{{ formatPrice(product.price) }}</span>
            <span class="product-stock">库存 {{ product.stock }}</span>
          </div>
          <div class="product-actions">
            <el-button plain @click="goDetail(product)">查看详情</el-button>
            <el-button type="primary" :icon="ShoppingCart" :disabled="product.stock <= 0" @click="buyNow(product)">
              立即购买
            </el-button>
          </div>
        </div>
      </article>
    </section>

    <div v-if="!state.loading && !state.error && state.products.length === 0" class="empty-tip">
      <el-empty description="暂无匹配商品">
        <el-button type="primary" plain @click="onSearch">重新加载</el-button>
      </el-empty>
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
  width: min(1180px, calc(100% - 32px));
  margin: 0 auto;
  padding: 26px 0 40px;
}

.shop-hero {
  min-height: 310px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  align-items: end;
  gap: 28px;
  padding: 36px;
  border-radius: 8px;
  background:
    linear-gradient(90deg, rgba(13, 42, 36, 0.96), rgba(31, 106, 88, 0.82), rgba(211, 152, 63, 0.2)),
    #244b3b;
  color: #fff;
  overflow: hidden;
}

.shop-hero-copy {
  max-width: 620px;
}

.eyebrow {
  margin: 0 0 10px;
  color: #f4bd62;
  font-size: 13px;
  font-weight: 900;
  text-transform: uppercase;
}

.shop-hero h1 {
  margin: 0;
  font-size: 40px;
  line-height: 1.15;
  font-weight: 900;
}

.shop-hero-copy p:not(.eyebrow) {
  max-width: 560px;
  margin: 14px 0 0;
  color: rgba(255, 255, 255, 0.88);
  line-height: 1.7;
  font-size: 16px;
}

.hero-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  flex-wrap: wrap;
}

.hero-panel {
  display: grid;
  gap: 12px;
}

.hero-stat {
  min-height: 78px;
  padding: 16px;
  border-radius: 8px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
  backdrop-filter: blur(8px);
}

.hero-stat strong {
  display: block;
  font-size: 28px;
  color: #ffffff;
  line-height: 1;
}

.hero-stat span {
  display: block;
  margin-top: 8px;
  color: rgba(255, 255, 255, 0.8);
}

.hero-stat.accent strong {
  color: #ffd36f;
}

.shop-toolbar {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 360px;
  align-items: center;
  gap: 18px;
  margin: 22px 0 18px;
}

.category-bar {
  display: flex;
  gap: 8px;
  overflow-x: auto;
}

.category-item {
  min-width: 86px;
  min-height: 40px;
  padding: 0 16px;
  border: 1px solid #d9e4de;
  border-radius: 6px;
  background: #fff;
  color: #53635d;
  font-size: 14px;
  font-weight: 800;
  cursor: pointer;
  transition: all 0.18s ease;
}

.category-item:hover,
.category-item.active {
  background: #1f6a58;
  border-color: #1f6a58;
  color: #fff;
}

.search-input {
  width: 100%;
}

.error-alert {
  margin-bottom: 16px;
}

.product-grid {
  min-height: 280px;
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 18px;
}

.product-card {
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 8px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(25, 47, 39, 0.07);
  transition: transform 0.18s ease, box-shadow 0.18s ease, border-color 0.18s ease;
}

.product-card:hover {
  transform: translateY(-3px);
  border-color: #bfd8ca;
  box-shadow: 0 16px 36px rgba(25, 47, 39, 0.12);
}

.product-media {
  position: relative;
  display: block;
  width: 100%;
  aspect-ratio: 4 / 3;
  border: 0;
  padding: 0;
  background: #edf4ef;
  cursor: pointer;
  overflow: hidden;
}

.product-media :deep(.el-image),
.product-visual {
  width: 100%;
  height: 100%;
  display: block;
}

.product-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  color: #173c34;
  background: #edf4ef;
}

.product-visual .el-icon {
  font-size: 42px;
  color: #1f6a58;
}

.product-visual strong {
  font-size: 20px;
  font-weight: 900;
}

.product-visual.bait {
  background: #fff8e8;
}

.product-visual.bait .el-icon {
  color: #b5742a;
}

.product-visual.fish {
  background: #edf5f7;
}

.product-visual.fish .el-icon {
  color: #3d6d8c;
}

.product-visual.food {
  background: #f7f1e8;
}

.product-visual.food .el-icon {
  color: #9b621b;
}

.category-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  padding: 5px 9px;
  border-radius: 6px;
  background: rgba(21, 60, 53, 0.88);
  color: #fff;
  font-size: 12px;
  font-weight: 900;
}

.product-info {
  padding: 16px;
}

.product-name {
  width: 100%;
  min-height: 44px;
  padding: 0;
  border: 0;
  background: transparent;
  color: #172521;
  font-size: 17px;
  font-weight: 900;
  line-height: 1.35;
  text-align: left;
  cursor: pointer;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-desc {
  height: 42px;
  margin: 8px 0 12px;
  color: #6b7973;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-meta {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 14px;
}

.product-price {
  color: #c7672e;
  font-size: 22px;
  font-weight: 900;
}

.product-stock {
  color: #7f8b86;
  font-size: 13px;
}

.product-actions {
  display: grid;
  grid-template-columns: 1fr 1.2fr;
  gap: 10px;
}

.empty-tip {
  margin-top: 48px;
}

.pagination-bar {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}

@media (max-width: 1080px) {
  .shop-hero,
  .shop-toolbar {
    grid-template-columns: 1fr;
  }

  .hero-panel {
    grid-template-columns: repeat(3, 1fr);
  }

  .product-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 640px) {
  .shop-home {
    width: calc(100% - 20px);
    padding: 12px 0 28px;
  }

  .shop-hero {
    padding: 24px;
  }

  .shop-hero h1 {
    font-size: 28px;
  }

  .hero-panel,
  .product-grid {
    grid-template-columns: 1fr;
  }

  .product-actions {
    grid-template-columns: 1fr;
  }
}
</style>
