import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePondStore = defineStore('pond_pc', () => {
  const raw = localStorage.getItem('fishing_pc_current_pond')
  const currentPondId = ref<number | undefined>(raw ? Number(raw) : undefined)

  const setCurrentPondId = (id: number | undefined) => {
    currentPondId.value = id
    if (id != null) {
      localStorage.setItem('fishing_pc_current_pond', String(id))
    } else {
      localStorage.removeItem('fishing_pc_current_pond')
    }
  }

  return { currentPondId, setCurrentPondId }
})
