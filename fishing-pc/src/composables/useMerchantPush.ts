import { onMounted, onUnmounted } from 'vue'
import { MerchantSseClient } from '@/utils/sse'
import { useUserStore } from '@/store/userStore'

export interface UseMerchantPushOptions {
  events: string[]
  onEvent?: (type: string, data: any) => void
  fallback?: () => void
  fallbackInterval?: number
}

export function useMerchantPush(options: UseMerchantPushOptions) {
  let client: MerchantSseClient | null = null

  onMounted(() => {
    const userStore = useUserStore()
    if (!userStore.token) {
      return
    }

    client = new MerchantSseClient({ token: userStore.token })

    options.events.forEach((eventType) => {
      client!.on(eventType, (data) => {
        options.onEvent?.(eventType, data)
      })
    })

    client.connect()

    if (options.fallback) {
      client.enableFallback(options.fallbackInterval ?? 30000, options.fallback)
    }
  })

  onUnmounted(() => {
    client?.disableFallback()
    client?.close()
    client = null
  })
}
