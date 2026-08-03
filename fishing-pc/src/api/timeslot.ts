import request from '@/utils/request'

export interface TimeSlot {
  id: number
  pondId?: number | null
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
  remain?: number
}

export interface TimeSlotParams {
  pondId?: number | null
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
}

export const getSlots = (params?: { slotDate?: string; pondId?: number; pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: TimeSlot[]; total: number } }>('/api/admin/time-slots', { params })
}

export const getUserSlots = (params?: { slotDate?: string; pondId?: number }) => {
  return request.get<any, { data: TimeSlot[] }>('/api/time-slots', { params })
}

export const createSlot = (data: TimeSlotParams) => {
  return request.post('/api/admin/time-slots', data)
}

export const updateSlot = (id: number, data: TimeSlotParams) => {
  return request.put(`/api/admin/time-slots/${id}`, data)
}

export const deleteSlot = (id: number) => {
  return request.delete(`/api/admin/time-slots/${id}`)
}
