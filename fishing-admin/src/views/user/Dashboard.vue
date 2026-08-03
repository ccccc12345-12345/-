<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { getUserSlots } from '@/api/slots'
import { getMyReservations } from '@/api/reservation'
import { getMyDraws } from '@/api/draw'

const router = useRouter()

// 统计数据
const slotCount = ref(0)
const pendingCount = ref(0)
const drawnCount = ref(0)
const latestSpotCode = ref<string | null>(null)
const loading = ref(true)

let timer: ReturnType<typeof setInterval> | null = null

const fetchData = async () => {
  try {
    const [slotsRes, myResRes, drawsRes] = await Promise.all([
      getUserSlots(),
      getMyReservations(),
      getMyDraws()
    ])

    // 可预约场次：过滤未来有效日期
    const slots = slotsRes.data ?? []
    slotCount.value = slots.filter((s: any) => {
      if (!s.slotDate) return false
      const d = new Date(s.slotDate + 'T00:00:00')
      return d >= new Date(new Date().toDateString())
    }).length

    // 我的预约统计
    const reservations = myResRes.data?.records ?? []
    pendingCount.value = reservations.filter((r: any) => r.status === '待抽号').length
    drawnCount.value = reservations.filter((r: any) => r.status === '已抽号').length

    // 最新抽中钓位
    const draws = drawsRes.data ?? []
    if (draws.length > 0 && draws[0].spotCode) {
      latestSpotCode.value = draws[0].spotCode
    } else {
      latestSpotCode.value = null
    }
  } catch {
    // 静默失败，保留上次数据
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  fetchData()
  timer = setInterval(fetchData, 30000)
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})

const goBooking = () => router.push('/user/booking')
const goReservations = () => router.push('/user/reservations')
const goSpots = () => router.push('/user/spots')
const goShop = () => router.push('/shop')
const goCatches = () => router.push('/user/catches')
const goRestaurant = () => router.push('/restaurant')
</script>

<template>
  <div class="dashboard">
    <h2 class="page-title">欢迎回来</h2>
    <div class="card-grid">
      <!-- 卡片一：场地预约 -->
      <el-card shadow="hover" class="dash-card card-booking" @click="goBooking">
        <div class="card-icon"><el-icon :size="48"><Calendar /></el-icon></div>
        <h3 class="card-title">场地预约</h3>
        <div class="card-stats">
          <el-tag type="primary" effect="dark" size="large">
            可预约场次：{{ loading ? '...' : slotCount }} 个
          </el-tag>
        </div>
        <p class="card-desc">选择日期和时段，预约您心仪的钓鱼时间</p>
        <div class="card-bottom">
          <span class="card-link">立即前往 <el-icon><ArrowRight /></el-icon></span>
        </div>
      </el-card>

      <!-- 卡片二：我的预约 -->
      <el-card shadow="hover" class="dash-card card-reservations" @click="goReservations">
        <div class="card-icon"><el-icon :size="48"><Tickets /></el-icon></div>
        <h3 class="card-title">我的预约</h3>
        <div class="card-stats">
          <el-tag type="warning" effect="dark" class="stat-tag">
            待抽号：{{ loading ? '...' : pendingCount }} 个
          </el-tag>
          <el-tag type="success" effect="dark" class="stat-tag">
            已抽号：{{ loading ? '...' : drawnCount }} 个
          </el-tag>
        </div>
        <p class="card-desc">查看预约记录，参与一键抽号</p>
        <div class="card-bottom">
          <span class="card-link">查看详情 <el-icon><ArrowRight /></el-icon></span>
        </div>
      </el-card>

      <!-- 卡片三：我的钓位 -->
      <el-card shadow="hover" class="dash-card card-spots" @click="goSpots">
        <div class="card-icon"><el-icon :size="48"><MapLocation /></el-icon></div>
        <h3 class="card-title">我的钓位</h3>
        <div class="card-stats">
          <el-tag
            :type="latestSpotCode ? 'success' : 'info'"
            effect="dark"
            size="large"
          >
            {{ loading ? '加载中...' : latestSpotCode ? '已抽中钓位：' + latestSpotCode : '暂无抽中钓位' }}
          </el-tag>
        </div>
        <p class="card-desc">查看已抽中的钓位信息，作为到场凭证</p>
        <div class="card-bottom">
          <span class="card-link">查看详情 <el-icon><ArrowRight /></el-icon></span>
        </div>
      </el-card>

      <!-- 卡片四：渔具商城 -->
      <el-card shadow="hover" class="dash-card card-shop" @click="goShop">
        <div class="card-icon"><el-icon :size="48"><Goods /></el-icon></div>
        <h3 class="card-title">渔具商城</h3>
        <div class="card-stats">
          <el-tag type="warning" effect="dark" size="large">钓具 / 饵料 / 鱼获</el-tag>
        </div>
        <p class="card-desc">在线购买钓具、饵料和新鲜鱼获</p>
        <div class="card-bottom">
          <span class="card-link">立即选购 <el-icon><ArrowRight /></el-icon></span>
        </div>
      </el-card>

      <!-- 卡片五：我的渔获 -->
      <el-card shadow="hover" class="dash-card card-catches" @click="goCatches">
        <div class="card-icon"><el-icon :size="48"><Document /></el-icon></div>
        <h3 class="card-title">我的渔获</h3>
        <div class="card-stats">
          <el-tag type="info" effect="dark" size="large">记录 / 回收 / 放流</el-tag>
        </div>
        <p class="card-desc">记录渔获并申请回收，轻松变现</p>
        <div class="card-bottom">
          <span class="card-link">记录渔获 <el-icon><ArrowRight /></el-icon></span>
        </div>
      </el-card>

      <!-- 卡片六：鱼塘餐厅 -->
      <el-card shadow="hover" class="dash-card card-restaurant" @click="goRestaurant">
        <div class="card-icon"><el-icon :size="48"><ForkSpoon /></el-icon></div>
        <h3 class="card-title">鱼塘餐厅</h3>
        <div class="card-stats">
          <el-tag type="danger" effect="dark" size="large">鲜鱼 / 加工 / 饮品</el-tag>
        </div>
        <p class="card-desc">点餐送到钓位，享受新鲜渔乐美食</p>
        <div class="card-bottom">
          <span class="card-link">去点餐 <el-icon><ArrowRight /></el-icon></span>
        </div>
      </el-card>
    </div>
  </div>
