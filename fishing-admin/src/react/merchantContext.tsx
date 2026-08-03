import { createContext, useContext } from 'react'
import type { Pond } from './types'

export interface MerchantContextValue {
  ponds: Pond[]
  currentPondId?: number
  currentPond?: Pond
  setCurrentPondId: (id: number | undefined) => void
  reloadPonds: () => Promise<void>
}

export const MerchantContext = createContext<MerchantContextValue | null>(null)

export const useMerchant = () => {
  const ctx = useContext(MerchantContext)
  if (!ctx) {
    throw new Error('useMerchant must be used within MerchantContext')
  }
  return ctx
}
