import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
  captchaKey: string
  captchaCode: string
}

export interface RegisterParams {
  phone: string
  nickname: string
  password: string
  confirmPassword: string
  role?: 'user' | 'merchant'
  inviteCode?: string
  captchaKey: string
  captchaCode: string
}

export interface CaptchaResult {
  captchaKey: string
  imageBase64: string
}

export interface LoginResult {
  token: string
  userId: string
  role: number
  adminType?: number
  pondId?: number
  staffId?: string
  merchantId?: string
  staffRole?: 'checker' | 'operator' | 'finance' | 'manager'
  staffName?: string
}

export const login = (data: LoginParams) => {
  return request.post<any, { data: LoginResult }>('/api/login', data)
}

export const register = (data: RegisterParams) => {
  return request.post<any, { data: number }>('/api/register', data)
}

export const getCaptcha = () => {
  return request.get<any, { data: CaptchaResult }>('/api/captcha')
}
