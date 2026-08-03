import axios, { AxiosError, AxiosResponse } from 'axios'
import { clearAuthStorage } from './auth'
import type {
  Announcement,
  BoardSpot,
  CatchRecord,
  CheckinResult,
  DashboardStats,
  EventItem,
  EventReview,
  EventSignup,
  FishingSpot,
  LoginResult,
  OperationLog,
  PageResult,
  Pond,
  Reservation,
  RestaurantMenu,
  RestaurantOrder,
  RevenueItem,
  RevenueSummary,
  ShopProduct,
  Staff,
  TimeSlot
} from './types'

interface Result<T> {
  code: number
  message: string
  data: T
}

export class ApiError extends Error {
  status?: number

  constructor(message: string, status?: number) {
    super(message)
    this.status = status
  }
}

export const request = axios.create({
  baseURL: '',
  timeout: 15000
})

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('fishing_admin_token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

request.interceptors.response.use(
  (response: AxiosResponse<Result<unknown>>) => {
    if (response.config.responseType === 'blob') {
      return response as any
    }
    const body = response.data
    if (body && typeof body.code === 'number' && body.code !== 200) {
      throw new ApiError(body.message || '请求失败', body.code)
    }
    return body as any
  },
  (error: AxiosError<{ message?: string }>) => {
    const status = error.response?.status
    const message = error.response?.data?.message || error.message || '网络错误'
    if (status === 401) {
      clearAuthStorage()
    }
    throw new ApiError(message, status)
  }
)

const data = async <T,>(promise: Promise<{ data: T }>): Promise<T> => (await promise).data

export const normalizePage = <T,>(value: PageResult<T> | T[] | undefined | null): PageResult<T> => {
  if (Array.isArray(value)) {
    return { records: value, total: value.length }
  }
  return { records: value?.records || [], total: value?.total || value?.records?.length || 0, current: value?.current, size: value?.size }
}

export interface CaptchaResult {
  captchaKey: string
  imageBase64: string
}

export const api = {
  captcha: () => data<CaptchaResult>(request.get('/api/captcha')),
  login: (payload: { username: string; password: string; captchaKey: string; captchaCode: string }) =>
    data<LoginResult>(request.post('/api/login', payload)),

  dashboardStats: (pondId?: number) => data<DashboardStats>(request.get('/api/merchant/dashboard/stats', { params: clean({ pondId }) })),

  ponds: () => data<Pond[]>(request.get('/api/merchant/ponds')),
  pond: (id: number) => data<Pond>(request.get(`/api/merchant/ponds/${id}`)),
  createPond: (payload: Partial<Pond>) => data<void>(request.post('/api/merchant/ponds', payload)),
  updatePond: (id: number, payload: Partial<Pond>) => data<void>(request.put(`/api/merchant/ponds/${id}`, payload)),
  deletePond: (id: number) => data<void>(request.delete(`/api/merchant/ponds/${id}`)),

  spots: (params: { pondId: number; pageNum?: number; pageSize?: number }) =>
    data<PageResult<FishingSpot>>(request.get('/api/merchant/fishing-spots', { params })),
  createSpot: (payload: Partial<FishingSpot>) => data<void>(request.post('/api/merchant/fishing-spots', payload)),
  updateSpot: (id: number, payload: Partial<FishingSpot>) => data<void>(request.put(`/api/merchant/fishing-spots/${id}`, payload)),
  deleteSpot: (id: number) => data<void>(request.delete(`/api/merchant/fishing-spots/${id}`)),

  board: (params: { pondId: number; slotId: number; date?: string }) =>
    data<BoardSpot[]>(request.get('/api/merchant/pond-board', { params })),

  sessions: (params: { pondId: number; slotDate?: string; pageNum?: number; pageSize?: number }) =>
    data<PageResult<TimeSlot>>(request.get('/api/merchant/time-slots', { params: clean(params) })),
  createSession: (payload: Partial<TimeSlot>) => data<void>(request.post('/api/merchant/time-slots', payload)),
  updateSession: (id: number, payload: Partial<TimeSlot>) => data<void>(request.put(`/api/merchant/time-slots/${id}`, payload)),
  deleteSession: (id: number) => data<void>(request.delete(`/api/merchant/time-slots/${id}`)),

  reservations: (params: { pondId: number; pageNum?: number; pageSize?: number; status?: string }) =>
    data<PageResult<Reservation>>(request.get('/api/merchant/reservations', { params: clean(params) })),
  cancelReservation: (id: number, reason: string) => data<void>(request.put(`/api/merchant/reservations/cancel/${id}`, { reason })),
  exportReservations: (params: { pondId: number; startDate?: string; endDate?: string; status?: string }) =>
    request.get('/api/merchant/reservations/export', { params: clean(params), responseType: 'blob' }),

  queryCheckin: (checkinCode: string) => data<CheckinResult>(request.get('/api/merchant/checkin', { params: { checkinCode } })),
  checkin: (payload: { checkinCode: string; actualFee?: number }) => data<CheckinResult>(request.post('/api/merchant/checkin', payload)),

  revenueSummary: (pondId: number) => data<RevenueSummary>(request.get('/api/merchant/revenue/summary', { params: { pondId } })),
  revenueList: (params: { pondId: number; startDate?: string; endDate?: string }) =>
    data<RevenueItem[]>(request.get('/api/merchant/revenue/list', { params: clean(params) })),
  exportRevenue: (params: { pondId: number; startDate?: string; endDate?: string }) =>
    request.get('/api/merchant/revenue/export', { params: clean(params), responseType: 'blob' }),

  shopProducts: (params?: { keyword?: string; category?: string; status?: string; pageNum?: number; pageSize?: number }) =>
    data<PageResult<ShopProduct>>(request.get('/api/merchant/shop/products', { params: clean(params || {}) })),
  createShopProduct: (payload: Partial<ShopProduct>) => data<void>(request.post('/api/merchant/shop/products', payload)),
  updateShopProduct: (id: number, payload: Partial<ShopProduct>) => data<void>(request.put(`/api/merchant/shop/products/${id}`, payload)),
  updateShopProductStatus: (id: number, status: 'on' | 'off') =>
    data<void>(request.put(`/api/merchant/shop/products/${id}/status`, null, { params: { status } })),
  deleteShopProduct: (id: number) => data<void>(request.delete(`/api/merchant/shop/products/${id}`)),

  restaurantMenus: (pondId?: number) => data<RestaurantMenu[]>(request.get('/api/merchant/restaurant/menus', { params: clean({ pondId }) })),
  createRestaurantMenu: (payload: Partial<RestaurantMenu>) => data<void>(request.post('/api/merchant/restaurant/menus', payload)),
  updateRestaurantMenu: (id: number, payload: Partial<RestaurantMenu>) => data<void>(request.put(`/api/merchant/restaurant/menus/${id}`, payload)),
  updateRestaurantMenuStatus: (id: number, status: 'on' | 'off') =>
    data<void>(request.put(`/api/merchant/restaurant/menus/${id}/status`, null, { params: { status } })),
  deleteRestaurantMenu: (id: number) => data<void>(request.delete(`/api/merchant/restaurant/menus/${id}`)),
  restaurantOrders: (params?: { pondId?: number; status?: string }) =>
    data<RestaurantOrder[]>(request.get('/api/merchant/restaurant/orders', { params: clean(params || {}) })),
  updateRestaurantOrderStatus: (id: number, status: string) =>
    data<void>(request.put(`/api/merchant/restaurant/orders/${id}/status`, null, { params: { status } })),

  catches: (params?: { pageNum?: number; pageSize?: number }) =>
    data<PageResult<CatchRecord>>(request.get('/api/merchant/catch/pending', { params: clean(params || {}) })),
  recycleCatch: (id: number, recyclePrice: number) => data<void>(request.put(`/api/merchant/catch/${id}/recycle`, { recyclePrice })),

  staff: (keyword?: string) => data<Staff[]>(request.get('/api/merchant/staff/list', { params: clean({ keyword }) })),
  createStaff: (payload: Partial<Staff> & { password?: string }) => data<void>(request.post('/api/merchant/staff', payload)),
  updateStaff: (id: number, payload: Partial<Staff>) => data<void>(request.put(`/api/merchant/staff/${id}`, payload)),
  updateStaffStatus: (id: number, status: 'normal' | 'disabled') => data<void>(request.put(`/api/merchant/staff/${id}/status`, { status })),
  resetStaffPassword: (id: number, newPassword: string) =>
    data<string>(request.put(`/api/merchant/staff/${id}/reset-password`, { newPassword })),
  deleteStaff: (id: number) => data<void>(request.delete(`/api/merchant/staff/${id}`)),

  announcements: (params?: { pondId?: number; status?: string }) =>
    data<Announcement[]>(request.get('/api/merchant/announcements', { params: clean(params || {}) })),
  createAnnouncement: (payload: Partial<Announcement>) => data<void>(request.post('/api/merchant/announcements', payload)),
  updateAnnouncement: (id: number, payload: Partial<Announcement>) => data<void>(request.put(`/api/merchant/announcements/${id}`, payload)),
  deleteAnnouncement: (id: number) => data<void>(request.delete(`/api/merchant/announcements/${id}`)),

  events: (params?: { status?: string }) => data<EventItem[]>(request.get('/api/merchant/events', { params: clean(params || {}) })),
  createEvent: (payload: Partial<EventItem>) => data<void>(request.post('/api/merchant/events', payload)),
  updateEvent: (id: number, payload: Partial<EventItem>) => data<void>(request.put(`/api/merchant/events/${id}`, payload)),
  updateEventStatus: (id: number, payload: Partial<Pick<EventItem, 'status' | 'recommended' | 'pinned'>>) =>
    data<void>(request.put(`/api/merchant/events/${id}/status`, payload)),
  deleteEvent: (id: number) => data<void>(request.delete(`/api/merchant/events/${id}`)),
  eventSignups: (eventId: number) => data<EventSignup[]>(request.get(`/api/merchant/events/${eventId}/signups`)),
  auditSignup: (eventId: number, signupId: number, payload: { auditStatus: string; auditReason?: string }) =>
    data<void>(request.put(`/api/merchant/events/${eventId}/signups/${signupId}/audit`, payload)),
  checkinSignup: (eventId: number, signupId: number, checkedIn: boolean) =>
    data<void>(request.put(`/api/merchant/events/${eventId}/signups/${signupId}/checkin`, { checkedIn })),
  clearSignups: (eventId: number) => data<void>(request.delete(`/api/merchant/events/${eventId}/signups`)),
  eventReviews: (eventId: number) => data<EventReview[]>(request.get(`/api/merchant/events/${eventId}/reviews`)),
  deleteEventReview: (eventId: number, reviewId: number) =>
    data<void>(request.delete(`/api/merchant/events/${eventId}/reviews/${reviewId}`)),

  logs: (params?: { operatorName?: string; startDate?: string; endDate?: string }) =>
    data<OperationLog[]>(request.get('/api/merchant/logs', { params: clean(params || {}) }))
}

export const clean = <T extends Record<string, unknown>>(params: T): T => {
  const next: Record<string, unknown> = {}
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      next[key] = value
    }
  })
  return next as T
}

export const downloadBlob = (response: AxiosResponse<Blob>, fallbackName: string) => {
  const blob = new Blob([response.data])
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fallbackName
  link.click()
  URL.revokeObjectURL(url)
}
