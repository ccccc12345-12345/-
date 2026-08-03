import request from '@/utils/request'

export interface SysUser {
  id: number
  phone: string
  nickname: string | null
  email: string | null
  role: number
  adminType: number | null
  pondId: number | null
  status: number
  createTime: string
  lastLoginTime: string | null
}

export const getUsers = (params?: {
  keyword?: string
  pageNum?: number
  pageSize?: number
  sortField?: string
  sortOrder?: 'asc' | 'desc'
}) => {
  return request.get<any, { data: { records: SysUser[]; total: number } }>('/api/admin/users', { params })
}

export const updateAdminBinding = (id: number, data: { adminType: number; pondId?: number | null }) => {
  return request.put(`/api/admin/users/${id}/admin-binding`, data)
}

export const updateUserStatus = (id: number, status: number) => {
  return request.put(`/api/admin/users/${id}/status`, { status })
}
