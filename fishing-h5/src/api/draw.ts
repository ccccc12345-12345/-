import request from '@/utils/request'

export interface DrawResult {
  id: number
  reservationId: number
  userId: number
  slotId: number
  spotId: number
  spotCode: string
  drawTime: string
}

export const drawSpot = (reservationId: number) => {
  return request.post<string>(`/api/draw/${reservationId}`)
}

export const getDrawResults = (params?: { userId?: number; slotId?: number; spotId?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<{ records: DrawResult[]; total: number }>('/api/admin/draw-results', { params })
}

export const exportDrawResults = () => {
  window.open('/api/admin/draw-results/export')
}
