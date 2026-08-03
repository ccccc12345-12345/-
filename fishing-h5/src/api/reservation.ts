import request from '@/utils/request'

export interface Reservation {
  id: number
  userId: number
  slotId: number
  status: string
  createTime: string
  cancelTime: string | null
  slot?: {
    slotDate: string
    slotName: string
    startTime: string
    endTime: string
  }
  drawResult?: {
    spotCode: string
  }
}

export const bookSlot = (slotId: number) => {
  return request.post<number>('/api/reservation', { slotId })
}

export const cancelReservation = (id: number) => {
  return request.put(`/api/reservation/cancel/${id}`)
}

export const getMyReservations = () => {
  return request.get<{ records: Reservation[]; total: number }>('/api/admin/reservations', {
    params: { pageNum: 1, pageSize: 1000 }
  })
}

export const adminCancelReservation = (id: number) => {
  return request.put(`/api/admin/reservations/cancel/${id}`)
}

export const getAdminReservations = (params?: { userId?: number; slotId?: number; status?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<{ records: Reservation[]; total: number }>('/api/admin/reservations', { params })
}
