import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  { path: '/login', component: () => import('@/views/Login.vue'), meta: { public: true } },
  { path: '/', component: () => import('@/views/Home.vue') },
  { path: '/reservations', component: () => import('@/views/Reservations.vue') },
  { path: '/draw/:id', component: () => import('@/views/DrawResult.vue') },
  { path: '/admin', component: () => import('@/views/admin/AdminHome.vue') },
  { path: '/admin/slots', component: () => import('@/views/admin/SlotManage.vue') },
  { path: '/admin/spots', component: () => import('@/views/admin/SpotManage.vue') },
  { path: '/admin/reservations', component: () => import('@/views/admin/ReservationManage.vue') },
  { path: '/admin/draws', component: () => import('@/views/admin/DrawManage.vue') }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (!to.meta.public && !userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
