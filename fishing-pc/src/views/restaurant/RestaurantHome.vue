<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPonds, type Pond } from '@/api/pond'
import {
  createRestaurantOrder,
  getRestaurantMenus,
  payRestaurantOrder,
  restaurantCategoryLabels,
  type RestaurantCategory,
  type RestaurantMenu
} from '@/api/restaurant'
import { getMyReservations, type ReservationVO } from '@/api/reservation'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const submitting = ref(false)
const ponds = ref<Pond[]>([])
const reservations = ref<ReservationVO[]>([])
const menus = ref<RestaurantMenu[]>([])
const category = ref<RestaurantCategory | 'all'>('all')
const pondId = ref<number | undefined>(undefined)
const reservationId = ref<number | undefined>(undefined)
const remark = ref('')
const cart = reactive<Record<number, number>>({})

const assignedReservations = computed(() => reservations.value.filter((item) => item.pondId && item.spotId && !item.status.includes('取消')))
const selectedReservation = computed(() => assignedReservations.value.find((item) => item.id === reservationId.value))
const filteredMenus = computed(() => menus.value.filter((item) => category.value === 'all' || item.category === category.value))
const cartItems = computed(() =>
  Object.entries(cart)
    .map(([id, quantity]) => {
      const menu = menus.value.find((item) => item.id === Number(id))
      return menu ? { menu, quantity } : null
    })
    .filter(Boolean) as Array<{ menu: RestaurantMenu; quantity: number }>
)
const totalAmount = computed(() => cartItems.value.reduce((sum, item) => sum + item.menu.price * item.quantity, 0))

const money = (value: number) => `¥${(value / 100).toFixed(2)}`

watch(selectedReservation, (item) => {
  if (item?.pondId) pondId.value = item.pondId
})

watch([pondId, category], () => loadMenus())

const loadBase = async () => {
  const [pondRes, reservationRes] = await Promise.all([getPonds(), getMyReservations()])
  ponds.value = pondRes.data || []
  reservations.value = reservationRes.data || []
  const queryPondId = Number(route.query.pondId)
  const queryReservationId = Number(route.query.reservationId)
  if (queryReservationId) reservationId.value = queryReservationId
  if (queryPondId) pondId.value = queryPondId
  if (!pondId.value) {
    pondId.value = selectedReservation.value?.pondId || ponds.value[0]?.id
  }
}

const loadMenus = async () => {
  if (!pondId.value) {
    menus.value = []
    return
  }
  loading.value = true
  try {
    const res = await getRestaurantMenus(pondId.value, category.value === 'all' ? undefined : category.value)
    menus.value = res.data || []
  } finally {
    loading.value = false
  }
}

const add = (menu: RestaurantMenu) => {
  if (menu.stock !== null && menu.stock >= 0 && (cart[menu.id] || 0) >= menu.stock) {
    ElMessage.warning('库存不足')
    return
  }
  cart[menu.id] = (cart[menu.id] || 0) + 1
}

const remove = (menu: RestaurantMenu) => {
  if (!cart[menu.id]) return
  cart[menu.id] -= 1
  if (cart[menu.id] <= 0) delete cart[menu.id]
}

const clearCart = () => {
  Object.keys(cart).forEach((key) => delete cart[Number(key)])
}

const submit = async () => {
  if (!pondId.value) {
    ElMessage.warning('请选择鱼塘')
    return
  }
  if (cartItems.value.length === 0) {
    ElMessage.warning('请先选择菜品')
    return
  }
  submitting.value = true
  try {
    const res = await createRestaurantOrder(pondId.value, {
      reservationId: reservationId.value || null,
      spotId: selectedReservation.value?.spotId || Number(route.query.spotId) || null,
      remark: remark.value,
      items: cartItems.value.map((item) => ({ menuId: item.menu.id, quantity: item.quantity }))
    })
    await payRestaurantOrder(res.data)
    ElMessage.success('下单并支付成功')
    clearCart()
    router.push('/restaurant/orders')
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  await loadBase()
  await loadMenus()
})
</script>

