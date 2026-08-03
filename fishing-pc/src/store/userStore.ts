import { defineStore } from 'pinia'
import { computed, ref } from 'vue'

export const ROLE_USER = 0
export const ROLE_MERCHANT = 1
export const ROLE_ADMIN = 2
export const ROLE_STAFF = 3

export const useUserStore = defineStore('user_pc', () => {
  const token = ref<string>(localStorage.getItem('fishing_pc_token') || '')
  const userId = ref<string>(localStorage.getItem('fishing_pc_user_id') || '')
  const username = ref<string>(localStorage.getItem('fishing_pc_username') || '')
  const role = ref<number>(Number(localStorage.getItem('fishing_pc_role') || String(ROLE_USER)))
  const adminType = ref<number | null>(readNumber('fishing_pc_admin_type'))
  const pondId = ref<string>(localStorage.getItem('fishing_pc_pond_id') || '')
  const staffId = ref<string>(localStorage.getItem('fishing_pc_staff_id') || '')
  const merchantId = ref<string>(localStorage.getItem('fishing_pc_merchant_id') || '')
  const staffRole = ref<string>(localStorage.getItem('fishing_pc_staff_role') || '')

  const isAdmin = computed(() => role.value === ROLE_ADMIN)
  const isMerchant = computed(() => role.value === ROLE_MERCHANT)
  const isStaff = computed(() => role.value === ROLE_STAFF)
  const isLogin = computed(() => !!token.value)

  const setUser = (data: {
    token: string
    userId: string
    username: string
    role: number
    adminType?: number | null
    pondId?: string | number | null
    staffId?: string | number | null
    merchantId?: string | number | null
    staffRole?: string | null
  }) => {
    token.value = data.token
    userId.value = String(data.userId)
    username.value = data.username
    role.value = data.role
    adminType.value = data.adminType ?? null
    pondId.value = data.pondId == null ? '' : String(data.pondId)
    staffId.value = data.staffId == null ? '' : String(data.staffId)
    merchantId.value = data.merchantId == null ? '' : String(data.merchantId)
    staffRole.value = data.staffRole || ''

    localStorage.setItem('fishing_pc_token', token.value)
    localStorage.setItem('fishing_pc_user_id', userId.value)
    localStorage.setItem('fishing_pc_username', username.value)
    localStorage.setItem('fishing_pc_role', String(role.value))
    writeOptional('fishing_pc_admin_type', adminType.value)
    writeOptional('fishing_pc_pond_id', pondId.value)
    writeOptional('fishing_pc_staff_id', staffId.value)
    writeOptional('fishing_pc_merchant_id', merchantId.value)
    writeOptional('fishing_pc_staff_role', staffRole.value)
  }

  const logout = () => {
    token.value = ''
    userId.value = ''
    username.value = ''
    role.value = ROLE_USER
    adminType.value = null
    pondId.value = ''
    staffId.value = ''
    merchantId.value = ''
    staffRole.value = ''
    ;[
      'fishing_pc_token',
      'fishing_pc_user_id',
      'fishing_pc_username',
      'fishing_pc_role',
      'fishing_pc_admin_type',
      'fishing_pc_pond_id',
      'fishing_pc_staff_id',
      'fishing_pc_merchant_id',
      'fishing_pc_staff_role'
    ].forEach((key) => localStorage.removeItem(key))
  }

  return {
    token,
    userId,
    username,
    role,
    adminType,
    pondId,
    staffId,
    merchantId,
    staffRole,
    isAdmin,
    isMerchant,
    isStaff,
    isLogin,
    setUser,
    logout
  }
})

function readNumber(key: string) {
  const value = localStorage.getItem(key)
  if (value == null || value === '') return null
  const number = Number(value)
  return Number.isFinite(number) ? number : null
}

function writeOptional(key: string, value: string | number | null) {
  if (value == null || value === '') {
    localStorage.removeItem(key)
  } else {
    localStorage.setItem(key, String(value))
  }
}
