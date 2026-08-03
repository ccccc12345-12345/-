import request from '@/utils/request'

export interface Staff {
  id: number
  staffName: string
  phone: string
  role: string
  status: 'normal' | 'disabled'
  createTime: string
}

export interface StaffParams {
  staffName: string
  phone: string
  role: string
  password?: string
}

export interface StaffUpdateParams {
  staffName: string
  phone: string
  role: string
}

export const getStaffList = (keyword?: string) => {
  return request.get<any, { data: Staff[] }>('/api/merchant/staff/list', { params: { keyword } })
}

export const createStaff = (data: StaffParams) => {
  return request.post('/api/merchant/staff', data)
}

export const updateStaff = (id: number, data: StaffUpdateParams) => {
  return request.put(`/api/merchant/staff/${id}`, data)
}

export const updateStaffStatus = (id: number, status: 'normal' | 'disabled') => {
  return request.put(`/api/merchant/staff/${id}/status`, { status })
}

export const resetStaffPassword = (id: number, newPassword: string) => {
  return request.put<any, { data: string }>(`/api/merchant/staff/${id}/reset-password`, { newPassword })
}

export const deleteStaff = (id: number) => {
  return request.delete(`/api/merchant/staff/${id}`)
}
