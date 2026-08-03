import request from '@/utils/request'

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
  userPhone: string | null
  userNickname: string | null
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

export interface PublicPond {
  id: number
  name: string
  address: string | null
  phone: string | null
  status: number
}

export interface PublicSpot {
  id: number
  spotCode: string
  status: number
  pondId: number
}

export const createCatchRecord = (data: CatchRecordParams) => {
  return request.post<any, { data: number }>('/api/catch', data)
}

export const getMyCatchRecords = () => {
  return request.get<any, { data: CatchRecord[] }>('/api/catch/my')
}

export const requestRecycle = (id: number) => {
  return request.put<any, { data: null }>(`/api/catch/${id}/request-recycle`)
}

export const getPublicPonds = () => {
  return request.get<any, { data: PublicPond[] }>('/api/ponds')
}

export const getPublicSpots = (pondId: number) => {
  return request.get<any, { data: { records: PublicSpot[]; total: number } }>('/api/fishing-spots', {
    params: { pondId, pageSize: 1000 }
  })
}

export const getPendingCatchRecords = (params?: { pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: CatchRecord[]; total: number } }>('/api/merchant/catch/pending', { params })
}

export const confirmRecycle = (id: number, recyclePrice: number) => {
  return request.put<any, { data: null }>(`/api/merchant/catch/${id}/recycle`, { recyclePrice })
}
