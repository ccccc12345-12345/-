import request from '@/utils/request'

export interface Pond {
  id: number
  name: string
  address: string | null
  phone: string | null
  status: number
}

export interface TimeSlot {
  id: number
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  maxBookings: number
  advanceDays: number
  drawStartTime: string
  drawEndTime: string
  status: number
  defaultPrice: number | null
  pondId: number | null
  remain?: number
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

export interface FishingSpot {
  id: number
  spotCode: string
  status: number
  pondId: number | null
  pondName: string | null
}

export interface FishingSpotParams {
  spotCode: string
  status: number
  pondId?: number | null
}

export interface BatchSpotParams {
  prefix: string
  startNum: number
  endNum: number
  pondId?: number | null
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
  today: number
  week: number
  month: number
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

export interface MissedDraw {
  reservationId: number
  userId: number
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

export interface CheckinData {
  checkinCode: string
  actualFee?: number
}

export interface DashboardStats {
  todayIncome: number
  todayReservationCount: number
  todayCheckinCount: number
  occupancyRate: number
  recentReservations: Reservation[]
}

export interface BoardSpot {
  spotId: number
  spotCode: string
  status: string
  reservationId: number | null
  userNickname: string | null
  userPhone: string | null
  reservationStatus: string | null
}

export interface BoardParams {
  pondId?: number
  slotDate?: string
  slotId?: number
}

export interface ShareBoardData {
  pondId: number
  pondName: string | null
  slotId: number
  slotDate: string | null
  slotName: string | null
  spots: BoardSpot[]
}

export const dashboardStats = (pondId?: number) => {
  return request.get<any, { data: DashboardStats }>('/api/merchant/dashboard/stats', { params: { pondId } })
}

export const getMerchantPonds = () => {
  return request.get<any, { data: Pond[] }>('/api/merchant/ponds')
}

export const createMerchantPond = (data: Omit<Pond, 'id'>) => {
  return request.post('/api/merchant/ponds', data)
}

export const updateMerchantPond = (id: number, data: Omit<Pond, 'id'>) => {
  return request.put(`/api/merchant/ponds/${id}`, data)
}

export const deleteMerchantPond = (id: number) => {
  return request.delete(`/api/merchant/ponds/${id}`)
}

export const getMerchantTimeSlots = (params?: { slotDate?: string; pageNum?: number; pageSize?: number }) => {
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

export const getMerchantSpots = (params?: { pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: FishingSpot[]; total: number } }>('/api/merchant/fishing-spots', { params })
}

export const createMerchantSpot = (data: FishingSpotParams) => {
  return request.post('/api/merchant/fishing-spots', data)
}

export const updateMerchantSpot = (id: number, data: FishingSpotParams) => {
  return request.put(`/api/merchant/fishing-spots/${id}`, data)
}

export const deleteMerchantSpot = (id: number) => {
  return request.delete(`/api/merchant/fishing-spots/${id}`)
}

export const batchCreateMerchantSpots = (data: BatchSpotParams) => {
  return request.post('/api/merchant/fishing-spots/batch', data)
}

export const getMerchantReservations = (params?: ReservationQuery) => {
  return request.get<any, { data: { records: Reservation[]; total: number } }>('/api/merchant/reservations', { params })
}

export const cancelMerchantReservation = (id: number, reason?: string) => {
  return request.put(`/api/merchant/reservations/cancel/${id}`, { reason })
}

export const exportMerchantReservations = (params?: Omit<ReservationQuery, 'pageNum' | 'pageSize'>) => {
  return request.get('/api/merchant/reservations/export', { params, responseType: 'blob' })
}

export const getMerchantRevenueSummary = (pondId?: number) => {
  return request.get<any, { data: RevenueSummary }>('/api/merchant/revenue/summary', { params: { pondId } })
}

export const getMerchantRevenueList = (params?: { startDate?: string; endDate?: string; pondId?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: RevenueItem[]; total: number } }>('/api/merchant/revenue/list', { params })
}

export const exportMerchantRevenue = (params?: { startDate?: string; endDate?: string; pondId?: number }) => {
  return request.get('/api/merchant/revenue/export', { params, responseType: 'blob' })
}

export const getMerchantDrawResults = (params?: { slotId?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: DrawResult[]; total: number } }>('/api/merchant/draw-results', { params })
}

export const getMerchantMissedDraws = (slotId: number) => {
  return request.get<any, { data: MissedDraw[] }>(`/api/merchant/draw-results/missed/${slotId}`)
}

export const exportMerchantDrawResults = (params?: { slotId?: number }) => {
  return request.get('/api/merchant/draw-results/export', { params, responseType: 'blob' })
}

export const merchantCheckin = (data: CheckinData) => {
  return request.post<any, { data: CheckinResult }>('/api/merchant/checkin', data)
}

export const queryMerchantCheckin = (checkinCode: string) => {
  return request.get<any, { data: CheckinResult }>('/api/merchant/checkin', { params: { checkinCode } })
}

export const getMerchantBoard = (params?: BoardParams) => {
  return request.get<any, { data: BoardSpot[] }>('/api/merchant/pond-board', { params })
}

export const createShareLink = (data: { pondId: number; slotId: number }) => {
  return request.post<any, { data: string }>('/api/share/create', data, {
    headers: { 'X-Frontend-Base-Url': window.location.origin }
  })
}

export const getShareBoard = (params: { pondId: number; slotId: number; token: string }) => {
  return request.get<any, { data: ShareBoardData }>('/api/share/board', { params })
}
