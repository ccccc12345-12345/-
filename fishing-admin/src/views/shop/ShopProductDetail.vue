<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getShopProduct, type ShopProduct } from '@/api/shop'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const productId = Number(route.params.productId)

const product = ref<ShopProduct | null>(null)
const loading = ref(false)
const quantity = ref(1)

const loadProduct = async () => {
  loading.value = true
  try {
    const res = await getShopProduct(productId)
    product.value = res.data
    quantity.value = 1
  } catch {
    product.value = null
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
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
    </div>

    <div v-loading="loading" class="detail-body">
      <template v-if="product">
        <div class="detail-image">
          <el-image
            :src="product.imageUrl || 'https://placehold.co/600x600/e8f4f8/0f4c75?text=No+Image'"
            fit="cover"
            style="width: 100%; height: 100%; border-radius: 12px"
          />
        </div>

        <div class="detail-info">
          <h2 class="detail-name">{{ product.name }}</h2>
          <div class="detail-price">¥{{ formatPrice(product.price) }}</div>
          <div class="detail-stock">库存：{{ product.stock }}</div>
          <div class="detail-desc">
            {{ product.description || '暂无商品描述' }}
          </div>

          <div class="quantity-row">
            <span class="quantity-label">数量</span>
            <el-input-number
              v-model="quantity"
              :min="1"
              :max="product.stock || 1"
              :disabled="product.stock <= 0"
              :controls-position="'right'"
            />
          </div>

          <el-button
            type="primary"
            size="large"
            class="buy-btn"
            :disabled="product.stock <= 0"
            @click="buyNow"
          >
            {{ product.stock <= 0 ? '已售罄' : '立即购买' }}
          </el-button>
        </div>
      </template>

      <el-empty v-else-if="!loading" description="商品不存在或已下架" />
    </div>
  </div>
</template>

<style scoped>
.product-detail {
  max-width: 800px;
  margin: 0 auto;
  padding: 16px;
}

.detail-header {
  margin-bottom: 16px;
}

.detail-body {
  display: flex;
  gap: 24px;
  background: #fff;
  border-radius: 16px;
  padding: 20px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.detail-image {
  flex: 1;
  min-width: 260px;
  max-width: 360px;
  aspect-ratio: 1 / 1;
  background: #f5f7fa;
  border-radius: 12px;
  overflow: hidden;
}

.detail-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  min-width: 260px;
}

.detail-name {
  margin: 0 0 12px;
  font-size: 22px;
  color: #303133;
}

.detail-price {
  color: #e8a838;
  font-size: 28px;
  font-weight: 700;
  margin-bottom: 8px;
}

.detail-stock {
  color: #909399;
  font-size: 14px;
  margin-bottom: 16px;
}

.detail-desc {
  color: #606266;
  font-size: 14px;
  line-height: 1.6;
  margin-bottom: 24px;
  white-space: pre-line;
}

.quantity-row {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}

.quantity-label {
  color: #303133;
  font-size: 14px;
}

.buy-btn {
  width: 100%;
  margin-top: auto;
  background: #0f4c75;
  border-color: #0f4c75;
}

.buy-btn:disabled {
  opacity: 0.6;
}

@media (max-width: 700px) {
  .detail-body {
    flex-direction: column;
    align-items: center;
  }

  .detail-image {
    max-width: 100%;
    width: 100%;
  }
}
</style>
