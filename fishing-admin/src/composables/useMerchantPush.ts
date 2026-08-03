import { onMounted, onUnmounted } from 'vue'
import { MerchantSseClient } from '@/utils/sse'
import { useUserStore } from '@/store/user'

export interface UseMerchantPushOptions {
  /** 监听的事件类型列表 */
  events: string[]
  /** 收到任意监听事件时的回调 */
  onEvent?: (type: string, data: any) => void
  /** SSE 不可用时的兜底刷新回调 */
  fallback?: () => void
  /** 兜底轮询间隔，默认 30000ms */
  fallbackInterval?: number
}

/**
 * 商家端实时推送组合式函数
 *
 * 页面挂载时自动建立 SSE 连接，卸载时关闭。
 * 收到指定事件后触发 onEvent，连续失败时启用 fallback 轮询。
 */
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
