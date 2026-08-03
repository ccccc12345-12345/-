import request from '@/utils/request'

export type RestaurantCategory = 'fresh_fish' | 'cooked' | 'drink'

export interface CookingMethod {
  name: string
  price: number
}

export interface RestaurantMenu {
  id: number
  pondId: number
  merchantId: number
  name: string
  category: RestaurantCategory
  price: number
  stock: number | null
  imageUrl: string | null
  description: string | null
  cookingMethods: CookingMethod[] | null
  isSpecial: number | null
  status: 'on' | 'off'
}

export interface RestaurantOrderItem {
  id: number
  orderId: number
  menuId: number
  menuName: string
  price: number
  cookingMethod: string | null
  quantity: number
  createTime: string
}

export interface RestaurantOrder {
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
  status: 'pending' | 'accepted' | 'cooking' | 'delivered' | 'completed' | 'cancelled'
  remark: string | null
  items: RestaurantOrderItem[]
  createTime: string
  updateTime: string
}

export interface CreateRestaurantOrderParams {
  spotId?: number | null
  reservationId?: number | null
  remark?: string
  items: Array<{ menuId: number; quantity: number; cookingMethod?: string | null }>
}

export const restaurantCategoryLabels: Record<RestaurantCategory | 'all', string> = {
  all: '全部',
  fresh_fish: '鲜鱼',
  cooked: '加工菜品',
  drink: '饮品'
}

export const restaurantStatusLabels: Record<string, string> = {
  pending: '待处理',
  accepted: '已接单',
  cooking: '制作中',
  delivered: '已送达',
  completed: '已完成',
  cancelled: '已取消'
}

export const getRestaurantMenus = (pondId: number, category?: string) => {
  return request.get<any, { data: RestaurantMenu[] }>(`/api/restaurant/${pondId}/menus`, { params: { category } })
}

export const createRestaurantOrder = (pondId: number, data: CreateRestaurantOrderParams) => {
  return request.post<any, { data: number }>(`/api/restaurant/${pondId}/orders`, data)
}

export const payRestaurantOrder = (id: number) => {
  return request.post(`/api/restaurant/orders/${id}/pay`)
}

export const getMyRestaurantOrders = () => {
  return request.get<any, { data: RestaurantOrder[] }>('/api/restaurant/orders/my')
}
