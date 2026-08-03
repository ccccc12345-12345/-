import request from '@/utils/request'

export interface DrawResultVO {
  id: number
  reservationId: number
  userId: number
  userPhone: string
  userNickname: string
  slotId: number
  slotDate: string
  slotName: string
  spotCode: string
  drawTime: string
}

export interface DrawQuery {
  userId?: number
  slotId?: number
  spotId?: number
  pageNum?: number
  pageSize?: number
}

export interface MissedDrawVO {
  reservationId: number
  userId: number
  userPhone: string
  userNickname: string
  slotId: number
  slotDate: string
  slotName: string
}

export const drawSpot = (reservationId: number) => {
  return request.post<any, { data: string }>(`/api/draw/${reservationId}`)
}

export const getMyDraws = () => {
  return request.get<any, { data: DrawResultVO[] }>('/api/draw/my')
}

export const getDrawResults = (params?: DrawQuery) => {
  return request.get<any, { data: { records: DrawResultVO[]; total: number } }>('/api/admin/draw-results', { params })
}

export const getMissedDrawList = (slotId: number) => {
  return request.get<any, { data: MissedDrawVO[] }>(`/api/admin/draw-results/missed/${slotId}`)
}

export const exportDrawResults = () => {
  window.open('/api/admin/draw-results/export')
}
