import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useUserStore = defineStore('user', () => {
  const token = ref<string>(localStorage.getItem('fishing_token') || '')
  const userId = ref<string>(localStorage.getItem('fishing_user_id') || '')
  const isAdmin = ref<boolean>(localStorage.getItem('fishing_is_admin') === 'true')

  const setUser = (data: { token: string; userId: string; isAdmin?: boolean }) => {
    token.value = data.token
    userId.value = data.userId
    isAdmin.value = data.isAdmin || false
    localStorage.setItem('fishing_token', data.token)
    localStorage.setItem('fishing_user_id', data.userId)
    localStorage.setItem('fishing_is_admin', String(data.isAdmin || false))
  }

  const logout = () => {
    token.value = ''
    userId.value = ''
    isAdmin.value = false
    localStorage.removeItem('fishing_token')
    localStorage.removeItem('fishing_user_id')
    localStorage.removeItem('fishing_is_admin')
  }

  return { token, userId, isAdmin, setUser, logout }
})
