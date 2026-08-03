import request from '@/utils/request'
import type { Pond } from './pond'
import type { TimeSlot } from './timeslot'
export type { Pond, TimeSlot }

// ===== Basic Types =====

export interface FishingSpot {
  id: number
  spotCode: string
  status: 0 | 1 | 2
  pondId: number
  coordinateX: number | string | null
  coordinateY: number | string | null
}

export interface FishingSpotPayload {
  pondId: number
  spotCode: string
  status: 0 | 1 | 2
  coordinateX?: number
  coordinateY?: number
}

export interface SpotBoardItem {
  spotId: number
  spotCode: string
  spotStatus: number
  coordinateX: number | string | null
  coordinateY: number | string | null
  status: 'free' | 'reserved' | 'using' | 'maintenance' | 'disabled' | string
  reservationId?: number | null
  reservationStatus?: string | null
  userNickname?: string | null
  userPhone?: string | null
}

export interface Reservation {
  id: number
  userId: number
  slotId: number
  status: string
  createTime: string
  cancelTime: string | null
  cancelReason: string | null
  actualFee: number | null
  checkInTime: string | null
  checkinCode: string | null
  pondId: number | null
  pondName: string | null
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  drawStartTime: string
  drawEndTime: string
  spotCode: string | null
  userPhone: string | null
  userNickname: string | null
}

export interface ReservationQuery {
  pondId?: number
  slotId?: number
  status?: string
  slotDate?: string
  startDate?: string
  endDate?: string
  pageNum?: number
  pageSize?: number
}

export interface RevenueSummary {
  todayIncome: number
  todayReservationCount: number
  todayCheckinCount: number
  occupancyRate: number
}

export interface RevenueItem {
  slotDate: string
  pondId: number | null
  pondName: string | null
  slotId: number
  slotName: string | null
  totalCount: number
  checkinCount: number
  occupancyRate: number
  totalIncome: number
}

export interface DrawResult {
  id: number
  reservationId: number
  userId: number
  slotId: number
  spotId: number
  spotCode: string
  drawTime: string
  pondId: number | null
  pondName: string | null
  userNickname: string | null
  userPhone: string | null
  slotDate: string | null
  slotName: string | null
}

export interface CheckinResult {
  reservationId: number
  userNickname: string | null
  slotDate: string | null
  slotName: string | null
  spotCode: string | null
  actualFee: number | null
  checkInTime: string | null
  status: string | null
  pondName: string | null
}

export interface DashboardStats {
  todayIncome: number
  todayReservationCount: number
  todayCheckinCount: number
  occupancyRate: number
  recentReservations: Reservation[]
}

export interface TimeSlotParams {
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  maxBookings: number
  advanceDays: number
  drawStartTime: string
  drawEndTime: string
  status: number
  defaultPrice?: number | null
  pondId?: number | null
}

// ===== Shop =====
export interface ShopProduct {
  id: number
  pondId: number | null
  merchantId: number
  category: 'equipment' | 'bait' | 'fish' | 'food'
  name: string
  price: number
  stock: number
  imageUrl: string | null
  description: string | null
  status: 'on' | 'off'
  createTime: string
  updateTime: string
}

export type ProductCategory = 'equipment' | 'bait' | 'fish' | 'food'

// ===== Restaurant =====
export interface CookingMethod {
  name: string
  price: number
}

export interface RestaurantMenu {
  id: number
  pondId: number
  merchantId: number
  name: string
  category: string
  price: number
  stock: number | null
  imageUrl: string | null
  description: string | null
  cookingMethods: CookingMethod[] | null
  isSpecial: number | null
  status: 'on' | 'off'
}

export interface RestaurantOrderItem {
  id: number
  orderId: number
  menuId: number
  menuName: string
  price: number
  quantity: number
  createTime: string
}

export interface RestaurantOrder {
  id: number
  orderNo: string
  orderType: string
  userId: number
  pondId: number
  pondName: string | null
  merchantId: number
  spotId: number | null
  spotCode: string | null
  totalAmount: number
  status: string
  remark: string | null
  userNickname: string | null
  userPhone: string | null
  items: RestaurantOrderItem[]
  createTime: string
  updateTime: string
}

// ===== Catch =====
export interface CatchRecord {
  id: number
  userId: number
  pondId: number
  pondName: string | null
  reservationId: number | null
  spotId: number | null
  spotCode: string | null
  fishType: string
  weight: number
  quantity: number
  imageUrl: string | null
  status: string
  recyclePrice: number | null
  userPhone?: string | null
  userNickname?: string | null
  createTime: string
  updateTime: string
}

