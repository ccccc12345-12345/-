import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    component: () => import('@/views/Login.vue'),
    meta: { public: true }
  },
  {
    path: '/share',
    component: () => import('@/views/ShareBoard.vue'),
    meta: { public: true }
  },
  { path: '/home', redirect: '/user/dashboard' },
  { path: '/reservation', redirect: '/user/booking' },
  { path: '/my-reservations', redirect: '/user/reservations' },
  { path: '/my-spot', redirect: '/user/spots' },
  { path: '/my-catches', redirect: '/user/catches' },
  {
    path: '/',
    component: () => import('@/layout/MainLayout.vue'),
    redirect: '/user/dashboard',
    children: [
      { path: 'user/dashboard', component: () => import('@/views/user/Dashboard.vue'), meta: { title: '首页' } },
      { path: 'user/booking', component: () => import('@/views/user/Booking.vue') },
      { path: 'user/reservations', component: () => import('@/views/user/Reservations.vue') },
      { path: 'user/spots', component: () => import('@/views/user/MySpots.vue') },
      { path: 'user/catches', component: () => import('@/views/user/MyCatches.vue') },
      { path: 'user/catches/add', component: () => import('@/views/user/MyCatchAdd.vue') },
      { path: 'shop', component: () => import('@/views/shop/ShopHome.vue') },
      { path: 'shop/product/:productId', component: () => import('@/views/shop/ShopProductDetail.vue') },
      { path: 'shop/checkout', component: () => import('@/views/shop/ShopCheckout.vue') },
      { path: 'shop/orders', component: () => import('@/views/shop/ShopOrders.vue') },
      { path: 'restaurant', component: () => import('@/views/restaurant/RestaurantList.vue') },
      { path: 'restaurant/:pondId', component: () => import('@/views/restaurant/Restaurant.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { admin: true },
    redirect: '/admin/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/admin/Dashboard.vue') },
      { path: 'reservations', component: () => import('@/views/admin/ReservationManage.vue') },
      { path: 'checkin', component: () => import('@/views/admin/CheckinPage.vue') },
      { path: 'revenue', component: () => import('@/views/admin/RevenuePage.vue') },
      { path: 'slots', component: () => import('@/views/admin/SlotManage.vue') },
      { path: 'spots', component: () => import('@/views/admin/SpotManage.vue') },
      { path: 'draws', component: () => import('@/views/admin/DrawManage.vue') },
      { path: 'ponds', component: () => import('@/views/admin/PondManage.vue') },
      { path: 'users', component: () => import('@/views/admin/UserManage.vue'), meta: { superAdmin: true } }
    ]
  },
  {
    path: '/merchant',
    component: () => import('@/layout/MainLayout.vue'),
    meta: { merchant: true },
    redirect: '/merchant/dashboard',
    children: [
      { path: 'dashboard', component: () => import('@/views/merchant/MerchantDashboard.vue') },
      { path: 'ponds', component: () => import('@/views/merchant/MerchantPonds.vue'), meta: { ownerOnly: true } },
      { path: 'time-slots', component: () => import('@/views/merchant/MerchantTimeSlots.vue') },
      { path: 'spots', component: () => import('@/views/merchant/MerchantSpots.vue') },
      { path: 'pond-board', component: () => import('@/views/merchant/MerchantPondBoard.vue') },
      { path: 'checkin', component: () => import('@/views/merchant/MerchantCheckin.vue') },
      { path: 'reservations', component: () => import('@/views/merchant/MerchantReservations.vue') },
      { path: 'revenue', component: () => import('@/views/merchant/MerchantRevenue.vue') },
      { path: 'draw-results', component: () => import('@/views/merchant/MerchantDrawResults.vue') },
      { path: 'staff', component: () => import('@/views/merchant/MerchantStaff.vue'), meta: { ownerOnly: true } },
      { path: 'shop/products', component: () => import('@/views/merchant/MerchantShopProducts.vue') },
      { path: 'catches', component: () => import('@/views/merchant/MerchantCatches.vue') },
      { path: 'restaurant/menus', component: () => import('@/views/merchant/MerchantRestaurantMenus.vue') },
      { path: 'restaurant/orders', component: () => import('@/views/merchant/MerchantRestaurantOrders.vue') }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.token) {
    next('/login')
    return
  }
  if (to.meta.admin && !userStore.isAdmin) {
    next('/')
    return
  }
  if (to.meta.superAdmin && !userStore.isSuperAdmin) {
    next('/')
    return
  }
  if (to.meta.merchant && !userStore.isMerchant && !userStore.isStaff) {
    next('/')
    return
  }
  // 仅商家老板可访问的页面（员工管理、鱼塘归属）
  if (to.meta.ownerOnly && !userStore.isMerchant) {
    next('/merchant/dashboard')
    return
  }
  // 员工角色权限控制
  if (userStore.isStaff && to.meta.merchant) {
    const allowed = getAllowedMerchantPaths(userStore.staffRole)
    if (!allowed.includes(to.path)) {
      next(allowed[0] || '/merchant/dashboard')
      return
    }
  }
  next()
})

function getAllowedMerchantPaths(staffRole: string): string[] {
  switch (staffRole) {
    case 'checker':
      return ['/merchant/checkin']
    case 'operator':
      return ['/merchant/time-slots', '/merchant/spots', '/merchant/pond-board']
    case 'finance':
      return ['/merchant/revenue', '/merchant/reservations', '/merchant/shop/products', '/merchant/restaurant/orders']
    case 'manager':
      return [
        '/merchant/dashboard', '/merchant/time-slots', '/merchant/spots', '/merchant/pond-board',
        '/merchant/checkin', '/merchant/reservations', '/merchant/revenue', '/merchant/draw-results',
        '/merchant/shop/products', '/merchant/catches', '/merchant/restaurant/menus', '/merchant/restaurant/orders'
      ]
    default:
      return ['/merchant/dashboard']
  }
}

export default router