<template>
  <section class="page">
    <div class="hero">
      <div>
        <p class="eyebrow">鱼塘餐厅</p>
        <h1>绑定预约钓位，现场点餐更快送达</h1>
      </div>
      <el-button type="primary" @click="router.push('/restaurant/orders')">我的餐厅订单</el-button>
    </div>

    <div class="filter-bar">
      <el-select v-model="pondId" placeholder="选择鱼塘" style="width: 220px">
        <el-option v-for="pond in ponds" :key="pond.id" :label="pond.name" :value="pond.id" />
      </el-select>
      <el-select v-model="reservationId" clearable placeholder="绑定已预约钓位" style="width: 320px">
        <el-option
          v-for="item in assignedReservations"
          :key="item.id"
          :label="`${item.pondName || '鱼塘'} · ${item.slotDate} · 钓位 ${item.spotCode}`"
          :value="item.id"
        />
      </el-select>
      <el-segmented v-model="category" :options="Object.entries(restaurantCategoryLabels).map(([value, label]) => ({ value, label }))" />
    </div>

    <div class="workspace">
      <div class="menu-grid" v-loading="loading">
        <el-empty v-if="filteredMenus.length === 0 && !loading" description="当前鱼塘暂无菜单" />
        <article v-for="menu in filteredMenus" :key="menu.id" class="menu-card">
          <img :src="menu.imageUrl || '/demo-assets/restaurant/dish-1.svg'" alt="" />
          <div class="menu-body">
            <div class="menu-title">
              <h3>{{ menu.name }}</h3>
              <el-tag v-if="menu.isSpecial === 1" type="warning">招牌</el-tag>
            </div>
            <p>{{ menu.description || '现场加工，新鲜出餐。' }}</p>
            <div class="menu-foot">
              <strong>{{ money(menu.price) }}</strong>
              <span>库存 {{ menu.stock == null || menu.stock < 0 ? '充足' : menu.stock }}</span>
            </div>
            <el-button type="primary" @click="add(menu)">加入购物车</el-button>
          </div>
        </article>
      </div>

      <aside class="cart">
        <h2>购物车</h2>
        <div v-if="selectedReservation" class="spot-box">送至钓位：{{ selectedReservation.spotCode }}</div>
        <el-empty v-if="cartItems.length === 0" description="还没有选择菜品" />
        <div v-for="item in cartItems" :key="item.menu.id" class="cart-row">
          <span>{{ item.menu.name }}</span>
          <div>
            <el-button size="small" circle @click="remove(item.menu)">-</el-button>
            <b>{{ item.quantity }}</b>
            <el-button size="small" circle @click="add(item.menu)">+</el-button>
          </div>
        </div>
        <el-input v-model="remark" type="textarea" :rows="3" placeholder="备注：口味、送餐位置等" />
        <div class="total">
          <span>合计</span>
          <strong>{{ money(totalAmount) }}</strong>
        </div>
        <el-button type="primary" size="large" :loading="submitting" @click="submit">提交订单并模拟支付</el-button>
        <el-button @click="clearCart">清空购物车</el-button>
      </aside>
    </div>
  </section>
</template>

<style scoped>
.page {
  width: min(1280px, calc(100% - 32px));
  margin: 22px auto 48px;
}

.hero,
.filter-bar,
.cart,
.menu-card {
  border: 1px solid #e1ebe5;
  border-radius: 8px;
  background: white;
  box-shadow: 0 14px 30px rgba(21, 60, 53, 0.08);
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 24px;
  background: linear-gradient(135deg, #153c35, #24745f);
  color: white;
}

.eyebrow {
  margin: 0 0 8px;
  color: #f8c966;
  font-weight: 900;
}

.hero h1 {
  margin: 0;
  font-size: 28px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
  align-items: center;
  margin: 16px 0;
  padding: 14px;
}

.workspace {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 16px;
}

.menu-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(250px, 1fr));
  gap: 16px;
  min-height: 260px;
}

.menu-card {
  overflow: hidden;
}

.menu-card img {
  width: 100%;
  height: 150px;
  object-fit: cover;
  background: #f4f8f5;
}

.menu-body {
  display: grid;
  gap: 10px;
  padding: 14px;
}

.menu-title,
.menu-foot,
.cart-row,
.total {
  display: flex;
  justify-content: space-between;
  gap: 10px;
  align-items: center;
}

.menu-title h3 {
  margin: 0;
  color: #172521;
}

.menu-body p {
  min-height: 42px;
  margin: 0;
  color: #66766f;
}

.menu-foot strong,
.total strong {
  color: #174c41;
  font-size: 18px;
}

.cart {
  position: sticky;
  top: 108px;
  align-self: start;
  display: grid;
  gap: 12px;
  padding: 18px;
}

.cart h2 {
  margin: 0;
}

.spot-box {
  padding: 10px;
  border-radius: 8px;
  background: #f4f8f5;
  color: #174c41;
  font-weight: 900;
}

.cart-row {
  padding: 10px 0;
  border-bottom: 1px solid #edf3ef;
}

.cart-row b {
  display: inline-block;
  width: 28px;
  text-align: center;
}

@media (max-width: 980px) {
  .workspace {
    grid-template-columns: 1fr;
  }

  .cart {
    position: static;
  }
}
</style>
