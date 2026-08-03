import type { LucideIcon } from 'lucide-react'

export type StaffRole = 'owner' | 'manager' | 'operator' | 'finance' | 'checker'
export type ApiStatus = 'idle' | 'loading' | 'success' | 'error'

export interface LoginResult {
  token: string
  userId: string
  role: number
  adminType?: number
  pondId?: number
  staffId?: string
  merchantId?: string
  staffRole?: Exclude<StaffRole, 'owner'>
  staffName?: string
}

export interface AuthUser extends LoginResult {
  username: string
  displayName: string
}

export interface PageResult<T> {
  records: T[]
  total: number
  size?: number
  current?: number
  pages?: number
}

export interface Pond {
  id: number
  name: string
  category?: 'lake' | 'river' | 'pond' | 'sea' | string | null
  address?: string | null
  phone?: string | null
  latitude?: number | null
  longitude?: number | null
  coverImage?: string | null
  floorPlanUrl?: string | null
  bookingNotice?: string | null
  cancelRule?: string | null
  refundRule?: string | null
  merchantId?: number | null
  status: number
  createTime?: string
  updateTime?: string
}

export interface FishingSpot {
  id: number
  spotCode: string
  status: number
  pondId: number
  pondName?: string | null
  coordinateX?: number | null
  coordinateY?: number | null
}

export interface BoardSpot {
  spotId: number
  spotCode: string
  status: string
  spotStatus?: number | null
  reservationId?: number | null
  userNickname?: string | null
  userPhone?: string | null
  reservationStatus?: string | null
  coordinateX?: number | null
  coordinateY?: number | null
}

export interface TimeSlot {
  id: number
  pondId: number
  slotDate: string
  slotName: string
  startTime: string
  endTime: string
  maxBookings: number
  advanceDays: number
  drawStartTime: string
  drawEndTime: string
  status: number
  defaultPrice?: number | null
  remain?: number | null
}

export interface Reservation {
  id: number
  userId: number
  slotId: number
  pondId?: number | null
  pondName?: string | null
  slotDate?: string | null
  slotName?: string | null
  startTime?: string | null
  endTime?: string | null
  status: string
  createTime?: string | null
  cancelTime?: string | null
  cancelReason?: string | null
  actualFee?: number | null
  checkInTime?: string | null
  checkinCode?: string | null
  spotCode?: string | null
  userPhone?: string | null
  userNickname?: string | null
}

export interface DashboardStats {
  todayIncome?: number
  todayReservationCount?: number
  todayCheckinCount?: number
  occupancyRate?: number
  recentReservations?: Reservation[]
}

export interface CheckinResult {
  reservationId: number
  userNickname?: string | null
  slotDate?: string | null
  slotName?: string | null
  spotCode?: string | null
  actualFee?: number | null
  checkInTime?: string | null
  status?: string | null
  pondName?: string | null
}

export interface RevenueSummary {
  today?: number
  week?: number
  month?: number
}

export interface RevenueItem {
  slotDate: string
  pondId?: number | null
  pondName?: string | null
  slotId?: number
  slotName?: string | null
  totalCount?: number
  checkinCount?: number
  occupancyRate?: number
  totalIncome?: number
}

export interface ShopProduct {
  id: number
  pondId?: number | null
  merchantId?: number
  category: 'equipment' | 'bait' | 'fish' | 'food' | string
  name: string
  price: number
  stock: number
  imageUrl?: string | null
  description?: string | null
  status: 'on' | 'off' | string
  createTime?: string
  updateTime?: string
}

export interface RestaurantMenu {
  id: number
  pondId: number
  merchantId?: number
  name: string
  category: 'fresh_fish' | 'cooked' | 'drink' | string
  price: number
  stock: number
  imageUrl?: string | null
  description?: string | null
  isSpecial?: number
  status: 'on' | 'off' | string
}

export interface RestaurantOrder {
  id: number
  orderNo: string
  pondId?: number
  pondName?: string | null
  spotCode?: string | null
  totalAmount: number
  status: string
  remark?: string | null
  userNickname?: string | null
  userPhone?: string | null
  items?: Array<{ menuName?: string; quantity: number; price: number }>
  createTime?: string
}

export interface CatchRecord {
  id: number
  userId: number
  pondId: number
  pondName?: string | null
  spotCode?: string | null
  fishType: string
  weight: number
  quantity: number
  imageUrl?: string | null
  status: string
  recyclePrice?: number | null
  userPhone?: string | null
  userNickname?: string | null
  createTime?: string
}

export interface Staff {
  id: number
  staffName: string
  phone: string
  role: Exclude<StaffRole, 'owner'>
  status: 'normal' | 'disabled'
  createTime?: string
}

export interface Announcement {
  id: number
  pondId?: number | null
  title: string
  content?: string | null
  coverImage?: string | null
  pushHome?: number | boolean
  status: string
  publishTime?: string
}

export interface EventItem {
  id: number
  pondId?: number | null
  title: string
  coverImage?: string | null
  eventTime?: string | null
  signupDeadline?: string | null
  capacity?: number
  location?: string | null
  introduction?: string | null
  auditEnabled?: number | boolean
  cancelRule?: string | null
  formFields?: string | null
  signupCount?: number
  status: string
  recommended?: number | boolean
  pinned?: number | boolean
}

export interface EventSignup {
  id: number
  eventId: number
  userName?: string | null
  phone?: string | null
  formData?: string | null
  auditStatus: string
  auditReason?: string | null
  checkedIn?: number | boolean
  createTime?: string
}

export interface EventReview {
  id: number
  eventId: number
  userName?: string | null
  rating?: number
  content?: string | null
  createTime?: string
}

export interface OperationLog {
  id: number
  operatorName?: string | null
  actionType: string
  targetType?: string | null
  targetId?: number | null
  ip?: string | null
  detail?: string | null
  createTime?: string
}

export interface MerchantRoute {
  path: string
  label: string
  icon: LucideIcon
  roles: StaffRole[]
  element: JSX.Element
  exact?: boolean
  hidden?: boolean
}
