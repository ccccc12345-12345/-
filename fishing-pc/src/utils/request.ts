import axios, { AxiosError, AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/userStore'
import router from '@/router'

declare module 'axios' {
  export interface AxiosRequestConfig {
    silent?: boolean
  }
  export interface InternalAxiosRequestConfig {
    silent?: boolean
  }
}

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

export interface Result<T = any> {
  code: number
  message: string
  data: T
}

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse<Result>) => {
    const res = response.data
    if (res.code !== 200) {
      const message = res.message || '请求失败'
      if (!response.config.silent) {
        ElMessage.error(message)
      }
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
      return Promise.reject(new Error(message))
    }
    return res as any
  },
  (error: AxiosError) => {
    const message = (error.response?.data as any)?.message || error.message || '网络错误，请稍后重试'
    if (!error.config?.silent) {
      ElMessage.error(message)
    }
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

export default request
