import request from '@/utils/request'

export interface ReservationVO {
  id: number
  userId: number
  slotId: number
  status: string
  createTime: string
  cancelTime: string | null
  cancelReason: string | null
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  drawStartTime: string
  drawEndTime: string
  spotCode: string | null
  spotId?: number | null
  pondId?: number | null
  pondName?: string | null
  checkinCode?: string | null
  actualFee?: number | null
  checkInTime?: string | null
}

export const bookSlot = (slotId: number) => {
  return request.post<any, { data: number }>('/api/reservation', { slotId })
}

export const bookSlotDirect = (slotId: number) => {
  return request.post<any, { data: ReservationVO }>('/api/reservation/direct', { slotId })
}

export const cancelReservation = (id: number) => {
  return request.put<any, any>(`/api/reservation/cancel/${id}`)
}

export interface ReservationPage {
  records?: ReservationVO[]
  total?: number
}

export const normalizeReservationList = (data: ReservationVO[] | ReservationPage | null | undefined): ReservationVO[] => {
  if (Array.isArray(data)) return data
  if (Array.isArray(data?.records)) return data.records
  return []
}

export const getMyReservations = async () => {
  try {
    const res = await request.get<any, { data: ReservationVO[] | ReservationPage }>('/api/reservation/my', { silent: true })
    return { ...res, data: normalizeReservationList(res.data) }
  } catch {
    return { data: [] as ReservationVO[] }
  }
}

export const getReservationDetail = (id: number) => {
  return request.get<any, { data: ReservationVO }>(`/api/reservation/${id}`)
}

export interface ReservationQuery {
  status?: string
  userId?: number
  slotId?: number
  pageNum?: number
  pageSize?: number
}

export interface ReservationRecord {
  id: number
  userId: number
  slotId: number
  status: string
  createTime: string
  cancelTime: string | null
  cancelReason: string | null
  userPhone: string | null
  userNickname: string | null
  slotDate: string | null
  slotName: string | null
  spotCode: string | null
}

export const getAdminReservations = (params?: ReservationQuery) => {
  return request.get<any, { data: { records: ReservationRecord[]; total: number } }>('/api/admin/reservations', { params })
}

export const adminCancelReservation = (id: number, reason?: string) => {
  return request.put<any, any>(`/api/admin/reservations/cancel/${id}`, { reason })
}

export const exportReservations = () => {
  window.open('/api/admin/reservations/export')
}
