import request from '@/utils/request'

export type ProductCategory = 'equipment' | 'bait' | 'fish' | 'food'

export interface ShopProduct {
  id: number
  pondId: number | null
  merchantId: number
  category: ProductCategory
  name: string
  price: number
  stock: number
  imageUrl: string | null
  description: string | null
  status: 'on' | 'off'
  createTime: string
  updateTime: string
}

export interface ShopOrderItem {
  id: number
  productId: number
  productName: string | null
  productImageUrl: string | null
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface ShopOrder {
  id: number
  orderNo: string
  userId: number
  merchantId: number
  pondId: number | null
  pondName?: string | null
  orderType: string
  totalAmount: number
  status: 'pending_pay' | 'paid' | 'completed' | 'cancelled'
  userPhone?: string | null
  userNickname?: string | null
  createTime: string
  paidAt: string | null
  items: ShopOrderItem[]
}

export interface CreateOrderItem {
  productId: number
  quantity: number
}

export interface CreateOrderParams {
  pondId?: number | null
  items: CreateOrderItem[]
}

export const categoryLabels: Record<ProductCategory | 'all', string> = {
  all: '全部',
  equipment: '钓具',
  bait: '饵料',
  fish: '鱼获',
  food: '餐饮'
}

export const statusLabels: Record<ShopOrder['status'], string> = {
  pending_pay: '待支付',
  paid: '已支付',
  completed: '已完成',
  cancelled: '已取消'
}

export const getShopProducts = (params?: {
  pondId?: number
  category?: string
  keyword?: string
  pageNum?: number
  pageSize?: number
}) => {
  return request.get<any, { data: { records: ShopProduct[]; total: number } }>('/api/shop/products', { params })
}

export const getShopProduct = (id: number) => {
  return request.get<any, { data: ShopProduct }>(`/api/shop/products/${id}`)
}

export const createShopOrder = (data: CreateOrderParams) => {
  return request.post<any, { data: ShopOrder }>('/api/shop/orders', data)
}

export const payShopOrder = (id: number) => {
  return request.post(`/api/shop/orders/${id}/pay`)
}

export const getMyShopOrders = (params?: { pageNum?: number; pageSize?: number }) => {
  return request.get<any, { data: { records: ShopOrder[]; total: number } }>('/api/shop/orders/my', { params })
}

export const getMerchantShopProducts = (params?: {
  keyword?: string
  category?: string
  status?: string
  pageNum?: number
  pageSize?: number
}) => {
  return request.get<any, { data: { records: ShopProduct[]; total: number } }>('/api/merchant/shop/products', { params })
}

export const createMerchantShopProduct = (data: Partial<ShopProduct>) => {
  return request.post('/api/merchant/shop/products', data)
}

export const updateMerchantShopProduct = (id: number, data: Partial<ShopProduct>) => {
  return request.put(`/api/merchant/shop/products/${id}`, data)
}

export const updateMerchantShopProductStatus = (id: number, status: 'on' | 'off') => {
  return request.put(`/api/merchant/shop/products/${id}/status`, null, { params: { status } })
}

export const deleteMerchantShopProduct = (id: number) => {
  return request.delete(`/api/merchant/shop/products/${id}`)
}

// ===== Merchant Shop Order API =====
export const getMerchantShopOrders = (params?: {
  status?: string
  pageNum?: number
  pageSize?: number
}) => {
  return request.get<any, { data: { records: ShopOrder[]; total: number } }>('/api/merchant/shop/orders', { params })
}

export const updateMerchantShopOrderStatus = (id: number, status: string) => {
  return request.put(`/api/merchant/shop/orders/${id}/status`, null, { params: { status } })
}
