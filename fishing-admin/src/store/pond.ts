import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import request from '@/utils/request'
import { getMerchantPonds } from '@/api/merchant'

export interface Pond {
  id: number
  name: string
  address?: string
  phone?: string
  status: number
}

export const usePondStore = defineStore('pond', () => {
  const currentPondId = ref<number | null>(null)
  const ponds = ref<Pond[]>([])
  const loading = ref(false)

  const currentPond = computed(() => {
    return ponds.value.find(p => p.id === currentPondId.value) || null
  })

  const applyPondSelection = (forcePondId?: number) => {
    if (forcePondId) {
      currentPondId.value = forcePondId
    } else if (currentPondId.value == null && ponds.value.length > 0) {
      currentPondId.value = ponds.value[0].id
    }
    const exist = ponds.value.find(p => p.id === currentPondId.value)
    if (!exist && ponds.value.length > 0) {
      currentPondId.value = ponds.value[0].id
    }
  }

  const loadPonds = async (forcePondId?: number) => {
    loading.value = true
    try {
      const res = await request.get<any, { data: Pond[] }>('/api/admin/ponds')
      ponds.value = res.data || []
      applyPondSelection(forcePondId)
    } finally {
      loading.value = false
    }
  }

  const loadMerchantPonds = async (forcePondId?: number) => {
    loading.value = true
    try {
      const res = await getMerchantPonds()
      ponds.value = res.data || []
      applyPondSelection(forcePondId)
    } finally {
      loading.value = false
    }
  }

  const setCurrentPond = (id: number | null) => {
    currentPondId.value = id
  }

  return { currentPondId, ponds, loading, currentPond, loadPonds, loadMerchantPonds, setCurrentPond }
})
