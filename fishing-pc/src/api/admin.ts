import request from '@/utils/request'

export interface FishingSpot {
  id: number
  spotCode: string
  status: number
}

export interface FishingSpotParams {
  spotCode: string
  status: number
}

export const getSpots = (params?: { pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: FishingSpot[]; total: number } }>('/api/admin/fishing-spots', { params })
}

export const createSpot = (data: FishingSpotParams) => {
  return request.post('/api/admin/fishing-spots', data)
}

export const updateSpot = (id: number, data: FishingSpotParams) => {
  return request.put(`/api/admin/fishing-spots/${id}`, data)
}

export const deleteSpot = (id: number) => {
  return request.delete(`/api/admin/fishing-spots/${id}`)
}
