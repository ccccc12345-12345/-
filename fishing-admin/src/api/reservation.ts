import request from '@/utils/request'

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
  // 时段信息（后端 ReservationVO 平铺返回）
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  drawStartTime: string
  drawEndTime: string
  // 抽号结果钓位号
  spotCode: string | null
  // 用户信息
  userPhone: string | null
  userNickname: string | null
}

export const bookSlot = (slotId: number) => {
  return request.post<any, { data: number }>('/api/reservation', { slotId })
}

export const cancelReservation = (id: number) => {
  return request.put(`/api/reservation/cancel/${id}`)
}

export const getMyReservations = () => {
  return request.get<any, { data: { records: Reservation[]; total: number } }>('/api/reservation/my')
}

export const adminCancelReservation = (id: number, reason?: string) => {
  return request.put(`/api/admin/reservations/cancel/${id}`, { reason })
}

export const updateActualFee = (id: number, actualFee: number) => {
  return request.put(`/api/admin/reservations/actual-fee/${id}`, { actualFee })
}

export const getAdminReservations = (params?: { userId?: number; slotId?: number; status?: string; phone?: string; startDate?: string; endDate?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: Reservation[]; total: number } }>('/api/admin/reservations', { params })
}

export const exportReservations = (params?: { userId?: number; slotId?: number; status?: string; phone?: string; startDate?: string; endDate?: string }) => {
  return request.get('/api/admin/reservations/export', {
    params,
    responseType: 'blob'
  })
}
