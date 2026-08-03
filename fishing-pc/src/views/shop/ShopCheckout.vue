<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ArrowLeft, CreditCard } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { createShopOrder, getShopProduct, payShopOrder, type ProductCategory, type ShopProduct } from '@/api/shop'

const route = useRoute()
const router = useRouter()

const productId = Number(route.query.productId)
const quantity = Math.max(Number(route.query.quantity) || 1, 1)

const categoryIcons: Record<ProductCategory, string> = {
  equipment: 'Goods',
  bait: 'Coin',
  fish: 'Aim',
  food: 'Bowl'
}

const product = ref<ShopProduct | null>(null)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')

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
  error.value = ''
  try {
    const res = await getShopProduct(productId)
    product.value = res.data
    if (res.data.stock < quantity) {
      ElMessage.warning('商品库存不足')
    }
  } catch (err: any) {
    product.value = null
    error.value = err?.message || '商品加载失败，请返回商城重新选择'
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
      <el-button plain :icon="ArrowLeft" @click="router.back()">返回</el-button>
      <h1>确认订单</h1>
      <el-button text @click="router.push('/shop')">继续逛商城</el-button>
    </div>

    <el-alert v-if="error" class="error-alert" type="error" :title="error" show-icon :closable="false">
      <template #default>
        <el-button size="small" type="danger" plain @click="loadProduct">重试</el-button>
      </template>
    </el-alert>

    <section v-loading="loading" class="checkout-body">
      <template v-if="product">
        <div class="checkout-main">
          <article class="order-section product-section">
            <div class="section-head">
              <span>商品信息</span>
              <strong>商城商品</strong>
            </div>
            <div class="product-row">
              <el-image v-if="product.imageUrl" :src="product.imageUrl" fit="cover" class="product-thumb" />
              <div v-else class="product-thumb product-visual" :class="product.category">
                <el-icon><component :is="categoryIcons[product.category]" /></el-icon>
              </div>
              <div class="product-main">
                <h2>{{ product.name }}</h2>
                <p>{{ product.description || '钓场精选商品，到场可用。' }}</p>
                <span>库存 {{ product.stock }}</span>
              </div>
              <div class="product-price">¥{{ formatPrice(product.price) }} x {{ quantity }}</div>
            </div>
          </article>

          <article class="order-section">
            <div class="section-head">
              <span>支付方式</span>
              <strong>后端模拟支付</strong>
            </div>
            <el-radio-group v-model="state.payMethod" class="pay-methods">
              <el-radio value="mock">
                <el-icon><CreditCard /></el-icon>
                在线模拟支付
              </el-radio>
            </el-radio-group>
          </article>

          <article class="order-section">
            <div class="section-head">
              <span>履约说明</span>
              <strong>到场自提</strong>
            </div>
            <p class="pickup-tip">下单并支付成功后，商家端会实时收到订单，到场后向商家出示订单即可领取商品。</p>
          </article>
        </div>

        <aside class="summary-panel">
          <h2>订单结算</h2>
          <div class="summary-line">
            <span>商品金额</span>
            <strong>¥{{ formatPrice(totalAmount) }}</strong>
          </div>
          <div class="summary-line">
            <span>履约方式</span>
            <strong>到场自提</strong>
          </div>
          <div class="summary-line">
            <span>支付状态</span>
            <strong>提交后自动支付</strong>
          </div>
          <div class="summary-total">
            <span>合计</span>
            <strong>¥{{ formatPrice(totalAmount) }}</strong>
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
        </aside>
      </template>

      <el-empty v-else-if="!loading && !error" description="商品不存在或已下架" />
    </section>
  </div>
</template>

<style scoped>
.checkout-page {
  width: min(1080px, calc(100% - 32px));
  margin: 0 auto;
  padding: 26px 0 40px;
}

.checkout-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.checkout-header h1 {
  margin: 0;
  color: #172521;
  font-size: 24px;
  font-weight: 900;
}

.error-alert {
  margin-bottom: 16px;
}

.checkout-body {
  min-height: 460px;
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 18px;
}

.checkout-main {
  display: grid;
  gap: 14px;
}

.order-section,
.summary-panel {
  background: #fff;
  border: 1px solid #e3ebe6;
  border-radius: 8px;
  box-shadow: 0 10px 28px rgba(25, 47, 39, 0.07);
}

.order-section {
  padding: 18px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  margin-bottom: 14px;
}

.section-head span {
  color: #172521;
  font-size: 16px;
  font-weight: 900;
}

.section-head strong {
  padding: 4px 9px;
  border-radius: 6px;
  background: #eef7f1;
  color: #1f6a58;
  font-size: 12px;
}

.product-row {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr) auto;
  align-items: center;
  gap: 14px;
}

.product-thumb {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  background: #edf4ef;
  flex-shrink: 0;
}

.product-visual {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  color: #1f6a58;
  font-size: 34px;
}

.product-visual.bait {
  background: #fff8e8;
  color: #b5742a;
}

.product-visual.fish {
  background: #edf5f7;
  color: #3d6d8c;
}

.product-visual.food {
  background: #f7f1e8;
  color: #9b621b;
}

.product-main {
  min-width: 0;
}

.product-main h2 {
  margin: 0 0 8px;
  color: #172521;
  font-size: 18px;
  line-height: 1.35;
}

.product-main p {
  margin: 0 0 8px;
  color: #697770;
  font-size: 13px;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.product-main span {
  color: #7d8984;
  font-size: 12px;
}

.product-price {
  color: #c7672e;
  font-size: 18px;
  font-weight: 900;
  white-space: nowrap;
}

.pickup-tip {
  margin: 0;
  color: #697770;
  font-size: 13px;
  line-height: 1.7;
}

.pay-methods :deep(.el-radio) {
  min-height: 40px;
}

.pay-methods :deep(.el-radio__label) {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-weight: 700;
}

.summary-panel {
  height: fit-content;
  padding: 20px;
  position: sticky;
  top: 88px;
}

.summary-panel h2 {
  margin: 0 0 18px;
  color: #172521;
  font-size: 20px;
  font-weight: 900;
}

.summary-line,
.summary-total {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.summary-line {
  padding: 10px 0;
  color: #65736d;
  border-bottom: 1px solid #edf2ef;
}

.summary-line strong {
  color: #263832;
}

.summary-total {
  margin: 18px 0;
  padding: 16px;
  border-radius: 8px;
  background: #172521;
  color: #fff;
}

.summary-total span {
  color: #b7c6bf;
}

.summary-total strong {
  color: #ffd36f;
  font-size: 24px;
}

.submit-btn {
  width: 100%;
  min-height: 44px;
  --el-button-bg-color: #1f6a58;
  --el-button-border-color: #1f6a58;
  --el-button-hover-bg-color: #185848;
  --el-button-hover-border-color: #185848;
}

@media (max-width: 880px) {
  .checkout-page {
    width: calc(100% - 20px);
    padding: 12px 0 28px;
  }

  .checkout-body {
    grid-template-columns: 1fr;
  }

  .summary-panel {
    position: static;
  }
}

@media (max-width: 620px) {
  .checkout-header {
    align-items: flex-start;
    flex-direction: column;
  }

  .product-row {
    grid-template-columns: 76px minmax(0, 1fr);
  }

  .product-thumb {
    width: 76px;
    height: 76px;
  }

  .product-price {
    grid-column: 1 / -1;
  }
}
</style>