// ===== Staff =====
export interface MerchantStaff {
  id: number
  merchantId: number
  staffName: string
  phone: string
  role: string
  status: number
  createTime: string
}

// ===== Pond API =====
export const getMerchantPonds = (params?: { pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: Pond[] | { records: Pond[] } }>('/api/merchant/ponds', { params })
}

export const getMerchantPond = (id: number) => {
  return request.get<any, { data: Pond }>(`/api/merchant/ponds/${id}`)
}

export const createMerchantPond = (data: Partial<Pond>) => {
  return request.post('/api/merchant/ponds', data)
}

export const updateMerchantPond = (id: number, data: Partial<Pond>) => {
  return request.put(`/api/merchant/ponds/${id}`, data)
}

export const deleteMerchantPond = (id: number) => {
  return request.delete(`/api/merchant/ponds/${id}`)
}

// ===== Fishing Spot API =====
export const getMerchantFishingSpots = (pondId: number, params?: { pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: FishingSpot[]; total: number } }>('/api/merchant/fishing-spots', {
    params: { pondId, pageNum: 1, pageSize: 200, ...params }
  })
}

export const createMerchantFishingSpot = (data: FishingSpotPayload) => {
  return request.post('/api/merchant/fishing-spots', data)
}

export const updateMerchantFishingSpot = (id: number, data: FishingSpotPayload) => {
  return request.put(`/api/merchant/fishing-spots/${id}`, data)
}

export const deleteMerchantFishingSpot = (id: number) => {
  return request.delete(`/api/merchant/fishing-spots/${id}`)
}

export const batchCreateMerchantSpots = (data: { prefix: string; startNum: number; endNum: number; pondId?: number }) => {
  return request.post('/api/merchant/fishing-spots/batch', data)
}

// ===== Time Slot API =====
export const getMerchantTimeSlots = (params?: { pondId?: number; slotDate?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: TimeSlot[]; total: number } }>('/api/merchant/time-slots', { params })
}

export const createMerchantTimeSlot = (data: TimeSlotParams) => {
  return request.post('/api/merchant/time-slots', data)
}

export const updateMerchantTimeSlot = (id: number, data: TimeSlotParams) => {
  return request.put(`/api/merchant/time-slots/${id}`, data)
}

export const deleteMerchantTimeSlot = (id: number) => {
  return request.delete(`/api/merchant/time-slots/${id}`)
}

// ===== Pond Board API =====
export const getMerchantPondBoard = (params: { pondId: number; slotId: number; date?: string }) => {
  return request.get<any, { data: SpotBoardItem[] }>('/api/merchant/pond-board', { params })
}

// ===== Dashboard API =====
export const dashboardStats = (pondId?: number) => {
  return request.get<any, { data: DashboardStats }>('/api/merchant/dashboard/stats', { params: { pondId } })
}

// ===== Reservation API =====
export const getMerchantReservations = (params?: ReservationQuery) => {
  return request.get<any, { data: { records: Reservation[]; total: number } }>('/api/merchant/reservations', { params })
}

export const cancelMerchantReservation = (id: number, reason?: string) => {
  return request.put(`/api/merchant/reservations/cancel/${id}`, { reason })
}

export const exportMerchantReservations = (params?: Omit<ReservationQuery, 'pageNum' | 'pageSize'>) => {
  return request.get('/api/merchant/reservations/export', { params, responseType: 'blob' })
}

// ===== Checkin API =====
export const merchantCheckin = (data: { checkinCode: string; actualFee?: number }) => {
  return request.post<any, { data: CheckinResult }>('/api/merchant/checkin', data)
}

export const queryMerchantCheckin = (checkinCode: string) => {
  return request.get<any, { data: CheckinResult }>('/api/merchant/checkin', { params: { checkinCode } })
}

// ===== Revenue API =====
export const getMerchantRevenueSummary = (pondId?: number) => {
  return request.get<any, { data: RevenueSummary }>('/api/merchant/revenue/summary', { params: { pondId } })
}

export const getMerchantRevenueList = (params?: { startDate?: string; endDate?: string; pondId?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: RevenueItem[] | { records: RevenueItem[]; total?: number } }>('/api/merchant/revenue/list', { params })
}

