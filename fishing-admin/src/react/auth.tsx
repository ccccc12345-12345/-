import React, { createContext, useContext, useMemo, useState } from 'react'
import type { AuthUser, LoginResult, StaffRole } from './types'

const TOKEN_KEY = 'fishing_admin_token'
const USER_KEY = 'fishing_admin_user'
const STORAGE_VERSION_KEY = 'fishing_admin_auth_version'
const STORAGE_VERSION = 'react-merchant-console-v2'
const LEGACY_KEYS = [
  'fishing_admin_user_id',
  'fishing_admin_username',
  'fishing_admin_role',
  'fishing_admin_admin_type',
  'fishing_admin_pond_id',
  'fishing_admin_staff_id',
  'fishing_admin_merchant_id',
  'fishing_admin_staff_role'
]
const STAFF_ROLES: StaffRole[] = ['manager', 'operator', 'finance', 'checker']

interface AuthContextValue {
  user: AuthUser | null
  token: string | null
  role: StaffRole | null
  isAuthenticated: boolean
  setLogin: (username: string, result: LoginResult) => void
  logout: () => void
}

const AuthContext = createContext<AuthContextValue | null>(null)

const isStaffRole = (value: unknown): value is Exclude<StaffRole, 'owner'> =>
  STAFF_ROLES.includes(value as StaffRole)

const clearLegacyKeys = () => {
  LEGACY_KEYS.forEach((key) => localStorage.removeItem(key))
}

const writeLegacyKeys = (username: string, user: AuthUser, role: StaffRole | null) => {
  localStorage.setItem('fishing_admin_user_id', String(user.userId || ''))
  localStorage.setItem('fishing_admin_username', username)
  localStorage.setItem('fishing_admin_role', String(user.role))
  if (user.adminType !== undefined && user.adminType !== null) {
    localStorage.setItem('fishing_admin_admin_type', String(user.adminType))
  } else {
    localStorage.removeItem('fishing_admin_admin_type')
  }
  if (user.pondId !== undefined && user.pondId !== null) {
    localStorage.setItem('fishing_admin_pond_id', String(user.pondId))
  } else {
    localStorage.removeItem('fishing_admin_pond_id')
  }
  if (user.staffId) {
    localStorage.setItem('fishing_admin_staff_id', String(user.staffId))
  } else {
    localStorage.removeItem('fishing_admin_staff_id')
  }
  if (user.merchantId) {
    localStorage.setItem('fishing_admin_merchant_id', String(user.merchantId))
  } else {
    localStorage.removeItem('fishing_admin_merchant_id')
  }
  if (role && role !== 'owner') {
    localStorage.setItem('fishing_admin_staff_role', role)
  } else {
    localStorage.removeItem('fishing_admin_staff_role')
  }
}

export const clearAuthStorage = () => {
  localStorage.removeItem(TOKEN_KEY)
  localStorage.removeItem(USER_KEY)
  localStorage.removeItem(STORAGE_VERSION_KEY)
  localStorage.removeItem('fishing_admin_current_pond')
  clearLegacyKeys()
}

const normalizeAuthUser = (username: string, result: LoginResult): AuthUser => {
  const role = resolveRole(result)
  const next: AuthUser = {
    ...result,
    username,
    displayName: result.staffName || (role === 'owner' ? '商家老板' : username)
  }
  if (role === 'owner' || !isStaffRole(result.staffRole)) {
    delete next.staffRole
  }
  return next
}

const clearBridgeHash = () => {
  if (!window.location.hash) return
  const params = new URLSearchParams(window.location.hash.slice(1))
  if (!params.has('merchantAuth')) return
  params.delete('merchantAuth')
  const nextHash = params.toString()
  const nextUrl = `${window.location.pathname}${window.location.search}${nextHash ? `#${nextHash}` : ''}`
  window.history.replaceState(null, document.title, nextUrl)
}

const consumeBridgeAuth = (): AuthUser | null => {
  if (!window.location.hash) return null
  const params = new URLSearchParams(window.location.hash.slice(1))
  const raw = params.get('merchantAuth')
  if (!raw) return null
  try {
    const payload = JSON.parse(decodeURIComponent(raw)) as { username?: string; result?: LoginResult }
    if (!payload?.result?.token) return null
    const role = resolveRole(payload.result)
    if (!role) return null
    const username = payload.username || payload.result.staffName || 'merchant'
    const normalized = normalizeAuthUser(username, payload.result)
    localStorage.setItem(TOKEN_KEY, payload.result.token)
    localStorage.setItem(USER_KEY, JSON.stringify(normalized))
    localStorage.setItem(STORAGE_VERSION_KEY, STORAGE_VERSION)
    writeLegacyKeys(username, normalized, role)
    return normalized
  } catch {
    return null
  } finally {
    clearBridgeHash()
  }
}

const readUser = (): AuthUser | null => {
  const raw = localStorage.getItem(USER_KEY)
  if (!raw) return null
  try {
    const parsed = JSON.parse(raw) as AuthUser
    const role = resolveRole(parsed)
    if (!role) {
      clearAuthStorage()
      return null
    }
    const normalized = normalizeAuthUser(parsed.username || '', parsed)
    if (localStorage.getItem(STORAGE_VERSION_KEY) !== STORAGE_VERSION || JSON.stringify(normalized) !== raw) {
      localStorage.setItem(STORAGE_VERSION_KEY, STORAGE_VERSION)
      localStorage.setItem(USER_KEY, JSON.stringify(normalized))
      writeLegacyKeys(normalized.username || '', normalized, role)
    }
    return normalized
  } catch {
    clearAuthStorage()
    return null
  }
}

export const resolveRole = (result?: Partial<LoginResult> | null): StaffRole | null => {
  if (!result) return null
  if (Number(result.role) === 1) return 'owner'
  if (isStaffRole(result.staffRole)) return result.staffRole
  return null
}

const readInitialAuth = () => {
  const bridgeUser = consumeBridgeAuth()
  const user = bridgeUser || readUser()
  return {
    token: localStorage.getItem(TOKEN_KEY),
    user
  }
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [initialAuth] = useState(() => readInitialAuth())
  const [token, setToken] = useState(initialAuth.token)
  const [user, setUser] = useState<AuthUser | null>(initialAuth.user)

  const value = useMemo<AuthContextValue>(() => {
    const role = resolveRole(user)
    return {
      user,
      token,
      role,
      isAuthenticated: Boolean(token && user),
      setLogin: (username, result) => {
        const role = resolveRole(result)
        const next = normalizeAuthUser(username, result)
        localStorage.setItem(TOKEN_KEY, result.token)
        localStorage.setItem(USER_KEY, JSON.stringify(next))
        localStorage.setItem(STORAGE_VERSION_KEY, STORAGE_VERSION)
        writeLegacyKeys(username, next, role)
        setToken(result.token)
        setUser(next)
      },
      logout: () => {
        clearAuthStorage()
        setToken(null)
        setUser(null)
      }
    }
  }, [token, user])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export const useAuth = () => {
  const ctx = useContext(AuthContext)
  if (!ctx) {
    throw new Error('useAuth must be used within AuthProvider')
  }
  return ctx
}
