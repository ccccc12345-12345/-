import request from '@/utils/request'

export interface Pond {
  id: number
  name: string
  address: string | null
  phone: string | null
  status: number
}

export const getPonds = () => {
  return request.get<any, { data: Pond[] }>('/api/admin/ponds')
}

export const getPondDetail = (id: number) => {
  return request.get<any, { data: Pond }>(`/api/admin/ponds/${id}`)
}

export const createPond = (data: Omit<Pond, 'id'>) => {
  return request.post('/api/admin/ponds', data)
}

export const updatePond = (id: number, data: Omit<Pond, 'id'>) => {
  return request.put(`/api/admin/ponds/${id}`, data)
}

export const deletePond = (id: number) => {
  return request.delete(`/api/admin/ponds/${id}`)
}
