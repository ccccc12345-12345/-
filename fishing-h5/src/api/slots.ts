import request from '@/utils/request'

export interface TimeSlot {
  id: number
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  maxBookings: number
  advanceDays: number
  drawStartTime: string
  drawEndTime: string
  status: number
  remain?: number
}

export interface TimeSlotParams {
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  maxBookings: number
  advanceDays: number
  drawStartTime: string
  drawEndTime: string
  status: number
}

export const getSlots = (params?: { slotDate?: string; pageNum?: number; pageSize?: number }) => {
  return request.get<{ records: TimeSlot[]; total: number }>('/api/admin/time-slots', { params })
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
