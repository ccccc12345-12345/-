import request from '@/utils/request'

export interface RestaurantMenu {
  id: number
  pondId: number
  merchantId: number
  name: string
  category: 'fresh_fish' | 'cooked' | 'drink'
  price: number
  stock: number
  imageUrl: string | null
  description: string | null
  isSpecial: number
  status: 'on' | 'off'
  createTime: string
  updateTime: string
}

export interface RestaurantMenuParams {
  pondId: number
  name: string
  category: 'fresh_fish' | 'cooked' | 'drink'
  price: number
  stock?: number | null
  imageUrl?: string | null
  description?: string | null
  isSpecial?: number
  status?: 'on' | 'off'
}

export interface RestaurantOrderItemVO {
  id: number
  orderId: number
  menuId: number
  menuName: string
  price: number
  quantity: number
  createTime: string
}

export interface RestaurantOrderVO {
  id: number
  orderNo: string
  orderType: string
  userId: number
  pondId: number
  pondName: string | null
  merchantId: number
  spotId: number | null
  spotCode: string | null
  totalAmount: number
  status: string
  remark: string | null
  userNickname: string | null
  userPhone: string | null
  items: RestaurantOrderItemVO[]
  createTime: string
  updateTime: string
}

export const getMerchantRestaurantMenus = (pondId?: number) => {
  return request.get<any, { data: RestaurantMenu[] }>('/api/merchant/restaurant/menus', {
    params: pondId ? { pondId } : {}
  })
}

export const createMerchantRestaurantMenu = (data: RestaurantMenuParams) => {
  return request.post('/api/merchant/restaurant/menus', data)
}

export const updateMerchantRestaurantMenu = (id: number, data: RestaurantMenuParams) => {
  return request.put(`/api/merchant/restaurant/menus/${id}`, data)
}

export const updateMerchantRestaurantMenuStatus = (id: number, status: 'on' | 'off') => {
  return request.put(`/api/merchant/restaurant/menus/${id}/status`, null, { params: { status } })
}

export const deleteMerchantRestaurantMenu = (id: number) => {
  return request.delete(`/api/merchant/restaurant/menus/${id}`)
}

export const getMerchantRestaurantOrders = (params?: { pondId?: number; status?: string }) => {
  return request.get<any, { data: RestaurantOrderVO[] }>('/api/merchant/restaurant/orders', {
    params: params || {}
  })
}

export const updateMerchantRestaurantOrderStatus = (id: number, status: string) => {
  return request.put(`/api/merchant/restaurant/orders/${id}/status`, null, { params: { status } })
}
