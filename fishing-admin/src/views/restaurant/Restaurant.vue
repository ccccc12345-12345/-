<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  getRestaurantMenus,
  createRestaurantOrder,
  payRestaurantOrder,
  type RestaurantMenu,
  type RestaurantOrderItem
} from '@/api/restaurant'
import request from '@/utils/request'

interface Pond {
  id: number
  name: string
  address: string | null
  phone: string | null
  status: number
}

interface FishingSpot {
  id: number
  spotCode: string
  status: number
  pondId: number | null
  pondName: string | null
}

const route = useRoute()
const router = useRouter()

const pondId = Number(route.params.pondId)

const pond = ref<Pond | null>(null)
const allMenus = ref<RestaurantMenu[]>([])
const spots = ref<FishingSpot[]>([])
const loading = ref(false)
const activeCategory = ref<'special' | 'fresh_fish' | 'cooked' | 'drink'>('special')

const cart = reactive<Map<number, number>>(new Map())
const settleDialogVisible = ref(false)
const spotId = ref<number | null>(null)
const paying = ref(false)

const categories = [
  { key: 'special', label: '招牌菜' },
  { key: 'fresh_fish', label: '鲜鱼' },
  { key: 'cooked', label: '加工菜品' },
  { key: 'drink', label: '饮品' }
]

const filteredMenus = computed(() => {
  if (activeCategory.value === 'special') {
    return allMenus.value.filter(m => m.isSpecial === 1)
  }
  return allMenus.value.filter(m => m.category === activeCategory.value)
})

const cartList = computed(() => {
  const list: { menu: RestaurantMenu; quantity: number }[] = []
  cart.forEach((quantity, menuId) => {
    const menu = allMenus.value.find(m => m.id === menuId)
    if (menu && quantity > 0) list.push({ menu, quantity })
  })
  return list
})

const totalCount = computed(() => {
  let count = 0
  cart.forEach(q => { count += q })
  return count
})

const totalAmount = computed(() => {
  return cartList.value.reduce((sum, item) => sum + item.menu.price * item.quantity, 0)
})

const loadPond = async () => {
  try {
    const res = await request.get<any, { data: Pond[] }>('/api/ponds')
    pond.value = res.data.find(p => p.id === pondId) || null
  } catch {}
}

const loadMenus = async () => {
  loading.value = true
  try {
    const res = await getRestaurantMenus(pondId)
    allMenus.value = res.data || []
  } finally {
    loading.value = false
  }
}

const loadSpots = async () => {
  try {
    const res = await request.get<any, { data: { records: FishingSpot[] } }>('/api/fishing-spots', {
      params: { pondId, pageSize: 200 }
    })
    spots.value = res.data.records || []
  } catch {}
}

const addToCart = (menu: RestaurantMenu) => {
  const current = cart.get(menu.id) || 0
  if (menu.stock >= 0 && current >= menu.stock) {
    ElMessage.warning('库存不足')
    return
  }
  cart.set(menu.id, current + 1)
}

const removeFromCart = (menu: RestaurantMenu) => {
  const current = cart.get(menu.id) || 0
  if (current <= 1) {
    cart.delete(menu.id)
  } else {
    cart.set(menu.id, current - 1)
  }
}

const getQuantity = (menuId: number) => cart.get(menuId) || 0

const openSettle = () => {
  if (totalCount.value === 0) {
    ElMessage.warning('请选择菜品')
    return
  }
  spotId.value = null
  settleDialogVisible.value = true
}

const submitOrder = async () => {
  const items: RestaurantOrderItem[] = cartList.value.map(item => ({
    menuId: item.menu.id,
    quantity: item.quantity
  }))
  paying.value = true
  try {
    const res = await createRestaurantOrder(pondId, {
      items,
      spotId: spotId.value ?? undefined,
      remark: undefined
    })
    const orderId = res.data
    await payRestaurantOrder(orderId)
    ElMessage.success('支付成功')
    cart.clear()
    settleDialogVisible.value = false
    router.push('/user/reservations')
  } catch (e: any) {
    ElMessage.error(e.message || '下单失败')
  } finally {
    paying.value = false
  }
}

const goBack = () => router.back()

const formatPrice = (price: number) => (price / 100).toFixed(2)

onMounted(() => {
  loadPond()
  loadMenus()
  loadSpots()
})
</script>

