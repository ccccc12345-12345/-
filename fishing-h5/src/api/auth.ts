import request from '@/utils/request'

export interface LoginParams {
  username: string
  password: string
}

export interface LoginResult {
  token: string
  userId: string
}

export const login = (data: LoginParams) => {
  return request.post<LoginResult>('/api/login', data)
}
