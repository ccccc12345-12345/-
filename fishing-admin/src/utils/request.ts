import axios, { AxiosError, AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { usePondStore } from '@/store/pond'
import router from '@/router'

const request = axios.create({
  baseURL: '',
  timeout: 15000
})

request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const userStore = useUserStore()
    if (userStore.token) {
      config.headers.Authorization = `Bearer ${userStore.token}`
    }
    // 自动携带当前选中鱼塘ID（普通管理员由后端强制绑定，前端统一传参）
    const pondStore = usePondStore()
    const pondId = pondStore.currentPondId
    if (pondId != null) {
      if (config.method?.toLowerCase() === 'get' || config.method?.toLowerCase() === 'delete') {
        config.params = { ...config.params, pondId }
      } else if (config.data && !(config.data instanceof FormData) && typeof config.data === 'object') {
        config.data = { ...config.data, pondId }
      }
    }
    return config
  },
  (error) => Promise.reject(error)
)

request.interceptors.response.use(
  (response: AxiosResponse<Result<any>>) => {
    const res = response.data
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        const userStore = useUserStore()
        userStore.logout()
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    return res as any
  },
  (error: AxiosError) => {
    const message = (error.response?.data as any)?.message || error.message || '网络错误'
    ElMessage.error(message)
    if (error.response?.status === 401) {
      const userStore = useUserStore()
      userStore.logout()
      router.push('/login')
    }
    return Promise.reject(error)
  }
)

interface Result<T> {
  code: number
  message: string
  data: T
}

export default request