<template>
  <div class="restaurant-page">
    <header class="restaurant-header">
      <el-button text class="back-btn" @click="goBack">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <div class="header-content">
        <h1>{{ pond?.name || '鱼塘餐厅' }}</h1>
        <p class="intro">现点现做，配送至您的钓位</p>
      </div>
    </header>

    <div class="category-tabs">
      <div
        v-for="cat in categories"
        :key="cat.key"
        class="tab-item"
        :class="{ active: activeCategory === cat.key }"
        @click="activeCategory = cat.key as any"
      >
        {{ cat.label }}
      </div>
    </div>

    <div class="menu-list" v-loading="loading">
      <div v-for="menu in filteredMenus" :key="menu.id" class="menu-card">
        <div class="menu-image">
          <img v-if="menu.imageUrl" :src="menu.imageUrl" :alt="menu.name" />
          <div v-else class="image-placeholder">
            <el-icon><Food /></el-icon>
          </div>
          <div v-if="menu.isSpecial === 1" class="special-badge">招牌</div>
        </div>
        <div class="menu-info">
          <h3>{{ menu.name }}</h3>
          <p v-if="menu.description" class="desc">{{ menu.description }}</p>
          <div class="menu-footer">
            <span class="price">¥{{ formatPrice(menu.price) }}</span>
            <span v-if="menu.stock >= 0" class="stock">库存 {{ menu.stock }}</span>
            <div class="quantity-control">
              <el-button v-if="getQuantity(menu.id) > 0" circle size="small" @click="removeFromCart(menu)">
                <el-icon><Minus /></el-icon>
              </el-button>
              <span v-if="getQuantity(menu.id) > 0" class="quantity">{{ getQuantity(menu.id) }}</span>
              <el-button circle size="small" type="primary" @click="addToCart(menu)">
                <el-icon><Plus /></el-icon>
              </el-button>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="!loading && filteredMenus.length === 0" description="暂无菜品" />
    </div>

    <div class="cart-bar">
      <div class="cart-info">
        <el-badge :value="totalCount" :hidden="totalCount === 0" class="cart-badge">
          <el-icon :size="28"><ShoppingCart /></el-icon>
        </el-badge>
        <span class="total">合计 <strong>¥{{ formatPrice(totalAmount) }}</strong></span>
      </div>
      <el-button type="primary" size="large" :disabled="totalCount === 0" @click="openSettle">
        去结算
      </el-button>
    </div>

    <el-dialog v-model="settleDialogVisible" title="确认订单" width="480px">
      <div class="settle-body">
        <el-form label-width="80px">
          <el-form-item label="送达钓位">
            <el-select v-model="spotId" placeholder="选择钓位（可选）" clearable style="width: 100%">
              <el-option
                v-for="spot in spots"
                :key="spot.id"
                :label="spot.spotCode"
                :value="spot.id"
              />
            </el-select>
          </el-form-item>
        </el-form>
        <div class="order-items">
          <div v-for="item in cartList" :key="item.menu.id" class="order-item">
            <span>{{ item.menu.name }} x{{ item.quantity }}</span>
            <span>¥{{ formatPrice(item.menu.price * item.quantity) }}</span>
          </div>
        </div>
        <div class="order-total">
          <span>合计</span>
          <strong>¥{{ formatPrice(totalAmount) }}</strong>
        </div>
      </div>
      <template #footer>
        <el-button @click="settleDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="paying" @click="submitOrder">确认支付</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.restaurant-page {
  min-height: 100vh;
  background: #f5f7fa;
  padding-bottom: 80px;
}

.restaurant-header {
  background: linear-gradient(135deg, #0f4c75 0%, #3282b8 100%);
  color: white;
  padding: 16px 24px 24px;
  position: relative;
}

.back-btn {
  color: white;
  padding: 0;
  margin-bottom: 8px;
}

.header-content h1 {
  margin: 0 0 6px;
  font-size: 22px;
}

.intro {
  margin: 0;
  opacity: 0.85;
  font-size: 14px;
}

.category-tabs {
  display: flex;
  gap: 12px;
  padding: 16px 24px;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
  overflow-x: auto;
}

.tab-item {
  flex-shrink: 0;
  padding: 8px 18px;
  border-radius: 20px;
  background: #f0f2f5;
  color: #606266;
  font-size: 14px;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-item.active {
  background: #0f4c75;
  color: white;
}

.menu-list {
  padding: 16px 24px;
}

.menu-card {
  display: flex;
  background: white;
  border-radius: 12px;
  padding: 14px;
  margin-bottom: 12px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.menu-image {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  overflow: hidden;
  flex-shrink: 0;
  position: relative;
  background: #eef1f5;
}

.menu-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #909399;
}

.special-badge {
  position: absolute;
  top: 0;
  left: 0;
  background: #e6a23c;
  color: white;
  font-size: 12px;
  padding: 2px 8px;
  border-bottom-right-radius: 8px;
}

.menu-info {
  flex: 1;
  margin-left: 14px;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.menu-info h3 {
  margin: 0 0 6px;
  font-size: 16px;
  color: #303133;
}

.desc {
  margin: 0;
  font-size: 12px;
  color: #909399;
  line-height: 1.5;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.menu-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 8px;
}

.price {
  color: #f56c6c;
  font-size: 18px;
  font-weight: 700;
}

.stock {
  font-size: 12px;
  color: #909399;
  margin-left: 8px;
  flex: 1;
}

.quantity-control {
  display: flex;
  align-items: center;
  gap: 8px;
}

.quantity {
  min-width: 20px;
  text-align: center;
  font-size: 14px;
}

.cart-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: white;
  padding: 10px 24px;
  box-shadow: 0 -2px 10px rgba(0, 0, 0, 0.08);
  display: flex;
  align-items: center;
  justify-content: space-between;
  z-index: 100;
}

.cart-info {
  display: flex;
  align-items: center;
  gap: 12px;
}

.cart-badge :deep(.el-badge__content) {
  background: #f56c6c;
}

.total {
  color: #606266;
  font-size: 14px;
}

.total strong {
  color: #f56c6c;
  font-size: 20px;
}

.settle-body {
  max-height: 60vh;
  overflow-y: auto;
}

.order-items {
  margin-top: 16px;
  border-top: 1px solid #ebeef5;
  padding-top: 12px;
}

.order-item {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 14px;
  color: #606266;
}

.order-total {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #ebeef5;
  font-size: 16px;
}

.order-total strong {
  color: #f56c6c;
  font-size: 22px;
}

@media (max-width: 768px) {
  .restaurant-header {
    padding: 12px 16px 20px;
  }

  .category-tabs {
    padding: 12px 16px;
  }

  .menu-list {
    padding: 12px 16px;
  }

  .menu-card {
    padding: 12px;
  }

  .menu-image {
    width: 80px;
    height: 80px;
  }

  .cart-bar {
    padding: 10px 16px;
  }
}
</style>
