<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, ShoppingCart } from '@element-plus/icons-vue'
import { categoryLabels, getShopProduct, type ProductCategory, type ShopProduct } from '@/api/shop'

const route = useRoute()
const router = useRouter()
const productId = Number(route.params.productId)

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

const product = ref<ShopProduct | null>(null)
const loading = ref(false)
const error = ref('')
const quantity = ref(1)

const maxQuantity = computed(() => Math.max(product.value?.stock || 1, 1))
const totalPrice = computed(() => (product.value ? product.value.price * quantity.value : 0))

const loadProduct = async () => {
  loading.value = true
  error.value = ''
  try {
    const res = await getShopProduct(productId)
    product.value = res.data
    quantity.value = 1
  } catch (err: any) {
    product.value = null
    error.value = err?.message || '商品详情加载失败，请稍后重试'
  } finally {
    loading.value = false
  }
}

const buyNow = () => {
  if (!product.value) return
  router.push({
    path: '/shop/checkout',
    query: {
      productId: product.value.id,
      quantity: quantity.value
    }
  })
}

const formatPrice = (price?: number | null) => {
  if (price == null) return '0.00'
  return (price / 100).toFixed(2)
}

onMounted(loadProduct)
</script>

<template>
  <div class="product-detail">
    <div class="detail-header">
      <el-button plain :icon="ArrowLeft" @click="router.back()">返回商城</el-button>
      <el-button text @click="router.push('/shop/orders')">我的订单</el-button>
    </div>

    <el-alert v-if="error" class="error-alert" type="error" :title="error" show-icon :closable="false">
      <template #default>
        <el-button size="small" type="danger" plain @click="loadProduct">重试</el-button>
      </template>
    </el-alert>

    <section v-loading="loading" class="detail-body">
      <template v-if="product">
        <div class="detail-image">
          <el-image v-if="product.imageUrl" :src="product.imageUrl" fit="cover" />
          <div v-else class="detail-visual" :class="product.category">
            <el-icon><component :is="categoryIcons[product.category]" /></el-icon>
            <strong>{{ categoryVisualText[product.category] }}</strong>
          </div>
        </div>

        <div class="detail-info">
          <div class="product-tags">
            <span>{{ categoryLabels[product.category] }}</span>
            <span>{{ product.stock > 0 ? '现货' : '缺货' }}</span>
          </div>
          <h1 class="detail-name">{{ product.name }}</h1>
          <div class="detail-price">¥{{ formatPrice(product.price) }}</div>
          <p class="detail-desc">{{ product.description || '钓场精选商品，到场可用。' }}</p>

          <div class="info-grid">
            <div>
              <strong>{{ product.stock }}</strong>
              <span>当前库存</span>
            </div>
            <div>
              <strong>{{ categoryLabels[product.category] }}</strong>
              <span>商品分类</span>
            </div>
            <div>
              <strong>到场自提</strong>
              <span>履约方式</span>
            </div>
          </div>

          <div class="quantity-row">
            <span class="quantity-label">购买数量</span>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="maxQuantity"
              :disabled="product.stock <= 0"
              controls-position="right"
            />
          </div>

          <div class="checkout-bar">
            <div>
              <span>合计</span>
              <strong>¥{{ formatPrice(totalPrice) }}</strong>
            </div>
            <el-button
              type="primary"
              size="large"
              :icon="ShoppingCart"
              :disabled="product.stock <= 0"
              @click="buyNow"
            >
              {{ product.stock <= 0 ? '已售罄' : '立即购买' }}
            </el-button>
          </div>
        </div>
      </template>

      <el-empty v-else-if="!loading && !error" description="商品不存在或已下架" />
    </section>
  </div>
</template>

<style scoped>
.product-detail {
  width: min(1080px, calc(100% - 32px));
  margin: 0 auto;
  padding: 26px 0 40px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 18px;
}

.error-alert {
  margin-bottom: 16px;
}

.detail-body {
  min-height: 520px;
  display: grid;
  grid-template-columns: minmax(320px, 0.9fr) minmax(0, 1.1fr);
  gap: 28px;
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 8px;
  padding: 24px;
  box-shadow: 0 14px 34px rgba(25, 47, 39, 0.08);
}

.detail-image {
  aspect-ratio: 1 / 1;
  background: #edf4ef;
  border-radius: 8px;
  overflow: hidden;
}

.detail-image :deep(.el-image),
.detail-visual {
  width: 100%;
  height: 100%;
  display: block;
}

.detail-visual {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 16px;
  color: #173c34;
  background: #edf4ef;
}

.detail-visual .el-icon {
  color: #1f6a58;
  font-size: 64px;
}

.detail-visual strong {
  font-size: 26px;
  font-weight: 900;
}

.detail-visual.bait {
  background: #fff8e8;
}

.detail-visual.bait .el-icon {
  color: #b5742a;
}

.detail-visual.fish {
  background: #edf5f7;
}

.detail-visual.fish .el-icon {
  color: #3d6d8c;
}

.detail-visual.food {
  background: #f7f1e8;
}

.detail-visual.food .el-icon {
  color: #9b621b;
}

.detail-info {
  min-width: 0;
  display: flex;
  flex-direction: column;
}

.product-tags {
  display: flex;
  gap: 8px;
  margin-bottom: 14px;
}

.product-tags span {
  padding: 6px 10px;
  border-radius: 6px;
  background: #eef7f1;
  color: #1f6a58;
  font-size: 12px;
  font-weight: 900;
}

.product-tags span:last-child {
  background: #fff3dc;
  color: #a8661e;
}

.detail-name {
  margin: 0;
  color: #172521;
  font-size: 32px;
  line-height: 1.25;
  font-weight: 900;
}

.detail-price {
  margin-top: 16px;
  color: #c7672e;
  font-size: 34px;
  font-weight: 900;
}

.detail-desc {
  margin: 18px 0 0;
  color: #61706a;
  font-size: 15px;
  line-height: 1.8;
  white-space: pre-line;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 10px;
  margin: 24px 0;
}

.info-grid div {
  min-height: 76px;
  padding: 14px;
  border-radius: 8px;
  background: #f6faf7;
  border: 1px solid #e1ece6;
}

.info-grid strong,
.info-grid span {
  display: block;
}

.info-grid strong {
  color: #173c34;
  font-size: 18px;
}

.info-grid span {
  margin-top: 6px;
  color: #7a8782;
  font-size: 12px;
}

.quantity-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 22px;
  padding: 16px 0;
  border-top: 1px solid #edf2ef;
  border-bottom: 1px solid #edf2ef;
}

.quantity-label {
  color: #263832;
  font-size: 15px;
  font-weight: 800;
}

.checkout-bar {
  margin-top: auto;
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 16px;
  padding: 18px;
  border-radius: 8px;
  background: #172521;
  color: #fff;
}

.checkout-bar span,
.checkout-bar strong {
  display: block;
}

.checkout-bar span {
  color: #b7c6bf;
  font-size: 13px;
}

.checkout-bar strong {
  margin-top: 4px;
  color: #ffd36f;
  font-size: 26px;
}

@media (max-width: 820px) {
  .product-detail {
    width: calc(100% - 20px);
    padding: 12px 0 28px;
  }

  .detail-body {
    grid-template-columns: 1fr;
    padding: 16px;
  }

  .detail-name {
    font-size: 26px;
  }

  .info-grid {
    grid-template-columns: 1fr;
  }

  .checkout-bar {
    align-items: stretch;
    flex-direction: column;
  }
}
</style>