</template>

<style scoped>
.dashboard {
  max-width: 1200px;
  margin: 0 auto;
  padding: 20px;
}

.page-title {
  font-size: 22px;
  color: #303133;
  margin-bottom: 30px;
  font-weight: 600;
}

.card-grid {
  display: flex;
  gap: 24px;
  justify-content: center;
  flex-wrap: wrap;
}

.dash-card {
  width: 340px;
  min-width: 300px;
  border-radius: 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
  border: none;
}

.dash-card:hover {
  transform: translateY(-6px);
  box-shadow: 0 12px 32px rgba(0, 0, 0, 0.12) !important;
}

.dash-card :deep(.el-card__body) {
  padding: 32px 28px;
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
  min-height: 280px;
}

.card-booking {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
}

.card-reservations {
  background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%);
  color: white;
}

.card-spots {
  background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%);
  color: white;
}

.card-shop {
  background: linear-gradient(135deg, #f6d365 0%, #fda085 100%);
  color: white;
}

.card-catches {
  background: linear-gradient(135deg, #84fab0 0%, #8fd3f4 100%);
  color: white;
}

.card-restaurant {
  background: linear-gradient(135deg, #a18cd1 0%, #fbc2eb 100%);
  color: white;
}

.card-icon {
  margin-bottom: 16px;
  opacity: 0.9;
}

.card-icon :deep(.el-icon) {
  filter: drop-shadow(0 2px 8px rgba(255, 255, 255, 0.3));
}

.card-title {
  font-size: 22px;
  font-weight: 700;
  margin: 0 0 16px 0;
  color: white;
  letter-spacing: 1px;
}

.card-stats {
  margin-bottom: 12px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  align-items: center;
}

.stat-tag {
  margin: 0;
}

:deep(.el-tag) {
  font-size: 14px;
  padding: 0 14px;
  height: 30px;
  line-height: 30px;
  border: none;
  font-weight: 500;
}

.card-desc {
  font-size: 13px;
  opacity: 0.85;
  margin: 8px 0 0;
  line-height: 1.5;
  color: white;
}

.card-bottom {
  margin-top: auto;
  padding-top: 16px;
}

.card-link {
  color: white;
  font-size: 14px;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  opacity: 0.9;
  transition: opacity 0.2s;
}

.dash-card:hover .card-link {
  opacity: 1;
}

@media (max-width: 1100px) {
  .card-grid {
    flex-direction: column;
    align-items: center;
  }
  .dash-card {
    width: 100%;
    max-width: 400px;
  }
}
</style>
