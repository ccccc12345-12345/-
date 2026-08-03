import request from '@/utils/request'

export interface Pond {
  id: number
  merchantId?: number
  name: string
  category?: string | null
  address?: string | null
  phone?: string | null
  coverImage?: string | null
  floorPlanUrl?: string | null
  bookingNotice?: string | null
  cancelRule?: string | null
  refundRule?: string | null
  status?: number
}

export const getPonds = () => {
  return request.get<any, { data: Pond[] }>('/api/ponds')
}
