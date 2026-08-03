import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

// 后端角色定义：0-普通用户 1-商家 2-平台管理员 3-员工
export const ROLE_USER = 0
export const ROLE_MERCHANT = 1
export const ROLE_ADMIN = 2
export const ROLE_STAFF = 3

export type StaffRole = 'checker' | 'operator' | 'finance' | 'manager'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('fishing_admin_token') || '')
  const userId = ref<string>(localStorage.getItem('fishing_admin_user_id') || '')
  const username = ref<string>(localStorage.getItem('fishing_admin_username') || '')
  const role = ref<number | null>(localStorage.getItem('fishing_admin_role') ? Number(localStorage.getItem('fishing_admin_role')) : null)
  const adminType = ref<number | null>(localStorage.getItem('fishing_admin_admin_type') ? Number(localStorage.getItem('fishing_admin_admin_type')) : null)
  const pondId = ref<number | null>(localStorage.getItem('fishing_admin_pond_id') ? Number(localStorage.getItem('fishing_admin_pond_id')) : null)
  const staffId = ref<string>(localStorage.getItem('fishing_admin_staff_id') || '')
  const merchantId = ref<string>(localStorage.getItem('fishing_admin_merchant_id') || '')
  const staffRole = ref<StaffRole | ''>(localStorage.getItem('fishing_admin_staff_role') as StaffRole || '')

  const isUser = computed(() => role.value === ROLE_USER)
  const isMerchant = computed(() => role.value === ROLE_MERCHANT)
  const isAdmin = computed(() => role.value === ROLE_ADMIN)
  const isStaff = computed(() => role.value === ROLE_STAFF)
  const isSuperAdmin = computed(() => isAdmin.value && (adminType.value == null || adminType.value === 0))
  const isNormalAdmin = computed(() => isAdmin.value && adminType.value === 1)

  // 员工权限判断
  const isChecker = computed(() => isStaff.value && staffRole.value === 'checker')
  const isOperator = computed(() => isStaff.value && staffRole.value === 'operator')
  const isFinance = computed(() => isStaff.value && staffRole.value === 'finance')
  const isManager = computed(() => isStaff.value && staffRole.value === 'manager')

  const setUser = (data: {
    token: string
    userId: string
    username: string
    role: number
    adminType?: number
    pondId?: number
    staffId?: string
    merchantId?: string
    staffRole?: StaffRole
  }) => {
    token.value = data.token
    userId.value = data.userId
    username.value = data.username
    role.value = data.role
    adminType.value = data.adminType ?? null
    pondId.value = data.pondId ?? null
    staffId.value = data.staffId ?? ''
    merchantId.value = data.merchantId ?? ''
    staffRole.value = data.staffRole ?? ''
    localStorage.setItem('fishing_admin_token', data.token)
    localStorage.setItem('fishing_admin_user_id', data.userId)
    localStorage.setItem('fishing_admin_username', data.username)
    localStorage.setItem('fishing_admin_role', String(data.role))
    if (data.adminType != null) {
      localStorage.setItem('fishing_admin_admin_type', String(data.adminType))
    } else {
      localStorage.removeItem('fishing_admin_admin_type')
    }
    if (data.pondId != null) {
      localStorage.setItem('fishing_admin_pond_id', String(data.pondId))
    } else {
      localStorage.removeItem('fishing_admin_pond_id')
    }
    if (data.staffId) {
      localStorage.setItem('fishing_admin_staff_id', data.staffId)
    } else {
      localStorage.removeItem('fishing_admin_staff_id')
    }
    if (data.merchantId) {
      localStorage.setItem('fishing_admin_merchant_id', data.merchantId)
    } else {
      localStorage.removeItem('fishing_admin_merchant_id')
    }
    if (data.staffRole) {
      localStorage.setItem('fishing_admin_staff_role', data.staffRole)
    } else {
      localStorage.removeItem('fishing_admin_staff_role')
    }
  }

  const logout = () => {
    token.value = ''
    userId.value = ''
    username.value = ''
    role.value = null
    adminType.value = null
    pondId.value = null
    staffId.value = ''
    merchantId.value = ''
    staffRole.value = ''
    localStorage.removeItem('fishing_admin_token')
    localStorage.removeItem('fishing_admin_user_id')
    localStorage.removeItem('fishing_admin_username')
    localStorage.removeItem('fishing_admin_role')
    localStorage.removeItem('fishing_admin_admin_type')
    localStorage.removeItem('fishing_admin_pond_id')
    localStorage.removeItem('fishing_admin_staff_id')
    localStorage.removeItem('fishing_admin_merchant_id')
    localStorage.removeItem('fishing_admin_staff_role')
  }

  return {
    token, userId, username, role, adminType, pondId, staffId, merchantId, staffRole,
    isUser, isMerchant, isAdmin, isStaff, isSuperAdmin, isNormalAdmin,
    isChecker, isOperator, isFinance, isManager,
    setUser, logout
  }
})
