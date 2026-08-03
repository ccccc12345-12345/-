import request from '@/utils/request'

export interface RevenueSummary {
  today: number
  week: number
  month: number
}

export interface RevenueItem {
  slotDate: string
  pondId: number | null
  pondName: string | null
  slotId: number
  slotName: string | null
  totalCount: number
  checkinCount: number
  occupancyRate: number
  totalIncome: number
}

export const getRevenueSummary = () => {
  return request.get<any, { data: RevenueSummary }>('/api/admin/revenue/summary')
}

export const getRevenueList = (params?: { startDate?: string; endDate?: string; pondId?: number }) => {
  return request.get<any, { data: RevenueItem[] }>('/api/admin/revenue/list', { params })
}

export const exportRevenue = (params?: { startDate?: string; endDate?: string; pondId?: number }) => {
  return request.get('/api/admin/revenue/export', {
    params,
    responseType: 'blob'
  })
}
