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

export interface RestaurantOrderItem {
  menuId: number
  quantity: number
}

export interface CreateRestaurantOrderParams {
  items: RestaurantOrderItem[]
  spotId?: number | null
  remark?: string
}

export const getRestaurantMenus = (pondId: number, category?: string) => {
  return request.get<any, { data: RestaurantMenu[] }>(`/api/restaurant/${pondId}/menus`, {
    params: category ? { category } : {}
  })
}

export const createRestaurantOrder = (pondId: number, data: CreateRestaurantOrderParams) => {
  return request.post<any, { data: number }>(`/api/restaurant/${pondId}/orders`, data)
}

export const payRestaurantOrder = (orderId: number) => {
  return request.post<any, { data: unknown }>(`/api/restaurant/orders/${orderId}/pay`)
}
