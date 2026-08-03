import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/userStore'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/user/booking',
    children: [
      { path: 'user/booking', component: () => import('@/views/user/Booking.vue'), meta: { title: '预约' } },
      { path: 'user/reservations', component: () => import('@/views/user/Reservations.vue'), meta: { title: '我的预约' } },
      { path: 'user/reservations/:id', component: () => import('@/views/user/ReservationDetail.vue'), meta: { title: '预约详情' } },
      { path: 'user/spots', component: () => import('@/views/user/MySpots.vue'), meta: { title: '我的钓位' } },
      { path: 'user/catches', component: () => import('@/views/user/MyCatches.vue'), meta: { title: '渔获回收' } },
      { path: 'restaurant', component: () => import('@/views/restaurant/RestaurantHome.vue'), meta: { title: '鱼塘餐厅' } },
      { path: 'restaurant/orders', component: () => import('@/views/restaurant/RestaurantOrders.vue'), meta: { title: '餐厅订单' } },
      { path: 'shop', component: () => import('@/views/shop/ShopHome.vue'), meta: { title: '商城' } },
      { path: 'shop/product/:productId', component: () => import('@/views/shop/ShopProductDetail.vue'), meta: { title: '商品详情' } },
      { path: 'shop/checkout', component: () => import('@/views/shop/ShopCheckout.vue'), meta: { title: '确认订单' } },
      { path: 'shop/orders', component: () => import('@/views/shop/ShopOrders.vue'), meta: { title: '商城订单' } }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { admin: true },
    redirect: '/admin/slots',
    children: [
      { path: 'slots', component: () => import('@/views/admin/SlotManage.vue'), meta: { title: '时段配置' } },
      { path: 'spots', component: () => import('@/views/admin/SpotManage.vue'), meta: { title: '钓位管理' } },
      { path: 'reservations', component: () => import('@/views/admin/ReservationManage.vue'), meta: { title: '预约管理' } },
      { path: 'draws', component: () => import('@/views/admin/DrawManage.vue'), meta: { title: '抽号记录' } }
    ]
  },
  {
    path: '/merchant',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { merchant: true },
    redirect: '/merchant/ponds',
    children: [
      { path: 'ponds', component: () => import('@/views/merchant/MerchantPonds.vue'), meta: { title: '鱼塘管理' } },
      { path: 'ponds/:id/spots', component: () => import('@/views/merchant/MerchantSpotEditor.vue'), meta: { title: '钓位可视化' } },
      { path: 'pond-board', component: () => import('@/views/merchant/MerchantPondBoard.vue'), meta: { title: '鱼塘看板' } },
      { path: 'shop/products', component: () => import('@/views/merchant/MerchantShopProducts.vue'), meta: { title: '商品管理' } },
      { path: 'shop/orders', component: () => import('@/views/merchant/MerchantShopOrders.vue'), meta: { title: '商城订单' } },
      { path: 'dashboard', component: () => import('@/views/merchant/MerchantDashboard.vue'), meta: { title: '工作台' } },
      { path: 'time-slots', component: () => import('@/views/merchant/MerchantTimeSlots.vue'), meta: { title: '场次管理' } },
      { path: 'reservations', component: () => import('@/views/merchant/MerchantReservations.vue'), meta: { title: '预约管理' } },
      { path: 'checkin', component: () => import('@/views/merchant/MerchantCheckin.vue'), meta: { title: '核销签到' } },
      { path: 'revenue', component: () => import('@/views/merchant/MerchantRevenue.vue'), meta: { title: '收益统计' } },
      { path: 'restaurant/menus', component: () => import('@/views/merchant/MerchantRestaurantMenus.vue'), meta: { title: '餐厅菜单' } },
      { path: 'restaurant/orders', component: () => import('@/views/merchant/MerchantRestaurantOrders.vue'), meta: { title: '餐厅订单' } },
      { path: 'catches', component: () => import('@/views/merchant/MerchantCatches.vue'), meta: { title: '渔获回收' } },
      { path: 'staff', component: () => import('@/views/merchant/MerchantStaff.vue'), meta: { title: '员工管理' } }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const merchantAccount = userStore.isMerchant || userStore.isStaff

  if (!to.meta.public && !userStore.token) {
    next('/login')
    return
  }

  if (to.path === '/login' && userStore.token) {
    if (userStore.isAdmin) {
      next('/admin/slots')
    } else if (merchantAccount) {
      next('/merchant/ponds')
    } else {
      next('/user/booking')
    }
    return
  }

  // 商家也可以访问用户端（商城、餐厅、预约等）
  if (to.meta.admin && !userStore.isAdmin) {
    next('/user/booking')
    return
  }

  if (to.meta.merchant && !merchantAccount) {
    next(userStore.isAdmin ? '/admin/slots' : '/user/booking')
    return
  }

  next()
})

export default router