export const exportMerchantRevenue = (params?: { startDate?: string; endDate?: string; pondId?: number }) => {
  return request.get('/api/merchant/revenue/export', { params, responseType: 'blob' })
}

// ===== Draw Result API =====
export const getMerchantDrawResults = (params?: { slotId?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: DrawResult[]; total: number } }>('/api/merchant/draw-results', { params })
}

export const exportMerchantDrawResults = (params?: { slotId?: number }) => {
  return request.get('/api/merchant/draw-results/export', { params, responseType: 'blob' })
}

// ===== Share API =====
export const createShareLink = (data: { pondId: number; slotId: number }) => {
  return request.post<any, { data: string }>('/api/share/create', data, {
    headers: { 'X-Frontend-Base-Url': window.location.origin }
  })
}

// ===== Restaurant Merchant API =====
export const getMerchantRestaurantMenus = (params?: { pondId?: number; category?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: RestaurantMenu[] | { records: RestaurantMenu[] } }>('/api/merchant/restaurant/menus', { params })
}

export const createMerchantRestaurantMenu = (data: Partial<RestaurantMenu>) => {
  return request.post('/api/merchant/restaurant/menus', data)
}

export const updateMerchantRestaurantMenu = (id: number, data: Partial<RestaurantMenu>) => {
  return request.put(`/api/merchant/restaurant/menus/${id}`, data)
}

export const updateMerchantRestaurantMenuStatus = (id: number, status: 'on' | 'off') => {
  return request.put(`/api/merchant/restaurant/menus/${id}/status`, null, { params: { status } })
}

export const deleteMerchantRestaurantMenu = (id: number) => {
  return request.delete(`/api/merchant/restaurant/menus/${id}`)
}

export const getMerchantRestaurantOrders = (params?: { pondId?: number; status?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: RestaurantOrder[]; total: number } }>('/api/merchant/restaurant/orders', { params })
}

export const updateMerchantRestaurantOrderStatus = (id: number, status: string) => {
  return request.put(`/api/merchant/restaurant/orders/${id}/status`, null, { params: { status } })
}

// ===== Catch Merchant API =====
export const getMerchantCatches = (params?: { pondId?: number; status?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: CatchRecord[]; total: number } }>('/api/merchant/catches', { params })
}

export const updateMerchantCatchStatus = (id: number, data: { status: string; recyclePrice?: number }) => {
  return request.put(`/api/merchant/catches/${id}/status`, data)
}

// ===== Staff API =====
export const getMerchantStaffs = (params?: { pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: MerchantStaff[] | { records: MerchantStaff[]; total?: number } }>('/api/merchant/staff/list', { params })
}

export const createMerchantStaff = (data: { staffName: string; phone: string; role: string }) => {
  return request.post('/api/merchant/staff', data)
}

export const updateMerchantStaff = (id: number, data: { staffName?: string; phone?: string; role?: string }) => {
  return request.put(`/api/merchant/staff/${id}`, data)
}

export const updateMerchantStaffStatus = (id: number, status: number) => {
  return request.put(`/api/merchant/staff/${id}/status`, null, { params: { status } })
}

export const resetMerchantStaffPassword = (id: number) => {
  return request.put(`/api/merchant/staff/${id}/reset-password`)
}

export const deleteMerchantStaff = (id: number) => {
  return request.delete(`/api/merchant/staff/${id}`)
}

// ===== Shop Merchant API =====
export const getMerchantShopProducts = (params?: { keyword?: string; category?: string; status?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: ShopProduct[]; total: number } }>('/api/merchant/shop/products', { params })
}

export const createMerchantShopProduct = (data: Partial<ShopProduct>) => {
  return request.post('/api/merchant/shop/products', data)
}

export const updateMerchantShopProduct = (id: number, data: Partial<ShopProduct>) => {
  return request.put(`/api/merchant/shop/products/${id}`, data)
}

export const updateMerchantShopProductStatus = (id: number, status: 'on' | 'off') => {
  return request.put(`/api/merchant/shop/products/${id}/status`, null, { params: { status } })
}

export const deleteMerchantShopProduct = (id: number) => {
  return request.delete(`/api/merchant/shop/products/${id}`)
}

// ===== Status Labels =====
export const spotStatusText: Record<number, string> = {
  0: '禁用',
  1: '可用',
  2: '维修'
}

export const boardStatusText: Record<string, string> = {
  free: '空闲',
  reserved: '已预约',
  using: '使用中',
  maintenance: '维修',
  disabled: '禁用'
}
