import request from '@/utils/request'

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

export const drawSpot = (reservationId: number) => {
  return request.post<any, { data: string }>(`/api/draw/${reservationId}`)
}

export const getMyDraws = () => {
  return request.get<any, { data: DrawResult[] }>('/api/draw/my')
}

export const getDrawResults = (params?: { userId?: number; slotId?: number; spotId?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: DrawResult[]; total: number } }>('/api/admin/draw-results', { params })
}

export const exportDrawResults = (params?: { userId?: number; slotId?: number; spotId?: number }) => {
  return request.get('/api/admin/draw-results/export', { params, responseType: 'blob' })
}
