import request from '@/utils/request'

export type CatchStatus = 'pending' | 'recycle_requested' | 'sold_recycle' | 'sold_restaurant' | 'released'

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
  status: CatchStatus
  recyclePrice: number | null
  userPhone?: string | null
  userNickname?: string | null
  createTime: string
  updateTime: string
}

export interface CatchRecordParams {
  pondId: number
  reservationId?: number | null
  spotId?: number | null
  fishType: string
  weight: number
  quantity: number
  imageUrl?: string | null
}

export const catchStatusLabels: Record<string, string> = {
  pending: '待处理',
  recycle_requested: '已申请回收',
  sold_recycle: '已回收',
  sold_restaurant: '已入餐厅',
  released: '已放生'
}

export const createCatchRecord = (data: CatchRecordParams) => {
  return request.post<any, { data: number }>('/api/catch', data)
}

export const getMyCatches = () => {
  return request.get<any, { data: CatchRecord[] }>('/api/catch/my')
}

export const requestRecycle = (id: number) => {
  return request.put(`/api/catch/${id}/request-recycle`)
}
