<script setup lang="ts">
import { ref, reactive, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft } from '@element-plus/icons-vue'
import { getShopProduct, createShopOrder, payShopOrder, type ShopProduct } from '@/api/shop'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()

const productId = Number(route.query.productId)
const quantity = Number(route.query.quantity) || 1

const product = ref<ShopProduct | null>(null)
const loading = ref(false)
const submitting = ref(false)

const state = reactive({
  payMethod: 'mock'
})

const totalAmount = computed(() => {
  if (!product.value) return 0
  return product.value.price * quantity
})

const loadProduct = async () => {
  if (!productId) {
    ElMessage.error('缺少商品信息')
    router.replace('/shop')
    return
  }
  loading.value = true
  try {
    const res = await getShopProduct(productId)
    product.value = res.data
    if (res.data.stock < quantity) {
      ElMessage.warning('商品库存不足')
    }
  } catch {
    product.value = null
  } finally {
    loading.value = false
  }
}

const submitOrder = async () => {
  if (!product.value) return
  if (product.value.stock < quantity) {
    ElMessage.error('库存不足')
    return
  }
  submitting.value = true
  try {
    const orderRes = await createShopOrder({
      pondId: product.value.pondId,
      items: [{ productId: product.value.id, quantity }]
    })
    const orderId = orderRes.data?.id
    if (orderId) {
      await payShopOrder(orderId)
      ElMessage.success('支付成功')
      router.push('/shop/orders')
    }
  } catch (err: any) {
    ElMessage.error(err?.message || '下单失败')
  } finally {
    submitting.value = false
  }
}

const formatPrice = (price?: number | null) => {
  if (price == null) return '0.00'
  return (price / 100).toFixed(2)
}

onMounted(loadProduct)
</script>

<template>
  <div class="checkout-page">
    <div class="checkout-header">
      <el-button text :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h2 class="checkout-title">确认订单</h2>
      <span style="width: 60px"></span>
    </div>

    <div v-loading="loading" class="checkout-body">
      <template v-if="product">
        <el-card class="order-card" shadow="never">
          <div class="product-row">
            <el-image
              :src="product.imageUrl || 'https://placehold.co/200x200/e8f4f8/0f4c75?text=No+Image'"
              fit="cover"
              class="product-thumb"
            />
            <div class="product-main">
              <div class="product-name">{{ product.name }}</div>
              <div class="product-price">¥{{ formatPrice(product.price) }} × {{ quantity }}</div>
            </div>
            <div class="product-subtotal">¥{{ formatPrice(totalAmount) }}</div>
          </div>
        </el-card>

        <el-card class="order-card" shadow="never">
          <div class="section-title">支付方式</div>
          <el-radio-group v-model="state.payMethod">
            <el-radio value="mock">模拟支付</el-radio>
          </el-radio-group>
        </el-card>

        <div class="total-bar">
          <span class="total-label">合计：</span>
          <span class="total-price">¥{{ formatPrice(totalAmount) }}</span>
        </div>

        <el-button
          type="primary"
          size="large"
          class="submit-btn"
          :loading="submitting"
          :disabled="product.stock < quantity"
          @click="submitOrder"
        >
          {{ product.stock < quantity ? '库存不足' : '提交订单' }}
        </el-button>
      </template>

      <el-empty v-else-if="!loading" description="商品不存在或已下架" />
    </div>
  </div>
</template>

<style scoped>
.checkout-page {
  max-width: 640px;
  margin: 0 auto;
  padding: 16px;
}

.checkout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.checkout-title {
  margin: 0;
  font-size: 18px;
  color: #303133;
}

.checkout-body {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.order-card {
  border-radius: 12px;
}

.product-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.product-thumb {
  width: 80px;
  height: 80px;
  border-radius: 8px;
  flex-shrink: 0;
}

.product-main {
  flex: 1;
  min-width: 0;
}

.product-name {
  font-size: 15px;
  color: #303133;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-price {
  color: #e8a838;
  font-size: 14px;
  font-weight: 600;
}

.product-subtotal {
  color: #e8a838;
  font-size: 18px;
  font-weight: 700;
}

.section-title {
  font-size: 15px;
  font-weight: 600;
  color: #303133;
  margin-bottom: 12px;
}

.total-bar {
  display: flex;
  justify-content: flex-end;
  align-items: baseline;
  padding: 12px 0;
}

.total-label {
  font-size: 15px;
  color: #303133;
}

.total-price {
  color: #e8a838;
  font-size: 26px;
  font-weight: 700;
}

.submit-btn {
  width: 100%;
  background: #0f4c75;
  border-color: #0f4c75;
}

.submit-btn:disabled {
  opacity: 0.6;
}
</style>
